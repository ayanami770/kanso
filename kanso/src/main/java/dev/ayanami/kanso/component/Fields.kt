/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

/**
 * An outlined Material 3 text field with a label, optional supporting/helper text and an
 * error state — the standard kanso form input.
 *
 * [isError] gates the error state and [errorText] supplies the message, so a caller can hold
 * a constant message and flip only the flag. When both are present the message is attached to
 * the field's semantics as well as shown: Material 3 on its own announces a generic "invalid
 * input" and never the reason.
 *
 * Pass [visualTransformation] to mask the value — use `PasswordVisualTransformation()` for
 * PINs and passwords, which a numeric-password [keyboardType] alone does *not* hide.
 */
@Composable
fun KansoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    // Without this the error is signalled by red outline and a generic "invalid input" — the
    // actual reason never reaches a screen reader.
    val errorSemantics = if (isError && errorText != null) {
        Modifier.semantics { error(errorText) }
    } else {
        Modifier
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().then(errorSemantics),
        label = { Text(label) },
        singleLine = singleLine,
        maxLines = maxLines,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = when {
            isError && errorText != null -> ({ Text(errorText) })
            supporting != null -> ({ Text(supporting) })
            else -> null
        },
    )
}
