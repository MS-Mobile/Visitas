# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep JavaScript interface classes for WebView
-keepclassmembers class com.msmobile.visitas.visit.VisitsMapJavascriptInterface {
   public *;
}

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
-dontwarn org.slf4j.*
# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
# Navigation 3 saves the back stack by key class and looks the serializer up reflectively when it
# restores it, so keep the keys and their generated serializers.
-keep class com.msmobile.visitas.navigation.AppDestination { *; }
-keep class com.msmobile.visitas.navigation.AppDestination$* { *; }
