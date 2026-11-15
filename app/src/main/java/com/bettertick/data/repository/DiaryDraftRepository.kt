package com.bettertick.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryDraftRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("diary_drafts", Context.MODE_PRIVATE)
    }

    fun saveDraft(dateStr: String, content: String, mood: Int = 0) {
        prefs.edit().putString(dateStr, content).putInt("$dateStr:mood", mood).apply()
    }

    fun getDraft(dateStr: String): String? = prefs.getString(dateStr, null)

    fun getDraftMood(dateStr: String): Int = prefs.getInt("$dateStr:mood", 0)

    fun deleteDraft(dateStr: String) {
        prefs.edit().remove(dateStr).remove("$dateStr:mood").apply()
    }
}
