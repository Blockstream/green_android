plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.blockstream.base"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Android / Google ------------------------------------------------------------------- */
    api(libs.androidx.core.ktx)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.swiperefreshlayout)
    api(libs.androidx.biometric)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.browser)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager2)
    api(libs.installreferrer)
    api(libs.androidx.startup.runtime)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Lifecycle -------------------------------------------------------------------------- */
    api(libs.androidx.lifecycle.livedata.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.process)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Logging ---------------------------------------------------------------------------- */
    api(libs.slf4j.simple)
    api(libs.kotlin.logging.jvm)
    /** ----------------------------------------------------------------------------------------- */
}