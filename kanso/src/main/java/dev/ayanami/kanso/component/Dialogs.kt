/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.ayanami.kanso.theme.Kanso

/**
 * The confirmation dialog every app needs and every app otherwise rebuilds slightly differently
 * — delete, log out, discard.
 *
 * Set [destructive] when confirming loses something the user cannot get back: the confirm button
 * turns to the error role. That is the design system making the "this is dangerous" decision
 * once, rather than each screen choosing its own red.
 *
 * Deliberately not a slot API. This is the nine-tenths case; a dialog that needs arbitrary
 * content is not this component, and Material 3's own `AlertDialog` is right there.
 */
@Composable
public fun KansoAlertDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    destructive: Boolean = false,
    icon: ImageVector? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (destructive) Kanso.colors.error else Kanso.colors.primary,
                )
            }
        },
        dismissButton = dismissText?.let {
            { TextButton(onClick = onDismiss) { Text(it) } }
        },
    )
}

/**
 * A modal bottom sheet with the kanso container colour.
 *
 * Show and hide it by composing it conditionally — `if (open) KansoBottomSheet(onDismiss = { open
 * = false }) { … }`. There is deliberately no `sheetState` parameter: `SheetState` is itself part
 * of Material 3's experimental sheet API, so taking one — even as a default argument, which is
 * evaluated at the *call site* rather than here — would push the opt-in onto every consumer,
 * which is the one thing this wrapper exists to prevent. [skipPartiallyExpanded] is the knob
 * that state is usually reached for, and it costs no opt-in. A sheet that needs more than that
 * wants Material 3's own `ModalBottomSheet`, the same escape hatch [KansoAlertDialog] leaves
 * open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun KansoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded),
        containerColor = Kanso.colors.surfaceContainerLow,
        content = content,
    )
}
