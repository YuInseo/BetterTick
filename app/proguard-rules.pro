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
