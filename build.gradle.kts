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
        classpath(libs.navigation.safe.args.gradle.plugin)
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
    alias(libs.plugins.kotlinParcelize) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.nativeCocoapods) apply false
    id("org.barfuin.gradle.taskinfo") version "2.2.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven ("https://jitpack.io")
        maven("https://mvn.breez.technology/releases")
        maven("https://zendesk.jfrog.io/zendesk/repo")
    }
}
