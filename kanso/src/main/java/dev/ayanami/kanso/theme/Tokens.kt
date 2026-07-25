/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

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
     * point where the eye reliably finds the next line — see [KansoContentContainer], which is
     * what applies it.
     */
    val contentMaxWidth: Dp = 640.dp,
)

public val LocalKansoSpacing: ProvidableCompositionLocal<KansoSpacing> =
    staticCompositionLocalOf { KansoSpacing() }

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
