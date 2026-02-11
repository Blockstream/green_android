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
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

task("fetchAndroidBinaries") {
    doFirst {
        val jniLibs = project.file("src/main/jniLibs")
        if (!jniLibs.exists()) {
            val gdkCommit = System.getenv("GDK_COMMIT")
            val command = if (!gdkCommit.isNullOrBlank()) {
                println("GDK: Binaries in ${jniLibs.absolutePath} does not exist. Executing ./fetch_android_binaries.sh -c $gdkCommit")
                listOf("./fetch_android_binaries.sh", "-c", gdkCommit)
            } else {
                println("GDK: Binaries in ${jniLibs.absolutePath} does not exist. Executing ./fetch_android_binaries.sh")
                listOf("./fetch_android_binaries.sh")
            }
            providers.exec {
                workingDir = project.projectDir
                commandLine(command)
            }.result.get()
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