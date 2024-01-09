
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.app.cash.sqldelight)
}

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
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.coroutines.FlowPreview")
            }
        }

        commonMain.dependencies {
            /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.serialization.cbor)
            api(libs.kotlinx.datetime)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Koin   ----------------------------------------------------------------------------- */
            api(libs.koin.core)
            api(libs.koin.annotations)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Voyager ---------------------------------------------------------------------------- */
            api(libs.voyager.screenmodel)
            // Required for iOS target compilation
            compileOnly(compose.runtime)
            compileOnly(compose.runtimeSaveable)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Breez ------------------------------------------------------------------------------ */
            api(libs.breez.sdk.kmp)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Misc. ------------------------------------------------------------------------------ */
            api(libs.sqldelight.coroutines.extensions)
            api(libs.kmm.viewmodel)
            api(libs.stately.concurrent.collections)
            api(libs.uri.kmp)
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

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)

            // Required by Voyager
            compileOnly(compose.runtime)
            compileOnly(compose.runtimeSaveable)
        }

        androidMain.dependencies {
            implementation(project(":gdk"))
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.sqldelight.android.driver)
            api(libs.koin.android)
            api(libs.androidx.biometric)


            /**  --- Breez FDroid ----------------------------------------------------------------------- */
            // Temp fix for FDroid breez dependencies
            // api(libs.breez.sdk.android)
            /** ----------------------------------------------------------------------------------------- */
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

        iosMain.dependencies {
            implementation(libs.stately.common) // until this is fixed https://github.com/touchlab/Stately/issues/93
            implementation(libs.sqldelight.native.driver)
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