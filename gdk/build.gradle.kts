plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.blockstream.gdk"
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

kotlin {
    jvmToolchain(17)
}

task("fetchAndroidBinaries") {
    doFirst{
        val exists = File("./gdk/src/main/jniLibs").exists()
        if (!exists) {
            exec {
                commandLine("./fetch_android_binaries.sh")
            }
        }else{
            print("-- Skipped --")
        }
    }
    outputs.upToDateWhen { false }
}

afterEvaluate {
    android.libraryVariants.all {
        preBuildProvider.configure { dependsOn("fetchAndroidBinaries") }
    }
}

tasks.getByName("clean").doFirst {
    delete(project.file("src/main/jniLibs"))
}