import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
}

compose.resources {
    packageOfResClass = "blockstream_green.common.generated.resources" // Keep the same package name
    publicResClass = true
    generateResClass = auto
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    androidTarget()

    jvm("desktop")

    val xcf = XCFramework()
    listOf(
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
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        commonMain.dependencies {
            /**  --- Modules ---------------------------------------------------------------------------- */
            api(project(":domain"))
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Compose ---------------------------------------------------------------------------- */
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(libs.constraintlayout.compose.multiplatform)
            implementation(libs.material3.adaptive)
            implementation(libs.navigation.compose)
            implementation(libs.compose.backhandler)
            api(compose.components.resources)
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
            implementation(libs.phosphor.icon)
            /** ----------------------------------------------------------------------------------------- */
            implementation(libs.koalaplot.core)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            api(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            api(libs.kotlin.multiplatform.appdirs)
        }

        androidMain.dependencies {
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

    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

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
        debugImplementation(libs.ui.tooling.preview)
    }
}