plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    androidTarget {
        compilations.configureEach {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.majorVersion
            }
        }
    }

    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {

        all {
            languageSettings.apply {
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        commonMain.dependencies {
            /**  --- Kotlin & KotlinX ------------------------------------------------------------------- */
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.cbor)
            implementation(libs.kotlinx.datetime)
            /** ----------------------------------------------------------------------------------------- */

            implementation(libs.kotlincrypto.hash.md)
            implementation(libs.kotlincrypto.hash.sha2)
            implementation(libs.kable.core)
            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            /**  --- USB -------------------------------------------------------------------------------- */
            implementation(libs.usb.serial)
            /** ----------------------------------------------------------------------------------------- */
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}

android {
    namespace = "com.blockstream.jade"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
}