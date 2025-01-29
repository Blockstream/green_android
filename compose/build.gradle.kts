
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
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

            /**  --- Voyager ---------------------------------------------------------------------------- */
            api(libs.voyager.navigator)
            api(libs.voyager.transitions)
            api(libs.voyager.koin)
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
            /** ----------------------------------------------------------------------------------------- */
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        androidMain.dependencies {
            implementation(compose.preview)

            /**  --- Modules ---------------------------------------------------------------------------- */
            implementation(project(":ui-common"))
            implementation(project(":base-gms"))
            api(project(":hardware"))
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Compose ---------------------------------------------------------------------------- */
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.ui.tooling.preview)
            /** ----------------------------------------------------------------------------------------- */

            /**  --- Material --------------------------------------------------------------------------- */
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.android)
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