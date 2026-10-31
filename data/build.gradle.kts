import java.io.StringReader
import java.util.Properties

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

// BuildConfig for every platform: the base64 Greenlight credentials from the GREENLIGHT_CERT and
// GREENLIGHT_KEY environment variables, which CI sets, else from local.properties, and DEV_PIN_CODE
// from local.properties. CI builds fail without the credentials, so no CI build loses Lightning for
// lack of them; local builds without them run with Lightning turned off. CI builds also fail with a
// DEV_PIN_CODE, so it is null in every CI build and no release built there contains a PIN.
val localProperties = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.map {
    Properties().apply { load(StringReader(it)) }
}
fun localProperty(name: String) = localProperties.map { it.getProperty(name) ?: "" }.orElse("")
val greenlightCert = providers.environmentVariable("GREENLIGHT_CERT").orElse(localProperty("GREENLIGHT_CERT"))
val greenlightKey = providers.environmentVariable("GREENLIGHT_KEY").orElse(localProperty("GREENLIGHT_KEY"))
val devPinCode = localProperty("DEV_PIN_CODE")
// The same test settings.gradle.kts uses for the remote build cache: GitLab CI sets CI to "true"
val isCi = providers.environmentVariable("CI").map { it == "true" }.orElse(false)
val buildConfigDir = layout.buildDirectory.dir("generated/buildConfig/kotlin")
val generateBuildConfig by tasks.registering {
    // Gradle skips this task while its inputs and outputs are unchanged, so each value the file holds is an
    // input, or a changed local.properties would leave the old one in place. isCi changes nothing in the file,
    // but a build that switches to CI has to run the checks again.
    inputs.property("greenlightCert", greenlightCert)
    inputs.property("greenlightKey", greenlightKey)
    inputs.property("devPinCode", devPinCode)
    inputs.property("isCi", isCi)
    outputs.dir(buildConfigDir)

    doLast {
        val cert = greenlightCert.get().trim()
        val key = greenlightKey.get().trim()
        val pin = devPinCode.get().trim()

        if (isCi.get()) {
            check(cert.isNotEmpty() && key.isNotEmpty()) { "GREENLIGHT_CERT and GREENLIGHT_KEY must be set on CI" }
            check(pin.isEmpty()) { "DEV_PIN_CODE must not be set on CI" }
        }

        fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""

        buildConfigDir.get().file("com/blockstream/data/BuildConfig.kt").asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                |package com.blockstream.data
                |
                |object BuildConfig {
                |    const val GREENLIGHT_CERT = ${quoted(cert)}
                |    const val GREENLIGHT_KEY = ${quoted(key)}
                |    val DEV_PIN_CODE: String? = ${if (pin.isEmpty()) "null" else quoted(pin)}
                |}
                |""".trimMargin()
            )
        }
    }
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
            kotlin.srcDir(generateBuildConfig)

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
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
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
