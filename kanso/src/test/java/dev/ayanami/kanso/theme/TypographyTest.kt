/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [kansoTypography]'s contract: it changes the family and *only* the family. A brand font that
 * quietly rescales the type system would undo the reason a shared type scale exists.
 */
class TypographyTest {

    private val brand = FontFamily.Monospace
    private val display = FontFamily.Cursive

    /** The fifteen public Material 3 styles, paired with the default they must otherwise match. */
    private fun styles(t: Typography): Map<String, TextStyle> = mapOf(
        "displayLarge" to t.displayLarge,
        "displayMedium" to t.displayMedium,
        "displaySmall" to t.displaySmall,
        "headlineLarge" to t.headlineLarge,
        "headlineMedium" to t.headlineMedium,
        "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge,
        "titleMedium" to t.titleMedium,
        "titleSmall" to t.titleSmall,
        "bodyLarge" to t.bodyLarge,
        "bodyMedium" to t.bodyMedium,
        "bodySmall" to t.bodySmall,
        "labelLarge" to t.labelLarge,
        "labelMedium" to t.labelMedium,
        "labelSmall" to t.labelSmall,
    )

    @Test
    fun `every public style carries the requested family`() {
        styles(kansoTypography(brand)).forEach { (name, style) ->
            assertEquals("$name kept the default family", brand, style.fontFamily)
        }
    }

    /**
     * The whole point of building on `Typography()` rather than hand-writing the scale: sizes,
     * weights, line heights and letter spacing must come through untouched.
     */
    @Test
    fun `nothing but the family changes`() {
        val defaults = styles(Typography())
        styles(kansoTypography(brand)).forEach { (name, style) ->
            val default = defaults.getValue(name)
            assertEquals("$name fontSize", default.fontSize, style.fontSize)
            assertEquals("$name fontWeight", default.fontWeight, style.fontWeight)
            assertEquals("$name lineHeight", default.lineHeight, style.lineHeight)
            assertEquals("$name letterSpacing", default.letterSpacing, style.letterSpacing)
            // Belt and braces: identical once the family is put back.
            assertEquals(name, default, style.copy(fontFamily = default.fontFamily))
        }
    }

    @Test
    fun `a separate display family covers display and headline only`() {
        val t = kansoTypography(bodyFamily = brand, displayFamily = display)
        styles(t).forEach { (name, style) ->
            val expected = if (name.startsWith("display") || name.startsWith("headline")) {
                display
            } else {
                brand
            }
            assertEquals(name, expected, style.fontFamily)
        }
    }

    @Test
    fun `display family defaults to the body family`() {
        assertEquals(styles(kansoTypography(brand)), styles(kansoTypography(brand, brand)))
    }
}
