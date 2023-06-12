plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":base"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Google Mobile Services ------------------------------------------------------------- */
    implementation(libs.review.ktx)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Hilt Dependency Injection  --------------------------------------------------------- */
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    /** ----------------------------------------------------------------------------------------- */

}