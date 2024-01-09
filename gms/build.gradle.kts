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

        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":base"))
    implementation(project(":common"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Google Mobile Services ------------------------------------------------------------- */
    implementation(libs.review.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    /** ----------------------------------------------------------------------------------------- */

    /**  --- Koin   ----------------------------------------------------------------------------- */
    ksp(libs.koin.ksp.compiler)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Zendesk ---------------------------------------------------------------------------- */
    implementation(libs.zendesk.support.providers)
    /** ----------------------------------------------------------------------------------------- */
}