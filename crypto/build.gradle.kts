import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("kotlinx-serialization")
}

android {
    namespace = "com.blockstream.crypto"
    compileSdk = 33

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

task("fetchBinaries") {
    doFirst{
        val exists = File("./crypto/src/main/jniLibs").exists()
        if (!exists) {
            exec {
                commandLine("./fetch_gdk_binaries.sh")
            }
        }else{
            print("-- Skipped --")
        }
    }
    outputs.upToDateWhen { false }
}

afterEvaluate {
    android.libraryVariants.all {
        preBuildProvider.configure { dependsOn("fetchBinaries") }
    }
}

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    api(project(":lightning"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    /** ----------------------------------------------------------------------------------------- */
    
    /**  --- AndroidX --------------------------------------------------------------------------- */
    implementation(libs.androidx.annotation)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Lifecycle -------------------------------------------------------------------------- */
    implementation(libs.androidx.lifecycle.livedata.ktx)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Logging ---------------------------------------------------------------------------- */
    implementation(libs.slf4j.simple)
    implementation(libs.kotlin.logging.jvm)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Testing ---------------------------------------------------------------------------- */
    testImplementation(libs.junit)
    /** ----------------------------------------------------------------------------------------- */
}