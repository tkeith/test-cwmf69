// Android Gradle Plugin Version: 8.1.0
// Kotlin Gradle Plugin Version: 1.8.20
// Hilt Gradle Plugin Version: 2.47

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.47")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.8.20" apply false
    kotlin("android") version "1.8.20" apply false
    kotlin("kapt") version "1.8.20" apply false
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("com.google.dagger.hilt.android") version "2.47" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
                "-Xexplicit-api=strict"
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
}

extra.apply {
    set("compileSdk", 34)
    set("targetSdk", 34)
    set("minSdk", 24)
    
    // Core dependencies versions
    set("kotlinVersion", "1.8.20")
    set("coreKtxVersion", "1.10.1")
    set("appCompatVersion", "1.6.1")
    set("materialVersion", "1.9.0")
    
    // Architecture components versions
    set("lifecycleVersion", "2.6.1")
    set("navigationVersion", "2.7.0")
    set("roomVersion", "2.5.2")
    set("hiltVersion", "2.47")
    
    // Testing versions
    set("junitVersion", "4.13.2")
    set("androidJunitVersion", "1.1.5")
    set("espressoVersion", "3.5.1")
    set("mockitoVersion", "5.4.0")
    
    // Network versions
    set("retrofitVersion", "2.9.0")
    set("okhttpVersion", "4.11.0")
    
    // Firebase versions
    set("firebaseBomVersion", "32.2.2")
}