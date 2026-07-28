plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "dev.ayanami.kanso"
    compileSdk = 36

    defaultConfig {
        // Low minSdk so the design system never constrains a consumer (Compose Material 3
        // supports 21+). Consumers set their own, higher, minSdk as needed. dynamicColor is
        // already guarded by Build.VERSION >= S at runtime.
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }

    lint {
        // Re-enabled: the crash that forced `checkReleaseBuilds = false` was an AGP 8.7 lint
        // (UAST) vs Kotlin 2.0 analysis-API mismatch, and the toolchain has moved past it.
        // Lint matters more for a library than an app -- it is the consumer-facing gate that
        // catches an unguarded API-level call against the advertised minSdk 24, and a consumer
        // cannot lint kanso's compiled code themselves.
        abortOnError = true
    }

    testOptions {
        // Compose UI tests need real resources; Robolectric supplies the Android runtime so
        // these still run on the JVM, with no emulator.
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// AGP 9 removed the `android.kotlinOptions` block; jvmTarget moves to the Kotlin plugin.
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }

    // Everything is public by Kotlin default, so an internal helper can drift into the public
    // contract by accident and nobody notices until a consumer depends on it. Explicit API mode
    // makes every symbol a deliberate choice and requires a declared return type on each one.
    explicitApi()
}

dependencies {
    // Compose BOM: a single source of truth for every Compose artifact version. Exposed via
    // `api` so consuming apps inherit the same aligned versions from the :kanso submodule.
    // `platform` publishes a floor, not a ceiling — a consumer that wants a newer Compose
    // declares its own BOM and wins.
    val composeBom = platform(libs.compose.bom)
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
    api(libs.compose.material3)
    api(libs.compose.foundation)
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.text)
    api(libs.compose.ui.unit)

    // Internal only — WindowCompat, for the edge-to-edge system-bar contrast in KansoTheme.
    // Nothing from core-ktx reaches kanso's public API.
    implementation(libs.androidx.core.ktx)

    // The @Preview annotations in component/Previews.kt. `implementation`, not `api`: the
    // previews are kanso's own development tooling and appear in no public signature, so a
    // consumer that wants @Preview declares it themselves.
    implementation(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)

    // The colour engine is pure Kotlin (no android.graphics), so its contract — every seed
    // yields a WCAG AA scheme — is pinned by plain JVM tests. Deliberately NOT paired with
    // `testOptions.unitTests.isReturnDefaultValues`: stubbed framework calls return zeroes,
    // which would collapse every seed onto one hue and pass against a broken scheme.
    testImplementation(libs.junit)

    // Behaviour tests for the contracts that live in a single expression and would otherwise
    // only be checked by reading the code: that `loading` really swallows a click, that
    // errorText really wins over supporting. Robolectric keeps them on the JVM.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    // Screenshot goldens, scoped to the text-layout-sensitive components at the two font
    // scales that actually break them. Roborazzi rather than Paparazzi (which cannot run
    // interaction tests) or AGP's screenshotTest (still alpha, and it renders only @Preview
    // functions): it shares the Robolectric runtime, so goldens, behaviour tests and the pure
    // colour tests all run in one invocation with no emulator.
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
