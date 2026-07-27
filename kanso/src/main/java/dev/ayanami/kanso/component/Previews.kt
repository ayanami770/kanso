/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso
import dev.ayanami.kanso.theme.KansoTheme

/**
 * The configurations every kanso component is reviewed in.
 *
 * Every layout defect this library has had was a render-at-a-non-default-configuration bug:
 * a header that ellipsised at 200% font scale, an empty state whose call-to-action fell off a
 * bounded parent, a list row that dropped under the 48dp touch target. None are visible while
 * developing at default settings, which is exactly why they survived. Rendering all four
 * configurations side by side is the cheapest way to keep catching them.
 *
 * Dark mode is here rather than in a separate annotation because a colour-scheme bug and a
 * layout bug look identical in a single-configuration preview. RTL uses Arabic; kanso uses
 * start/end throughout, so a preview that mirrors cleanly is the evidence for that claim.
 */
@Preview(name = "light", group = "theme")
@Preview(name = "dark", group = "theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "font 200%", group = "a11y", fontScale = 2f)
@Preview(name = "rtl", group = "a11y", locale = "ar")
internal annotation class KansoPreviews

/**
 * Hosts a preview in the kanso theme on a real themed surface.
 *
 * `dynamicColor = false` on purpose: previews must show the brand scheme, which is the thing
 * under review, not a wallpaper-derived one.
 */
@Composable
private fun PreviewHost(content: @Composable () -> Unit) {
    KansoTheme(dynamicColor = false) {
        Surface(color = Kanso.colors.surface) {
            Box(Modifier.padding(Kanso.spacing.lg)) { content() }
        }
    }
}

/**
 * A stand-in icon for the previews, drawn here rather than taken from material-icons-core.
 * kanso deliberately ships no icon set — a consumer chooses their own — and pulling one in so
 * that development previews can render would put it on every consumer's runtime classpath.
 * The previews are checking layout, not iconography, so one neutral glyph is enough.
 */
private val PreviewIcon: ImageVector = ImageVector.Builder(
    name = "kanso_preview_icon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    // Icon() tints the whole vector, so this fill is only a placeholder.
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
        reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
        reflectiveCurveTo(17.52f, 2f, 12f, 2f)
        close()
    }
}.build()

// ---- buttons --------------------------------------------------------------------------

@KansoPreviews
@Composable
private fun KansoButtonPreview() = PreviewHost {
    Column {
        KansoButtonStyle.entries.forEach { style ->
            KansoButton(
                text = style.name,
                onClick = {},
                style = style,
                icon = PreviewIcon,
                modifier = Modifier.padding(bottom = Kanso.spacing.sm),
            )
        }
    }
}

/**
 * The loading state must not change the button's width — compare this against
 * [KansoButtonPreview] at the same configuration.
 */
@KansoPreviews
@Composable
private fun KansoButtonLoadingPreview() = PreviewHost {
    Column {
        KansoButton("Save changes", onClick = {})
        KansoButton(
            "Save changes",
            onClick = {},
            loading = true,
            modifier = Modifier.padding(top = Kanso.spacing.sm),
        )
        KansoButton(
            "Save changes",
            onClick = {},
            enabled = false,
            modifier = Modifier.padding(top = Kanso.spacing.sm),
        )
    }
}

// ---- fields ---------------------------------------------------------------------------

/** The reveal toggle in both states, with the masked default beside it. */
@KansoPreviews
@Composable
private fun KansoPasswordFieldPreview() = PreviewHost {
    Column {
        KansoPasswordField(value = "hunter2", onValueChange = {}, label = "Password")
        KansoPasswordField(
            value = "1234",
            onValueChange = {},
            label = "Control PIN",
            numeric = true,
            isError = true,
            errorText = "PIN must be at least 6 digits.",
        )
    }
}

@KansoPreviews
@Composable
private fun KansoTextFieldPreview() = PreviewHost {
    Column {
        KansoTextField(
            value = "ayanami",
            onValueChange = {},
            label = "Display name",
            supporting = "Shown to peers on the control channel.",
        )
        KansoTextField(
            value = "1234",
            onValueChange = {},
            label = "Control PIN",
            isError = true,
            errorText = "PIN must be at least 6 digits.",
            modifier = Modifier.padding(top = Kanso.spacing.md),
        )
    }
}

// ---- surfaces -------------------------------------------------------------------------

/** The seams added for real screens: a clickable card, a title row action, no padding. */
@KansoPreviews
@Composable
private fun KansoCardSeamsPreview() = PreviewHost {
    Column {
        KansoCard(
            title = "Peers",
            titleTrailing = { KansoButton("See all", onClick = {}, style = KansoButtonStyle.Text) },
            onClick = {},
        ) {
            KansoStatusRow("Online", "3")
        }
        KansoCard(contentPadding = PaddingValues(Kanso.spacing.none)) {
            KansoListItem(headline = "Edge-to-edge row", onClick = {})
        }
    }
}

/** Destructive sits beside Filled so the emphasis difference is visible, not asserted. */
@KansoPreviews
@Composable
private fun KansoDestructiveButtonPreview() = PreviewHost {
    Column {
        KansoButton("Save", onClick = {})
        KansoButton(
            "Delete everything",
            onClick = {},
            style = KansoButtonStyle.Destructive,
            modifier = Modifier.padding(top = Kanso.spacing.sm),
        )
    }
}

