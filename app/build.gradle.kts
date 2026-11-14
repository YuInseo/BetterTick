import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

android {
    namespace = "com.bettertick"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bettertick"
        minSdk = 28
        targetSdk = 34
        // CI는 CI_VERSION_CODE/CI_VERSION_NAME을 주입해 commit count 기반으로
        // versionCode를 단조 증가시킴 → AppUpdater가 새 버전을 정확히 감지.
        // 로컬 빌드는 fallback 값 사용.
        versionCode = System.getenv("CI_VERSION_CODE")?.toIntOrNull() ?: 3
        versionName = System.getenv("CI_VERSION_NAME") ?: "0.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // debug: repo에 박힌 고정 keystore — 로컬/CI 모두 같은 SHA-1로 서명
        // 그래야 Firebase Google 로그인 SHA-1 등록이 한 번에 영구 적용됨.
        getByName("debug") {
            storeFile    = file("debug.keystore")
            storePassword = "android"
            keyAlias     = "androiddebugkey"
            keyPassword  = "android"
        }
        create("release") {
            if (keystorePropertiesFile.exists()) {
                // 로컬 개발: keystore.properties 읽기
                storeFile    = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias     = keystoreProperties["keyAlias"] as String
                keyPassword  = keystoreProperties["keyPassword"] as String
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
        // 테스트용 작은 APK (BetterTick-small.apk): debug와 같은 서명·패키지라
        // 그 위에 바로 설치된다. R8로 쓰지 않는 코드·리소스를 빼서 크기를 줄인다.
        // 기기에서 이상 없음이 확인되기 전까지는 자동 업데이트에 쓰지 않는다.
        create("small") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-small.pro"
            )
        }
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
}

dependencies {
    testImplementation("junit:junit:4.13.2")
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
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

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

    // ML Kit Document Scanner (Play Services) — powers 첨부 → 문서 스캔
    // with auto-capture + edge detection UI, matching the TickTick UX.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
}
