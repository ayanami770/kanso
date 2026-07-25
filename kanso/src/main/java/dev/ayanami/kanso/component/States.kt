/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso

private val EmptyStateIconSize = 56.dp

/**
 * A centred full-area placeholder: a large [icon], a [title] and optional [description] with an
 * optional call-to-action. Use for empty lists, first-run states and non-fatal errors.
 *
 * At a large font scale — or in landscape — this content can outgrow a bounded parent, and the
 * [action] is the first thing to go, stranding the user with no way out. Pass a [scrollState]
 * when the parent does not already scroll, so the whole state stays reachable. Leave it null
 * if the caller scrolls: nesting two vertical scrolls measures this one with an unbounded
 * height and throws.
 */
@Composable
fun KansoEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    scrollState: ScrollState? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .then(if (scrollState != null) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(Kanso.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Kanso.colors.onSurfaceVariant,
            modifier = Modifier.size(EmptyStateIconSize))
        Spacer(Modifier.size(Kanso.spacing.lg))
        Text(title, style = Kanso.typography.titleMedium, color = Kanso.colors.onSurface,
            textAlign = TextAlign.Center)
        if (description != null) {
            Spacer(Modifier.size(Kanso.spacing.sm))
            Text(description, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.size(Kanso.spacing.xl))
            action()
        }
    }
}

/**
 * A centred progress indicator with an optional [label] — the standard full-area loading state.
 *
 * See [KansoEmptyState] for when to pass [scrollState].
 */
@Composable
fun KansoLoadingState(
    modifier: Modifier = Modifier,
    label: String? = null,
    scrollState: ScrollState? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .then(if (scrollState != null) Modifier.verticalScroll(scrollState) else Modifier)
            .padding(Kanso.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        if (label != null) {
            Spacer(Modifier.size(Kanso.spacing.lg))
            Text(label, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
