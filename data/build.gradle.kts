plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.app.cash.sqldelight)
}

sqldelight {
    databases {
        create("WalletDB") {
            packageName.set("com.blockstream.data.database.wallet")
            srcDirs.setFrom("src/commonMain/database_wallet")
        }
        create("LocalDB") {
            packageName.set("com.blockstream.data.database.local")
            srcDirs.setFrom("src/commonMain/database_local")
        }
    }
    linkSqlite.set(true)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

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

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "/META-INF/LICENSE.md"
                excludes += "/META-INF/LICENSE-notice.md"
            }
        }
    }

    jvm()

    sourceSets {

        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                optIn("kotlinx.coroutines.FlowPreview")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        commonMain {
            dependencies {
                /**  --- Modules  --- */
                api(project(":utils"))
                api(project(":network"))
                api(project(":jade"))

                /**  --- Compose -------------------------------------------------------------------- */
                api(libs.lifecycle.viewmodel.compose)
                /** --------------------------------------------------------------------------------- */

                /**  --- Kotlin  --- */
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.serialization.cbor)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.atomicfu)

                /**  --- Blockstream  --- */
                api(libs.blockstream.lwk)
                api(libs.blockstream.glsdk)

                /**  --- Utils  --- */
                api(libs.sqldelight.coroutines.extensions)
                api(libs.stately.concurrent.collections)
                api(libs.kotlin.retry)
                api(libs.ksoup.entites) // html entities
                api(libs.filekit.core)
                api(libs.filekit.dialogs)
                api(libs.okio) // Filesystem
                api(libs.kable.core)
                api(libs.kotlincrypto.hash.sha2)
                api(libs.multiplatform.settings)
                api(libs.multiplatform.settings.no.arg)
                api(libs.multiplatform.settings.make.observable)
                api(libs.multiplatform.settings.coroutines)
                api(libs.tuulbox.coroutines)
                api(libs.uri.kmp)
                api(libs.state.keeper)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }


        jvmMain.dependencies {
            api(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite.driver)
        }

        androidMain.dependencies {
            implementation(project(":gdk"))
            implementation(libs.sqldelight.android.driver)
            api(libs.koin.android)
            api(libs.androidx.biometric)

            api(libs.androidx.preference.ktx)
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.junit)
                implementation(libs.mockk)
            }
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mockk)
        }
    }
}
