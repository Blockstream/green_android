

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    androidTarget()

    jvm("desktop")

    listOf(
        iosX64(),
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

            /**  --- Kotlin & KotlinX ----------------------------------------------------------- */
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.json)

            /**  --- Phosphor ------------------------------------------------------------------- */
            api(libs.phosphor.icon)

            implementation(libs.compose.rebugger)

            implementation(libs.kermit)
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

    buildFeatures {
        compose = true
    }

    dependencies {
        debugImplementation(compose.uiTooling)
    }
}