/** The extended roles, which are fixed literals rather than seed-derived. */
@KansoPreviews
@Composable
private fun KansoExtendedColorsPreview() = PreviewHost {
    Column {
        listOf(
            "Success" to (Kanso.extendedColors.successContainer to Kanso.extendedColors.onSuccessContainer),
            "Warning" to (Kanso.extendedColors.warningContainer to Kanso.extendedColors.onWarningContainer),
            "Info" to (Kanso.extendedColors.infoContainer to Kanso.extendedColors.onInfoContainer),
        ).forEach { (label, pair) ->
            Surface(
                color = pair.first,
                shape = Kanso.shapes.small,
                modifier = Modifier.padding(bottom = Kanso.spacing.sm),
            ) {
                Text(
                    label,
                    color = pair.second,
                    modifier = Modifier.padding(Kanso.spacing.md),
                )
            }
        }
    }
}

@KansoPreviews
@Composable
private fun KansoCardPreview() = PreviewHost {
    KansoCard(title = "Connection", subtitle = "Encrypted transport status") {
        KansoStatusRow("Protocol", "TLS 1.3")
        KansoStatusRow("Cipher", "TLS_AES_256_GCM_SHA384")
    }
}

/** A long title is the case that used to ellipsise — it must wrap at every font scale. */
@KansoPreviews
@Composable
private fun KansoSectionHeaderPreview() = PreviewHost {
    Column {
        KansoSectionHeader("Encrypted transport", supporting = "Applies to every channel.")
        KansoSectionHeader("Encrypted transport configuration and peer verification")
    }
}

// ---- list items -----------------------------------------------------------------------

@KansoPreviews
@Composable
private fun KansoListItemPreview() = PreviewHost {
    Column {
        KansoListItem(
            headline = "Search peers",
            supporting = "Discover devices on the local network",
            icon = PreviewIcon,
            onClick = {},
        )
        KansoListItem(
            headline = "Background sync",
            icon = PreviewIcon,
            trailing = { Switch(checked = true, onCheckedChange = {}) },
        )
        KansoListItem(headline = "No icon, no trailing, not clickable")
    }
}

/** A long value must wrap in its own half rather than starving the label. */
@KansoPreviews
@Composable
private fun KansoStatusRowPreview() = PreviewHost {
    Column {
        KansoStatusRow("Last sync", "2 minutes ago")
        KansoStatusRow("Fingerprint", "SHA256:pOxq7l9mNc5vVQr2wZ8kT1hYbJ4uE6aD0fGsX3iL")
    }
}

// ---- states ---------------------------------------------------------------------------

/** The call-to-action is the first thing lost when this overflows — it must stay visible. */
@KansoPreviews
@Composable
private fun KansoEmptyStatePreview() = PreviewHost {
    KansoEmptyState(
        icon = PreviewIcon,
        title = "Nothing here yet",
        description = "Empty, loading and error states share one visual language so every " +
            "screen feels the same.",
        action = { KansoButton("Add a peer", onClick = {}) },
    )
}

@KansoPreviews
@Composable
private fun KansoLoadingStatePreview() = PreviewHost {
    KansoLoadingState(label = "Establishing session…")
}

// ---- content container ----------------------------------------------------------------

/**
 * Only meaningful at a wide window — hence the extra device preview here rather than on the
 * shared annotation, where it would triple the matrix for components that have no adaptive
 * behaviour to show.
 */
@Preview(name = "tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(name = "phone", device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun KansoContentContainerPreview() = PreviewHost {
    KansoContentContainer {
        KansoCard(title = "Readable width") {
            Text(
                "Every kanso component fills the width it is given. On a phone that is right; " +
                    "on a 1280dp window it runs body text to about 160 characters a line, " +
                    "roughly three times what stays comfortable to read. This container caps " +
                    "it and centres what is left.",
                style = Kanso.typography.bodyMedium,
                color = Kanso.colors.onSurface,
            )
        }
    }
}

// ---- scaffold -------------------------------------------------------------------------

/**
 * The version is passed explicitly because a preview host resolves no real package — what is
 * under review is the two-line title block the default produces in a running app.
 */
@KansoPreviews
@Composable
private fun KansoScaffoldPreview() = KansoTheme(dynamicColor = false) {
    KansoScaffold(title = "Peers", version = "1.4.2") { inner ->
        Column(Modifier.padding(inner).padding(horizontal = Kanso.spacing.screen)) {
            KansoSectionHeader("Nearby")
            KansoListItem(headline = "dao-node", supporting = "192.168.1.24", onClick = {})
            KansoListItem(headline = "edge-01", supporting = "192.168.1.31", onClick = {})
        }
    }
}

/** The start-aligned bar with actions and a bottom bar — the shape a real app shell takes. */
@KansoPreviews
@Composable
private fun KansoScaffoldWithBarsPreview() = KansoTheme(dynamicColor = false) {
    KansoScaffold(
        title = "Peers",
        version = "1.4.2",
        centeredTitle = false,
        actions = {
            IconButton(onClick = {}) { Icon(PreviewIcon, contentDescription = "Filter") }
        },
        bottomBar = {
            NavigationBar {
                listOf("Peers", "Activity", "Settings").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = i == 0,
                        onClick = {},
                        icon = { Icon(PreviewIcon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { inner ->
        Column(Modifier.padding(inner).padding(horizontal = Kanso.spacing.screen)) {
            KansoListItem(headline = "dao-node", supporting = "192.168.1.24", onClick = {})
        }
    }
}
