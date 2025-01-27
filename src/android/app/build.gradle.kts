plugins {
    id("com.android.application")
    // Kotlin plugin v1.8.20
    id("org.jetbrains.kotlin.android") version "1.8.20"
    // Kotlin annotation processing v1.8.20
    id("org.jetbrains.kotlin.kapt") version "1.8.20"
    // Hilt dependency injection
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.delayedmessaging.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.delayedmessaging.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Signing configuration will be provided by CI/CD pipeline
            // or local properties file
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Kotlin Standard Library and Coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // AndroidX Core Libraries
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.5.2")

    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:2.47")
    kapt("com.google.dagger:hilt-android-compiler:2.47")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("io.socket:socket.io-client:2.1.0")

    // Testing Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.4.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}

// Configure kapt for Hilt
kapt {
    correctErrorTypes = true
    useBuildCache = true
}