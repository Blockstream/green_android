plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.blockstream.hardware"
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
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":crypto"))
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
