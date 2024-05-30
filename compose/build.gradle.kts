@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.blockstream.compose"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs.pickFirsts.add("**/*.so")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

composeCompiler {
    enableStrongSkippingMode = true
    }

kotlin {
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.addAll("-P", "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.blockstream.common.Parcelize")
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }
    }
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":common"))
    implementation(project(":base"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Compose ---------------------------------------------------------------------------- */
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.constraint)

    implementation(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Material --------------------------------------------------------------------------- */
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.android)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Android / Google ------------------------------------------------------------------- */
    api(libs.androidx.browser)
    implementation ("com.google.accompanist:accompanist-permissions:0.34.0")
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Voyager ---------------------------------------------------------------------------- */
    api(libs.voyager.navigator)
    api(libs.voyager.bottomSheetNavigator)
    api(libs.voyager.transitions)
    api(libs.voyager.koin)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Koin   ----------------------------------------------------------------------------- */
    implementation(libs.koin.androidx.compose)
    ksp(libs.koin.ksp.compiler)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Compose QR Code -------------------------------------------------------------------- */
    implementation(libs.compose.qr.code)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- QR Scanner ------------------------------------------------------------------------- */
    implementation(libs.zxing.android.embedded)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Rive ------------------------------------------------------------------------------- */
    api(libs.rive.android)
    /** ----------------------------------------------------------------------------------------- */

    implementation("io.github.mataku:middle-ellipsis-text3:1.1.0")

    implementation(libs.state.keeper) // state keeper
}