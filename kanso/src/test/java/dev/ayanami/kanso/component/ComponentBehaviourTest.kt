/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ayanami.kanso.theme.KansoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

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

    /** The version follows the title on the same line as part of the standard bar. */
    @Test
    fun `the scaffold shows the version after the title`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoScaffold(title = "Peers", version = "2.1.0") { Text("body") }
            }
        }
        compose.onNodeWithText("Peers").assertIsDisplayed()
        compose.onNodeWithText("2.1.0").assertIsDisplayed()
    }

    /** The default is the package's versionName, `v`-prefixed — a variable, not a literal. */
    @Test
    fun `the default version is read from the package and v-prefixed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName = "7.7.7"
        shadowOf(context.packageManager).installPackage(info)
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoScaffold(title = "Peers") { Text("body") }
            }
        }
        compose.onNodeWithText("v7.7.7").assertIsDisplayed()
    }

    /** A versionName that already carries the prefix must not become "vv…". */
    @Test
    fun `an already-prefixed versionName is not prefixed again`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName = "v8.0.0"
        shadowOf(context.packageManager).installPackage(info)
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoScaffold(title = "Peers") { Text("body") }
            }
        }
        compose.onNodeWithText("v8.0.0").assertIsDisplayed()
        compose.onNodeWithText("vv8.0.0").assertDoesNotExist()
    }

    /** titleContent replaces the whole title line — the version included. */
    @Test
    fun `titleContent wins over title and version`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoScaffold(
                    title = "Peers",
                    version = "2.1.0",
                    titleContent = { Text("custom") },
                ) { Text("body") }
            }
        }
        compose.onNodeWithText("custom").assertIsDisplayed()
        compose.onNodeWithText("Peers").assertDoesNotExist()
        compose.onNodeWithText("2.1.0").assertDoesNotExist()
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

    /** The large bar is a different composable — the title has to survive the swap. */
    @Test
    fun `the large scaffold renders its title and content`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoScaffold(title = "Settings", largeTopBar = true, version = "") {
                    Text("body")
                }
            }
        }
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithText("body").assertIsDisplayed()
    }

    /** An unlabelled back arrow is the defect this component exists to prevent. */
    @Test
    fun `the back button is labelled and fires onBack`() {
        var backs = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoBackButton(onBack = { backs++ })
            }
        }
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    // ---- setting rows ---------------------------------------------------------------

    /**
     * The whole row toggles, not just the switch. This is the reason these components exist:
     * `KansoListItem(trailing = { Switch(…) })` gives a row whose tap does nothing.
     */
    @Test
    fun `tapping a switch row's text toggles it`() {
        var checked = false
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoSwitchRow(
                    headline = "Background sync",
                    checked = checked,
                    onCheckedChange = { checked = it },
                    supporting = "Keeps peers up to date",
                )
            }
        }
        compose.onNodeWithText("Background sync").performClick()
        assertEquals(true, checked)
    }

    /**
     * Exactly one toggleable node — the row. A `Switch` left interactive would give a screen
     * reader two stops that disagree about what was tapped.
     */
    @Test
    fun `a switch row is a single toggleable node`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoSwitchRow(headline = "Background sync", checked = true, onCheckedChange = {})
            }
        }
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.onNode(isToggleable())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    /** Same contract for the single-choice row, via `selectable` rather than `toggleable`. */
    @Test
    fun `a radio row is a single selectable node that fires onSelect`() {
        var selects = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoRadioRow(headline = "Metric", selected = false, onSelect = { selects++ })
            }
        }
        compose.onAllNodes(isSelectable()).assertCountEquals(1)
        compose.onNodeWithText("Metric").performClick()
        assertEquals(1, selects)
    }

    /** A disabled row must not fire, however the tap arrives. */
    @Test
    fun `a disabled checkbox row swallows clicks`() {
        var changes = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoCheckboxRow(
                    headline = "Analytics",
                    checked = false,
                    onCheckedChange = { changes++ },
                    enabled = false,
                )
            }
        }
        compose.onNodeWithText("Analytics").performClick()
        assertEquals(0, changes)
    }

    // ---- dialog ---------------------------------------------------------------------

    /** Confirm and dismiss must reach different callbacks — the classic wiring slip. */
    @Test
    fun `the alert dialog routes confirm and dismiss separately`() {
        var confirms = 0
        var dismisses = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoAlertDialog(
                    title = "Delete peer?",
                    text = "This cannot be undone.",
                    confirmText = "Delete",
                    dismissText = "Cancel",
                    destructive = true,
                    onConfirm = { confirms++ },
                    onDismiss = { dismisses++ },
                )
            }
        }
        compose.onNodeWithText("Delete peer?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, confirms)
        assertEquals(1, dismisses)
        compose.onNodeWithText("Delete").performClick()
        assertEquals(1, confirms)
    }

    /** No `dismissText` means an acknowledge-only dialog, with nothing to cancel. */
    @Test
    fun `the alert dialog omits its dismiss button when unlabelled`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoAlertDialog(
                    title = "Session expired",
                    text = "Sign in again to continue.",
                    confirmText = "OK",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        compose.onNodeWithText("OK").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
    }

    // ---- error state ----------------------------------------------------------------

    /** Retry is the whole point of separating this from an empty state. */
    @Test
    fun `the error state shows a retry action only when it can retry`() {
        var retries = 0
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoErrorState(
                    title = "Could not reach the server",
                    description = "Check your connection.",
                    onRetry = { retries++ },
                )
            }
        }
        compose.onNodeWithText("Could not reach the server").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `the error state has no retry action without onRetry`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoErrorState(title = "This account was closed")
            }
        }
        compose.onNodeWithText("This account was closed").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }

    // ---- select field ---------------------------------------------------------------

    /**
     * The generic signature earns its keep here: what comes back is the option itself, not an
     * index into a list of strings that the call site has to map back.
     */
    @Test
    fun `the select field hands back the option, not its label`() {
        val options = listOf(Unit1("mg/dL", 1), Unit1("mmol/L", 2))
        var picked: Unit1? = null
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoSelectField(
                    value = options[0],
                    options = options,
                    onSelect = { picked = it },
                    label = "Unit",
                    optionLabel = { it.symbol },
                )
            }
        }
        compose.onNodeWithText("mg/dL").performClick()
        compose.onNodeWithText("mmol/L").performClick()
        assertEquals(options[1], picked)
    }

    /**
     * A null value leaves the field blank. The obvious implementation — `value.toString()` —
     * renders the literal text "null" into the box, which is the kind of thing that ships.
     */
    @Test
    fun `the select field renders nothing for a null value`() {
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoSelectField(
                    value = null,
                    options = listOf("mg/dL", "mmol/L"),
                    onSelect = {},
                    label = "Unit",
                )
            }
        }
        compose.onNodeWithText("Unit").assertIsDisplayed()
        compose.onNodeWithText("null").assertDoesNotExist()
    }

    // ---- skeleton -------------------------------------------------------------------

    /**
     * The placeholder bars mean nothing read aloud, so they are replaced by one node saying
     * the content is loading.
     *
     * `autoAdvance = false` because the shimmer never ends: with the clock advancing itself,
     * the composition never reports idle and every assertion below would hang. This is the
     * caveat [KansoSkeleton]'s KDoc states, exercised.
     */
    @Test
    fun `the skeleton announces itself as one loading node`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoSkeleton(rows = 3, icon = true)
            }
        }
        compose.onNodeWithContentDescription("Loading content").assertExists()
    }
}

/** A domain type for the select-field test — the point being that it is not a String. */
private data class Unit1(val symbol: String, val id: Int)
