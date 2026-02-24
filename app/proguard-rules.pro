# Arena AI ProGuard Rules
# Keep the main activity
-keep class com.arenaproject.ai.app.MainActivity { *; }

# Keep EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Suppress warnings for security-crypto
-dontwarn com.google.crypto.tink.**
