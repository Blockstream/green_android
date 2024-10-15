plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.blockstream.base"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    api(project(":common"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Android / Google ------------------------------------------------------------------- */
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.swiperefreshlayout)
    api(libs.androidx.browser)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager2)
    api(libs.androidx.startup.runtime)
    api(libs.compose.material3)
    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.activity.compose)
    api(libs.androidx.core.splashscreen)
    api(libs.androidx.lifecycle.process)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Countly ---------------------------------------------------------------------------- */
    api(libs.countly.sdk.android)
    /** ----------------------------------------------------------------------------------------- */
}