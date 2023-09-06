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


#  --- Kotlin Serialization  -------------------------------------------------------------------- */
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Change here com.yourcompany.yourpackage
-keep,includedescriptorclasses class com.blockstream.**$$serializer { *; }
-keepclassmembers class com.blockstream.** {
    *** Companion;
}
-keepclasseswithmembers class com.blockstream.** {
    kotlinx.serialization.KSerializer serializer(...);
}
#  ---------------------------------------------------------------------------------------------- */

-mergeinterfacesaggressively

-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep class androidx.core.app.CoreComponentFactory { *; }

-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.ClassValue
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn org.slf4j.**
-dontwarn sun.nio.**
-dontwarn sun.misc.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

-keepnames class ** {*;}
-keepattributes SourceFile,LineNumberTable

-keepattributes InnerClasses,EnclosingMethod

# Proguard configuration for Jackson 2.x (fasterxml package instead of codehaus package)
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

-assumenosideeffects class android.util.Log {
    public static *** d(...);
}

# Countly
-keep class org.openudid.** { *; }
-keep class ly.count.android.sdk.** { *; }

# Until this is fixed https://youtrack.jetbrains.com/issue/KTOR-5528/Missing-class-warning-when-using-R8-with-ktor-client-in-android-application
# https://github.com/square/okhttp/commit/9da841c24c3b3dabc1d9230ab2f1e71105768771
# https://stackoverflow.com/questions/76042330/android-gradle-plugin-8-0-0-with-kotlin-1-8-20-causes-okhttp3-r8-minify-problem
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.impl.StaticLoggerBinder


# Temp until KMP is fixed
# for JNA
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
-keep class technology.breez.* { *; }
-keep class breez_sdk.** { *; }
-keepclassmembers class * extends technology.breez.* { public *; }
-keepclassmembers class * extends breez_sdk.** { public *; }