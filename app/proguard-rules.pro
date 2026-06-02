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
