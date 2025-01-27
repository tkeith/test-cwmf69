// Gradle Settings API v8.1.0
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.resolve.DependencyResolutionManagement
import org.gradle.plugin.management.PluginManagementSpec

// Project name configuration
rootProject.name = "delayed-messaging"

// Plugin management configuration
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    
    // Android plugin resolution strategy
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:8.1.0")
            }
        }
    }
}

// Dependency resolution management
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    
    // Version catalog configuration
    versionCatalogs {
        create("libs") {
            from(files("gradle/version-catalog.toml"))
        }
    }
}

// Build cache configuration
buildCache {
    local {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 7
    }
    
    remote<HttpBuildCache> {
        isEnabled = true
        url = uri("https://build-cache.example.com")
        credentials {
            username = providers.gradleProperty("GRADLE_CACHE_USERNAME").orNull
            password = providers.gradleProperty("GRADLE_CACHE_PASSWORD").orNull
        }
    }
}

// Include application module
include(":app")

// Build scan configuration
gradle.buildFinished {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}