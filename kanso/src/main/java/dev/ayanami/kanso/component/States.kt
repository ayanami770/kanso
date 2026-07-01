/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * A centred full-area placeholder: a large [icon], a [title] and optional [description] with an
 * optional call-to-action. Use for empty lists, first-run states and non-fatal errors.
 */
@Composable
fun KansoEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(Kanso.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Kanso.colors.onSurfaceVariant,
            modifier = Modifier.size(56.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.lg))
        Text(title, style = Kanso.typography.titleMedium, color = Kanso.colors.onSurface,
            textAlign = TextAlign.Center)
        if (description != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.sm))
            Text(description, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
        if (action != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.xl))
            action()
        }
    }
}

/** A centred progress indicator with an optional [label] — the standard full-area loading state. */
@Composable
fun KansoLoadingState(modifier: Modifier = Modifier, label: String? = null) {
    Column(
        modifier.fillMaxSize().padding(Kanso.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        if (label != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.lg))
            Text(label, style = Kanso.typography.bodyMedium, color = Kanso.colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
