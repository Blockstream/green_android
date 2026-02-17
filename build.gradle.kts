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

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
    resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.MINUTES)
}

task("printLwkVersion") {
    doLast {
        val lwkVersion = libs.versions.lwk.get()
        println("LWK declared version: $lwkVersion")

        if (lwkVersion.endsWith("-SNAPSHOT")) {
            // Find the most recently cached maven-metadata.xml for lwk-jvm to get the actual timestamp
            val resourcesDir = file("${gradle.gradleUserHomeDir}/caches/modules-2/resources-2.1")
            val latest = resourcesDir.walkTopDown()
                .filter { it.name == "maven-metadata.xml" }
                .filter {
                    val text = it.readText()
                    text.contains("<artifactId>lwk-jvm</artifactId>") && text.contains("com.blockstream")
                }
                .maxByOrNull { it.lastModified() }

            if (latest != null) {
                val content = latest.readText()
                val timestamp = Regex("<timestamp>(.*?)</timestamp>").find(content)?.groupValues?.get(1)
                val buildNumber = Regex("<buildNumber>(.*?)</buildNumber>").find(content)?.groupValues?.get(1)
                if (timestamp != null && buildNumber != null) {
                    val base = lwkVersion.replace("-SNAPSHOT", "")
                    println("LWK cached snapshot: $base-$timestamp-$buildNumber")
                    val sdf = java.text.SimpleDateFormat("yyyyMMdd.HHmmss")
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(timestamp)
                    val display = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    println("LWK snapshot build date: ${display.format(date)}")
                }
            } else {
                println("LWK snapshot metadata not found in Gradle cache")
            }
        }
    }
}

task("useBlockstreamKeys") {
    doLast {
        println("AppKeys: Use Blockstream Keys")
        rootProject.file("contrib/blockstream_keys.txt").copyTo(
            rootProject.file("app_keys.txt"), overwrite = true
        )
    }
}