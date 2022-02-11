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

# BEGIN for protobuf in trezor:
-keep class com.satoshilabs.trezor.** { *; }
-keepattributes InnerClasses,EnclosingMethod
# END for protobuf in trezor

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

-keep class mehdi.sakout.aboutpage.** {*;}
-keep class com.blockstream.libwally.** {*;}
-keep class com.blockstream.libgreenaddress.** {*;}
-keep class com.greenaddress.greenapi.data.** {*;}
-keep class com.greenaddress.jade.entities** {*;}


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