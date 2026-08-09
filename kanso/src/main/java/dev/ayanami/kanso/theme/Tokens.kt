/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
@file:Suppress("ComposeCompositionLocalUsage")

package dev.ayanami.kanso.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens on a 4dp grid — the single source of truth for gaps, insets and paddings.
 * Provided ambiently via [LocalKansoSpacing]; read through `Kanso.spacing`. (If you find
 * yourself hard-coding a `.dp` gap in more than two places, add a token here instead.)
 */
@Immutable
public data class KansoSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** The standard screen edge inset. */
    val screen: Dp = 16.dp,
    /** Vertical gap between stacked cards / sections. */
    val section: Dp = 12.dp,
    /**
     * The widest a column of body text should get. Beyond roughly this, line length passes the
     * point where the eye reliably finds the next line — see
     * [dev.ayanami.kanso.component.KansoContentContainer], which is
     * what applies it.
     */
    val contentMaxWidth: Dp = 640.dp,
)

public val LocalKansoSpacing: ProvidableCompositionLocal<KansoSpacing> =
    staticCompositionLocalOf { KansoSpacing() }

/**
 * Icon sizes, named by the role the icon plays rather than by its number.
 *
 * These arrived because the same numbers had been written down three times each: 24dp as the
 * leading icon of a list row, of a setting row, and of the skeleton that stands in for them;
 * 48dp as a minimum height in the same three files. Three copies of a number that must agree is
 * a number that eventually will not.
 *
 * Deliberately only icons. The 48dp minimum touch target is *not* here: it is an
 * accessibility floor that comes from the platform rather than a taste decision, so it is an
 * internal constant no theme can lower. Neither is there a general `rowHeight`, because a row's
 * height is a consequence of its content and its font scale, not a value to dial in.
 */
@Immutable
public data class KansoSizing(
    /** A list row's leading icon, and anything that has to line up with one. */
    val icon: Dp = 24.dp,
    /** The glyph inside a button, sized to the label beside it rather than to the row. */
    val iconSmall: Dp = 18.dp,
    /** The glyph in a status badge, sized to `labelSmall`. */
    val iconBadge: Dp = 14.dp,
    /** The large glyph of a full-area empty, error or first-run state. */
    val iconLarge: Dp = 56.dp,
)

public val LocalKansoSizing: ProvidableCompositionLocal<KansoSizing> =
    staticCompositionLocalOf { KansoSizing() }

/**
 * The Material minimum touch target — anything tappable clears this at any font scale.
 *
 * Internal, and not a field on [KansoSizing], on purpose. Every other token here is a taste
 * decision the consuming app is invited to make differently; this one is an accessibility floor
 * that comes from the platform. Exposing it would let a theme set it to 32dp, which is not a
 * brand choice — it is a defect that no test in this repo would catch, because the components
 * would still faithfully render whatever they were given.
 */
internal val KansoMinTouchTarget: Dp = 48.dp

/** Elevation tokens (Material 3 tonal + shadow levels). */
@Immutable
public data class KansoElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

public val LocalKansoElevation: ProvidableCompositionLocal<KansoElevation> =
    staticCompositionLocalOf { KansoElevation() }

/**
 * Motion tokens — durations in milliseconds, plus the two easing curves kanso uses.
 *
 * Named by intent rather than by number, so a component says what kind of motion it wants and
 * the system decides how long that is. `quick` is for a state change the eye should register
 * without reading as animation; `standard` is the default for something entering or leaving;
 * `deliberate` is for motion the user is meant to watch. `shimmer` is one pass of a loading
 * placeholder — deliberately slow, because a fast shimmer reads as an error rather than as
 * waiting.
 *
 * These arrived with [dev.ayanami.kanso.component.KansoSkeleton], the first component that
 * animates. A token with no call site is a guess; this one has one.
 */
@Immutable
public data class KansoMotion(
    val quick: Int = 100,
    val standard: Int = 250,
    val deliberate: Int = 400,
    val shimmer: Int = 1200,
    /** For motion that enters or settles — most things. */
    val easing: Easing = FastOutSlowInEasing,
    /** For motion that only leaves, where slowing down at the end says nothing. */
    val exitEasing: Easing = LinearOutSlowInEasing,
)

public val LocalKansoMotion: ProvidableCompositionLocal<KansoMotion> =
    staticCompositionLocalOf { KansoMotion() }
