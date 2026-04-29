import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 로컬: keystore.properties 파일 사용
// CI:    GitHub Secrets (STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD, KEYSTORE_BASE64)
private val keystorePropertiesFile = rootProject.file("keystore.properties")
private val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

// 버전은 git 커밋 카운트로 자동 산출. CI 에서 -PappVersionCode 등으로 덮어쓸
// 수도 있게 property/env 우선. 로컬 빌드도 git 만 있으면 동작.
fun gitCommitCount(): Int = try {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        standardOutput = out
        isIgnoreExitValue = true
    }
    out.toString().trim().toIntOrNull() ?: 1
} catch (_: Exception) { 1 }

fun gitShortSha(): String = try {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = out
        isIgnoreExitValue = true
    }
    out.toString().trim().ifEmpty { "dev" }
} catch (_: Exception) { "dev" }

private val versionCodeValue: Int =
    (project.findProperty("appVersionCode") as? String)?.toIntOrNull()
        ?: System.getenv("APP_VERSION_CODE")?.toIntOrNull()
        ?: gitCommitCount()
private val versionNameValue: String =
    (project.findProperty("appVersionName") as? String)
        ?: System.getenv("APP_VERSION_NAME")
        ?: "1.0.$versionCodeValue"
private val gitSha: String =
    (project.findProperty("appGitSha") as? String)
        ?: System.getenv("APP_GIT_SHA")
        ?: gitShortSha()

android {
    namespace = "com.bettertick"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bettertick"
        minSdk = 28
        targetSdk = 34
        versionCode = versionCodeValue
        versionName = versionNameValue

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // UpdateChecker 가 BuildConfig.VERSION_MANIFEST_URL 을 폴링한다.
        // 고정 태그 latest-debug 에 매번 동일 파일을 덮어써 게시.
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")
        buildConfigField(
            "String",
            "VERSION_MANIFEST_URL",
            "\"https://github.com/yuinseo/bettertick/releases/download/latest-debug/version.json\""
        )
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                // 로컬 개발: keystore.properties 읽기
                storeFile    = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias     = keystoreProperties.getProperty("keyAlias")
                keyPassword  = keystoreProperties.getProperty("keyPassword")
            } else {
                // CI: 환경변수에서 읽기 (release.yml에서 주입)
                storeFile    = file("bettertick.keystore")
                storePassword = System.getenv("STORE_PASSWORD") ?: ""
                keyAlias     = System.getenv("KEY_ALIAS") ?: ""
                keyPassword  = System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Lint은 보고용으로만 사용 — 산출물은 CI 아티팩트로 업로드되지만 발견된
    // 이슈가 빌드를 막지는 않는다. 작은 프로젝트에서 빨간 빌드를 유발하기
    // 보다 리포트를 보고 점진 정리하는 쪽을 택했다. textReport 를 켜면 CI
    // 로그에서 곧바로 실패 원인을 보고 작업 우선순위를 정할 수 있다.
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false
        textReport = true
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // AndroidX
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.core.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Sign-In (Credential Manager)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Image Loading (Coil)
    implementation(libs.coil.compose)

    // Glance (App Widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // WorkManager
    implementation(libs.work.runtime)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Auto-update (UpdateChecker 가 version.json 을 파싱)
    implementation(libs.kotlinx.serialization.json)

    // ML Kit Document Scanner (Play Services) — powers 첨부 → 문서 스캔
    // with auto-capture + edge detection UI, matching the TickTick UX.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
}
