
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    id("kotlin-parcelize")
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.app.cash.sqldelight)
}

apply(plugin = "kotlinx-atomicfu")

sqldelight {
    databases {
        create("GreenDB") {
            packageName.set("com.blockstream.common.database")
        }
    }
    linkSqlite.set(true)
}

kotlin {
    // Enable the default target hierarchy:
    applyDefaultHierarchyTemplate()

    androidTarget {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.majorVersion
            }
        }
    }

    val xcf = XCFramework()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach {
        it.binaries.framework {
            baseName = "common"
            xcf.add(this)
            isStatic = true
        }

        val platform = when (it.targetName) {
            "iosX64" -> "ios_simulator_x86"
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
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                optIn("kotlin.experimental.ExperimentalObjCName")
                optIn("kotlinx.coroutines.FlowPreview")
            }
        }

        val commonMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
                /** ----------------------------------------------------------------------------------------- */

                /**  --- Koin   ----------------------------------------------------------------------------- */
                api(libs.koin.core)
                api(libs.koin.annotations)
                /** ----------------------------------------------------------------------------------------- */

                /**  --- Breez ------------------------------------------------------------------------------ */
                api(libs.breez.sdk.kmp)
                /** ----------------------------------------------------------------------------------------- */

                /**  --- Misc. ------------------------------------------------------------------------------ */
                api(libs.sqldelight.coroutines.extensions)
                api(libs.kmm.viewmodel)
                api(libs.stately.concurrent.collections)
                api(libs.uuid)
                api(libs.multiplatform.settings)
                api(libs.okio) // Filesystem
                api(libs.kermit) //Add latest version
                api(libs.parcelable) // parcelable
                api(libs.state.keeper)
                api(libs.kase64) // base64
                api(libs.ksoup.entites) // html entities
                /** ----------------------------------------------------------------------------------------- */
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":gdk"))
                implementation(libs.androidx.lifecycle.viewmodel.ktx)
                implementation(libs.koin.android)
                implementation(libs.sqldelight.android.driver)

                /**  --- Breez FDroid ----------------------------------------------------------------------- */
                // Temp fix for FDroid breez dependencies
                // api(libs.breez.sdk.android)
                /** ----------------------------------------------------------------------------------------- */
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.stately.common) // until this is fixed https://github.com/touchlab/Stately/issues/93
                implementation(libs.sqldelight.native.driver)
            }
        }

        val iosArm64Main by getting {
            kotlin.srcDir("build/generated/ksp/metadata/iosArm64Main/kotlin")
        }

        val iosSimulatorArm64Main by getting {
            kotlin.srcDir("build/generated/ksp/metadata/iosSimulatorArm64Main/kotlin")
        }

        val iosX64Main by getting {
            kotlin.srcDir("build/generated/ksp/metadata/iosX64Main/kotlin")
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.koin.test)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.koin.test)
                implementation(libs.koin.test.junit4)
                implementation(libs.mockk)
            }
        }
    }
}

task("fetchIosBinaries") {
    doFirst{

        val exists = project.file("src/include").exists() && project.file("src/libs").exists()
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

tasks.configureEach {
    if(name.contains("cinterop")){
        dependsOn("fetchIosBinaries")
    }
}

// https://kotlinlang.org/docs/ksp-multiplatform.html
// https://github.com/InsertKoinIO/hello-kmp/blob/annotations/shared/build.gradle.kts
dependencies {
    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
}

// WORKAROUND: ADD this dependsOn("kspCommonMainKotlinMetadata") instead of above dependencies
tasks.withType<KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
tasks.configureEach {
    if(name.contains("SourcesJar", true)){
        println("SourceJarTask====>${name}")
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

android {
    namespace = "com.blockstream.common"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
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