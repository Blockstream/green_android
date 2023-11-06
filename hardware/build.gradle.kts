plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinxSerialization)
}

android {
    namespace = "com.blockstream.hardware"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
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
    implementation(project(":common"))
    implementation(project(":gdk"))
    implementation(project(":jade"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Guava ------------------------------------------------------------------------------ */
    implementation(libs.guava)
    implementation(libs.protobuf.java)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- RxJava ----------------------------------------------------------------------------- */
    implementation(libs.rxjava)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Jackson ---------------------------------------------------------------------------- */
    implementation(libs.jackson.datatype.json.org)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Bluetooth -------------------------------------------------------------------------- */
    api(libs.rxandroidble)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Logging ---------------------------------------------------------------------------- */
    implementation(libs.slf4j.simple)
    implementation(libs.kotlin.logging.jvm)
    /** ----------------------------------------------------------------------------------------- */

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mockito.kotlin)
}
