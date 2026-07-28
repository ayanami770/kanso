/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso

/**
 * The meanings a status can carry. Deliberately a closed set — a design system's job here is to
 * stop every screen inventing its own green.
 */
public enum class KansoStatus { Success, Warning, Error, Info, Neutral }

/** Container / on-container pair for a status, resolved against the current theme. */
@Composable
private fun KansoStatus.colors(): Pair<Color, Color> = when (this) {
    KansoStatus.Success ->
        Kanso.extendedColors.successContainer to Kanso.extendedColors.onSuccessContainer

    KansoStatus.Warning ->
        Kanso.extendedColors.warningContainer to Kanso.extendedColors.onWarningContainer

    KansoStatus.Info ->
        Kanso.extendedColors.infoContainer to Kanso.extendedColors.onInfoContainer

    KansoStatus.Error -> Kanso.colors.errorContainer to Kanso.colors.onErrorContainer

    KansoStatus.Neutral -> Kanso.colors.surfaceVariant to Kanso.colors.onSurfaceVariant
}

/**
 * A small pill stating a status — "Valid", "Expiring", "Failed". Sized to sit in a
 * [KansoListItem]'s `trailing` slot.
 *
 * [text] is required rather than optional on purpose. The colour is a reinforcement, never the
 * message: a badge that says only "green" is unreadable to anyone who cannot separate it from
 * the amber one, and unreadable to a screen reader regardless. [icon] is decorative for the same
 * reason — the text already carries the meaning, so it takes no content description.
 */
@Composable
public fun KansoStatusBadge(
    text: String,
    status: KansoStatus,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val (container, onContainer) = status.colors()
    Row(
        modifier
            .semantics(mergeDescendants = true) {}
            .background(container, Kanso.shapes.small)
            .padding(horizontal = Kanso.spacing.sm, vertical = Kanso.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(Kanso.sizing.iconBadge),
            )
            Spacer(Modifier.size(Kanso.spacing.xs))
        }
        Text(text, style = Kanso.typography.labelSmall, color = onContainer)
    }
}

/**
 * A full-width banner stating something about the screen as a whole — an outage, a stale cache,
 * a degraded mode — with an optional action.
 *
 * Louder than a [KansoStatusBadge] and quieter than a dialog. If the message needs a decision
 * from the user before they can continue, it wants [KansoAlertDialog] instead.
 */
@Composable
public fun KansoInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    status: KansoStatus = KansoStatus.Info,
    icon: ImageVector? = null,
    action: @Composable (() -> Unit)? = null,
) {
    val (container, onContainer) = status.colors()
    Row(
        modifier
            .background(container, Kanso.shapes.medium)
            .padding(Kanso.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(Kanso.spacing.xl),
            )
            Spacer(Modifier.size(Kanso.spacing.md))
        }
        Text(
            text,
            style = Kanso.typography.bodyMedium,
            color = onContainer,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Spacer(Modifier.size(Kanso.spacing.sm))
            action()
        }
    }
}

/**
 * A hairline between list rows or sections.
 *
 * [inset] indents the line to line up with a row's text rather than its leading icon, so a
 * divider between two icon rows does not cut across the icon column.
 *
 * The indent is *derived* — the leading icon plus the gap [KansoListItem] puts after it — rather
 * than the 40dp that arithmetic currently comes to. Written as a number it would be correct
 * today and silently wrong the moment `Kanso.sizing.icon` moved, in a way no test would catch:
 * the divider would still render, just no longer aligned to anything.
 */
@Composable
public fun KansoDivider(modifier: Modifier = Modifier, inset: Boolean = false) {
    val indent = if (inset) Kanso.sizing.icon + Kanso.spacing.lg else 0.dp
    HorizontalDivider(
        modifier = modifier.padding(start = indent),
        color = Kanso.colors.outlineVariant,
    )
}
