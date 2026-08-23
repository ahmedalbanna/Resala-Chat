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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Phase 0 hardening: keep Room entities and Security Crypto
-keep class com.example.data.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Phase 1: keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Phase 2: keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Phase 3: keep Signal + WebRTC + Camera
-keep class org.whispersystems.libsignal.** { *; }
-keep class org.signal.** { *; }
-keep class org.webrtc.** { *; }
-dontwarn org.whispersystems.**
-dontwarn org.webrtc.**
-keep class androidx.camera.** { *; }
