plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.blockstream.lightning"
    compileSdk = 33
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    /**  --- Breez SDK -------------------------------------------------------------------------- */
     api("breez_sdk:bindings-android:0.1.3")
    // api("technology.breez:sdk-kotlin-multiplatform:0.0.1")
    /** ----------------------------------------------------------------------------------------- */
}