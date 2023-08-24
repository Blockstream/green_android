import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.blockstream.gms"
    compileSdk = 34
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = 23

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

    /**  --- Hilt Dependency Injection  --------------------------------------------------------- */
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Zendesk ---------------------------------------------------------------------------- */
    implementation(libs.zendesk.support.providers)
    /** ----------------------------------------------------------------------------------------- */

}