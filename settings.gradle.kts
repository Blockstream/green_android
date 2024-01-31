rootProject.name = "Blockstream_Green"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
include(":green", ":base", ":compose", ":hardware", ":jade", ":gms" ,":no-gms", ":common", ":gdk")
