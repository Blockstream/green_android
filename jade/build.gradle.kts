plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinxSerialization)
}

android {
    namespace = "com.blockstream.jade"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":common"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Android / Google ------------------------------------------------------------------- */
    api(libs.androidx.core.ktx)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Jackson ---------------------------------------------------------------------------- */
    implementation(libs.jackson.datatype.json.org)
    implementation(libs.jackson.dataformat.cbor)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- RxJava ----------------------------------------------------------------------------- */
    implementation(libs.rxjava)
    implementation(libs.replaying.share)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Bluetooth -------------------------------------------------------------------------- */
    implementation(libs.rxandroidble)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- USB -------------------------------------------------------------------------------- */
    implementation(libs.usb.serial)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Logging ---------------------------------------------------------------------------- */
    implementation(libs.slf4j.simple)
    implementation(libs.kotlin.logging.jvm)
    /** ----------------------------------------------------------------------------------------- */


    testImplementation(libs.junit)
}