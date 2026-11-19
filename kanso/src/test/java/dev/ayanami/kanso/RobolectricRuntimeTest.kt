/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * That the Android runtime these tests run against is the one Gradle resolved.
 *
 * Robolectric will fetch that jar itself if it is allowed to, with its own Maven client and no
 * checksum to hold it to. The build tells it to work offline against the copy Gradle resolved
 * and verified instead; this fails if that wiring is ever dropped, because nothing else would —
 * the tests would simply go back to downloading what they run on.
 */
class RobolectricRuntimeTest {

    @Test
    fun `robolectric is offline and pointed at the runtime gradle resolved`() {
        assertEquals("true", System.getProperty("robolectric.offline"))

        val directory = File(System.getProperty("robolectric.dependency.dir").orEmpty())
        val runtimes = directory.listFiles().orEmpty().filter { file ->
            file.name.startsWith("android-all-instrumented-") && file.extension == "jar"
        }
        assertEquals("exactly one runtime jar in $directory", 1, runtimes.size)
    }
}
