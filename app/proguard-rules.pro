# Keep annotations
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep crash info
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Room entities
-keep class ru.company.izhs_planner.data.local.entity.** { *; }

# Domain models (serialized with Gson)
-keep class ru.company.izhs_planner.domain.model.** { *; }

# Keep Room annotated methods
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep JNI native methods
-keepclassmembers class ru.company.izhs_planner.ai.LLMInference {
    native <methods>;
    private static native <methods>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Yandex Mobile Ads
-keep class com.yandex.mobile.ads.** { *; }
-dontwarn com.yandex.mobile.ads.**

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}