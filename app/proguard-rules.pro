-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

-keepattributes Signature
-keepattributes *Annotation*

-keep class com.nmmart.retailos.models.** { *; }
-keep class com.nmmart.retailos.data.SupabaseAuthConfig$** { *; }
-keep class com.nmmart.retailos.data.SupabaseFunctionsConfig$** { *; }

-keep class retrofit2.** { *; }
