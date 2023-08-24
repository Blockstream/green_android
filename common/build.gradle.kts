import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    // Enable the default target hierarchy:
    targetHierarchy.default()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.majorVersion
            }
        }
    }

    val xcf = XCFramework()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "common"
            xcf.add(this)
            isStatic = true
        }

        val platform = when (it.targetName) {
            "iosSimulatorArm64" -> "ios_simulator_arm64"
            "iosArm64" -> "ios_arm64"
            else -> error("Unsupported target $name")
        }

        it.compilations["main"].cinterops {
            create("gdkCInterop") {
                defFile(project.file("src/nativeInterop/cinterop/gdk.def"))
                includeDirs(project.file("src/include"), project.file("src/libs/$platform"))
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.ExperimentalStdlibApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        val commonMain by getting {
            dependencies {
                /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
                /** ----------------------------------------------------------------------------------------- */

                api("com.benasher44:uuid:0.7.1")
                api("com.russhwolf:multiplatform-settings:1.0.0")
                api("com.squareup.okio:okio:3.3.0") // Filesystem
                api("co.touchlab:kermit:2.0.0-RC4") //Add latest version
                api("com.arkivanov.essenty:parcelable:1.1.0") // parcelable
                api("de.peilicke.sascha:kase64:1.0.6") // base64
                api("com.mohamedrejeb.ksoup:ksoup-entites:0.1.2") // html entities
            }
        }

        getByName("androidMain"){
            dependencies {
                implementation(project(":gdk"))
            }
        }

        getByName("iosMain"){
            dependsOn(commonMain)
        }

        getByName("commonTest"){
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
            }
        }
    }
}

// assembleXCFramework

task("fetchIosBinaries") {
    doFirst{
        val exists = File("./common/src/include/").exists() && File("./common/src/libs/").exists()
        if (!exists) {
            exec {
                commandLine("./fetch_ios_binaries.sh")
            }
        }else{
            print("-- Skipped --")
        }
    }
    outputs.upToDateWhen { false }
}

afterEvaluate {
    tasks.forEach {
        if(it.name.contains("cinterop")){
            it.dependsOn("fetchIosBinaries")
        }
    }
}

android {
    namespace = "com.blockstream.common"
    compileSdk = 34
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.getByName("clean").doFirst {
    delete(project.file("src/include"))
    delete(project.file("src/libs"))
}