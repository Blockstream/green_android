plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = 34
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = 23
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

    /**  --- Hilt Dependency Injection  --------------------------------------------------------- */
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    /** ----------------------------------------------------------------------------------------- */
}