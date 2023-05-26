plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.blockstream.lightning"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
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


task("fetchBinaries") {
    doFirst{
        val exists = File("./lightning/src/main/jniLibs").exists()
        val disable = File("./lightning/disable_fetch_binaries").exists()
        if (!exists && !disable) {
            exec {
                commandLine("./fetch_breez_binaries.sh")
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
    /**  --- Java Native Access ----------------------------------------------------------------- */
    implementation (libs.jna) { artifact { type = "aar" } }
    /** ----------------------------------------------------------------------------------------- */
}