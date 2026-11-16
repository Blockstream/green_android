plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
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
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    androidLibrary {
        namespace = "com.blockstream.compose"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        androidResources { enable = true }

        withHostTestBuilder {
        }
    }

    jvm("desktop")

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
            api(project(":designsystem"))
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

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val desktopTest by getting
        desktopTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            api(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            api(libs.kotlin.multiplatform.appdirs)

            /**  --- Rive ------------------------------------------------------------------------------- */
            implementation(libs.rive.cmp)
            /** ----------------------------------------------------------------------------------------- */
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

            implementation(libs.ui.tooling.preview)

            /**  --- Rive ------------------------------------------------------------------------------- */
            api(libs.rive.android)
            /** ----------------------------------------------------------------------------------------- */

            implementation(libs.peekaboo.image.picker)
        }
    }
}
