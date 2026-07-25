/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ayanami.kanso.theme.KansoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The component contracts that live in a single expression, where the only other check is
 * someone reading the code and believing it.
 *
 * Robolectric keeps these on the JVM — no emulator, same invocation as the colour tests. They
 * deliberately assert *behaviour*, not pixels: what a screenshot would add here is covered by
 * the previews, and a golden image is the wrong tool for "does this click do nothing".
 */
@RunWith(AndroidJUnit4::class)
class ComponentBehaviourTest {

    @get:Rule
    val compose = createComposeRule()

    /** `loading` disables the button. A click landing anyway would fire work twice. */
    @Test
    fun `a loading button swallows clicks`() {
        var clicks = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoButton("Save", onClick = { clicks++ }, loading = true)
            }
        }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Save").performClick()
        assertEquals("a loading button must not fire its onClick", 0, clicks)
    }

    /** The label stays in the tree while loading — that is what keeps the width stable. */
    @Test
    fun `a loading button keeps its label laid out`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoButton("Save changes", onClick = {}, loading = true)
            }
        }
        compose.onNodeWithText("Save changes").assertIsDisplayed()
    }

    @Test
    fun `an idle button fires its onClick`() {
        var clicks = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoButton("Save", onClick = { clicks++ })
            }
        }
        compose.onNodeWithText("Save").performClick()
        assertEquals(1, clicks)
    }

    /** In the error state the message replaces the helper text rather than joining it. */
    @Test
    fun `errorText takes precedence over supporting text`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoTextField(
                    value = "",
                    onValueChange = {},
                    label = "PIN",
                    supporting = "Pairs this device.",
                    isError = true,
                    errorText = "PIN must be at least 6 digits.",
                )
            }
        }
        compose.onNodeWithText("PIN must be at least 6 digits.").assertIsDisplayed()
        compose.onNodeWithText("Pairs this device.").assertDoesNotExist()
    }

    /** Without `isError` the helper text is what shows, even when a message is supplied. */
    @Test
    fun `supporting text shows when not in the error state`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoTextField(
                    value = "",
                    onValueChange = {},
                    label = "PIN",
                    supporting = "Pairs this device.",
                    isError = false,
                    errorText = "PIN must be at least 6 digits.",
                )
            }
        }
        compose.onNodeWithText("Pairs this device.").assertIsDisplayed()
        compose.onNodeWithText("PIN must be at least 6 digits.").assertDoesNotExist()
    }

    /**
     * The reveal toggle starts masked and flips on tap.
     *
     * Asserted through the toggle's content description rather than the field's text: a
     * masked field still reports the raw value in its `EditableText` semantics — that is
     * correct Compose behaviour, and it means `onNodeWithText` cannot tell masked from
     * revealed. The state a screen-reader user actually gets is the description, and that
     * is kanso's to get right; the masking itself is `PasswordVisualTransformation`'s job.
     */
    @Test
    fun `the password reveal toggle starts masked and flips`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoPasswordField(value = "hunter2", onValueChange = {}, label = "Password")
            }
        }
        compose.onNodeWithContentDescription("Hide password").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show password").performClick()
        compose.onNodeWithContentDescription("Hide password").assertIsDisplayed()
        compose.onNodeWithContentDescription("Show password").assertDoesNotExist()
    }

    /** `revealable = false` is for a field that must never be shown; no toggle at all. */
    @Test
    fun `a non-revealable password field has no toggle`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoPasswordField(
                    value = "hunter2",
                    onValueChange = {},
                    label = "Password",
                    revealable = false,
                )
            }
        }
        compose.onNodeWithContentDescription("Show password").assertDoesNotExist()
    }

    /** KansoScaffold is the only component nothing else in the repo exercises. */
    @Test
    fun `the scaffold renders its title, bottom bar and content`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoScaffold(
                    title = "Peers",
                    bottomBar = { Text("nav") },
                ) { Text("body") }
            }
        }
        compose.onNodeWithText("Peers").assertIsDisplayed()
        compose.onNodeWithText("nav").assertIsDisplayed()
        compose.onNodeWithText("body").assertIsDisplayed()
    }

    /** A card given onClick must actually be clickable, not merely look like it. */
    @Test
    fun `a clickable card fires its onClick`() {
        var clicks = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoCard(title = "Peers", onClick = { clicks++ }) { Text("3 online") }
            }
        }
        compose.onNodeWithText("3 online").performClick()
        assertEquals(1, clicks)
    }
}
