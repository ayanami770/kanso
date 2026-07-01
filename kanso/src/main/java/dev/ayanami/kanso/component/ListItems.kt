/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso

/** A one/two-line list row with an optional leading [icon] and trailing content. */
@Composable
fun KansoListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Kanso.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Kanso.colors.primary,
                modifier = Modifier.size(24.dp).padding(end = Kanso.spacing.none))
            androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(headline, style = Kanso.typography.bodyLarge, color = Kanso.colors.onSurface)
            if (supporting != null) {
                Text(supporting, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.md))
            trailing()
        }
    }
}

/** A key/value status row: a muted [label] on the left, an emphasised [value] on the right. */
@Composable
fun KansoStatusRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = Kanso.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurface)
    }
}
