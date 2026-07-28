plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.dokka)
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

// API documentation. The KDoc in this library carries design rationale an adopter needs — why
// the badge requires text, why the touch-target floor is not a token, what `dynamicColor = true`
// costs a brand — and none of it is visible to someone consuming the compiled module. This is
// what makes it readable without cloning the repo.
dokka {
    moduleName.set("kanso")

    dokkaSourceSets.configureEach {
        // Only the release variant. Documenting debug as well would publish two copies of every
        // symbol, differing in nothing an adopter can act on.
        suppressGeneratedFiles.set(true)
        includes.from("docs/module.md")

        // Every documented symbol links to the line it is declared on. The rationale in these
        // comments is often longer than the code it describes, and being one click from the
        // source is what stops the docs becoming a second, drifting account of the library.
        sourceLink {
            localDirectory.set(file("src/main/java"))
            remoteUrl("https://github.com/ayanami770/kanso/blob/main/kanso/src/main/java")
            remoteLineSuffix.set("#L")
        }

        externalDocumentationLinks.register("androidx") {
            url("https://developer.android.com/reference/kotlin/")
            packageListUrl("https://developer.android.com/reference/kotlin/androidx/package-list")
        }
    }

    dokkaPublications.configureEach {
        // A KDoc link that no longer resolves is a defect, not a warning: it renders as plain
        // text, so the only person who finds out is the reader who needed it. One such link
        // (`[KansoContentContainer]`, referenced from another package without qualification)
        // was already in the tree when this was switched on.
        failOnWarning.set(true)
    }
}

// `failOnWarning` above catches a KDoc reference Dokka cannot resolve at all. It does NOT catch
// the other way a link dies: a reference that resolves to a symbol with no published page —
// anything `internal`, and a data class's constructor parameters. Dokka emits those silently as
// `data-unresolved-link`, so they render as plain text and the only person who finds out is the
// reader who needed them. There were five in the tree when this was switched on.
//
// Reading the generated HTML back is unglamorous and is the only thing that actually sees them.
val checkDokkaLinks = tasks.register("checkDokkaLinks") {
    description = "Fails if the generated API docs contain a link that renders as plain text."
    group = "verification"

    val generate = tasks.named("dokkaGeneratePublicationHtml")
    dependsOn(generate)
    val htmlDir = layout.buildDirectory.dir("dokka/html")
    inputs.dir(htmlDir)

    doLast {
        val dead = htmlDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "html" }
            .flatMap { file ->
                Regex("""data-unresolved-link="([^"]*)"""")
                    .findAll(file.readText())
                    .map { "${file.name}: ${it.groupValues[1]}" }
            }
            .toList()
        if (dead.isNotEmpty()) {
            error(
                dead.joinToString(
                    prefix = "${dead.size} KDoc link(s) render as plain text:\n  ",
                    separator = "\n  ",
                    postfix = "\n\nA link resolves but has no page when it points at an " +
                        "`internal` symbol or at a data class's constructor parameter. " +
                        "Use backticks, or link the property instead.",
                ),
            )
        }
    }
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
    // Easing is on KansoMotion, so it is part of the public surface.
    api(libs.compose.animation.core)

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
