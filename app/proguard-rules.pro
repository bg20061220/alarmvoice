# Proguard rules for VoiceSnooze
# Used for code obfuscation in release builds

# Keep Android system classes (required for reflection)
-keep class android.** { *; }
-keep class androidx.** { *; }

# Keep our classes (optional in debug, required in release if using reflection)
-keep class com.voicesnooze.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep reflection-based classes (SpeechRecognizer uses reflection)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin metadata preservation (required for Kotlin)
-keepattributes *Annotation*, InnerClasses, SourceFile, LineNumberTable

# Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
