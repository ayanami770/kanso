/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The one number in the README that a consumer's build has to match.
 *
 * `compileSdk` reaches every app through AAR metadata: raise it here and an app that has not
 * followed stops building. It was raised once with the README and the changelog both left
 * saying the old value, so the only place the requirement was written down was the error
 * message. Unit tests run with the module directory as their working directory, which is what
 * the relative paths below rely on.
 */
class ReadmeContractTest {

    private val compileSdk: String =
        Regex("""compileSdk = (\d+)""").find(File("build.gradle.kts").readText())!!.groupValues[1]

    @Test
    fun `the README states the compileSdk the build uses`() {
        val readme = File("../README.md").readText()
        assertTrue(
            "README.md's compatibility table must say compileSdk $compileSdk",
            readme.contains("| `compileSdk` | $compileSdk "),
        )
    }

    @Test
    fun `the changelog records the move to the current compileSdk`() {
        val changelog = File("../CHANGELOG.md").readText()
        assertTrue(
            "CHANGELOG.md must carry an entry for the move to compileSdk $compileSdk",
            Regex("""`compileSdk` \d+ → $compileSdk""").containsMatchIn(changelog),
        )
    }
}
