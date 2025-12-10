import java.net.URI

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
    ":domain"
)

// Determine if the build is running in a CI environment
// GitLab CI automatically sets the "CI" environment variable to "true"
val isCi = System.getenv("CI") == "true"

buildCache {
    local {
        isEnabled = true
        // removeUnusedEntriesAfter = Days.days(7)
    }

    if (isCi) {
        val gitlabApiV4Url = System.getenv("CI_API_V4_URL")
        val gitlabProjectId = System.getenv("CI_PROJECT_ID")
        val gitlabJobToken = System.getenv("CI_JOB_TOKEN")

        if (gitlabApiV4Url.isNullOrBlank() || gitlabProjectId.isNullOrBlank() || gitlabJobToken.isNullOrBlank()) {
            println("WARN: GitLab Remote Build Cache environment variables (CI_API_V4_URL, CI_PROJECT_ID, CI_JOB_TOKEN) are not fully set. Remote cache will be disabled for this build.")
        } else {
            println("INFO: Configuring GitLab Remote Build Cache.")
            remote(HttpBuildCache::class) {
                url = URI("$gitlabApiV4Url/projects/$gitlabProjectId/packages/generic/gradle-build-cache/v1")
                credentials {
                    username = "gitlab-ci-token"
                    password = gitlabJobToken
                }
                isPush = true
            }
        }
    }
}

gradle.settingsEvaluated {
    val localCacheStatus = if (settings.buildCache.local.isEnabled) "ENABLED" else "DISABLED"
    var remoteCacheInfo = "DISABLED" // Declare remoteCacheInfo

    val remoteCacheConfig = settings.buildCache.remote
    if (remoteCacheConfig != null && remoteCacheConfig.isEnabled) {
        if (remoteCacheConfig is HttpBuildCache) {
            remoteCacheInfo = "ENABLED (URL: ${remoteCacheConfig.url}, Push: ${remoteCacheConfig.isPush})" // Assign to remoteCacheInfo
        } else {
            remoteCacheInfo = "ENABLED (Type: ${remoteCacheConfig::class.simpleName}, Push: ${remoteCacheConfig.isPush})" // Assign to remoteCacheInfo
        }
    }
    
    println("""
    ============================================================
    Gradle Build Cache Status:
      Local Cache:  $localCacheStatus
      Remote Cache: $remoteCacheInfo 
    ============================================================
    """.trimIndent()) // Print remoteCacheInfo
}