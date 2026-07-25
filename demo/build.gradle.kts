plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    // The design system — theme, tokens, components. The aligned Compose BOM and the Compose
    // artifacts in kanso's public API (material3, foundation, runtime, ui, ui-graphics,
    // ui-text, ui-unit) come with it.
    implementation(project(":kanso"))

    // Everything below is the DEMO's own choice, not something kanso imposes. That is the
    // point of the split: an app picks its own activity plumbing and its own icon set, and a
    // design system has no business deciding either. The gallery happens to want both.
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material.icons.extended)

    debugImplementation(libs.compose.ui.tooling)
}
