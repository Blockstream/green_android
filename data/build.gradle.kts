plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "com.blockstream.green.data"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                /**  --- Modules  --- */
                api(project(":utils"))
                api(project(":network"))

                /**  --- Kotlin  --- */
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)

                /**  --- Koin  --- */
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)

                /**  --- LWK  --- */
                api(libs.lwk)

                /**  --- Utils  --- */
                api(libs.ksoup.entites) // html entities
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                 implementation(libs.koin.test)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
            }
        }
    }
}
