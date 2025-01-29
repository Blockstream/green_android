plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}
dependencies {
    implementation(project(":base-gms"))
}

