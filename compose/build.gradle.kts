@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.google.devtools.ksp)
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":common"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Compose ---------------------------------------------------------------------------- */
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Material --------------------------------------------------------------------------- */
    implementation(libs.compose.material3)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Compose QR Code -------------------------------------------------------------------- */
    implementation(libs.compose.qr.code)
    /** ----------------------------------------------------------------------------------------- */
}