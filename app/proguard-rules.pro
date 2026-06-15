# Firebase
-keepattributes Signature
-keepattributes *Annotation*

# Firestore model classes
-keep class com.bettertick.data.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }

# Credential Manager (Google Sign-In)
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Kakao Maps SDK — release 빌드(isMinifyEnabled=true)에서 난독화로 지도가
# 통째로 렌더링되지 않던 문제 방지. SDK가 내부에서 리플렉션/JNI로 클래스를
# 참조하므로 keep 필요.
-keep class com.kakao.vectormap.** { *; }
-keep interface com.kakao.vectormap.** { *; }
-keepclassmembers class com.kakao.vectormap.** { *; }
