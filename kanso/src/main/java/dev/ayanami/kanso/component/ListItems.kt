/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso

/** The Material minimum touch target — a tappable row must clear this at any font scale. */
private val MinTouchTarget = 48.dp

private val LeadingIconSize = 24.dp

/**
 * A one/two-line list row with an optional leading [icon] or [leading] slot, and trailing
 * content. Use [leading] for an avatar or a checkbox — anything that is not an [ImageVector];
 * it takes precedence over [icon].
 *
 * The headline and supporting line are merged into a single accessibility node, so a screen
 * reader reads the row as one item rather than two disconnected stops. A row with [onClick]
 * is also held to the minimum touch target, which the vertical padding alone does not
 * guarantee once the user shrinks their font scale.
 */
@Composable
public fun KansoListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    leading: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    // `clickable` merges descendants itself, so this path already reads as a
                    // single node.
                    Modifier
                        .heightIn(min = MinTouchTarget)
                        .clickable(onClick = onClick)
                } else {
                    Modifier.semantics(mergeDescendants = true) {}
                },
            )
            .padding(vertical = Kanso.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // `icon` is the shorthand; `leading` is the escape hatch for an avatar, a checkbox or
        // anything else that is not an ImageVector. An explicit `leading` wins.
        val leadingSlot = leading ?: icon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    tint = Kanso.colors.primary,
                    modifier = Modifier.size(LeadingIconSize),
                )
            }
        }
        if (leadingSlot != null) {
            leadingSlot()
            Spacer(Modifier.size(Kanso.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(headline, style = Kanso.typography.bodyLarge, color = Kanso.colors.onSurface)
            if (supporting != null) {
                Text(
                    supporting,
                    style = Kanso.typography.bodyMedium,
                    color = Kanso.colors.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.size(Kanso.spacing.md))
            trailing()
        }
    }
}

/**
 * A key/value status row: a muted [label] on the left, an emphasised [value] on the right.
 * Both sides are weighted so a long value wraps within its own half (right-aligned) instead of
 * starving the label into a one-character-per-line column.
 *
 * The pair is merged into one accessibility node — read apart, a label and its value lose the
 * association that is the entire point of the row.
 */
@Composable
public fun KansoStatusRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Kanso.spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = Kanso.typography.bodyMedium,
            color = Kanso.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = Kanso.spacing.sm),
        )
        Text(
            value,
            style = Kanso.typography.bodyMedium,
            color = Kanso.colors.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
