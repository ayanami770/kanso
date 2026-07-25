/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.ayanami.kanso.theme.Kanso

/**
 * A grouped section as a filled Material 3 card with an optional [title] (accented) and
 * [subtitle]. Children are laid out in a padded column — the standard "settings section" look.
 *
 * The [title] is marked as a heading, so a screen-reader user can skim a long screen by its
 * sections instead of stepping through every row.
 */
@Composable
fun KansoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(Kanso.spacing.lg)) {
            if (title != null) {
                Text(
                    title,
                    style = Kanso.typography.titleMedium,
                    color = Kanso.colors.primary,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = Kanso.typography.bodySmall,
                    color = Kanso.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = Kanso.spacing.xxs),
                )
            }
            if (title != null || subtitle != null) {
                Spacer(Modifier.height(Kanso.spacing.md))
            }
            content()
        }
    }
}

/**
 * A standalone section header (title + optional supporting line) for use outside a card.
 *
 * The title wraps rather than truncating: it is a short label the user chose to read, and
 * ellipsising it at a large font scale turns the one piece of orienting text on the screen
 * into "Encrypted tra…".
 */
@Composable
fun KansoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Kanso.spacing.sm)) {
        Text(
            title,
            style = Kanso.typography.titleSmall,
            color = Kanso.colors.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (supporting != null) {
            Text(supporting, style = Kanso.typography.bodySmall, color = Kanso.colors.onSurfaceVariant)
        }
    }
}
