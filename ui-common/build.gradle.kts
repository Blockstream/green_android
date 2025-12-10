

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.kmp.nativecoroutines)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    androidTarget()

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    sourceSets {
        all {
            languageSettings.apply {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }

        commonMain.dependencies {
            /**  --- Compose -------------------------------------------------------------------- */
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.uiToolingPreview)
            api(libs.constraintlayout.compose.multiplatform)
            api(libs.navigation.compose)
            api(libs.compose.backhandler)
            api(libs.androidx.lifecycle.runtime.compose)
            api(libs.lifecycle.viewmodel.compose)
            api(compose.components.resources)
            /** --------------------------------------------------------------------------------- */

            /**  --- Kotlin & KotlinX ----------------------------------------------------------- */
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.json)

            /**  --- Misc. ---------------------------------------------------------------------- */
            api(libs.phosphor.icon)
            api(libs.kmp.observableviewmodel)

            implementation(libs.kermit)

            implementation(libs.compose.rebugger)

        }

        val desktopMain by getting
        desktopMain.dependencies {
            api(compose.desktop.currentOs)
        }

        androidMain.dependencies {
            /**  --- Compose -------------------------------------------------------------------- */
            api(compose.preview)
            api(libs.androidx.activity.compose)
            api(libs.compose.ui.tooling.preview)
//            api(libs.compose.material3.android)
        }
    }
}

android {
    namespace = "com.blockstream.ui"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        // Ensure androidTest manifests merge with a valid minSdk and avoid minSdk=1 default
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    buildFeatures {
        compose = true
    }

    dependencies {
        debugImplementation(compose.uiTooling)
    }
}