/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import dev.ayanami.kanso.component.neutralStatusContainer
import dev.ayanami.kanso.component.skeletonBarColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cbrt
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
    private fun seedAt(hue: Int, chroma: Double = 0.11, tone: Int = 45): Color =
        toneColor(hue.toDouble(), chroma, tone)

    /** A colour's tone — CIE L* — worked out here rather than asked of the engine. */
    private fun tone(color: Color): Double {
        val y = luminance(color)
        return if (y > 216.0 / 24389.0) 116.0 * cbrt(y) - 16.0 else y * 24389.0 / 27.0
    }

    private fun hueDistance(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return min(d, 360.0 - d)
    }

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
        // Not on-pairs, but the ones kanso paints most: KansoSectionHeader draws its title in
        // `primary` on `surface`, and KansoCard draws its own on the filled card's container.
        Triple("surface/primary", s.surface, s.primary),
        Triple("surfaceContainerHighest/primary", s.surfaceContainerHighest, s.primary),
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
            "teal" to Color(0xFF006A60),
            "indigo" to Color(0xFF3F5AA6),
            "amber" to Color(0xFF8A4F00),
            "green" to Color(0xFF386A20),
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

    /**
     * The extended roles are fixed literals, so unlike the seeded ones they are not protected
     * by the tone system — nothing stops a hand-picked hex from being unreadable. Pinned here
     * for exactly that reason.
     */
    @Test
    fun `extended semantic colours meet AA in both schemes`() {
        listOf(
            "light" to KansoLightExtendedColors,
            "dark" to KansoDarkExtendedColors,
        ).forEach { (mode, e) ->
            listOf(
                "success/onSuccess" to (e.success to e.onSuccess),
                "successContainer/on" to (e.successContainer to e.onSuccessContainer),
                "warning/onWarning" to (e.warning to e.onWarning),
                "warningContainer/on" to (e.warningContainer to e.onWarningContainer),
                "info/onInfo" to (e.info to e.onInfo),
                "infoContainer/on" to (e.infoContainer to e.onInfoContainer),
            ).forEach { (name, pair) ->
                val ratio = contrast(pair.first, pair.second)
                assertTrue("$mode $name = ${"%.2f".format(ratio)}", ratio >= aa)
            }
        }
    }

    /**
     * Each extended role must also be legible as text straight on the surface, which is how a
     * status label is most often used — not only inside its own container — and on a filled
     * card, which is where a list of statuses usually lives.
     */
    @Test
    fun `extended roles are legible on the scheme surface`() {
        listOf(
            "light" to (kansoLightColorScheme(KansoDefaultBrand.seed) to KansoLightExtendedColors),
            "dark" to (kansoDarkColorScheme(KansoDefaultBrand.seed) to KansoDarkExtendedColors),
        ).forEach { (mode, pair) ->
            val (scheme, e) = pair
            listOf("success" to e.success, "warning" to e.warning, "info" to e.info)
                .forEach { (name, color) ->
                    listOf(
                        "surface" to scheme.surface,
                        "card" to scheme.surfaceContainerHighest,
                    ).forEach { (where, under) ->
                        val ratio = contrast(under, color)
                        assertTrue("$mode $name on $where = ${"%.2f".format(ratio)}", ratio >= aa)
                    }
                }
        }
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
        val mutedChroma = kansoLightColorScheme(Color(0xFF44506E)).primary.toOklch().chroma
        val vividChroma = kansoLightColorScheme(Color(0xFF1A3FE0)).primary.toOklch().chroma
        assertTrue(
            "a muted navy ($mutedChroma) and a vivid blue ($vividChroma) produced the same " +
                "chroma — the seed's own saturation was normalised away",
            vividChroma - mutedChroma > 0.05,
        )
    }

    // ---- the tone system itself -------------------------------------------------------

    /** Gamut mapping may only reduce chroma; the requested tone must survive it exactly. */
    @Test
    fun `gamut mapping preserves tone`() {
        listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100).forEach { tone ->
            (0 until 360 step 5).forEach { hue ->
                // Deliberately far outside the sRGB gamut, to force the chroma search.
                val actual = tone(toneColor(hue.toDouble(), 0.4, tone))
                // Compose packs an sRGB Color at 8 bits per channel, so a round trip carries
                // that quantisation with it — measured worst case is 0.22 of a tone.
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
            val black = toneColor(hue.toDouble(), 0.15, 0)
            val white = toneColor(hue.toDouble(), 0.15, 100)
            assertTrue("tone 0 at hue $hue was $black", luminance(black) < 0.001)
            assertTrue("tone 100 at hue $hue was $white", luminance(white) > 0.99)
        }
    }

    /**
     * Gamut mapping moves along the chroma axis only, so a colour's hue must survive a
     * decompose/rebuild round trip even when its chroma has to be reduced.
     */
    @Test
    fun `an oklch round trip preserves hue`() {
        listOf(
            Color(0xFF006A60),
            Color(0xFF3F5AA6),
            Color(0xFF8A4F00),
            Color(0xFF386A20),
            Color(0xFFFF0000),
            Color(0xFF00FF00),
            Color(0xFF0000FF),
        ).forEach { original ->
            val lch = original.toOklch()
            val rebuilt = toneColor(lch.hue, lch.chroma, tone(original).toInt()).toOklch()
            assertEquals(
                "$original round-tripped from hue ${lch.hue} to ${rebuilt.hue}",
                lch.hue,
                rebuilt.hue,
                2.0,
            )
        }
    }

    /**
     * Asked for more chroma than sRGB has at a tone, the engine gives less chroma at the same
     * hue. Clipping the channel that overshoots would land inside the gamut too — at another
     * hue. Tones 20 to 90, because nearer the ends there is too little chroma left for a hue
     * to be read back out of eight bits a channel.
     */
    @Test
    fun `gamut mapping preserves hue`() {
        listOf(20, 30, 40, 50, 60, 70, 80, 90).forEach { tone ->
            (0 until 360 step 5).forEach { hue ->
                // Far more chroma than sRGB holds anywhere, to force the search at every hue.
                val mapped = toneColor(hue.toDouble(), 0.4, tone).toOklch()
                assertTrue(
                    "tone $tone at hue $hue came back at hue ${mapped.hue}",
                    hueDistance(hue.toDouble(), mapped.hue) <= 3.0,
                )
            }
        }
    }

    // ---- colours drawn over a surface rather than as one --------------------------------

    /** The surfaces a list — and so its placeholder — is drawn on: bare, in a card, in a sheet. */
    private fun listSurfaces(s: ColorScheme): List<Pair<String, Color>> = listOf(
        "surface" to s.surface,
        "card" to s.surfaceContainerHighest,
        "sheet" to s.surfaceContainerLow,
    )

    private fun bothSchemes(seed: Color): List<Pair<String, ColorScheme>> = listOf(
        "light" to kansoLightColorScheme(seed),
        "dark" to kansoDarkColorScheme(seed),
    )

    /**
     * A skeleton that cannot be seen is a blank screen. The bars used to pulse between the two
     * highest surface containers, which is what a filled card is painted in: inside a card they
     * measured 1.00:1 at one end of the pulse and 1.05:1 at the other.
     *
     * 1.15:1 is not a WCAG figure — a placeholder is not text — it is "visibly a shape", a
     * little under what a filled card measures against the surface it sits on.
     */
    @Test
    fun `skeleton bars stand out from every surface a list is drawn on`() {
        everyHue(step = 15) { hue, seed ->
            bothSchemes(seed).forEach { (mode, scheme) ->
                listSurfaces(scheme).forEach { (name, under) ->
                    listOf(0f, 1f).forEach { progress ->
                        val bar = skeletonBarColor(scheme.onSurface, progress).compositeOver(under)
                        val ratio = contrast(under, bar)
                        assertTrue(
                            "$mode hue $hue bar at $progress on $name = ${"%.2f".format(ratio)}",
                            ratio >= 1.15,
                        )
                    }
                }
            }
        }
    }

    /**
     * A neutral badge used to share its container tone with the filled card it sits in, so on a
     * card it was a label with no pill (1.00:1). Its container must be distinguishable from the
     * surface under it, and its text must still meet AA on the result.
     */
    @Test
    fun `the neutral status container shows on every surface and keeps its text readable`() {
        everyHue(step = 15) { hue, seed ->
            bothSchemes(seed).forEach { (mode, scheme) ->
                listSurfaces(scheme).forEach { (name, under) ->
                    val container = neutralStatusContainer(scheme.onSurface).compositeOver(under)
                    val edge = contrast(under, container)
                    val text = contrast(container, scheme.onSurfaceVariant)
                    assertTrue(
                        "$mode hue $hue neutral container on $name = ${"%.2f".format(edge)}",
                        edge >= 1.15,
                    )
                    assertTrue(
                        "$mode hue $hue neutral text on $name = ${"%.2f".format(text)}",
                        text >= aa,
                    )
                }
            }
        }
    }

    /**
     * The figure the documentation quotes. 6.46 is the exact ratio of tone 40 to tone 100;
     * packing each colour into eight bits a channel moves it a few hundredths either way, hue by
     * hue. The README said 6.42–6.46 and called that pinned, while the only assertions were "at
     * least 4.5" and "a spread under 0.25".
     */
    @Test
    fun `onPrimary on primary stays inside the documented band`() {
        val ratios = (0 until 360).map { hue ->
            val scheme = kansoLightColorScheme(seedAt(hue))
            contrast(scheme.primary, scheme.onPrimary)
        }
        assertTrue("lowest ratio ${ratios.min()} is under 6.41", ratios.min() >= 6.405)
        assertTrue("highest ratio ${ratios.max()} is over 6.51", ratios.max() <= 6.515)
    }

    /** The README states the band a seed's chroma is held to; this is what keeps it there. */
    @Test
    fun `seed chroma is carried through inside its band and no further`() {
        fun primaryChroma(seed: Color) = kansoLightColorScheme(seed).primary.toOklch().chroma

        // Above the band: a vivid red and a very vivid one end up at the same edge, 0.15.
        assertEquals(primaryChroma(Color(0xFFFF0000)), primaryChroma(Color(0xFFE0453A)), 0.004)
        assertTrue(primaryChroma(Color(0xFFFF0000)) <= 0.154)
        // Below it but not grey: lifted to the lower edge, 0.068, not left almost colourless.
        assertTrue(primaryChroma(Color(0xFF808A80)) >= 0.064)
        // Inside it: the seed's own chroma, as it came.
        val seed = KansoDefaultBrand.seed
        assertEquals(seed.toOklch().chroma, primaryChroma(seed), 0.004)
    }

    /**
     * The grey line is where it always was: CIELAB chroma 5, the one number still measured on
     * that axis. The two seeds the README names sit either side of it, and a seed nobody can
     * tell from black is grey — on the OKLab axis #010000 would have counted as a colour and
     * been given a red scheme.
     */
    @Test
    fun `the grey line is where the documentation puts it`() {
        fun isNeutral(seed: Color): Boolean {
            val primary = kansoLightColorScheme(seed).primary
            return maxOf(primary.red, primary.green, primary.blue) -
                minOf(primary.red, primary.green, primary.blue) < 0.02f
        }
        assertFalse("#F0FFFF is just on the coloured side", isNeutral(Color(0xFFF0FFFF)))
        assertTrue("#000F0F is just on the grey side", isNeutral(Color(0xFF000F0F)))
        assertTrue("#010000 is black to look at", isNeutral(Color(0xFF010000)))
    }
}
