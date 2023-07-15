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
        classpath(libs.atomicfu.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.google.devtools.ksp) apply false
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
