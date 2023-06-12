
import com.adarshr.gradle.testlogger.theme.ThemeType
import com.android.build.gradle.internal.api.*
import org.codehaus.groovy.runtime.ProcessGroovyMethods
import java.io.FileInputStream
import java.util.*

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.devtools.ksp)
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.adarshr.test-logger") version "3.2.0"
}
true // Needed to make the Suppress annotation work for the plugins block

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
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        targetSdk = 33
        versionCode = 408
        versionName = "4.0.8"
        setProperty("archivesBaseName", "BlockstreamGreen-v$versionName")

        testApplicationId = "com.blockstream.green.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            var keystoreFile = (keystoreProperties["storeFile"] as? String)?.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
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

        create("production") {
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
        outputs.forEach { output ->
            (output as ApkVariantOutputImpl).versionCodeOverride = 22000000 + (android.defaultConfig.versionCode ?: 0)
        }
    }

    buildFeatures {
        dataBinding = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("normal")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a") // includes ARM & x86_64 .so files only, so no x86 .so file
            }

            signingConfigs.getByName("release").also {
                if(it.storeFile != null){
                    signingConfig = it
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    lint {
        abortOnError=  false
        disable += listOf("MissingTranslation", "SpUsage")
        ignoreWarnings = false
    }
}

// For KSP, configure using KSP extension:
ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
}

kotlin {
    jvmToolchain(17)
}

// Configure kapt to correct error types by setting correctErrorTypes to true
kapt {
    correctErrorTypes = true
}

testlogger {
    theme = ThemeType.MOCHA
}
val developmentImplementation by configurations
val productionGoogleImplementation by configurations
val productionImplementation by configurations

dependencies {
    /**  --- Modules ---------------------------------------------------------------------------- */
    implementation(project(":base"))
    implementation(project(":hardware"))
    implementation(project(":jade"))
    implementation(project(":crypto"))

    developmentImplementation(project(":gms"))
    productionGoogleImplementation(project(":gms"))
    productionImplementation(project(":no-gms")) // F-Droid
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Navigation ------------------------------------------------------------------------- */
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.navigation.testing)
    androidTestImplementation(libs.androidx.navigation.navigation.testing)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Room ------------------------------------------------------------------------------- */
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.room.testing)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Hilt Dependency Injection  --------------------------------------------------------- */
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    // For instrumentation tests
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)

    // For local unit tests
    testImplementation(libs.google.hilt.android.testing)
    kaptTest(libs.hilt.android.compiler)
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
    implementation(libs.zxing.android.embedded) { isTransitive = false }
    implementation(libs.zxing.core) // API <= 24 compatibility
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Uniform Resources ------------------------------------------------------------------ */
    implementation("com.sparrowwallet:hummingbird:1.6.6")
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Countly ---------------------------------------------------------------------------- */
    implementation(libs.countly.sdk.android)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Beagle  ---------------------------------------------------------------------------- */
    developmentImplementation(libs.beagle.ui.drawer)
    developmentImplementation(libs.beagle.log.crash)

    productionGoogleImplementation(libs.beagle.noop)
    productionGoogleImplementation(libs.beagle.log.crash.noop)

    productionImplementation(libs.beagle.noop)
    productionImplementation(libs.beagle.log.crash.noop)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Slide To Act ----------------------------------------------------------------------- */
    implementation(libs.slidetoact)
    /** ----------------------------------------------------------------------------------------- */

    /**  --- Rive ------------------------------------------------------------------------------- */
    implementation(libs.rive.android)
    /** ----------------------------------------------------------------------------------------- */

    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

fun appendGdkCommitHash(project: Project, enableGitSubmodule: Boolean): String{
    val gdkCommit = System.getenv("GDK_COMMIT")
    val gdkCommitFile = project.file("crypto/gdk_commit")
    var hash: String? = null

    if (gdkCommit != null) {
        hash = gdkCommit
    } else if (gdkCommitFile.exists()){
        val content = gdkCommitFile.readText().trim()
        hash = content.substring(0, Math.min(8, content.length))
    } else if (enableGitSubmodule) {
        val cmd = "git --git-dir=crypto/gdk/.git rev-parse --short HEAD"
        val proc = ProcessGroovyMethods.execute(cmd)
        hash = ProcessGroovyMethods.getText(proc).trim()
    }

    return hash?.let { "-gdk:${it}" } ?: ""
}

task("verifyDependencies", GradleBuild::class) {
    tasks = listOf("lintDevelopmentRelease", "assembleProductionRelease")
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
