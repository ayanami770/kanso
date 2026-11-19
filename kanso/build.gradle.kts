import com.android.build.api.variant.HostTestBuilder

plugins {
    // By id and without versions, on purpose. kanso is consumed as a git submodule, so THIS file
    // is evaluated by the consuming build — where `libs` is the consumer's version catalog, not
    // kanso's, and an alias the consumer has not mirrored is a script compilation error. The
    // host build is a Compose app, so it already has the Android Gradle plugin and the Compose
    // compiler plugin on its classpath, and that is where their versions come from.
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

// kanso's dependency versions come from kanso's own catalog, gradle/libs.versions.toml. They are
// looked up by catalog NAME rather than through the generated `libs.…` accessors, for the same
// reason as above: an accessor for an alias the consuming build does not define fails to compile,
// which is how adopting kanso used to mean mirroring two dozen aliases by hand and keeping them
// in step with every kanso update.
//
// A consumer exposes this catalog as `kansoLibs` with one block in settings.gradle.kts (README,
// "Installing"). Inside kanso's own build it is simply `libs`, and a consumer that already
// mirrors the aliases in its own `libs` keeps working through the same fallback.
val kansoCatalog: VersionCatalog =
    extensions.getByType<VersionCatalogsExtension>().let { catalogs ->
        catalogs.find("kansoLibs").orElseGet {
            catalogs.find("libs").orElseThrow {
                GradleException(
                    "kanso: no version catalog to read its dependency versions from. Add this " +
                        "to settings.gradle.kts, inside dependencyResolutionManagement { }:\n" +
                        "    versionCatalogs { create(\"kansoLibs\") { " +
                        "from(files(\"<path to kanso>/gradle/libs.versions.toml\")) } }",
                )
            }
        }
    }

fun kansoLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    kansoCatalog.findLibrary(alias).orElseThrow {
        GradleException(
            "kanso: version catalog '${kansoCatalog.name}' has no library '$alias'. " +
                "Expose kanso's own catalog as `kansoLibs` (README, \"Installing\") " +
                "instead of mirroring its aliases by hand.",
        )
    }

// Set by kanso's own root script, and by nobody else's — see the note above `dependencies`.
val kansoOwnBuild: Boolean = extra.has("kansoOwnBuild")

android {
    namespace = "dev.ayanami.kanso"
    // 37: androidx.core 1.19.0 (Dependabot, 9c7740d) publishes AAR metadata requiring every
    // dependent to compile against android-37, and checkDebugUnitTestAarMetadata enforces it —
    // main has been red on exactly that since 08:37 on 2026-08-09. Raising compileSdk is what
    // that metadata asks for; minSdk stays 24, so the range of devices kanso supports does not
    // move. Consumers inherit the requirement: kanso's own AAR metadata now asks THEM for 37
    // too, so every app embedding this submodule needs the same bump.
    compileSdk = 37

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

        // Roadmap 3.1 deferred this "until CI exists and the real noise level is known". Both
        // conditions are now met, and the measured noise on :kanso is exactly one category: a
        // newer version of something exists. That is Dependabot's job and it would turn every
        // upstream release into a red build here, so it is the one thing excluded.
        //
        // TWO ids, not one. GradleDependency covers the declared dependencies;
        // AndroidGradlePluginVersion covers the Gradle distribution named in
        // gradle-wrapper.properties and says the same kind of thing about it — "A newer version
        // of Gradle than 9.6.1 is available: 9.7.0". It was invisible when the line below was
        // first written, because 9.6.1 was current then; it turned the build red the day 9.7.0
        // shipped, which is exactly the failure this exclusion exists to prevent. Same category,
        // same treatment.
        //
        // What this buys: most compose-lints rules report at warning severity, so without it
        // they would be a report rather than a gate — `ComposeModifierWithoutDefault` happened
        // to be an error and caught a real defect, but `ModifierFactoryExtensionFunction` and
        // the rest would have gone through silently.
        warningsAsErrors = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion")
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

androidComponents {
    beforeVariants { variant ->
        // kanso's tests need Robolectric, Roborazzi and the Compose test artifacts, which only
        // kanso's own root script declares. In a consuming build they are absent by design, so
        // the unit-test component is switched off there rather than left to fail at compile time
        // the first time someone runs `./gradlew test`.
        variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = kansoOwnBuild
    }
}

// Everything kanso needs only to build, lint and test ITSELF — Dokka, Roborazzi, Robolectric,
// the Compose test artifacts, Slack's compose-lints, the preview tooling — is configured for this
// module from the ROOT build script, not from here. kanso is consumed as a git submodule, which
// means THIS file is evaluated by the consuming build: declaring Dokka in the plugins block above
// made every consumer resolve its whole toolchain (jackson, woodstox, kotlinx, ~20 artifacts) to
// produce documentation none of them read — and under Gradle dependency verification each
// artifact then has to be pinned in the consumer's own verification-metadata.xml before their
// build will configure at all. Roborazzi and the test dependencies were the same cost in the
// same place, and the two `debugImplementation` artifacts went further: they put two exported
// activities into every consumer's debug APK. The root script is never evaluated by a consumer,
// so what lives there costs them nothing and needs no opt-out on their side.

dependencies {
    // Compose BOM: a single source of truth for every Compose artifact version. Exposed via
    // `api` so consuming apps inherit the same aligned versions from the :kanso submodule.
    // `platform` publishes a floor, not a ceiling — a consumer that wants a newer Compose
    // declares its own BOM and wins.
    api(platform(kansoLibrary("compose-bom")))

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
    api(kansoLibrary("compose-material3"))
    api(kansoLibrary("compose-foundation"))
    api(kansoLibrary("compose-runtime"))
    api(kansoLibrary("compose-ui"))
    api(kansoLibrary("compose-ui-graphics"))
    api(kansoLibrary("compose-ui-text"))
    api(kansoLibrary("compose-ui-unit"))
    // Easing is on KansoMotion, so it is part of the public surface.
    api(kansoLibrary("compose-animation-core"))

    // Internal only — WindowCompat, for the edge-to-edge system-bar contrast in KansoTheme.
    // Nothing from core-ktx reaches kanso's public API.
    implementation(kansoLibrary("androidx-core-ktx"))

    // The @Preview annotations in component/Previews.kt. `implementation`, not `api`: the
    // previews are kanso's own development tooling and appear in no public signature, so a
    // consumer that wants @Preview declares it themselves.
    implementation(kansoLibrary("compose-ui-tooling-preview"))
}
