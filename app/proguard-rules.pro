# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore — Datenmodell-Klassen nicht entfernen
-keep class com.bestfriends.beachbingo.**.model.** { *; }
-keep class com.bestfriends.beachbingo.**.data.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembernames class * { @dagger.hilt.* <methods>; }

# ZXing (QR-Code Scanner)
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# Compose — kein Stripping von Lambdas
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
