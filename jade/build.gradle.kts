plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.blockstream.jade"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
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