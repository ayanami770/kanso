/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.ayanami.kanso.R

/**
 * An outlined Material 3 text field with a label, optional supporting/helper text and an
 * error state — the standard kanso form input.
 *
 * [isError] gates the error state and [errorText] supplies the message, so a caller can hold
 * a constant message and flip only the flag. When both are present the message is attached to
 * the field's semantics as well as shown: Material 3 on its own announces a generic "invalid
 * input" and never the reason.
 *
 * For a password or PIN use [KansoPasswordField] — a numeric-password [keyboardType] selects
 * the keyboard but does *not* mask what is drawn.
 */
@Composable
public fun KansoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
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
        enabled = enabled,
        readOnly = readOnly,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        supportingText = when {
            isError && errorText != null -> ({ Text(errorText) })
            supporting != null -> ({ Text(supporting) })
            else -> null
        },
    )
}

/**
 * A masked field with a reveal toggle — the input a design system should own rather than leave
 * every app to reassemble.
 *
 * The characters are masked by [PasswordVisualTransformation]; a numeric-password keyboard type
 * on its own only picks the keyboard, and Compose still draws the glyphs. Set [numeric] for a
 * PIN. The toggle is a real button with a state-dependent content description, so a screen
 * reader announces whether the value is currently shown or hidden.
 */
@Composable
public fun KansoPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    numeric: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    revealable: Boolean = true,
) {
    var revealed by remember { mutableStateOf(false) }
    val showing = revealable && revealed
    KansoTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        supporting = supporting,
        isError = isError,
        errorText = errorText,
        enabled = enabled,
        trailingIcon = if (revealable) {
            {
                IconButton(onClick = { revealed = !revealed }, enabled = enabled) {
                    Icon(
                        imageVector = if (showing) EyeOffIcon else EyeIcon,
                        contentDescription = stringResource(
                            if (showing) R.string.kanso_hide_password else R.string.kanso_show_password,
                        ),
                    )
                }
            }
        } else {
            null
        },
        keyboardType = when {
            numeric -> KeyboardType.NumberPassword
            else -> KeyboardType.Password
        },
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        visualTransformation = if (showing) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
    )
}
