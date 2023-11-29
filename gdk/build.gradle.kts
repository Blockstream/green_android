plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.blockstream.gdk"
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

task("fetchAndroidBinaries") {
    doFirst{
        val jniLibs = project.file("src/main/jniLibs")
        if (!jniLibs.exists()) {
            println("GDK: Binaries in ${jniLibs.absolutePath} does not exist. Executing ./fetch_android_binaries.sh")
            exec {
                commandLine("./fetch_android_binaries.sh")
            }
        } else {
            println("GDK: Binaries ✔")
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