/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.ayanami.kanso.theme.Kanso

/**
 * A grouped section as a filled Material 3 card with an optional [title] (accented) and
 * [subtitle]. Children are laid out in a padded column — the standard "settings section" look.
 *
 * The [title] is marked as a heading, so a screen-reader user can skim a long screen by its
 * sections instead of stepping through every row. [titleTrailing] takes the "See all" link or
 * overflow button that belongs on the title row.
 *
 * Pass [onClick] to make the whole card tappable rather than wrapping it in
 * `Modifier.clickable` — the modifier lands outside the card's internal `Surface`, so its
 * ripple is not clipped to the corner radius and renders as a rectangle bleeding past the card
 * edge. Set [contentPadding] to `PaddingValues(0.dp)` for edge-to-edge content such as an
 * image or a full-bleed list.
 */
@Composable
public fun KansoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    titleTrailing: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Kanso.spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(contentPadding)) {
            if (title != null || titleTrailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) {
                        Text(
                            title,
                            style = Kanso.typography.titleMedium,
                            color = titleColor.takeOrElse { Kanso.colors.primary },
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    titleTrailing?.invoke()
                }
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = Kanso.typography.bodySmall,
                    color = Kanso.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = Kanso.spacing.xxs),
                )
            }
            if (title != null || subtitle != null || titleTrailing != null) {
                Spacer(Modifier.height(Kanso.spacing.md))
            }
            content()
        }
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(),
            content = body,
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(),
            content = body,
        )
    }
}

/**
 * A standalone section header (title + optional supporting line) for use outside a card.
 *
 * The title wraps rather than truncating: it is a short label the user chose to read, and
 * ellipsising it at a large font scale turns the one piece of orienting text on the screen
 * into "Encrypted tra…". [trailing] takes the "See all" link that belongs on the title row.
 */
@Composable
public fun KansoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = Kanso.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = Kanso.typography.titleSmall,
                color = Kanso.colors.primary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            trailing?.invoke()
        }
        if (supporting != null) {
            Text(
                supporting,
                style = Kanso.typography.bodySmall,
                color = Kanso.colors.onSurfaceVariant,
            )
        }
    }
}
