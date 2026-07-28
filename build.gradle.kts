// kanso — a shared Jetpack Compose + Material 3 design system.
// One theme + token set + component library for every app; consumed as a git submodule (a
// :kanso module) or, in future, as a Maven artifact. Per-app brand accent via a seed color;
// dynamic color (Material You) on Android 12+. Apache-2.0.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.spotless)
}

// Formatting is configured at the root and covers both modules, so there is one place that
// decides it and no way for a module to drift. CONTRIBUTING.md used to say "match the file you
// are editing"; this is what replaces that.
//
// The three rule overrides below are where ktlint's defaults and a Compose codebase genuinely
// disagree. Nothing else is turned off, and each one is here rather than in .editorconfig
// because Spotless does not discover that file on its own — the values there are what an editor
// reads while you type, and these are what the build enforces. They are duplicated on purpose
// and must be kept in step; the alternative is a repo where the editor and CI quietly disagree.
val ktlintRules = mapOf(
    // Composable functions are PascalCase by Compose convention — every Google sample and the
    // tooling rely on it. This rule would rename every component in the library.
    "ktlint_standard_function-naming" to "disabled",
    // `val EyeIcon: ImageVector` follows the same convention as Material's own icon properties.
    // The rule wants SCREAMING_SNAKE_CASE for anything immutable, which would make the icons
    // read as constants rather than as the drawables they are.
    "ktlint_standard_property-naming" to "disabled",
    // kanso groups related declarations per file on purpose — Buttons.kt holds KansoButton and
    // KansoButtonStyle. The rule wants one top-level declaration named after its file.
    "ktlint_standard_filename" to "disabled",
    // 100, not ktlint's default. It is what the codebase already wraps at: 14 lines exceeded it
    // when this was introduced and every one was rewrapped rather than the limit raised, because
    // a ceiling set above the code it governs only ratifies drift.
    "max_line_length" to "100",
)

spotless {
    kotlin {
        // src/** only. Generated sources under build/ are not ours to format, and a licence
        // header on generated code would be a claim about authorship that is not true.
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)

        // Every .kt file in the repo already carries this. Enforcing it means a new file cannot
        // quietly ship without one — a licence is only load-bearing if it is actually attached
        // to the source. ${'$'}YEAR is filled in for a new file and left alone on an existing one,
        // so this does not rewrite every header each January.
        licenseHeader(
            """
            /*
             * Copyright ${'$'}YEAR ayanami770
             * Licensed under the Apache License, Version 2.0.
             */
            """.trimIndent() + "\n",
        )
    }

    // Build scripts get the same formatter and no licence header — one on build.gradle.kts
    // would be claiming authorship of Gradle boilerplate.
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}
