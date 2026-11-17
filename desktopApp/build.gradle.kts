
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJvmToolOperationTask
import org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask
import org.jetbrains.compose.desktop.application.tasks.AbstractSuggestModulesTask
import org.jetbrains.compose.reload.gradle.AbstractComposeHotRun
import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":compose"))
            }
        }
    }
}

// Since JDK 24 the JVM warns when code outside a named module, such as Skiko's, GDK's or JNA's loader, loads a
// native library, and a later release will refuse it
val enableNativeAccess = "--enable-native-access=ALL-UNNAMED"

compose.desktop {
    application {
        mainClass = "com.blockstream.green.desktop.MainKt"

        jvmArgs += enableNativeAccess

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.blockstream.green"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
        }
    }
}

// build.gradle.kts
tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("com.blockstream.green.desktop.DevMainKt")
}
tasks.withType<AbstractComposeHotRun>().configureEach {
    jvmArgs(enableNativeAccess)
}

// The app runs and is packaged on the project's JDK toolchain rather than the JDK running Gradle, which Compose
// defaults to. Setting compose.desktop.application.javaHome instead would look the toolchain up while this project
// is configured, which Android builds do too, so the tasks get it lazily.
val desktopJavaLauncher = javaToolchains.launcherFor(java.toolchain)
val desktopJavaHome = desktopJavaLauncher.map { it.metadata.installationPath.asFile.absolutePath }

// Compose registers these tasks after evaluation and sets javaHome when it creates them, which would override
// a configureEach declared before that
afterEvaluate {
    tasks.withType<AbstractJvmToolOperationTask>().configureEach { javaHome.set(desktopJavaHome) }
    tasks.withType<AbstractProguardTask>().configureEach { javaHome.set(desktopJavaHome) }
    tasks.withType<AbstractSuggestModulesTask>().configureEach { javaHome.set(desktopJavaHome) }
}
tasks.withType<JavaExec>().configureEach {
    val run = this
    // Hot reload runs on a JetBrains Runtime it finds or downloads itself, as it needs JBR's class redefinition
    if (run is AbstractComposeHotRun) return@configureEach
    run.javaLauncher.set(desktopJavaLauncher)
    // Compose's run tasks also set an executable, from the JDK running Gradle
    doFirst {
        if (run.executable != null) run.setExecutable(desktopJavaLauncher.get().executablePath.asFile.absolutePath)
    }
}