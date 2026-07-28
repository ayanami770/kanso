/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.theme.Kanso

// A settings row is not a KansoListItem with a control bolted on.
//
// The failure that makes these worth having: `KansoListItem(trailing = { Switch(...) })` gives
// you two independently focusable targets that a screen reader reads as unrelated, a row whose
// tap does nothing, and no state announcement. The fix is `Modifier.toggleable`/`selectable`
// with a Role on the *row*, and the control rendered as a non-interactive indicator — which is
// why these are written as their own Row rather than delegating.

private val RowMinHeight = 48.dp
private val LeadingIconSize = 24.dp

@Composable
private fun SettingRowBody(
    headline: String,
    supporting: String?,
    icon: ImageVector?,
    enabled: Boolean,
    control: @Composable () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        Modifier.padding(vertical = Kanso.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = Kanso.colors.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(LeadingIconSize),
            )
            Spacer(Modifier.size(Kanso.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(
                headline,
                style = Kanso.typography.bodyLarge,
                color = Kanso.colors.onSurface.copy(alpha = contentAlpha),
            )
            if (supporting != null) {
                Text(
                    supporting,
                    style = Kanso.typography.bodyMedium,
                    color = Kanso.colors.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
        Spacer(Modifier.size(Kanso.spacing.md))
        control()
    }
}

/**
 * A settings row whose whole width toggles a switch.
 *
 * The row carries the interaction and the `Switch` is a read-only indicator, so there is one
 * focusable target announced as a switch with its state — not a row and a control that disagree
 * about what was tapped. Held to the 48dp minimum at any font scale.
 */
@Composable
public fun KansoSwitchRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingRowBody(headline, supporting, icon, enabled) {
            // null: the row owns the interaction, so the control must not be a second target.
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

/** As [KansoSwitchRow], with a checkbox. */
@Composable
public fun KansoCheckboxRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingRowBody(headline, supporting, icon, enabled) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

/**
 * One option in a single-choice group.
 *
 * Wrap the group in `Modifier.selectableGroup()` so a screen reader announces "1 of 3" — a
 * radio row on its own cannot know how many siblings it has.
 */
@Composable
public fun KansoRadioRow(
    headline: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingRowBody(headline, supporting, icon, enabled) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
        }
    }
}
