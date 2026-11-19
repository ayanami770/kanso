/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
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
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.ayanami.kanso.R
import dev.ayanami.kanso.theme.Kanso
import kotlin.math.max
import kotlin.math.min

/**
 * The standard kanso screen scaffold: a Material 3 top app bar that reacts to scroll, an
 * optional navigation icon + actions, an optional bottom bar and FAB, and a snackbar host. The
 * content receives the inner [PaddingValues] to consume the app-bar + system insets.
 *
 * The [title] is followed on the same line by the app's version — "Acme v1.2.3" — resolved from
 * the installed package by [kansoAppVersionLabel], so every kanso screen carries the running
 * build's version with no wiring, and the shown value can never drift from the build it shipped
 * in. Pass [version] to show a different string, rendered verbatim (a build variant, a git
 * hash), or an empty string for the rare screen that must not carry one.
 *
 * Neither text wraps. A title too long for the bar is ellipsised — on one line, or two under
 * [largeTopBar] — and the version is drawn only when the whole of it fits beside the title: cut
 * short, "v1.2.31" reads as "v1.2.3", which is worse than not showing it. The pair is a single
 * heading for accessibility services.
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
 * [containerColor] is the screen's background and [contentColor] what is drawn on it by
 * default. Left unset, the content colour is the role the scheme pairs with the container — or,
 * for a background from outside the scheme, whichever of the scheme's text colours reads better
 * on it.
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
    contentColor: Color = Color.Unspecified,
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
        // A large bar has room for a second line of title; a single-row bar does not.
        TitleLine(title = title, version = version, maxTitleLines = if (largeTopBar) 2 else 1)
    }
    // Unspecified means "whatever Scaffold would have used" — its own default, the theme
    // background. Passing a concrete colour through unconditionally would hard-code that choice
    // and break a consumer that re-themes the background.
    val container = containerColor.takeOrElse { Kanso.colors.background }
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
        containerColor = container,
        contentColor = contentColor.takeOrElse { scaffoldContentColor(Kanso.colors, container) },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

/**
 * What a scaffold draws on [container] when the caller named no content colour.
 *
 * A role of the scheme gets the role Material pairs with it. A colour from outside the scheme
 * has no pair, and Material's own answer is whatever `LocalContentColor` holds at that point —
 * black at the top of a composition, which is unreadable on a dark background of the caller's
 * own. So the fallback is chosen by contrast instead: whichever of the scheme's two text colours
 * reads better on the colour actually given.
 */
internal fun scaffoldContentColor(scheme: ColorScheme, container: Color): Color =
    scheme.contentColorFor(container).takeOrElse {
        // A translucent container shows the background through it; judge what will be seen.
        val seen = container.compositeOver(scheme.background)
        val onSurface = contrastRatio(scheme.onSurface, seen)
        val inverse = contrastRatio(scheme.inverseOnSurface, seen)
        if (onSurface >= inverse) scheme.onSurface else scheme.inverseOnSurface
    }

/** The WCAG contrast ratio between two opaque colours. */
private fun contrastRatio(a: Color, b: Color): Float {
    val lighter = max(a.luminance(), b.luminance())
    val darker = min(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * The scaffold's title and, when there is room for the whole of it, the version after it.
 *
 * A `Row` of two texts is the obvious way to write this, and it fails at every width that
 * matters. Once the title fills the bar the version is measured with nothing left, wraps one
 * character per line and runs down the screen; with a little room left it breaks mid-number.
 * The rule here is that neither text wraps on the other's account: the title is ellipsised at
 * [maxTitleLines], and the version is drawn whole or not at all, because a clipped version is a
 * different version.
 */
@Composable
private fun TitleLine(title: String, version: String, maxTitleLines: Int) {
    val gap = Kanso.spacing.xs
    Layout(
        content = {
            Text(title, maxLines = maxTitleLines, overflow = TextOverflow.Ellipsis)
            // The version inherits the bar's own title style — it is part of the name the
            // header states ("Acme v1.2.3"), not a caption beside it.
            if (version.isNotBlank()) Text(version, maxLines = 1, softWrap = false)
        },
        // One stop for a screen reader, announced as the heading it is. Read apart, "Peers" and
        // "v1.4.2" are two unrelated fragments, and a title that is not a heading cannot be
        // reached by heading navigation the way a card's title can.
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titleMeasurable = measurables[0]
        val versionMeasurable = measurables.getOrNull(1)
        val gapPx = gap.roundToPx()

        val versionPlaceable = versionMeasurable?.let { measurable ->
            val wanted = titleMeasurable.maxIntrinsicWidth(loose.maxHeight) + gapPx +
                measurable.maxIntrinsicWidth(loose.maxHeight)
            if (!constraints.hasBoundedWidth || wanted <= constraints.maxWidth) {
                measurable.measure(loose)
            } else {
                null
            }
        }
        val titleMaxWidth = if (versionPlaceable != null && constraints.hasBoundedWidth) {
            (constraints.maxWidth - gapPx - versionPlaceable.width).coerceAtLeast(0)
        } else {
            constraints.maxWidth
        }
        val titlePlaceable = titleMeasurable.measure(loose.copy(maxWidth = titleMaxWidth))

        // Both texts share the bar's title style, so these offsets are normally zero; they only
        // matter when a caller's typography gives the two different metrics.
        val titleBaseline = titlePlaceable[FirstBaseline]
        val versionBaseline = versionPlaceable?.get(FirstBaseline) ?: AlignmentLine.Unspecified
        val aligned = titleBaseline != AlignmentLine.Unspecified &&
            versionBaseline != AlignmentLine.Unspecified
        val baseline = if (aligned) max(titleBaseline, versionBaseline) else 0
        val titleY = if (aligned) baseline - titleBaseline else 0
        val versionY = if (aligned) baseline - versionBaseline else 0

        val width = titlePlaceable.width + (versionPlaceable?.let { gapPx + it.width } ?: 0)
        val height = max(
            titleY + titlePlaceable.height,
            versionY + (versionPlaceable?.height ?: 0),
        )
        layout(width, height) {
            titlePlaceable.placeRelative(0, titleY)
            versionPlaceable?.placeRelative(titlePlaceable.width + gapPx, versionY)
        }
    }
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
            if (name.isEmpty() || name.startsWith("v") || name.startsWith("V")) {
                name
            } else {
                "v$name"
            }
        }.getOrDefault("")
    }
}
