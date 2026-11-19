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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
 * [alignment] places the capped content when there is width to spare. It is centred by
 * default, and any `Alignment.Horizontal` is honoured — the absolute and the biased ones too.
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
    val boxAlignment = remember(alignment) { alignment.toBoxAlignment() }
    Box(modifier.fillMaxWidth(), contentAlignment = boxAlignment) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}

/**
 * Any horizontal alignment as the two-dimensional one a `Box` takes, pinned to the top.
 *
 * It goes through the alignment's own `align` instead of a table of the usual three. A table
 * has to end in an `else`, and that is where `AbsoluteAlignment.Left` and every
 * `BiasAlignment.Horizontal` used to land: silently centred.
 */
private fun Alignment.Horizontal.toBoxAlignment(): Alignment {
    val horizontal = this
    return Alignment { size, space, layoutDirection ->
        IntOffset(horizontal.align(size.width, space.width, layoutDirection), 0)
    }
}
