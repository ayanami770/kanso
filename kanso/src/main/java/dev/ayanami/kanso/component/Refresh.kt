/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ayanami.kanso.theme.Kanso

/**
 * Wraps a scrollable list so that dragging past the top refreshes it.
 *
 * Thin by design — the pull gesture, the threshold and the indicator's own animation are
 * Material 3's, and re-deriving them would be a worse version of the same thing. What kanso
 * fixes is the indicator, which otherwise draws on `surfaceContainerHigh` with an
 * `onSurfaceVariant` arc no matter what the theme says — a grey spinner in a branded app.
 *
 * [content] is the scrollable itself — a `LazyColumn`, or a `Column` with `verticalScroll`. The
 * gesture is driven by nested scroll, so a non-scrollable child simply never triggers it.
 */
@Composable
public fun KansoRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        contentAlignment = contentAlignment,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Kanso.colors.surfaceContainerHigh,
                color = Kanso.colors.primary,
            )
        },
        content = content,
    )
}
