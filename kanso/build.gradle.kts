plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.ayanami.kanso"
    compileSdk = 35

    defaultConfig {
        // Low minSdk so the design system never constrains a consumer (Compose Material 3
        // supports 21+). Consumers set their own, higher, minSdk as needed. dynamicColor is
        // already guarded by Build.VERSION >= S at runtime.
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }

    lint {
        // AGP 8.7's bundled lint (UAST) crashes analyzing Compose sources under Kotlin 2.0.x —
        // IncompatibleClassChangeError in NonNullableMutableLiveDataDetector (a lint-vs-Kotlin
        // analysis-API mismatch, not a code issue). Skip the release-blocking lint pass so the
        // design system and every app that consumes it can build release/lintVital cleanly.
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Compose BOM: a single source of truth for every Compose artifact version. Exposed via
    // `api` so consuming apps inherit the same aligned versions from the :kanso submodule.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    api(composeBom)

    api("androidx.compose.material3:material3")
    api("androidx.compose.material3:material3-window-size-class")
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-tooling-preview")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material:material-icons-extended")
    api("androidx.activity:activity-compose:1.9.3")
    api("androidx.navigation:navigation-compose:2.8.4")
    api("androidx.core:core-ktx:1.13.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
