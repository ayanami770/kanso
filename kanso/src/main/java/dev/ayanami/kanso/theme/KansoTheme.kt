/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The single theme entry point for every kanso app. Wraps [MaterialTheme] with:
 *  - a colour scheme derived from the app's [brand] seed (per-app accent), OR the wallpaper
 *    (Material You) when [dynamicColor] is on and the device is Android 12+,
 *  - the type / shape scales and the kanso spacing + elevation tokens,
 *  - edge-to-edge system-bar icon contrast that follows light/dark.
 *
 * Usage (in an app):  setContent { KansoTheme(brand = MyBrand) { AppRoot() } }
 *
 * **[dynamicColor] defaults to true, and it wins over [brand].** On Android 12+ the scheme
 * comes from the user's wallpaper and the seed is never read, so the call above renders in
 * Material You rather than in MyBrand on most current devices. That is deliberate — the user's
 * system-wide colour preference outranks the app's — but if your brand identity has to hold,
 * pass `dynamicColor = false`. Below Android 12 the seed is always used.
 *
 * Every other axis of the theme is a parameter, so an app can adopt kanso without forking it:
 *
 * ```
 * KansoTheme(
 *     brand = MyBrand,
 *     typography = kansoTypography(MyFontFamily),   // the M3 scale in your face
 *     shapes = Shapes(medium = RoundedCornerShape(4.dp)),
 *     spacing = KansoSpacing(screen = 24.dp),
 * ) { AppRoot() }
 * ```
 *
 * For a scheme that is *nearly* the seeded one, start from the builder and override the roles
 * you care about — `kansoLightColorScheme(seed).copy(primary = …)` — and pass the result as
 * [colorScheme]. Supplying [colorScheme] takes precedence over both [dynamicColor] and [brand];
 * [brand] is still published to [Kanso.brand] either way, so a splash screen or a chart can read
 * the app's raw seed no matter which scheme is in force.
 */
@Composable
fun KansoTheme(
    brand: KansoBrand = KansoDefaultBrand,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorScheme: ColorScheme? = null,
    typography: Typography = KansoTypography,
    shapes: Shapes = KansoShapes,
    spacing: KansoSpacing = KansoSpacing(),
    elevation: KansoElevation = KansoElevation(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resolvedColorScheme = colorScheme ?: when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> kansoDarkColorScheme(brand.seed)
        else -> kansoLightColorScheme(brand.seed)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalKansoBrand provides brand,
        LocalKansoSpacing provides spacing,
        LocalKansoElevation provides elevation,
    ) {
        MaterialTheme(
            colorScheme = resolvedColorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

/**
 * The brand in force. Provided by [KansoTheme] so anything downstream can read the app's own
 * name and raw seed — `Kanso.colors.primary` is a derived tone, not the seed, and under dynamic
 * colour it has no relationship to the brand at all.
 */
val LocalKansoBrand = staticCompositionLocalOf { KansoDefaultBrand }

/**
 * Ergonomic accessors for the current theme, so components read
 * `Kanso.spacing.lg` / `Kanso.colors.primary` instead of the longer CompositionLocal /
 * MaterialTheme paths.
 */
object Kanso {
    val brand: KansoBrand
        @Composable @ReadOnlyComposable get() = LocalKansoBrand.current
    val spacing: KansoSpacing
        @Composable @ReadOnlyComposable get() = LocalKansoSpacing.current
    val elevation: KansoElevation
        @Composable @ReadOnlyComposable get() = LocalKansoElevation.current
    val colors
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme
    val typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography
    val shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}

// `KansoBrands` used to live here, holding the seeds of one author's four private apps. A
// design system that hard-codes its first consumers into its public API cannot be adopted by
// anyone else without shipping someone else's brand list, so the seeds moved into :demo where
// they are sample data. Define your own: `KansoBrand("my app", Color(0xFF6750A4))`.
