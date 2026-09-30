plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

compose.resources {
    packageOfResClass = "com.blockstream.designsystem.generated.resources"
    publicResClass = true
    generateResClass = auto
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }

    jvmToolchain(libs.versions.jvm.get().toInt())

    androidLibrary {
        namespace = "com.blockstream.designsystem"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        androidResources { enable = true }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            api(compose.components.resources)
        }
    }
}
