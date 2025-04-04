
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
//    alias(libs.plugins.composeHotReload)
}

//composeCompiler {
//    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
//}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    androidTarget()

    jvm("desktop")

    val xcf = XCFramework()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            xcf.add(this)
            isStatic = true
        }
    }

    sourceSets {

        all {
            languageSettings.apply {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }

        commonMain.dependencies {
            /**  --- Modules ---------------------------------------------------------------------------- */
            implementation(project(":common"))
            implementation(project(":ui-common"))
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Compose ---------------------------------------------------------------------------- */
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.constraintlayout.compose.multiplatform)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Lifecycle -------------------------------------------------------------------------- */
            implementation(libs.androidx.lifecycle.runtime.compose)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Koin   ----------------------------------------------------------------------------- */
            implementation(libs.koin.compose)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- UI --------------------------------------------------------------------------------- */
            implementation(libs.qrose)
            implementation(libs.mpfilepicker)
            implementation(libs.middle.ellipsis.text3)
            implementation(libs.coil.compose)
            implementation(libs.coil.svg)
            implementation(libs.coil.test)
            implementation(libs.coil.network.ktor3)
            implementation(libs.compose.action.menu)
            /** ----------------------------------------------------------------------------------------- */
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(compose.desktop.currentOs)
        }

        androidMain.dependencies {
            implementation(compose.preview)

            /**  --- Modules ---------------------------------------------------------------------------- */
            implementation(project(":base-gms"))
            api(project(":hardware"))
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Android / Google ------------------------------------------------------------------- */
            api(libs.androidx.browser)
            implementation (libs.accompanist.permissions)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- QR Scanner ------------------------------------------------------------------------- */
            implementation(libs.zxing.android.embedded)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Rive ------------------------------------------------------------------------------- */
            api(libs.rive.android)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- media3 ----------------------------------------------------------------------------- */
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            /** ----------------------------------------------------------------------------------------- */

            implementation(libs.peekaboo.image.picker)
        }

        iosMain.dependencies {
            implementation(libs.peekaboo.image.picker)
        }
    }
}

android {
    namespace = "com.blockstream.compose"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    // sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    packaging {
        jniLibs.pickFirsts.add("**/*.so")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.blockstream.green"
            packageVersion = "1.0.0"
        }
    }
}

//tasks.register<ComposeHotRun>("runHot") {
//    mainClass.set("com.blockstream.compose.DevMainKt")
//}