# Peak AI ProGuard rules

# Keep Health Connect models
-keep class androidx.health.connect.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Room entities
-keep class com.peakai.fitness.data.local.** { *; }

# Keep domain models (used via reflection in some places)
-keep class com.peakai.fitness.domain.model.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# General Android
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
