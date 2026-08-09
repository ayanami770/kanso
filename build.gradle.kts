import org.jetbrains.dokka.gradle.DokkaExtension

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
    alias(libs.plugins.dokka)
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

// API documentation for :kanso. The KDoc in that library carries design rationale an adopter
// needs — why the badge requires text, why the touch-target floor is not a token, what
// `dynamicColor = true` costs a brand — and none of it is visible to someone consuming the
// compiled module. This is what makes it readable without cloning the repo.
//
// Configured from HERE rather than from kanso/build.gradle.kts because that file is evaluated by
// every consuming build (kanso is used as a git submodule), which would make each of them
// resolve the entire Dokka toolchain to produce docs they never read — and pin all ~20 artifacts
// under their own dependency verification before their build would configure. This script is
// kanso's alone, so none of that reaches a consumer.
project(":kanso") {
    apply(plugin = "org.jetbrains.dokka")

    extensions.configure<DokkaExtension> {
        moduleName.set("kanso")

        dokkaSourceSets.configureEach {
            // Only the release variant. Documenting debug as well would publish two copies of
            // every symbol, differing in nothing an adopter can act on.
            suppressGeneratedFiles.set(true)
            includes.from("docs/module.md")

            // Every documented symbol links to the line it is declared on. The rationale in
            // these comments is often longer than the code it describes, and being one click
            // from the source is what stops the docs becoming a second, drifting account of
            // the library.
            sourceLink {
                localDirectory.set(file("src/main/java"))
                remoteUrl("https://github.com/ayanami770/kanso/blob/main/kanso/src/main/java")
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks.register("androidx") {
                url("https://developer.android.com/reference/kotlin/")
                packageListUrl(
                    "https://developer.android.com/reference/kotlin/androidx/package-list",
                )
            }
        }

        dokkaPublications.configureEach {
            // A KDoc link that no longer resolves is a defect, not a warning: it renders as
            // plain text, so the only person who finds out is the reader who needed it. One
            // such link (`[KansoContentContainer]`, referenced from another package without
            // qualification) was already in the tree when this was switched on.
            failOnWarning.set(true)
        }
    }

    // `failOnWarning` above catches a KDoc reference Dokka cannot resolve at all. It does NOT
    // catch the other way a link dies: a reference that resolves to a symbol with no published
    // page — anything `internal`, and a data class's constructor parameters. Dokka emits those
    // silently as `data-unresolved-link`, so they render as plain text and the only person who
    // finds out is the reader who needed them. There were five in the tree when this was
    // switched on.
    //
    // Reading the generated HTML back is unglamorous and is the only thing that actually sees
    // them.
    tasks.register("checkDokkaLinks") {
        description = "Fails if the generated API docs contain a link that renders as plain text."
        group = "verification"

        dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
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
}
