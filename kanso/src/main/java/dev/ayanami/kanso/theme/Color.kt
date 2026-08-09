/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A per-app brand: a display name + a single seed colour. Every Material 3 colour role is
 * derived from the seed (tonal palettes), so an app gets a whole coherent scheme from one
 * value — the "shared system + per-app accent" model. On Android 12+ dynamic colour can
 * override this from the wallpaper.
 */
public data class KansoBrand(val name: String, val seed: Color)

/** The default kanso brand — a calm, secure teal. */
public val KansoDefaultBrand: KansoBrand = KansoBrand("kanso", Color(0xFF006A60))

// ---- tone system ----------------------------------------------------------------------
// Material 3's "tone" (0..100) is CIE L*. L* is a function of luminance alone, so the
// contrast ratio between two tones is fixed no matter the hue — which is exactly what makes
// an arbitrary brand seed safe to accept. Colours are therefore built in CIELCh(ab) and
// gamut-mapped into sRGB by reducing *chroma only*, so L* survives the mapping and the
// contrast guarantee holds for every seed.
//
// This replaces an earlier HSL approximation. HSL lightness is not perceptual: a fixed tone
// landed at very different real luminance depending on hue, so onPrimary-on-primary fell to
// 3.17:1 at hue 60 and failed WCAG AA for 157 of 360 hues — including kanso's own default
// teal, at 3.60:1. ColorContrastTest pins the guarantee across the whole hue circle.
//
// Pure Kotlin and dependency-free on purpose: no android.graphics call, so the engine is
// covered by plain JVM unit tests rather than needing an instrumented run.

/** D65 white point (Y is 1.0 by definition). */
private const val WHITE_X = 0.95047
private const val WHITE_Z = 1.08883

private fun srgbToLinear(channel: Float): Double {
    val u = channel.toDouble()
    return if (u <= 0.04045) u / 12.92 else ((u + 0.055) / 1.055).pow(2.4)
}

private fun linearToSrgb(u: Double): Float {
    val v = if (u <= 0.0031308) 12.92 * u else 1.055 * u.pow(1.0 / 2.4) - 0.055
    return v.coerceIn(0.0, 1.0).toFloat()
}

// The CIELAB companding function and its inverse, using the exact 6/29 breakpoint.
private fun labF(t: Double): Double = if (t >
    216.0 / 24389.0
) {
    t.pow(1.0 / 3.0)
} else {
    t * (841.0 / 108.0) + 4.0 / 29.0
}

private fun labFInv(t: Double): Double = if (t >
    6.0 / 29.0
) {
    t * t * t
} else {
    (t - 4.0 / 29.0) * (108.0 / 841.0)
}

/** A colour as CIELCh(ab): [lightness] is L*, [hue] is in degrees. */
internal class Lch(val lightness: Double, val chroma: Double, val hue: Double)

/** Decompose an sRGB colour into CIELCh(ab). */
internal fun Color.toLch(): Lch {
    val r = srgbToLinear(red)
    val g = srgbToLinear(green)
    val b = srgbToLinear(blue)
    val x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
    val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    val z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b
    val fx = labF(x / WHITE_X)
    val fy = labF(y)
    val fz = labF(z / WHITE_Z)
    val a = 500.0 * (fx - fy)
    val bStar = 200.0 * (fy - fz)
    return Lch(
        lightness = 116.0 * fy - 16.0,
        chroma = sqrt(a * a + bStar * bStar),
        hue = (atan2(bStar, a) * 180.0 / PI + 360.0) % 360.0,
    )
}

