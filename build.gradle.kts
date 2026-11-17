// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots/") // LWK Snapshots
    }
}

// Every module compiles with the JDK in libs.versions.jvm but for Java libs.versions.jvmTarget: the class files,
// and the JDK API Kotlin compiles against. Android compilations see the toolchain's JDK next to android.jar, so
// without -Xjdk-release a newer JDK's methods, such as Java 21's List.removeFirst, would compile and then fail on
// older Android versions.
val javaTarget = libs.versions.jvmTarget.get()

subprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaTarget))
            freeCompilerArgs.add("-Xjdk-release=$javaTarget")
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = javaTarget
        targetCompatibility = javaTarget
    }
    // Android modules' Java compile takes its level from compileOptions, which AGP otherwise derives from the
    // toolchain, overriding the rule above
    listOf("com.android.application", "com.android.library").forEach { plugin ->
        pluginManager.withPlugin(plugin) {
            extensions.configure<CommonExtension> {
                compileOptions.sourceCompatibility = JavaVersion.toVersion(javaTarget)
                compileOptions.targetCompatibility = JavaVersion.toVersion(javaTarget)
            }
        }
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
    resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.MINUTES)
}

task("printLwkVersion") {
    doLast {
        val lwkVersion = libs.versions.blockstream.lwk.get()
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