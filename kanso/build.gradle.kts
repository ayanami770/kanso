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
    // `platform` publishes a floor, not a ceiling — a consumer that wants a newer Compose
    // declares its own BOM and wins.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    api(composeBom)

    // `api` is reserved for artifacts that appear in kanso's OWN public signatures, so a
    // consumer can name every type kanso hands them without declaring anything extra:
    //   material3    ColorScheme, Typography, Shapes, SnackbarHostState
    //   foundation   PaddingValues, RowScope, ColumnScope, ScrollState
    //   runtime      @Composable, the ProvidableCompositionLocal token holders
    //   ui           Modifier, Alignment
    //   ui-graphics  Color, ImageVector
    //   ui-text      KeyboardType, VisualTransformation, TextAlign
    //   ui-unit      Dp, on every spacing and elevation token
    // Anything not on that list is the consumer's choice to make, not kanso's to impose.
    api("androidx.compose.material3:material3")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.runtime:runtime")
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-text")
    api("androidx.compose.ui:ui-unit")

    // Internal only — WindowCompat, for the edge-to-edge system-bar contrast in KansoTheme.
    // Nothing from core-ktx reaches kanso's public API.
    implementation("androidx.core:core-ktx:1.13.1")

    // The @Preview annotations in component/Previews.kt. `implementation`, not `api`: the
    // previews are kanso's own development tooling and appear in no public signature, so a
    // consumer that wants @Preview declares it themselves.
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // The colour engine is pure Kotlin (no android.graphics), so its contract — every seed
    // yields a WCAG AA scheme — is pinned by plain JVM tests. Deliberately NOT paired with
    // `testOptions.unitTests.isReturnDefaultValues`: stubbed framework calls return zeroes,
    // which would collapse every seed onto one hue and pass against a broken scheme.
    testImplementation("junit:junit:4.13.2")
}
