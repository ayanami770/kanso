/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.ayanami.kanso.theme.Kanso

/**
 * The standard kanso screen scaffold: a Material 3 top app bar that reacts to scroll, an
 * optional navigation icon + actions, an optional bottom bar and FAB, and a snackbar host. The
 * content receives the inner [PaddingValues] to consume the app-bar + system insets.
 *
 * The title is centred by default; pass `centeredTitle = false` for the start-aligned bar that
 * suits a screen with several actions, or [titleContent] to put something other than text there
 * — a logo, or a title with a subtitle. [titleContent] wins over [title] when both are given.
 *
 * The keyboard is deliberately *not* handled here. Applying `imePadding()` to the scaffold root
 * compresses the top bar every time the keyboard opens; a screen with a text field should apply
 * it to its own content, or pass [contentWindowInsets] as
 * `ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KansoScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    centeredTitle: Boolean = true,
    titleContent: (@Composable () -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val titleSlot: @Composable () -> Unit = titleContent ?: { Text(title) }
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (centeredTitle) {
                CenterAlignedTopAppBar(
                    title = titleSlot,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = titleSlot,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = floatingActionButton,
        // Unspecified means "whatever Scaffold would have used" — its own default, the
        // theme background. Passing a concrete colour through unconditionally would hard-code
        // that choice and break a consumer that re-themes the background.
        containerColor = containerColor.takeOrElse { Kanso.colors.background },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
