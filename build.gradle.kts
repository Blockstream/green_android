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
        classpath(libs.hilt.android.gradle.plugin)
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.google.dagger.hilt.android) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}
true // Needed to make the Suppress annotation work for the plugins block

allprojects {
    repositories {
        google()
        mavenCentral()
        maven ("https://jitpack.io")
        maven("https://mvn.breez.technology/releases")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}