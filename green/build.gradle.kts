
import com.adarshr.gradle.testlogger.theme.ThemeType
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.codehaus.groovy.runtime.ProcessGroovyMethods
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt") // until @BindingAdapter supports KSP
    id("androidx.navigation.safeargs.kotlin")
    id("com.adarshr.test-logger") version "3.2.0"
    alias(libs.plugins.googleServices)
}

// https://developer.android.com/studio/publish/app-signing#secure-key
// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")
// Initialize a new Properties() object called keystoreProperties.
val keystoreProperties = Properties()
// Load your keystore.properties file into the keystoreProperties object.
if (keystorePropertiesFile.exists()){
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else { // or if not exists get them from env variables
    keystoreProperties["storeFile"] = System.getenv("KEYSTORE_FILE") ?: ""
    keystoreProperties["storePassword"] = System.getenv("KEYSTORE_PASSWORD") ?: ""
    keystoreProperties["keyAlias"] = System.getenv("KEY_ALIAS") ?: ""
    keystoreProperties["keyPassword"] = System.getenv("KEY_PASSWORD") ?: ""
}

android {
    namespace = "com.blockstream.green"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 433
        versionName = "4.0.33"

        setProperty("archivesBaseName", "BlockstreamGreen-v$versionName")
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        testApplicationId = "com.blockstream.green.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = (keystoreProperties["storeFile"] as? String)?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
            keyAlias = keystoreProperties["keyAlias"] as? String
            keyPassword = keystoreProperties["keyPassword"] as? String
            storeFile = if(keystoreFile != null && keystoreFile.exists() ) keystoreFile else null
            storePassword = keystoreProperties["storePassword"] as? String
        }
    }

    flavorDimensions += listOf("normal")
    productFlavors {
        create("development") {
            applicationId = "com.greenaddress.greenbits_android_wallet.dev"
            versionNameSuffix  = "-dev" + appendGdkCommitHash(rootProject, true)
            resValue("string", "app_name", "Green Dev")
            resValue("string", "application_id", applicationId!!)
            resValue("bool", "feature_lightning", "true")
            resValue("bool", "feature_analytics", "true")
            resValue("bool", "feature_rate_google_play", "true")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_dev"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_dev_round"
            manifestPlaceholders["enableQATester"] = true
        }

        create("productionGoogle") {
            applicationId = "com.greenaddress.greenbits_android_wallet"
            resValue("string", "app_name", "Green")
            resValue("string", "application_id", applicationId!!)
            resValue("bool", "feature_lightning", "true")
            resValue("bool", "feature_analytics", "true")
            resValue("bool", "feature_rate_google_play", "true")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
            manifestPlaceholders["enableQATester"] = false
        }

        create("productionFDroid") {
            applicationId = "com.greenaddress.greenbits_android_wallet"
            versionNameSuffix = appendGdkCommitHash(rootProject, false)
            resValue("string", "app_name", "Green")
            resValue("string", "application_id", applicationId!!)
            resValue("bool", "feature_lightning", "false")
            resValue("bool", "feature_analytics", "false")
            resValue("bool", "feature_rate_google_play", "false")
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            manifestPlaceholders["appIconRound"] = "@mipmap/ic_launcher_round"
            manifestPlaceholders["enableQATester"] = false
        }
    }
    applicationVariants.all {
        outputs.all {
            (this as ApkVariantOutputImpl).versionCodeOverride = 22000000 + (android.defaultConfig.versionCode ?: 0)
        }
    }
    buildFeatures {
        compose = true
        dataBinding = true
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("normal")

            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a") // includes ARM & x86_64 .so files only, no x86 .so file
            }

            signingConfigs.getByName("release").also {
                if(it.storeFile != null){
                    signingConfig = it
                }
            }
        }
    }
    compileOptions {
        // SDK 23 support
        isCoreLibraryDesugaringEnabled = true
    }
    packaging {
        jniLibs.pickFirsts.add("**/*.so")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    lint {
        abortOnError = false
        disable += listOf("MissingTranslation", "SpUsage", "Instantiatable")
        ignoreWarnings = false
    }
}

// For KSP, configure using KSP extension:
ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
}

composeCompiler {
    enableStrongSkippingMode = true
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlinx.coroutines.FlowPreview")
            }
        }
    }
}

testlogger {
    theme = ThemeType.MOCHA
}

val developmentImplementation by configurations
val productionGoogleImplementation by configurations
val productionFDroidImplementation by configurations

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":base"))
    implementation(project(":compose"))
    implementation(project(":hardware"))
    implementation(project(":jade"))

    developmentImplementation(project(":gms"))
    productionGoogleImplementation(project(":gms"))
    productionFDroidImplementation(project(":no-gms")) // F-Droid
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Java 8+ API desugaring support ----------------------------------------------------- */
    coreLibraryDesugaring(libs.desugar)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Navigation ------------------------------------------------------------------------- */
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.navigation.testing)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Room ------------------------------------------------------------------------------- */
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Koin   ----------------------------------------------------------------------------- */
    ksp(libs.koin.ksp.compiler)

    // For instrumentation tests
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit4)

    // For local unit tests
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- RxJava  ---------------------------------------------------------------------------- */
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxkotlin)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- FastAdapter  ----------------------------------------------------------------------- */
    implementation(libs.fastadapter)
    implementation(libs.fastadapter.extensions.diff) // diff util helpers
    implementation(libs.fastadapter.extensions.binding) // view binding helpers
    implementation(libs.fastadapter.extensions.expandable)
    implementation(libs.fastadapter.extensions.ui) // pre-defined ui components
    implementation(libs.fastadapter.extensions.scroll)
    implementation(libs.fastadapter.extensions.utils)
    implementation(libs.itemanimators)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- QR Scanner ------------------------------------------------------------------------- */
    implementation(libs.zxing.android.embedded)
    /** ----------------------------------------------------------------------------------------- */

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

fun appendGdkCommitHash(project: Project, enableGitSubmodule: Boolean): String{
    val gdkCommit = System.getenv("GDK_COMMIT")
    val gdkCommitFile = project.file("gdk/gdk_commit")
    var hash: String? = null

    if (!gdkCommit.isNullOrBlank()) {
        hash = gdkCommit
    } else if (gdkCommitFile.exists()){
        val content = gdkCommitFile.readText().trim()
        hash = content.substring(0, 8.coerceAtMost(content.length))
    } else if (enableGitSubmodule) {
        val cmd = "git --git-dir=gdk/gdk/.git rev-parse --short HEAD"
        val proc = ProcessGroovyMethods.execute(cmd)
        hash = ProcessGroovyMethods.getText(proc).trim()
    }

    return hash?.takeIf { it.isNotBlank() }?.let { "-gdk:${it}" } ?: ""
}

task("verifyDependencies", GradleBuild::class) {
    tasks = listOf("lintDevelopmentRelease", "assembleProductionRelease")
}

// Disable Google Services Plugin for FDroid flavor
afterEvaluate {
    tasks.matching {
        it.name.contains("FDroid") && it.name.contains("GoogleServices")
    }.all {
        enabled = false
    }
}

class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File
) : CommandLineArgumentProvider {

    override fun asArguments(): Iterable<String> {
        return listOf("room.schemaLocation=${schemaDir.path}")
    }
}
