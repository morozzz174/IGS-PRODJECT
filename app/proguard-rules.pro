-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class ru.company.izhs_planner.data.local.entity.** { *; }
-keep class ru.company.izhs_planner.domain.model.** { *; }

-keepclassmembers class * {
    @androidx.room.* <fields>;
}

-dontwarn org.**
-keep class org.** { *; }
-dontwarn com.google.**
-keep class com.google.** { *; }
-dontwarn io.**
-keep class io.** { *; }