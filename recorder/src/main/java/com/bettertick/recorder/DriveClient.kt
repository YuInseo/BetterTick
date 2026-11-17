package com.bettertick.recorder

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 구글 드라이브 REST API를 직접 호출한다(추가 라이브러리 없이).
 * 권한은 drive.file: 이 앱이 만든 파일·폴더에만 접근한다.
 */
object DriveClient {
    const val FOLDER_NAME = "Daydrift 녹음"
    private const val SCOPE = "https://www.googleapis.com/auth/drive.file"

    class NeedsSignInException : Exception("드라이브에 다시 연결해야 해요")
    class HttpException(val code: Int, body: String) : IOException("HTTP $code: ${body.take(300)}")

    fun authRequest(account: String?): AuthorizationRequest {
        val b = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE)))
        if (account != null) b.setAccount(Account(account, "com.google"))
        return b.build()
    }

    /** 화면에서 연결할 때. 사용자 동의가 필요하면 hasResolution()이 true. */
    suspend fun authorize(context: Context, account: String?): AuthorizationResult =
        Identity.getAuthorizationClient(context).authorize(authRequest(account)).await()

    /** 백그라운드에서 토큰 얻기. 동의 창이 필요하면 [NeedsSignInException]. */
    suspend fun token(context: Context): String {
        val account = Settings.get(context).driveAccount ?: throw NeedsSignInException()
        val result = try {
            authorize(context, account)
        } catch (e: Exception) {
            throw IOException("권한 확인 실패: ${e.message}", e)
        }
        if (result.hasResolution()) throw NeedsSignInException()
        return result.accessToken ?: throw NeedsSignInException()
    }

    suspend fun clearToken(context: Context, token: String) = withContext(Dispatchers.IO) {
        runCatching { GoogleAuthUtil.clearToken(context, token) }
    }

    suspend fun accountEmail(token: String): String? = withContext(Dispatchers.IO) {
        val json = request("GET", "https://www.googleapis.com/drive/v3/about?fields=user(emailAddress)", token)
        JSONObject(json).optJSONObject("user")?.optString("emailAddress")?.takeIf { it.isNotBlank() }
    }

    /** 백업 폴더 id. 저장된 게 없거나 지워졌으면 찾거나 새로 만든다. */
    suspend fun folderId(context: Context, token: String): String = withContext(Dispatchers.IO) {
        val settings = Settings.get(context)
        settings.driveFolderId?.let { id ->
            val ok = runCatching {
                val j = JSONObject(request("GET", "https://www.googleapis.com/drive/v3/files/$id?fields=id,trashed", token))
                !j.optBoolean("trashed", false)
            }.getOrDefault(false)
            if (ok) return@withContext id
        }
        val q = "name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val found = JSONObject(
            request(
                "GET",
                "https://www.googleapis.com/drive/v3/files?q=${enc(q)}&fields=files(id)&spaces=drive",
                token,
            )
        ).optJSONArray("files")
        val id = if (found != null && found.length() > 0) {
            found.getJSONObject(0).getString("id")
        } else {
            val body = JSONObject()
                .put("name", FOLDER_NAME)
                .put("mimeType", "application/vnd.google-apps.folder")
                .toString()
            JSONObject(request("POST", "https://www.googleapis.com/drive/v3/files?fields=id", token, body))
                .getString("id")
        }
        settings.driveFolderId = id
        id
    }

    /** 이어받기(resumable) 업로드. 올라간 파일 id를 돌려준다. */
    suspend fun upload(file: File, folderId: String, token: String): String = withContext(Dispatchers.IO) {
        val length = file.length()
        val meta = JSONObject()
            .put("name", file.name)
            .put("mimeType", "audio/mp4")
            .put("parents", org.json.JSONArray().put(folderId))
            .toString()

        val start = open("POST", "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id", token)
        start.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        start.setRequestProperty("X-Upload-Content-Type", "audio/mp4")
        start.setRequestProperty("X-Upload-Content-Length", length.toString())
        start.doOutput = true
        start.outputStream.use { it.write(meta.toByteArray()) }
        val code = start.responseCode
        if (code !in 200..299) throw HttpException(code, readError(start))
        val session = start.getHeaderField("Location") ?: throw IOException("업로드 주소를 받지 못했어요")
        start.disconnect()

        val put = (URL(session).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 30_000
            readTimeout = 120_000
            doOutput = true
            setFixedLengthStreamingMode(length)
            setRequestProperty("Content-Type", "audio/mp4")
        }
        file.inputStream().use { input -> put.outputStream.use { input.copyTo(it, 64 * 1024) } }
        val putCode = put.responseCode
        if (putCode !in 200..299) throw HttpException(putCode, readError(put))
        val id = JSONObject(put.inputStream.bufferedReader().use { it.readText() }).getString("id")
        put.disconnect()
        id
    }

    // ─── HTTP ───

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun open(method: String, url: String, token: String) =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun request(method: String, url: String, token: String, jsonBody: String? = null): String {
        val c = open(method, url, token)
        try {
            if (jsonBody != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                c.outputStream.use { it.write(jsonBody.toByteArray()) }
            }
            val code = c.responseCode
            if (code !in 200..299) throw HttpException(code, readError(c))
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun readError(c: HttpURLConnection): String =
        runCatching { c.errorStream?.bufferedReader()?.use { it.readText() } ?: "" }.getOrDefault("")
}
