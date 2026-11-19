/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ayanami.kanso.theme.KansoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The layout contracts that only show up at a real size: a 360dp-wide screen, real text metrics,
 * and the font scales and content lengths the previews do not reach.
 *
 * `ComponentBehaviourTest` runs without native graphics, where text has no meaningful width, so
 * nothing there can tell a title that fits from one that wraps. These run with
 * `GraphicsMode.NATIVE` for exactly that reason, and they assert on what was measured — line
 * counts, bounds, whether a node was placed — rather than on pixels: a golden would notice that
 * something moved, but not say that a version number is now unreadable.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class LayoutContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun Host(
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val current = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale)) {
            KansoTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
        }
    }

    private fun textLayout(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    private fun lineCount(node: SemanticsNode): Int = textLayout(node).lineCount

    /** The text nodes that were actually placed — a node a layout measured but left out is not. */
    private fun placed(text: String): List<SemanticsNode> =
        compose.onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { it.layoutInfo.isPlaced }

    // ---- scaffold title line --------------------------------------------------------

    private fun scaffold(title: String, version: String, actions: Int, fontScale: Float = 1f) {
        compose.setContent {
            Host(fontScale) {
                KansoScaffold(
                    title = title,
                    version = version,
                    navigationIcon = { KansoBackButton(onBack = {}) },
                    actions = {
                        repeat(actions) { index ->
                            IconButton(onClick = {}) {
                                Icon(ErrorIcon, contentDescription = "action $index")
                            }
                        }
                    },
                ) { inner -> Text("body", Modifier.padding(inner)) }
            }
        }
    }

    /**
     * Ten full-width characters, a back arrow and two actions on a 360dp screen at the default
     * font size: the title used to wrap, and the version — measured with no width left — ran
     * down the screen one character per line.
     */
    @Test
    fun `a title that fills the bar drops the version instead of breaking it`() {
        scaffold(title = "セッション詳細と設定", version = "v0.7.0", actions = 2)

        val title = placed("セッション詳細と設定").single()
        assertEquals("the title must stay on one line", 1, lineCount(title))
        assertTrue("a version with no room must not be drawn", placed("v0.7.0").isEmpty())
    }

    @Test
    fun `a short title keeps the whole version beside it`() {
        scaffold(title = "Peers", version = "v5.33.11", actions = 0)

        val title = placed("Peers").single()
        val version = placed("v5.33.11").single()
        assertEquals(1, lineCount(title))
        assertEquals(1, lineCount(version))
        assertTrue(
            "the version follows the title on the same line",
            version.boundsInRoot.left >= title.boundsInRoot.right &&
                version.boundsInRoot.top == title.boundsInRoot.top,
        )
    }

    /** At 200% the version used to break mid-number: "v5.33.1" with a stray "1" under it. */
    @Test
    fun `the version is drawn whole or not at all at a large font scale`() {
        scaffold(title = "Settings", version = "v5.33.11", actions = 1, fontScale = 2f)

        assertEquals(1, lineCount(placed("Settings").single()))
        placed("v5.33.11").forEach { version ->
            assertEquals("a version that is drawn is drawn on one line", 1, lineCount(version))
        }
    }

    /** One stop, announced as a heading — not two fragments a screen reader reads apart. */
    @Test
    fun `the title and its version are one heading`() {
        scaffold(title = "Peers", version = "v1.4.2", actions = 0)

        compose.onNodeWithText("Peers")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assert(hasText("v1.4.2"))
    }

    // ---- alert dialog ---------------------------------------------------------------

    /**
     * 320x480dp at 200% font scale: the message is taller than the dialog is allowed to be. It
     * used to be cut mid-line with nothing to scroll, so the sentence a destructive confirmation
     * exists to deliver never reached the screen.
     */
    @Test
    @Config(qualifiers = "w320dp-h480dp-xhdpi")
    fun `a dialog message taller than the window scrolls instead of being cut`() {
        // A dialog is its own window: it takes the font scale from the system configuration,
        // which a LocalDensity override in the calling composition does not reach.
        RuntimeEnvironment.setFontScale(2f)
        val message = "The pairing and its saved PIN are removed from this device and from " +
            "every relay that has cached them. Sessions that are still open are closed, and " +
            "the peer will have to scan a new code before it can reach this device again. " +
            "This cannot be undone."
        compose.setContent {
            KansoTheme(dynamicColor = false) {
                KansoAlertDialog(
                    title = "Forget this device?",
                    text = message,
                    confirmText = "Forget",
                    dismissText = "Cancel",
                    destructive = true,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText(message).assert(hasScrollAction())
        val layout = textLayout(compose.onNodeWithText(message).fetchSemanticsNode())
        assertFalse("no part of the message may be dropped to make it fit", layout.didOverflowHeight)
        compose.onNodeWithText("Forget").assertIsDisplayed()
    }
}
