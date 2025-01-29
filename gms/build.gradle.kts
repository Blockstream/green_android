plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    implementation(project(":base-gms"))
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":common"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Google Mobile Services ------------------------------------------------------------- */
    implementation(libs.review.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.installreferrer)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Zendesk ---------------------------------------------------------------------------- */
    implementation(libs.zendesk.support.providers)
    /** ----------------------------------------------------------------------------------------- */
}