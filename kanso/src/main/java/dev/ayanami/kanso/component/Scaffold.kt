/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.ayanami.kanso.R
import dev.ayanami.kanso.theme.Kanso

/**
 * The standard kanso screen scaffold: a Material 3 top app bar that reacts to scroll, an
 * optional navigation icon + actions, an optional bottom bar and FAB, and a snackbar host. The
 * content receives the inner [PaddingValues] to consume the app-bar + system insets.
 *
 * The [title] is followed on the same line by the app's version — "CertWatch v5.33.11" —
 * resolved from the installed package by [kansoAppVersionLabel], so every kanso screen carries
 * the running build's version with no wiring, and the shown value can never drift from the
 * build it shipped in. Pass [version] to show a different string, rendered verbatim (a build
 * variant, a git hash), or an empty string for the rare screen that must not carry one.
 *
 * The title is centred by default; pass `centeredTitle = false` for the start-aligned bar that
 * suits a screen with several actions, or [titleContent] to put something other than text there
 * — a logo, or a title with a subtitle. [titleContent] wins over [title] when both are given,
 * and replaces the whole title line, version included.
 *
 * [largeTopBar] gives the tall header that collapses as the content scrolls, for a screen that
 * starts a hierarchy rather than sitting inside one — a settings root, a library home. It
 * overrides [centeredTitle], because a large app bar is start-aligned by definition. Do not use
 * it on a screen reached by a back button: a header that big under a back arrow spends a
 * quarter of the screen restating where the user just came from.
 *
 * For that back arrow, pass [KansoBackButton] as the [navigationIcon].
 *
 * The keyboard is deliberately *not* handled here. Applying `imePadding()` to the scaffold root
 * compresses the top bar every time the keyboard opens; a screen with a text field should apply
 * it to its own content, or pass [contentWindowInsets] as
 * `ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KansoScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    centeredTitle: Boolean = true,
    largeTopBar: Boolean = false,
    version: String = kansoAppVersionLabel(),
    titleContent: (@Composable () -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    // A large bar has to collapse to its small form as the content scrolls, which
    // enterAlwaysScrollBehavior does not do — it hides the bar outright and the title with it.
    val scrollBehavior = if (largeTopBar) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }
    val titleSlot: @Composable () -> Unit = titleContent ?: {
        if (version.isBlank()) {
            Text(title)
        } else {
            // The version inherits the bar's own title style — it is part of the name the
            // header states ("CertWatch v5.33.11"), not a caption beside it.
            Row(horizontalArrangement = Arrangement.spacedBy(Kanso.spacing.xs)) {
                Text(title, Modifier.alignByBaseline())
                Text(version, Modifier.alignByBaseline())
            }
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (largeTopBar) {
                LargeTopAppBar(
                    title = titleSlot,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            } else if (centeredTitle) {
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

/**
 * The back affordance for [KansoScaffold]'s `navigationIcon` slot.
 *
 * Four lines a caller could write themselves, and the two things they get wrong when they do:
 * an arrow with no content description, which a screen reader reads as an unlabelled button;
 * and an arrow that keeps pointing left in an RTL layout, where back is the other way. Both are
 * settled here — the icon is drawn with `autoMirror`, and the description is a translatable
 * resource.
 *
 * ```
 * KansoScaffold(title = "Details", navigationIcon = { KansoBackButton(onBack = ::finish) })
 * ```
 */
@Composable
public fun KansoBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onBack, modifier = modifier, enabled = enabled) {
        Icon(ArrowBackIcon, contentDescription = stringResource(R.string.kanso_back))
    }
}

/**
 * The version label of the app kanso is running inside: the installed package's `versionName`
 * prefixed with `v` — `versionName = "5.33.11"` reads back as `"v5.33.11"`. This is what
 * [KansoScaffold] shows after its title by default, and it is a variable rather than a literal
 * on purpose: the header always states the version of the build actually running, with nothing
 * for a release checklist to forget. A `versionName` that already starts with `v` is not
 * prefixed again, and an empty string comes back when the package declares no `versionName` or
 * cannot be resolved (a preview host, for instance).
 */
@Composable
public fun kansoAppVersionLabel(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val packageManager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(context.packageName, 0)
            }
            val name = info.versionName.orEmpty()
            if (name.isEmpty() || name.startsWith("v") || name.startsWith("V")) name
            else "v$name"
        }.getOrDefault("")
    }
}
