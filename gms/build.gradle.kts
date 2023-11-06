import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

        val zendeskClientId = System.getenv("ZENDESK_CLIENT_ID") ?: gradleLocalProperties(rootDir).getProperty("zendesk.clientId", "")

        buildConfigField("String", "ZENDESK_CLIENT_ID", "\"${zendeskClientId}\"")

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

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":base"))
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Google Mobile Services ------------------------------------------------------------- */
    implementation(libs.review.ktx)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Koin   ----------------------------------------------------------------------------- */
    ksp(libs.koin.ksp.compiler)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Zendesk ---------------------------------------------------------------------------- */
    implementation(libs.zendesk.support.providers)
    /** ----------------------------------------------------------------------------------------- */
}