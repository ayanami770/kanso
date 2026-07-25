/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The single theme entry point for every kanso app. Wraps [MaterialTheme] with:
 *  - a colour scheme derived from the app's [brand] seed (per-app accent), OR the wallpaper
 *    (Material You) when [dynamicColor] is on and the device is Android 12+,
 *  - the shared type / shape scales and the kanso spacing + elevation tokens,
 *  - edge-to-edge system-bar icon contrast that follows light/dark.
 *
 * Usage (in an app):  setContent { KansoTheme(brand = MyBrand) { AppRoot() } }
 *
 * **[dynamicColor] defaults to true, and it wins over [brand].** On Android 12+ the scheme
 * comes from the user's wallpaper and the seed is never read, so the call above renders in
 * Material You rather than in MyBrand on most current devices. That is deliberate — the user's
 * system-wide colour preference outranks the app's — but if your brand identity has to hold,
 * pass `dynamicColor = false`. Below Android 12 the seed is always used.
 */
@Composable
fun KansoTheme(
    brand: KansoBrand = KansoDefaultBrand,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
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
        LocalKansoSpacing provides KansoSpacing(),
        LocalKansoElevation provides KansoElevation(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KansoTypography,
            shapes = KansoShapes,
            content = content,
        )
    }
}

/**
 * Ergonomic accessors for the current theme, so components read
 * `Kanso.spacing.lg` / `Kanso.colors.primary` instead of the longer CompositionLocal /
 * MaterialTheme paths.
 */
object Kanso {
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
