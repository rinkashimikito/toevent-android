# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.immedio.toevent.**$$serializer { *; }
-keepclassmembers class com.immedio.toevent.** { *** Companion; }
-keepclasseswithmembers class com.immedio.toevent.** { kotlinx.serialization.KSerializer serializer(...); }

# MSAL
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.device.display.** { *; }

# Glance
-keep class androidx.glance.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Event and model classes for serialization
-keep class com.immedio.toevent.domain.model.** { *; }
-keep class com.immedio.toevent.data.calendar.Google** { *; }
-keep class com.immedio.toevent.data.calendar.Microsoft** { *; }
-keep class com.immedio.toevent.data.cache.** { *; }
