# --- Strong Obfuscation Configuration ---

# Remove all debug logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Aggressive obfuscation
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Retrofit rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Gson rules
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes *Annotation*
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Model classes (Crucial for JSON parsing)
-keep class com.nmmart.retailos.models.** { *; }
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Supabase and Data classes
-keep class com.nmmart.retailos.data.** { *; }

# Glide rules
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Lottie rules
-keep class com.airbnb.lottie.** { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }

# AndroidX
-keep class androidx.appcompat.widget.** { *; }
-keep class androidx.recyclerview.widget.** { *; }

# ZXing Barcode Scanner
-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Stetho (only for debug, so keep in debug but remove in release - but let's just keep rules for safety)
-dontwarn com.facebook.stetho.**
