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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
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
// an arbitrary brand seed safe to accept.
//
// Hue and chroma are OKLCH's. The colour at a tone is the one with that tone's luminance, the
// seed's OKLab hue, and as much of the requested OKLab chroma as sRGB can show there. Where
// sRGB cannot show all of it, chroma alone is reduced and no channel is clipped, so the tone
// and the hue both survive the mapping — and with them the contrast guarantee and the
// brand's own colour.
//
// Hue was CIELAB's at first, and that is the one axis that moved. CIELAB's hue lines are not
// perceptually straight through the blues: holding its hue angle while L* changed bent a pure
// blue towards violet, so #0000FF gave a #6A4DA9 primary, 31° of OKLab hue from its seed and
// 39° by the lightest roles. OKLab was built to be straight there. HueFidelityTest holds
// every role to its seed's hue round the whole circle.
//
// Before either, an HSL approximation. HSL lightness is not perceptual: a fixed tone landed
// at very different real luminance depending on hue, so onPrimary-on-primary fell to 3.17:1
// at hue 60 and failed WCAG AA for 157 of 360 hues — including kanso's own default teal, at
// 3.60:1. ColorContrastTest pins the guarantee across the whole hue circle.
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

/** CIE luminance (Y) of a linear-light sRGB colour. */
private fun luminance(r: Double, g: Double, b: Double): Double =
    0.2126729 * r + 0.7151522 * g + 0.0721750 * b

private fun DoubleArray.luminance(): Double = luminance(this[0], this[1], this[2])

/** The luminance a tone names: CIE L*, inverted. */
private fun toneToLuminance(tone: Double): Double = labFInv((tone + 16.0) / 116.0)

/**
 * CIELAB chroma, kept for the one question it still answers best: is this seed grey?
 *
 * OKLab is the wrong ruler for that near black. Its cube root has no toe, so its chroma does
 * not fall away with the light: #010000, which nobody can tell from black, measures more
 * chromatic there than the palest tint that counts as a colour, at a hue that is one 8-bit
 * step of noise. CIELAB's chroma goes to zero with luminance, as what the eye can make out
 * does.
 */
