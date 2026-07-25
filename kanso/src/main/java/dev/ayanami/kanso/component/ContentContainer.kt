/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.ayanami.kanso.theme.Kanso

/**
 * Caps the width of a screen's content and centres it.
 *
 * Every kanso component calls `fillMaxWidth()` with no maximum, which is right on a phone and
 * wrong on anything wider: at a 1280dp window, body text runs to roughly 160 characters a line,
 * around three times the length that stays readable. This is the cheapest fix for the most
 * visible "built for a phone" symptom, and it needs no component changes at all — because each
 * component applies the caller's modifier *first*, an outer `widthIn(max = …)` narrows the
 * incoming constraint and their `fillMaxWidth()` resolves to the smaller width.
 *
 * ```
 * KansoScaffold(title = "Settings") { inner ->
 *     KansoContentContainer(Modifier.padding(inner)) {
 *         Column { … }
 *     }
 * }
 * ```
 *
 * It deliberately applies no horizontal padding of its own — screens already pad with
 * `Kanso.spacing.screen`, and adding more here would double it everywhere.
 *
 * This is not the whole adaptive story. A list-detail layout or a navigation rail is a
 * different problem; wrap Material's `NavigationSuiteScaffold` for that rather than
 * hand-rolling a width switch.
 */
@Composable
public fun KansoContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Kanso.spacing.contentMaxWidth,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = alignment.toBoxAlignment()) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}

private fun Alignment.Horizontal.toBoxAlignment(): Alignment = when (this) {
    Alignment.Start -> Alignment.TopStart
    Alignment.End -> Alignment.TopEnd
    else -> Alignment.TopCenter
}
