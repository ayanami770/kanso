/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import android.app.Activity
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ayanami.kanso.component.KansoDivider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
     * Measured on the composed divider. An earlier version of this test added the two tokens
     * together and compared the sum with a literal — it never called `KansoDivider`, so it kept
     * passing with the indent hard-coded back to 40dp, which is the one regression it was
     * written for.
     */
    @Test
    fun `the divider inset is derived from the icon token, not restated`() {
        compose.setContent {
            Column {
                KansoTheme(dynamicColor = false) {
                    KansoDivider(Modifier.testTag("flush"))
                    KansoDivider(Modifier.testTag("default"), inset = true)
                }
                KansoTheme(dynamicColor = false, sizing = KansoSizing(icon = 40.dp)) {
                    KansoDivider(Modifier.testTag("wide-icon"), inset = true)
                }
            }
        }

        // The line itself starts where the innermost coordinates do — after the start padding.
        fun lineStart(tag: String): Float = compose.onNodeWithTag(tag).fetchSemanticsNode()
            .layoutInfo.coordinates.positionInRoot().x / compose.density.density
        assertEquals(0f, lineStart("flush"), 0.01f)
        assertEquals(40f, lineStart("default"), 0.01f)
        assertEquals(56f, lineStart("wide-icon"), 0.01f)
    }

    // The three holders the tests above did not read back. Dropping any one of their
    // `provides` lines left every test green: nothing in the library reads `Kanso.brand` or
    // `Kanso.elevation`, and the goldens only ever render the light scheme.

    @Test
    fun `brand passed to the theme reaches Kanso brand`() {
        val brand = KansoBrand("contract", Color(0xFF123456))
        val seen = readFromTheme {
            var value = KansoDefaultBrand
            KansoTheme(brand = brand, dynamicColor = false) { value = Kanso.brand }
            value
        }
        assertEquals(brand, seen)
    }

    @Test
    fun `elevation passed to the theme reaches Kanso elevation`() {
        val seen = readFromTheme {
            var value = 0.dp
            KansoTheme(dynamicColor = false, elevation = KansoElevation(level1 = 9.dp)) {
                value = Kanso.elevation.level1
            }
            value
        }
        assertEquals(9.dp, seen)
    }

    @Test
    fun `extended colours passed to the theme reach Kanso extendedColors`() {
        val custom = KansoLightExtendedColors.copy(success = Color(0xFF00FF00))
        val seen = readFromTheme {
            var value = KansoLightExtendedColors
            KansoTheme(dynamicColor = false, extendedColors = custom) {
                value = Kanso.extendedColors
            }
            value
        }
        assertEquals(custom, seen)
    }

    /** With none passed, a dark theme must not fall back to the light set's holder default. */
    @Test
    fun `a dark theme provides the dark extended colours`() {
        val seen = readFromTheme {
            var value = KansoLightExtendedColors
            KansoTheme(darkTheme = true, dynamicColor = false) { value = Kanso.extendedColors }
            value
        }
        assertEquals(KansoDarkExtendedColors, seen)
    }

    // ---- system bars ----------------------------------------------------------------

    /** Composes [content] and reports whether the window ended up with dark status-bar icons. */
    private fun lightStatusBarsAfter(content: @Composable () -> Unit): Boolean {
        var activity: Activity? = null
        compose.setContent {
            activity = LocalContext.current as Activity
            content()
        }
        compose.waitForIdle()
        val window = activity!!.window
        return WindowCompat.getInsetsController(
            window,
            window.decorView,
        ).isAppearanceLightStatusBars
    }

    /**
     * A dark island on a light screen must not turn the window's bar icons light. The nested
     * theme's side effect used to run after its parent's, so the last theme composed decided
     * for the whole window.
     */
    @Test
    fun `a nested theme leaves the window's bars to the outermost one`() {
        val light = lightStatusBarsAfter {
            KansoTheme(darkTheme = false, dynamicColor = false) {
                KansoTheme(darkTheme = true, dynamicColor = false) {}
            }
        }
        assertTrue("a light screen keeps dark status-bar icons", light)
    }

    /** `LocalContext` wrapped in a ContextThemeWrapper is still a screen in an activity. */
    @Test
    fun `the bars follow the theme through a wrapped context`() {
        val light = lightStatusBarsAfter {
            val base = LocalContext.current
            val wrapped = remember(base) { ContextThemeWrapper(base, 0) }
            CompositionLocalProvider(LocalContext provides wrapped) {
                KansoTheme(darkTheme = false, dynamicColor = false) {}
            }
        }
        assertTrue("the activity is found behind the wrapper", light)
    }
}
