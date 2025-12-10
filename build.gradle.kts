// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.kotlin.serialization)
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.nativeCocoapods) apply false
    alias(libs.plugins.compose.hotReload) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://mvn.breez.technology/releases")
        maven("https://zendesk.jfrog.io/zendesk/repo")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://central.sonatype.com/repository/maven-snapshots/") // LWK Snapshots
    }
}
