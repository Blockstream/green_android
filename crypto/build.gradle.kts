import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinxSerialization)
}

android {
    namespace = "com.blockstream.crypto"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        val breezApiKey = System.getenv("BREEZ_API_KEY") ?: gradleLocalProperties(rootDir).getProperty("breez.apikey", "")
        val greenlightCertificate = System.getenv("GREENLIGHT_DEVICE_CERT") ?: gradleLocalProperties(rootDir).getProperty("greenlight.cert", "")
        val greenlightKey = System.getenv("GREENLIGHT_DEVICE_KEY") ?: gradleLocalProperties(rootDir).getProperty("greenlight.key", "")

        buildConfigField("String", "BREEZ_API_KEY", "\"${breezApiKey}\"")
        buildConfigField("String", "GREENLIGHT_DEVICE_CERT", "\"${greenlightCertificate}\"")
        buildConfigField("String", "GREENLIGHT_DEVICE_KEY", "\"${greenlightKey}\"")

        consumerProguardFiles("consumer-rules.pro")
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
    api(project(":gdk"))
    api(project(":common"))
    api(project(":lightning"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Logging ---------------------------------------------------------------------------- */
    implementation(libs.slf4j.simple)
    implementation(libs.kotlin.logging.jvm)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Testing ---------------------------------------------------------------------------- */
    testImplementation(libs.junit)
    /** ----------------------------------------------------------------------------------------- */
}