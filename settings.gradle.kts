rootProject.name = "Blockstream_App"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/firework/dev")
    }
}

include(
    ":androidApp",
    ":gdk",
    ":compose",
    ":ui-common",
    ":hardware",
    ":jade",
    ":base-gms",
    ":gms",
    ":no-gms",
    ":common",
    ":gdk",
    ":desktopApp",
    ":data",
    ":utils",
    ":network",
    ":domain",
)