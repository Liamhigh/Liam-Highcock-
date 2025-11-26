# Add project specific ProGuard rules here
-keep class com.veruomnis.forensic.** { *; }
-keepclassmembers class com.veruomnis.forensic.** { *; }

# Keep PDF library classes
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Keep metadata extractor
-keep class com.drew.** { *; }
-dontwarn com.drew.**

# Keep FFmpeg
-keep class com.arthenica.** { *; }
-dontwarn com.arthenica.**
