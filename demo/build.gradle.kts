plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.ayanami.kanso.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.ayanami.kanso.demo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // The whole design system — theme, tokens, components — inherited transitively (Compose
    // BOM, material3, navigation, icons) from :kanso's `api` dependencies.
    implementation(project(":kanso"))
}
