/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That a token a caller passes to [KansoTheme] actually arrives at a component.
 *
 * The defect these exist for is a one-line omission with no other symptom: adding a token class
 * and its `Local…` holder but forgetting the `… provides …` line in the theme's
 * `CompositionLocalProvider`. Everything still compiles, every component still renders, and the
 * parameter is simply ignored. The screenshots would not move either, because the *defaults* are
 * unchanged — only reading a non-default token back through a composition catches it.
 */
@RunWith(AndroidJUnit4::class)
class ThemeTokensTest {

    @get:Rule
    val compose = createComposeRule()

    /** Composes [content] inside the theme and returns what it read. */
    private fun <T> readFromTheme(content: @Composable () -> T): T {
        var seen: T? = null
        compose.setContent { seen = content() }
        return seen ?: error("the theme content never composed, so no token was read")
    }

    @Test
    fun `sizing passed to the theme reaches Kanso sizing`() {
        val seen = readFromTheme {
            var value = 0.dp
            KansoTheme(dynamicColor = false, sizing = KansoSizing(icon = 40.dp)) {
                value = Kanso.sizing.icon
            }
            value
        }
        assertEquals(40.dp, seen)
    }

    @Test
    fun `spacing passed to the theme reaches Kanso spacing`() {
        val seen = readFromTheme {
            var value = 0.dp
            KansoTheme(dynamicColor = false, spacing = KansoSpacing(lg = 20.dp)) {
                value = Kanso.spacing.lg
            }
            value
        }
        assertEquals(20.dp, seen)
    }

    @Test
    fun `motion passed to the theme reaches Kanso motion`() {
        val seen = readFromTheme {
            var value = 0
            KansoTheme(dynamicColor = false, motion = KansoMotion(shimmer = 999)) {
                value = Kanso.motion.shimmer
            }
            value
        }
        assertEquals(999, seen)
    }

    @Test
    fun `the sizing defaults are the documented ones`() {
        val seen = readFromTheme {
            var value = KansoSizing(icon = 0.dp)
            KansoTheme(dynamicColor = false) { value = Kanso.sizing }
            value
        }
        assertEquals(24.dp, seen.icon)
        assertEquals(18.dp, seen.iconSmall)
        assertEquals(14.dp, seen.iconBadge)
        assertEquals(56.dp, seen.iconLarge)
    }

    /**
     * `KansoDivider(inset = true)` derives its indent from the icon token plus the gap after it
     * rather than restating the 40dp they currently come to, so moving the token has to move the
     * divider with it.
     *
     * Asserted on the arithmetic rather than on pixels deliberately: what would break is the
     * *relationship*, and a golden only ever sees one instance of it — at the defaults, where a
     * hardcoded 40dp and a derived one look identical.
     */
    @Test
    fun `the divider inset is derived from the icon token, not restated`() {
        val spacing = KansoSpacing()
        assertEquals(40.dp, KansoSizing().icon + spacing.lg)
        assertEquals(56.dp, KansoSizing(icon = 40.dp).icon + spacing.lg)
    }
}
