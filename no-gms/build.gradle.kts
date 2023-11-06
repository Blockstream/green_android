plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":base"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Koin   ----------------------------------------------------------------------------- */
    ksp(libs.koin.ksp.compiler)
    /** ----------------------------------------------------------------------------------------- */
}