/** Linear-light sRGB for an LCh triple — components may fall outside [0,1] (out of gamut). */
private fun lchToLinearRgb(lightness: Double, chroma: Double, hue: Double): DoubleArray {
    val radians = hue * PI / 180.0
    val a = chroma * cos(radians)
    val b = chroma * sin(radians)
    val fy = (lightness + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0
    val x = WHITE_X * labFInv(fx)
    val y = labFInv(fy)
    val z = WHITE_Z * labFInv(fz)
    return doubleArrayOf(
        3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
        -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
        0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
    )
}

private const val GAMUT_EPSILON = 0.0005
private const val GAMUT_SEARCH_STEPS = 24

private fun DoubleArray.inGamut(): Boolean = all {
    it >= -GAMUT_EPSILON && it <= 1.0 + GAMUT_EPSILON
}

/**
 * The sRGB colour at [tone] (= L*) on the tonal palette at [hue] / [chroma]. If that point
 * lies outside the sRGB gamut, chroma is reduced until it fits — [tone], and therefore the
 * contrast against every other tone, is preserved exactly.
 */
internal fun toneColor(hue: Double, chroma: Double, tone: Int): Color {
    val lightness = tone.toDouble().coerceIn(0.0, 100.0)
    var c = chroma
    if (!lchToLinearRgb(lightness, c, hue).inGamut()) {
        var low = 0.0
        var high = c
        repeat(GAMUT_SEARCH_STEPS) {
            val mid = (low + high) / 2.0
            if (lchToLinearRgb(lightness, mid, hue).inGamut()) low = mid else high = mid
        }
        c = low
    }
    val rgb = lchToLinearRgb(lightness, c, hue)
    return Color(linearToSrgb(rgb[0]), linearToSrgb(rgb[1]), linearToSrgb(rgb[2]))
}

/** Below this chroma a seed reads as grey, and gets a neutral scheme rather than a hue. */
private const val ACHROMATIC_CHROMA = 5.0

// The seed's own chroma is kept — clamped into a usable band — rather than normalised away,
// so a muted navy and a neon blue produce visibly different schemes. The supporting palettes
// are derived as fractions of it, keeping the whole scheme as vivid, or as restrained, as
// the brand it came from.
private const val CHROMA_MIN = 24.0
private const val CHROMA_MAX = 56.0
private const val SECONDARY_CHROMA_RATIO = 0.34
private const val TERTIARY_CHROMA_RATIO = 0.62
private const val NEUTRAL_CHROMA = 3.0
private const val NEUTRAL_VARIANT_CHROMA = 7.0
private const val TERTIARY_HUE_SHIFT = 60.0

/** The five Material 3 tonal palettes derived from one brand seed. */
internal class KansoTones(seed: Color) {
    private val seedLch = seed.toLch()
    private val achromatic = seedLch.chroma < ACHROMATIC_CHROMA

    private val hue = if (achromatic) 0.0 else seedLch.hue
    private val tertiaryHue = (hue + TERTIARY_HUE_SHIFT) % 360.0

    private val primaryChroma =
        if (achromatic) 0.0 else seedLch.chroma.coerceIn(CHROMA_MIN, CHROMA_MAX)
    private val secondaryChroma = primaryChroma * SECONDARY_CHROMA_RATIO
    private val tertiaryChroma = primaryChroma * TERTIARY_CHROMA_RATIO
    private val neutralChroma = if (achromatic) 0.0 else NEUTRAL_CHROMA
    private val neutralVariantChroma = if (achromatic) 0.0 else NEUTRAL_VARIANT_CHROMA

    fun primary(tone: Int): Color = toneColor(hue, primaryChroma, tone)
    fun secondary(tone: Int): Color = toneColor(hue, secondaryChroma, tone)
    fun tertiary(tone: Int): Color = toneColor(tertiaryHue, tertiaryChroma, tone)
    fun neutral(tone: Int): Color = toneColor(hue, neutralChroma, tone)
    fun neutralVariant(tone: Int): Color = toneColor(hue, neutralVariantChroma, tone)
}

// The error ramp stays fixed rather than following the seed — Material 3 does the same, so
// that "danger" reads as danger under every brand.
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color.White
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

/** Build a light Material 3 [ColorScheme] whose accent tones come from [seed]. */
public fun kansoLightColorScheme(seed: Color): ColorScheme {
    val t = KansoTones(seed)
    return lightColorScheme(
        primary = t.primary(40), onPrimary = t.primary(100),
        primaryContainer = t.primary(90), onPrimaryContainer = t.primary(10),
        secondary = t.secondary(40), onSecondary = t.secondary(100),
        secondaryContainer = t.secondary(90), onSecondaryContainer = t.secondary(10),
        tertiary = t.tertiary(40), onTertiary = t.tertiary(100),
        tertiaryContainer = t.tertiary(90), onTertiaryContainer = t.tertiary(10),
        background = t.neutral(99), onBackground = t.neutral(10),
        surface = t.neutral(99), onSurface = t.neutral(10),
        surfaceVariant = t.neutralVariant(90), onSurfaceVariant = t.neutralVariant(30),
        surfaceContainerLowest = t.neutral(100), surfaceContainerLow = t.neutral(96),
        surfaceContainer = t.neutral(94), surfaceContainerHigh = t.neutral(92),
        surfaceContainerHighest = t.neutral(90),
        surfaceBright = t.neutral(98), surfaceDim = t.neutral(87),
        outline = t.neutralVariant(50), outlineVariant = t.neutralVariant(80),
        inverseSurface = t.neutral(20), inverseOnSurface = t.neutral(95),
        inversePrimary = t.primary(80),
        error = ErrorLight, onError = OnErrorLight,
        errorContainer = ErrorContainerLight, onErrorContainer = OnErrorContainerLight,
    )
}

/** Build a dark Material 3 [ColorScheme] whose accent tones come from [seed]. */
public fun kansoDarkColorScheme(seed: Color): ColorScheme {
    val t = KansoTones(seed)
    return darkColorScheme(
        primary = t.primary(80), onPrimary = t.primary(20),
        primaryContainer = t.primary(30), onPrimaryContainer = t.primary(90),
        secondary = t.secondary(80), onSecondary = t.secondary(20),
        secondaryContainer = t.secondary(30), onSecondaryContainer = t.secondary(90),
        tertiary = t.tertiary(80), onTertiary = t.tertiary(20),
        tertiaryContainer = t.tertiary(30), onTertiaryContainer = t.tertiary(90),
        background = t.neutral(8), onBackground = t.neutral(90),
        surface = t.neutral(8), onSurface = t.neutral(90),
        surfaceVariant = t.neutralVariant(30), onSurfaceVariant = t.neutralVariant(80),
        surfaceContainerLowest = t.neutral(4), surfaceContainerLow = t.neutral(10),
        surfaceContainer = t.neutral(12), surfaceContainerHigh = t.neutral(17),
        surfaceContainerHighest = t.neutral(22),
        surfaceBright = t.neutral(24), surfaceDim = t.neutral(6),
        outline = t.neutralVariant(60), outlineVariant = t.neutralVariant(30),
        inverseSurface = t.neutral(90), inverseOnSurface = t.neutral(20),
        inversePrimary = t.primary(40),
        error = ErrorDark, onError = OnErrorDark,
        errorContainer = ErrorContainerDark, onErrorContainer = OnErrorContainerDark,
    )
}
