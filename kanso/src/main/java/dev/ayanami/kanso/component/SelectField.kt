/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.ayanami.kanso.theme.Kanso

/**
 * A field whose value is chosen from [options] rather than typed — a unit, a filter, a category.
 *
 * Generic over the option type on purpose. The alternative — `List<String>` and an index — makes
 * every call site convert to strings on the way in and back to a domain type on the way out,
 * which is where the off-by-one lives. Here [onSelect] hands back the option itself and
 * [optionLabel] is the only thing that knows about text.
 *
 * ```
 * KansoSelectField(
 *     value = unit,
 *     options = Unit.entries,
 *     onSelect = { unit = it },
 *     label = "Unit",
 *     optionLabel = { it.symbol },
 * )
 * ```
 *
 * The anchor is read-only: it opens the menu and never the keyboard, so this is a picker rather
 * than a combo box that also accepts free text. A null [value] leaves the field blank, with the
 * [label] resting inside it — there is deliberately no `placeholder` parameter, because
 * Material 3 draws a placeholder only while the field has focus, and a picker that never takes
 * focus would carry one that never shows.
 *
 * The menu marks the option in force with a tick and as selected for a screen reader, and it
 * does not open at all while [options] is empty. Every option is composed when the menu opens,
 * so this is a picker for a handful of choices — tens, not hundreds; a long list wants a
 * searchable screen of its own.
 *
 * Material 3's experimental `ExposedDropdownMenuBox` opt-in is absorbed here, so a consumer
 * never inherits it — the same contract [KansoScaffold] and [KansoBottomSheet] keep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun <T> KansoSelectField(
    value: T?,
    options: List<T>,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
    supporting: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    // With nothing to choose from there is nothing to open: an empty menu is a sliver of popup
    // that takes the tap and offers no way forward.
    val canOpen = enabled && options.isNotEmpty()
    // Being disabled must actually close the menu. `enabled` only gated opening; if the consumer
    // disables the field (an authorization/loading/prerequisite state) WHILE the menu is open, the
    // stored `expanded` stays true, the menu keeps rendering, and its items keep firing onSelect.
    // Collapse on disable, and render/allow selection only while enabled, so a picker cannot be
    // driven after the UI meant to block it.
    LaunchedEffect(canOpen) { if (!canOpen) expanded = false }
    ExposedDropdownMenuBox(
        expanded = expanded && canOpen,
        onExpandedChange = { if (canOpen) expanded = it },
        modifier = modifier,
    ) {
        KansoTextField(
            value = value?.let(optionLabel).orEmpty(),
            // Read-only, so this is never called; the menu is the only way the value changes.
            onValueChange = {},
            label = label,
            modifier = Modifier.menuAnchor(
                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = canOpen,
            ),
            supporting = supporting,
            isError = isError,
            errorText = errorText,
            enabled = enabled,
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded && canOpen,
            onDismissRequest = { expanded = false },
        ) {
            // The option in force is marked twice: a tick for the eye, and `selected` for a
            // screen reader, which otherwise hears a list of equals with no hint of which one
            // the field is showing.
            val tick: @Composable () -> Unit = { Icon(CheckIcon, contentDescription = null) }
            options.forEach { option ->
                val current = option == value
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = Kanso.typography.bodyLarge) },
                    onClick = {
                        if (!enabled) return@DropdownMenuItem
                        onSelect(option)
                        expanded = false
                    },
                    modifier = if (current) Modifier.semantics { selected = true } else Modifier,
                    trailingIcon = if (current) tick else null,
                )
            }
        }
    }
}