private fun Color.cielabChroma(): Double {
    val r = srgbToLinear(red)
    val g = srgbToLinear(green)
    val b = srgbToLinear(blue)
    val fx = labF((0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / WHITE_X)
    val fy = labF(luminance(r, g, b))
    val fz = labF((0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / WHITE_Z)
    val a = 500.0 * (fx - fy)
    val bStar = 200.0 * (fy - fz)
    return sqrt(a * a + bStar * bStar)
}

// ---- OKLab ----------------------------------------------------------------------------
// Björn Ottosson's OKLab, for linear-light sRGB. A colour becomes three cone responses and
// then their cube roots; two opponent axes are read off those, and hue is the angle in the
// plane of the two axes, chroma the distance from its centre.

/** A colour's OKLCH [chroma] and [hue] (degrees). Its OKLab lightness is not kept: tone is L*. */
internal class Oklch(val chroma: Double, val hue: Double)

/** Decompose an sRGB colour into OKLCH chroma and hue. */
internal fun Color.toOklch(): Oklch {
    val r = srgbToLinear(red)
    val g = srgbToLinear(green)
    val b = srgbToLinear(blue)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bAxis = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return Oklch(
        chroma = sqrt(a * a + bAxis * bAxis),
        hue = (atan2(bAxis, a) * 180.0 / PI + 360.0) % 360.0,
    )
}

/** Linear-light sRGB from OKLab's three cone responses, cubed back out of their roots. */
private fun conesToLinearRgb(l: Double, m: Double, s: Double): DoubleArray = doubleArrayOf(
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
)

// What one unit of each cone response adds to luminance. The middle one carries nearly all of
// it, which is why the search below starts where it does.
private val LONG_LUMINANCE = conesToLinearRgb(1.0, 0.0, 0.0).luminance()
private val MEDIUM_LUMINANCE = conesToLinearRgb(0.0, 1.0, 0.0).luminance()
private val SHORT_LUMINANCE = conesToLinearRgb(0.0, 0.0, 1.0).luminance()

private const val LIGHTNESS_SEARCH_STEPS = 8
private const val LUMINANCE_TOLERANCE = 1e-9

private fun cube(x: Double): Double = x * x * x

/**
 * Linear-light sRGB of the colour with luminance [y], OKLab [hue] (degrees) and OKLab
 * [chroma] — or null when no colour has all three. Components may fall outside [0,1], which
 * is a colour sRGB cannot show.
 *
 * OKLab's own lightness is not luminance, so it is the unknown here. With hue and chroma
 * fixed, each cone response's root is that lightness plus a constant, and luminance is a
 * weighted sum of their cubes: one cubic in one unknown, rising and convex wherever a real
 * colour can be. That is the case Newton's method cannot miss, and it settles in a handful
 * of steps.
 */
private fun oklchToLinearRgb(y: Double, chroma: Double, hue: Double): DoubleArray? {
    val radians = hue * PI / 180.0
    val a = chroma * cos(radians)
    val b = chroma * sin(radians)
    val longOffset = 0.3963377774 * a + 0.2158037573 * b
    val mediumOffset = -0.1055613458 * a - 0.0638541728 * b
    val shortOffset = -0.0894841775 * a - 1.2914855480 * b
    // Exact at zero chroma, and close beyond it.
    var lightness = cbrt(y) - mediumOffset
    repeat(LIGHTNESS_SEARCH_STEPS) {
        val l = lightness + longOffset
        val m = lightness + mediumOffset
        val s = lightness + shortOffset
        val error = LONG_LUMINANCE * cube(l) + MEDIUM_LUMINANCE * cube(m) +
            SHORT_LUMINANCE * cube(s) - y
        val slope = LONG_LUMINANCE * l * l + MEDIUM_LUMINANCE * m * m + SHORT_LUMINANCE * s * s
        lightness -= error / (3.0 * slope)
    }
    val rgb = conesToLinearRgb(
        cube(lightness + longOffset),
        cube(lightness + mediumOffset),
        cube(lightness + shortOffset),
    )
    // Asked for a chroma no colour of this luminance has, the search ends somewhere else, or
    // nowhere, at NaN. That has to read as "no such colour" — never as a colour of another
    // tone, which is the one thing the contrast guarantee cannot absorb.
    return rgb.takeIf { abs(it.luminance() - y) <= LUMINANCE_TOLERANCE }
}

// Closer to the edge of sRGB than this is on it: floating-point noise, not a channel to clip.
private const val GAMUT_EPSILON = 1e-9
private const val GAMUT_SEARCH_STEPS = 24

private fun DoubleArray.inGamut(): Boolean = all {
    it >= -GAMUT_EPSILON && it <= 1.0 + GAMUT_EPSILON
}

/** The colour at this luminance, chroma and hue, if there is one and sRGB can show it. */
private fun displayable(y: Double, chroma: Double, hue: Double): DoubleArray? =
    oklchToLinearRgb(y, chroma, hue)?.takeIf { it.inGamut() }

/**
 * The sRGB colour at [tone] (= L*) on the tonal palette at OKLab [hue] (degrees) and OKLab
 * [chroma]. If sRGB cannot show that much chroma at this tone and hue, chroma is reduced
 * until it can, and nothing else is: [tone], and therefore the contrast against every other
 * tone, is preserved exactly, and so is [hue]. No channel is clipped to make a colour fit —
 * a clipped channel is a different hue.
 */
internal fun toneColor(hue: Double, chroma: Double, tone: Int): Color {
    // The two ends have no chroma to keep: every hue is black at tone 0 and white at 100.
    if (tone <= 0) return Color.Black
    if (tone >= 100) return Color.White
    val y = toneToLuminance(tone.toDouble())
    var rgb = displayable(y, chroma, hue)
    if (rgb == null) {
        // Grey can always be shown, so `low` always can: the search closes in on the edge of
        // the gamut from inside it.
        var low = 0.0
        var high = chroma
        repeat(GAMUT_SEARCH_STEPS) {
            val mid = (low + high) / 2.0
            if (displayable(y, mid, hue) != null) low = mid else high = mid
        }
        rgb = displayable(y, low, hue) ?: doubleArrayOf(y, y, y)
    }
    return Color(linearToSrgb(rgb[0]), linearToSrgb(rgb[1]), linearToSrgb(rgb[2]))
}

/**
 * Below this CIELAB chroma a seed reads as grey, and gets a neutral scheme rather than a hue.
 * See [cielabChroma] for why this one number is not on the OKLab axis.
 */
private const val ACHROMATIC_CHROMA = 5.0

// The seed's own chroma is kept — clamped into a usable band — rather than normalised away,
// so a muted navy and a neon blue produce visibly different schemes. The supporting palettes
// are derived as fractions of it, keeping the whole scheme as vivid, or as restrained, as
// the brand it came from.
//
// These are OKLCH chroma. They are the amounts the engine used when chroma was CIELAB's —
// 24, 56, 3 and 7 — restated on this axis: what each of those came to in OKLCH, averaged
// round the hue circle. Only the average matches. Hue by hue the two axes disagree about
// how much chroma is the same amount, so one seed's scheme is a little more saturated than
// it was and another's a little less.
private const val CHROMA_MIN = 0.068
private const val CHROMA_MAX = 0.15
private const val SECONDARY_CHROMA_RATIO = 0.34
private const val TERTIARY_CHROMA_RATIO = 0.62
private const val NEUTRAL_CHROMA = 0.0085
private const val NEUTRAL_VARIANT_CHROMA = 0.0197
private const val TERTIARY_HUE_SHIFT = 60.0

/** The five Material 3 tonal palettes derived from one brand seed. */
internal class KansoTones(seed: Color) {
    private val achromatic = seed.cielabChroma() < ACHROMATIC_CHROMA
    private val seedLch = seed.toOklch()

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
