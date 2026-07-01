/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * A per-app brand: a display name + a single seed colour. Every Material 3 colour role is
 * derived from the seed (tonal palettes), so an app gets a whole coherent scheme from one
 * value — the "shared system + per-app accent" model. On Android 12+ dynamic colour can
 * override this from the wallpaper.
 */
data class KansoBrand(val name: String, val seed: Color)

/** The default kanso brand — a calm, secure teal. */
val KansoDefaultBrand = KansoBrand("kanso", Color(0xFF006A60))

// ---- lightweight tonal palettes -------------------------------------------------------
// An HSL approximation of the MD3 tone system: a palette is the seed hue at a fixed
// saturation, and a "tone" (0..100) maps to HSL lightness. Not HCT-exact, but coherent and
// dependency-free; the role→tone mappings below follow the Material 3 spec.
private fun hueOf(seed: Color): Float {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), out)
    return out[0]
}

private fun tone(hue: Float, sat: Float, t: Int): Color =
    Color.hsl((hue % 360f + 360f) % 360f, sat.coerceIn(0f, 1f), (t / 100f).coerceIn(0f, 1f))

private const val S_PRIMARY = 0.46f
private const val S_SECONDARY = 0.20f
private const val S_TERTIARY = 0.38f
private const val S_NEUTRAL = 0.05f
private const val S_NEUTRAL_VAR = 0.11f
private const val TERTIARY_HUE_SHIFT = 60f

/** Build a light Material 3 [ColorScheme] whose accent tones come from [seed]. */
fun kansoLightColorScheme(seed: Color): ColorScheme {
    val h = hueOf(seed)
    val t = h + TERTIARY_HUE_SHIFT
    return lightColorScheme(
        primary = tone(h, S_PRIMARY, 40), onPrimary = tone(h, S_PRIMARY, 100),
        primaryContainer = tone(h, S_PRIMARY, 90), onPrimaryContainer = tone(h, S_PRIMARY, 10),
        secondary = tone(h, S_SECONDARY, 40), onSecondary = tone(h, S_SECONDARY, 100),
        secondaryContainer = tone(h, S_SECONDARY, 90), onSecondaryContainer = tone(h, S_SECONDARY, 10),
        tertiary = tone(t, S_TERTIARY, 40), onTertiary = tone(t, S_TERTIARY, 100),
        tertiaryContainer = tone(t, S_TERTIARY, 90), onTertiaryContainer = tone(t, S_TERTIARY, 10),
        background = tone(h, S_NEUTRAL, 99), onBackground = tone(h, S_NEUTRAL, 10),
        surface = tone(h, S_NEUTRAL, 99), onSurface = tone(h, S_NEUTRAL, 10),
        surfaceVariant = tone(h, S_NEUTRAL_VAR, 90), onSurfaceVariant = tone(h, S_NEUTRAL_VAR, 30),
        surfaceContainerLowest = tone(h, S_NEUTRAL, 100), surfaceContainerLow = tone(h, S_NEUTRAL, 96),
        surfaceContainer = tone(h, S_NEUTRAL, 94), surfaceContainerHigh = tone(h, S_NEUTRAL, 92),
        surfaceContainerHighest = tone(h, S_NEUTRAL, 90),
        outline = tone(h, S_NEUTRAL_VAR, 50), outlineVariant = tone(h, S_NEUTRAL_VAR, 80),
        inverseSurface = tone(h, S_NEUTRAL, 20), inverseOnSurface = tone(h, S_NEUTRAL, 95),
        inversePrimary = tone(h, S_PRIMARY, 80),
        error = Color(0xFFBA1A1A), onError = Color.White,
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    )
}

/** Build a dark Material 3 [ColorScheme] whose accent tones come from [seed]. */
fun kansoDarkColorScheme(seed: Color): ColorScheme {
    val h = hueOf(seed)
    val t = h + TERTIARY_HUE_SHIFT
    return darkColorScheme(
        primary = tone(h, S_PRIMARY, 80), onPrimary = tone(h, S_PRIMARY, 20),
        primaryContainer = tone(h, S_PRIMARY, 30), onPrimaryContainer = tone(h, S_PRIMARY, 90),
        secondary = tone(h, S_SECONDARY, 80), onSecondary = tone(h, S_SECONDARY, 20),
        secondaryContainer = tone(h, S_SECONDARY, 30), onSecondaryContainer = tone(h, S_SECONDARY, 90),
        tertiary = tone(t, S_TERTIARY, 80), onTertiary = tone(t, S_TERTIARY, 20),
        tertiaryContainer = tone(t, S_TERTIARY, 30), onTertiaryContainer = tone(t, S_TERTIARY, 90),
        background = tone(h, S_NEUTRAL, 8), onBackground = tone(h, S_NEUTRAL, 90),
        surface = tone(h, S_NEUTRAL, 8), onSurface = tone(h, S_NEUTRAL, 90),
        surfaceVariant = tone(h, S_NEUTRAL_VAR, 30), onSurfaceVariant = tone(h, S_NEUTRAL_VAR, 80),
        surfaceContainerLowest = tone(h, S_NEUTRAL, 4), surfaceContainerLow = tone(h, S_NEUTRAL, 10),
        surfaceContainer = tone(h, S_NEUTRAL, 12), surfaceContainerHigh = tone(h, S_NEUTRAL, 17),
        surfaceContainerHighest = tone(h, S_NEUTRAL, 22),
        outline = tone(h, S_NEUTRAL_VAR, 60), outlineVariant = tone(h, S_NEUTRAL_VAR, 30),
        inverseSurface = tone(h, S_NEUTRAL, 90), inverseOnSurface = tone(h, S_NEUTRAL, 20),
        inversePrimary = tone(h, S_PRIMARY, 40),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    )
}
