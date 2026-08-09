/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.R
import dev.ayanami.kanso.theme.Kanso

/**
 * Emphasis levels for [KansoButton], mapped to the Material 3 button hierarchy.
 *
 * [Destructive] is a filled button resolved against the error role rather than a raw `colors`
 * override, so "this action deletes something" is a decision the design system makes once
 * instead of every screen re-deriving it.
 */
public enum class KansoButtonStyle { Filled, Tonal, Outlined, Text, Elevated, Destructive }

/**
 * One button with a consistent emphasis system, optional leading [icon] and a [loading]
 * spinner that disables the button while work is in flight. Use [KansoButtonStyle.Filled]
 * for the single primary action on a screen, Tonal/Outlined for secondary, Text for tertiary.
 *
 * While [loading], the label stays laid out but invisible with the spinner centred over it,
 * so the button never changes width the instant work starts.
 */
@Composable
public fun KansoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KansoButtonStyle = KansoButtonStyle.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val loadingLabel = stringResource(R.string.kanso_loading)
    val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Row(
                // Holds the label's intrinsic width while the spinner shows, so swapping in
                // the spinner cannot resize the button.
                modifier = Modifier.alpha(if (loading) 0f else 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(Kanso.sizing.iconSmall),
                    )
                    Spacer(Modifier.size(Kanso.spacing.sm))
                }
                Text(text)
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Kanso.sizing.iconSmall),
                    // Without this the indicator falls back to colorScheme.primary and ignores
                    // the content colour the button provides — brand-saturated over the
                    // container, next to a disabled-emphasis label.
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                )
            }
        }
    }
    val on = enabled && !loading
    // Loading disables the button, which on its own announces only "disabled". The state
    // description is what tells a screen-reader user that work is actually in flight.
    val buttonModifier = if (loading) {
        modifier.semantics { stateDescription = loadingLabel }
    } else {
        modifier
    }
    when (style) {
        KansoButtonStyle.Filled ->
            Button(onClick, buttonModifier, enabled = on) { content() }

        KansoButtonStyle.Destructive ->
            Button(
                onClick,
                buttonModifier,
                enabled = on,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Kanso.colors.error,
                    contentColor = Kanso.colors.onError,
                ),
            ) { content() }

        KansoButtonStyle.Tonal ->
            FilledTonalButton(onClick, buttonModifier, enabled = on) { content() }

        KansoButtonStyle.Outlined ->
            OutlinedButton(onClick, buttonModifier, enabled = on) { content() }

        KansoButtonStyle.Text ->
            TextButton(onClick, buttonModifier, enabled = on) { content() }

        KansoButtonStyle.Elevated ->
            ElevatedButton(onClick, buttonModifier, enabled = on) { content() }
    }
}
