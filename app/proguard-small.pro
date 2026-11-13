# Extra rules for the "small" test build only (see app/build.gradle.kts).
# Shrink, but keep class/member names: crash reports stay readable and
# anything looked up by name (enum names saved in settings, Firestore fields,
# WorkManager/Glance receivers) behaves exactly as in the debug build.
-dontobfuscate
