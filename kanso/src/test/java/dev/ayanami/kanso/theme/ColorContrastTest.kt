/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The contract for the kanso colour engine: **any** seed produces a scheme whose text pairs
 * meet WCAG AA. kanso accepts an arbitrary brand seed, so a guarantee that holds only for the
 * hues someone happened to try is not a guarantee — every assertion here sweeps the full hue
 * circle.
 *
 * These are plain JVM tests. The engine deliberately contains no `android.graphics` call, so
 * no Robolectric and no emulator is needed. Do NOT "fix" a failure here by turning on
 * `unitTests.isReturnDefaultValues`: stubbed framework calls return zeroes, which collapses
 * every seed onto one hue and turns this suite green against a broken scheme.
 */
class ColorContrastTest {

    /** WCAG 2.x minimum for normal-size text. */
    private val aa = 4.5

    /** WCAG 2.x minimum for large text and UI component boundaries. */
    private val aaLarge = 3.0

    // ---- contrast ---------------------------------------------------------------------

    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val u = c.toDouble()
            return if (u <= 0.03928) u / 12.92 else ((u + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** A seed at [hue] with enough chroma to exercise the coloured (non-neutral) path. */
    private fun seedAt(hue: Int, chroma: Double = 45.0, tone: Int = 45): Color =
        toneColor(hue.toDouble(), chroma, tone)

    private fun everyHue(step: Int = 1, block: (Int, Color) -> Unit) {
        var hue = 0
        while (hue < 360) {
            block(hue, seedAt(hue))
            hue += step
        }
    }

    /** The (background, foreground) pairs a consumer actually renders text with. */
    private fun textPairs(s: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("primary/onPrimary", s.primary, s.onPrimary),
        Triple("primaryContainer/onPrimaryContainer", s.primaryContainer, s.onPrimaryContainer),
        Triple("secondary/onSecondary", s.secondary, s.onSecondary),
        Triple(
            "secondaryContainer/onSecondaryContainer",
            s.secondaryContainer,
            s.onSecondaryContainer,
        ),
        Triple("tertiary/onTertiary", s.tertiary, s.onTertiary),
        Triple("tertiaryContainer/onTertiaryContainer", s.tertiaryContainer, s.onTertiaryContainer),
        Triple("background/onBackground", s.background, s.onBackground),
        Triple("surface/onSurface", s.surface, s.onSurface),
        Triple("surfaceVariant/onSurfaceVariant", s.surfaceVariant, s.onSurfaceVariant),
        Triple("error/onError", s.error, s.onError),
        Triple("errorContainer/onErrorContainer", s.errorContainer, s.onErrorContainer),
        Triple("inverseSurface/inverseOnSurface", s.inverseSurface, s.inverseOnSurface),
        // Not an on-pair, but the one kanso paints most: KansoCard and KansoSectionHeader
        // both draw their title in `primary` directly on `surface`.
        Triple("surface/primary", s.surface, s.primary),
    )

    private fun assertPairsMeetAa(label: String, scheme: (Color) -> ColorScheme) {
        val failures = mutableListOf<String>()
        everyHue { hue, seed ->
            textPairs(scheme(seed)).forEach { (name, background, foreground) ->
                val ratio = contrast(background, foreground)
                if (ratio < aa) {
                    failures += "$label hue $hue $name = ${"%.2f".format(ratio)}"
                }
            }
        }
        assertTrue(
            "${failures.size} pair(s) below WCAG AA ($aa:1). First 10:\n" +
                failures.take(10).joinToString("\n"),
            failures.isEmpty(),
        )
    }

    @Test
    fun `light scheme text pairs meet AA at every hue`() {
        assertPairsMeetAa("light") { kansoLightColorScheme(it) }
    }

    @Test
    fun `dark scheme text pairs meet AA at every hue`() {
        assertPairsMeetAa("dark") { kansoDarkColorScheme(it) }
    }

    @Test
    fun `outline meets the non-text minimum against its surface at every hue`() {
        everyHue { hue, seed ->
            listOf(
                "light" to kansoLightColorScheme(seed),
                "dark" to kansoDarkColorScheme(seed),
            ).forEach { (label, scheme) ->
                val ratio = contrast(scheme.surface, scheme.outline)
                assertTrue(
                    "$label hue $hue outline/surface = ${"%.2f".format(ratio)}",
                    ratio >= aaLarge,
                )
            }
        }
    }

    /**
     * The regression that started this: kanso's own default teal rendered Filled button text
     * at 3.60:1, and three of the four shipped brands were sub-AA.
     */
    @Test
    fun `every shipped brand seed meets AA`() {
        val brands = listOf(
            "kanso" to KansoDefaultBrand.seed,
            "LMSA" to Color(0xFF006A60),
            "CertWatch" to Color(0xFF3F5AA6),
            "Semicon" to Color(0xFF8A4F00),
            "medcal" to Color(0xFF386A20),
        )
        brands.forEach { (name, seed) ->
            listOf(
                "light" to kansoLightColorScheme(seed),
                "dark" to kansoDarkColorScheme(seed),
            ).forEach { (mode, scheme) ->
                textPairs(scheme).forEach { (pair, background, foreground) ->
                    val ratio = contrast(background, foreground)
                    assertTrue(
                        "$name ($mode) $pair = ${"%.2f".format(ratio)}",
                        ratio >= aa,
                    )
                }
            }
        }
    }

    /**
     * Contrast must come out the same whatever the hue — that is the whole reason tone maps
     * to L* rather than to HSL lightness. The old engine spread this pair over 3.17..9.79.
     */
    @Test
    fun `primary contrast is hue-independent`() {
        val ratios = (0 until 360).map { hue ->
            val scheme = kansoLightColorScheme(seedAt(hue))
            contrast(scheme.primary, scheme.onPrimary)
        }
        val spread = ratios.max() - ratios.min()
        assertTrue(
            "onPrimary/primary varies by $spread across hues (min ${ratios.min()}, " +
                "max ${ratios.max()})",
            spread < 0.25,
        )
    }

    // ---- seed handling ----------------------------------------------------------------

    /**
     * A grey/black/white seed has no meaningful hue. The old engine read it as hue 0 and
     * produced a maroon scheme; it must produce a neutral one.
     */
    @Test
    fun `achromatic seeds produce a neutral scheme`() {
        listOf(Color(0xFF808080), Color.Black, Color.White, Color(0xFF2B2B2B)).forEach { seed ->
            val primary = kansoLightColorScheme(seed).primary
            val spread = maxOf(primary.red, primary.green, primary.blue) -
                minOf(primary.red, primary.green, primary.blue)
            assertTrue(
                "seed $seed produced a coloured primary $primary (channel spread $spread)",
                spread < 0.02f,
            )
        }
    }

    /** A muted seed and a vivid seed must not collapse into the same scheme. */
    @Test
    fun `seed chroma is preserved`() {
        // Real sRGB seeds, not colours built with toneColor — that gamut-clips on the way in,
        // which would hand both sides nearly the same chroma and make this assert nothing.
        val mutedChroma = kansoLightColorScheme(Color(0xFF44506E)).primary.toLch().chroma
        val vividChroma = kansoLightColorScheme(Color(0xFF1A3FE0)).primary.toLch().chroma
        assertTrue(
            "a muted navy ($mutedChroma) and a vivid blue ($vividChroma) produced the same " +
                "chroma — the seed's own saturation was normalised away",
            vividChroma - mutedChroma > 20.0,
        )
    }

    // ---- the tone system itself -------------------------------------------------------

    /** Gamut mapping may only reduce chroma; the requested tone must survive it exactly. */
    @Test
    fun `gamut mapping preserves tone`() {
        listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100).forEach { tone ->
            (0 until 360 step 5).forEach { hue ->
                // Deliberately far outside the sRGB gamut, to force the chroma search.
                val actual = toneColor(hue.toDouble(), 150.0, tone).toLch().lightness
                // Compose packs an sRGB Color at 8 bits per channel, so a round trip carries
                // that quantisation with it — measured worst case is 0.43 of a tone.
                assertEquals(
                    "tone $tone at hue $hue came back as $actual",
                    tone.toDouble(),
                    actual,
                    0.75,
                )
            }
        }
    }

    @Test
    fun `tone 0 is black and tone 100 is white`() {
        (0 until 360 step 30).forEach { hue ->
            val black = toneColor(hue.toDouble(), 40.0, 0)
            val white = toneColor(hue.toDouble(), 40.0, 100)
            assertTrue("tone 0 at hue $hue was $black", luminance(black) < 0.001)
            assertTrue("tone 100 at hue $hue was $white", luminance(white) > 0.99)
        }
    }

    /**
     * Gamut mapping moves along the chroma axis only, so a colour's hue must survive a
     * decompose/rebuild round trip even when its chroma gets clipped.
     */
    @Test
    fun `lch round-trip preserves hue`() {
        listOf(
            Color(0xFF006A60), Color(0xFF3F5AA6), Color(0xFF8A4F00), Color(0xFF386A20),
            Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF),
        ).forEach { original ->
            val lch = original.toLch()
            val roundTripped = toneColor(lch.hue, lch.chroma, lch.lightness.toInt()).toLch()
            assertEquals(
                "$original round-tripped from hue ${lch.hue} to ${roundTripped.hue}",
                lch.hue,
                roundTripped.hue,
                2.0,
            )
        }
    }
}
