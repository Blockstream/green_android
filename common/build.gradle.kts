import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kmp.nativecoroutines)
    alias(libs.plugins.app.cash.sqldelight)
    alias(libs.plugins.nativeCocoapods)
}

compose.resources {
    publicResClass = true
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
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            freeCompilerArgs.addAll("-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.blockstream.common.Parcelize")
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    jvm()

    val xcf = XCFramework()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach {
        it.binaries.framework {
            baseName = "Common"
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

            /**  --- Compose ---------------------------------------------------------------------------- */
            api(compose.components.resources)
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
            api(libs.kmp.observableviewmodel)
            api(libs.stately.concurrent.collections)
            api(libs.uri.kmp)
            api(libs.uuid)
            api(libs.multiplatform.settings)
            api(libs.okio) // Filesystem
            api(libs.kermit) //Add latest version
            api(libs.state.keeper)
            api(libs.parcelable)
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
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.blockstream.rover"
            packageVersion = "1.0.0"
        }
    }
}


task("useBlockstreamKeys") {
    doLast {
        println("AppKeys: Use Blockstream Keys")
        rootProject.file("contrib/blockstream_keys.txt")
            .copyTo(project.file("src/commonMain/composeResources/files/app_keys.txt"), overwrite = true)
    }
}

task("appKeys") {
    doFirst {
        val appKeys = project.file("src/commonMain/composeResources/files/app_keys.txt")
        if (appKeys.exists()) {
            println("AppKeys: ✔")
        } else {
            println("AppKeys: Use empty key file")
            appKeys.createNewFile()
        }
    }
    outputs.upToDateWhen { false }
}

tasks.getByName("preBuild").dependsOn(tasks.getByName("appKeys"))

tasks.getByName("clean").doFirst {
    delete(project.file("src/include"))
    delete(project.file("src/libs"))
}