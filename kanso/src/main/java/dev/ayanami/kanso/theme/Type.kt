/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * kanso typography — the Material 3 type scale (display / headline / title / body / label) on
 * the platform default type family, so every app reads consistently out of the box.
 *
 * To use your own type family, build one with [kansoTypography] and hand it to `KansoTheme`.
 */
val KansoTypography = Typography()

/**
 * The Material 3 type scale rendered in your own font.
 *
 * Every size, weight, line height and letter spacing is inherited from the Material 3 defaults
 * — only the family changes — so a brand font cannot accidentally rescale the type system.
 * [displayFamily] covers the display and headline styles, for brands whose display face differs
 * from their text face; it defaults to [bodyFamily].
 *
 * ```
 * val Brand = FontFamily(Font(R.font.inter))
 * KansoTheme(typography = kansoTypography(Brand)) { … }
 * ```
 *
 * **Known limitation.** Material 3 1.4 carries fifteen further `*Emphasized` styles, used by the
 * M3 Expressive components. Their accessors are `internal` to material3, so they can be neither
 * read nor set from outside it and they keep the platform default family. That is invisible to
 * kanso's own components — none of them use an emphasized style — but a consuming app reaching
 * for a stock Expressive component will see it render in the default face. Revisit if material3
 * opens those up.
 */
fun kansoTypography(
    bodyFamily: FontFamily,
    displayFamily: FontFamily = bodyFamily,
): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = displayFamily),
        displayMedium = base.displayMedium.copy(fontFamily = displayFamily),
        displaySmall = base.displaySmall.copy(fontFamily = displayFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = displayFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = displayFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = displayFamily),
        titleLarge = base.titleLarge.copy(fontFamily = bodyFamily),
        titleMedium = base.titleMedium.copy(fontFamily = bodyFamily),
        titleSmall = base.titleSmall.copy(fontFamily = bodyFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = base.bodySmall.copy(fontFamily = bodyFamily),
        labelLarge = base.labelLarge.copy(fontFamily = bodyFamily),
        labelMedium = base.labelMedium.copy(fontFamily = bodyFamily),
        labelSmall = base.labelSmall.copy(fontFamily = bodyFamily),
    )
}
