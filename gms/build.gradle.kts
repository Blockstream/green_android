plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    implementation(project(":base-gms"))
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":data"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Google Mobile Services ------------------------------------------------------------- */
    implementation(libs.review.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.installreferrer)
    /** ----------------------------------------------------------------------------------------- */
}