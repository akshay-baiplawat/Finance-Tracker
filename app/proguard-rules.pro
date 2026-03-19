# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ============================================================================
# Room Database - Keep entity classes and their fields
# ============================================================================
-keep class com.example.financetracker.data.local.entity.** { *; }

# Keep Room-generated code
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ============================================================================
# Kotlin - Keep metadata for reflection
# ============================================================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Keep Kotlin data classes used with Room
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================================================
# Coroutines
# ============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ============================================================================
# Hilt - Keep generated injector classes
# ============================================================================
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }

# Keep Hilt entry point injectors
-keep class **_GeneratedInjector { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltComponents$* { *; }
-keep class Hilt_* { *; }

# Keep classes annotated with Hilt annotations
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *

# Keep Hilt worker factory
-keep class androidx.hilt.work.HiltWorkerFactory { *; }
-keep class * extends androidx.hilt.work.HiltWorker { *; }

# ============================================================================
# Compose - Keep compose stability annotations
# ============================================================================
-keep class androidx.compose.runtime.** { *; }

# ============================================================================
# DataStore
# ============================================================================
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ============================================================================
# Enums - Keep enum values for Room type converters
# ============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}