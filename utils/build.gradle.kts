plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val appleTargetsEnabled = rootProject.extra["appleTargetsEnabled"] as Boolean

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    androidLibrary {
        namespace = "com.blockstream.green.utils"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    jvm()
    if (appleTargetsEnabled) {
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
