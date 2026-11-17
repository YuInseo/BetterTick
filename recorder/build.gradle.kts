plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Daydrift와 별개로 설치되는 백그라운드 녹음 앱.
// 서명은 Daydrift와 같은 debug.keystore를 써서 SHA-1이 같다(구글 OAuth 클라이언트 등록용).
android {
    namespace = "com.bettertick.recorder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bettertick.recorder"
        minSdk = 28
        targetSdk = 34
        versionCode = System.getenv("CI_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("CI_VERSION_NAME") ?: "0.1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)
    implementation(libs.work.runtime)
    implementation(libs.play.services.auth)
}
