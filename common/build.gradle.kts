import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.app.cash.sqldelight)
    alias(libs.plugins.nativeCocoapods)
    alias(libs.plugins.google.devtools.ksp)
}

compose.resources {
    packageOfResClass = "blockstream_green.common.generated.resources" // Keep the same package name
    publicResClass = true
    generateResClass = auto
}

sqldelight {
    databases {
        create("WalletDB") {
            packageName.set("com.blockstream.common.database.wallet")
            srcDirs.setFrom("src/commonMain/database_wallet")
        }
        create("LocalDB") {
            packageName.set("com.blockstream.common.database.local")
            srcDirs.setFrom("src/commonMain/database_local")
        }
    }
    linkSqlite.set(true)
}

nativeCoroutines { k2Mode = false }

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    androidLibrary {
        namespace = "com.blockstream.common"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "/META-INF/LICENSE.md"
                excludes += "/META-INF/LICENSE-notice.md"
            }
        }
    }
    jvm()

    val xcf = XCFramework()
    listOf(
        iosArm64(),
        iosSimulatorArm64()
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

    cocoapods {
        version = "2.0"
        ios.deploymentTarget = "15.3"

        pod("Countly") {
            source = git("https://github.com/angelix/countly-sdk-ios") {
                commit = "1892410d13fceccd7cf91f803f06f110efc215b3"
            }

            // Support for Objective-C headers with @import directives
            // https://kotlinlang.org/docs/native-cocoapods-libraries.html#support-for-objective-c-headers-with-import-directives
            extraOpts += listOf("-compiler-option", "-fmodules")
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
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        commonMain.dependencies {
            /**  --- Modules ---------------------------------------------------------------------------- */
            api(project(":data"))
            api(project(":ui-common"))
            api(project(":jade"))
            api(project(":domain"))
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.serialization.cbor)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.atomicfu)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Compose ---------------------------------------------------------------------------- */
            api(compose.components.resources)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Koin   ----------------------------------------------------------------------------- */
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Voyager ---------------------------------------------------------------------------- */
            // Required for iOS target compilation
            compileOnly(compose.runtime)
            compileOnly(compose.runtimeSaveable)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Breez ------------------------------------------------------------------------------ */
            api(libs.breez.sdk.kmp)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Misc. ------------------------------------------------------------------------------ */
            api(libs.stately.concurrent.collections)
            api(libs.sqldelight.coroutines.extensions)
            api(libs.kmp.observableviewmodel)
            api(libs.uri.kmp)
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.no.arg)
            api(libs.multiplatform.settings.make.observable)
            api(libs.multiplatform.settings.coroutines)
            api(libs.okio) // Filesystem
            api(libs.state.keeper)
            api(libs.kase64) // base64
            api(libs.kable.core)
            api(libs.kotlincrypto.hash.md)
            api(libs.kotlincrypto.hash.sha2)
            implementation(libs.kotlin.retry)

            api(libs.filekit.core)
            api(libs.filekit.dialogs)

            //implementation(libs.compose.action.menu)
            implementation(libs.phosphor.icon)

            implementation(libs.tuulbox.coroutines)
            /** ----------------------------------------------------------------------------------------- */
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            // implementation(libs.koin.test)

            compileOnly(compose.runtime)
            compileOnly(compose.runtimeSaveable)
        }

        val jvmMain by getting
        jvmMain.dependencies {
            api(libs.kotlinx.coroutines.swing)
            implementation(compose.desktop.currentOs)
            implementation(libs.sqldelight.sqlite.driver)
        }

        androidMain.dependencies {
            implementation(project(":gdk"))
            implementation(libs.sqldelight.android.driver)
            api(libs.koin.android)
            api(libs.androidx.biometric)

            api(libs.androidx.preference.ktx)

            /**  --- Breez FDroid ----------------------------------------------------------------------- */
            // Temp fix for FDroid breez dependencies
            // api(libs.breez.sdk.android.get().toString()) { exclude(group = "net.java.dev.jna", module = "jna") }
            // implementation("${libs.jna.get()}@aar")
            /** ----------------------------------------------------------------------------------------- */
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
                implementation(libs.mockk)
            }
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            // implementation(libs.koin.test)
            // implementation(libs.koin.test.junit4)
            implementation(libs.mockk)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

task("fetchIosBinaries") {
    doFirst {
        val exists = project.file("src/include").exists() && project.file("src/libs").exists()
        if (!exists) {
            providers.exec {
                workingDir = project.projectDir
                commandLine("./fetch_ios_binaries.sh")
            }.result.get()
        } else {
            print("-- Skipped --")
        }
    }
    outputs.upToDateWhen { false }
}

tasks.configureEach {
    if (name.contains("cinterop")) {
        dependsOn("fetchIosBinaries")
    }
}

task("useBlockstreamKeys") {
    doLast {
        println("AppKeys: Use Blockstream Keys")
        rootProject.file("contrib/blockstream_keys.txt")
            .copyTo(
                project.file("src/commonMain/composeResources/files/app_keys.txt"),
                overwrite = true
            )
    }
}

// Made the app work without app_keys.txt
//tasks.register("appKeys") {
//    doLast {
//        val appKeys = project.file("src/commonMain/composeResources/files/app_keys.txt")
//        if (appKeys.exists()) {
//            println("AppKeys: ✔")
//        } else {
//            println("AppKeys: Use empty key file")
//            appKeys.createNewFile()
//        }
//    }
//    outputs.upToDateWhen { false }
//}
//
// tasks.getByName("androidPreBuild").dependsOn(tasks.getByName("appKeys"))

tasks.getByName("clean").doFirst {
    delete(project.file("src/include"))
    delete(project.file("src/libs"))
}
