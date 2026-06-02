# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\bin\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn okhttp3.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.**
-dontwarn com.android.volley.toolbox.**


-keep class androidx.** { *; } 
-keep class com.google.android.gms.** { *; }

-keep class com.f_scratch.bdash.mobile.analytics.** { *; }


-printconfiguration build/full-r8-config.txt
-printusage build/usage.txt
-printmapping build/mapping.txt
