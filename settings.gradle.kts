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

include(
    ":green",
    ":gdk",
    ":compose",
    "ui-common",
    ":hardware",
    ":jade",
    ":base-gms",
    ":gms",
    ":no-gms",
    ":common",
    ":gdk",
    "ui-common"
)
