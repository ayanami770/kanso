/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso

/** Emphasis levels for [KansoButton], mapped to the Material 3 button hierarchy. */
enum class KansoButtonStyle { Filled, Tonal, Outlined, Text, Elevated }

/**
 * One button with a consistent emphasis system, optional leading [icon] and a [loading]
 * spinner that disables the button while work is in flight. Use [KansoButtonStyle.Filled]
 * for the single primary action on a screen, Tonal/Outlined for secondary, Text for tertiary.
 */
@Composable
fun KansoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KansoButtonStyle = KansoButtonStyle.Filled,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            when {
                loading -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            if (loading || icon != null) androidx.compose.foundation.layout.Spacer(Modifier.size(Kanso.spacing.sm))
            Text(text)
        }
    }
    val on = enabled && !loading
    when (style) {
        KansoButtonStyle.Filled -> Button(onClick, modifier, enabled = on) { content() }
        KansoButtonStyle.Tonal -> FilledTonalButton(onClick, modifier, enabled = on) { content() }
        KansoButtonStyle.Outlined -> OutlinedButton(onClick, modifier, enabled = on) { content() }
        KansoButtonStyle.Text -> TextButton(onClick, modifier, enabled = on) { content() }
        KansoButtonStyle.Elevated -> ElevatedButton(onClick, modifier, enabled = on) { content() }
    }
}
