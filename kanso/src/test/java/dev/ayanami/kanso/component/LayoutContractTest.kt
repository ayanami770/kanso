/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ayanami.kanso.theme.KansoSizing
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
        assertFalse(
            "no part of the message may be dropped to make it fit",
            layout.didOverflowHeight,
        )
        compose.onNodeWithText("Forget").assertIsDisplayed()
    }

    // ---- skeleton -------------------------------------------------------------------

    /**
     * The skeleton promises that nothing moves when the data lands, which is only true if its
     * rows are the height of the rows that replace them. A fixed 72dp was 4dp off a two-line
     * row at the default font scale and 31dp off at 200%.
     */
    @Test
    fun `a skeleton row is as tall as the list row it stands in for`() {
        val scales = listOf(0.85f, 1f, 2f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                scales.forEach { scale ->
                    Host(scale) {
                        Column {
                            KansoListItem(
                                "Search peers",
                                Modifier.testTag("two-$scale"),
                                supporting = "Discover devices",
                                onClick = {},
                            )
                            Box(Modifier.testTag("skeleton-two-$scale")) { KansoSkeleton(rows = 1) }
                            KansoListItem(
                                "Search peers",
                                Modifier.testTag(
                                    "one-$scale",
                                ),
                                onClick = {
                                },
                            )
                            Box(Modifier.testTag("skeleton-one-$scale")) {
                                KansoSkeleton(rows = 1, supporting = false)
                            }
                        }
                    }
                }
            }
        }

        fun height(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().size.height
        scales.forEach { scale ->
            assertEquals(
                "two-line row at font scale $scale",
                height("two-$scale"),
                height("skeleton-two-$scale"),
            )
            assertEquals(
                "one-line row at font scale $scale",
                height("one-$scale"),
                height("skeleton-one-$scale"),
            )
        }
    }

    // ---- trailing slots -------------------------------------------------------------

    /**
     * A timestamp in the trailing slot at 200% font scale used to take the whole row: the
     * headline was left a 25dp column and wrapped into nineteen lines, one or two characters
     * each.
     */
    @Test
    fun `a wide trailing slot leaves the headline half of the row`() {
        compose.setContent {
            Host(fontScale = 2f) {
                Column(Modifier.width(360.dp).padding(16.dp)) {
                    KansoListItem(
                        headline = "Weekly sync with the relay",
                        supporting = "Last run finished without errors",
                        trailing = { Text("2026-11-19 15:16 JST") },
                    )
                }
            }
        }

        val headline = placed("Weekly sync with the relay").single()
        val trailing = placed("2026-11-19 15:16 JST").single()
        val halfRow = (328 / 2) * compose.density.density
        assertTrue(
            "the trailing slot must stop at half of the row, was ${trailing.size.width}px",
            trailing.size.width <= halfRow,
        )
        assertTrue("the headline must keep real width", headline.size.width >= halfRow / 2)
        assertTrue("and wrap like a sentence, not a column", lineCount(headline) <= 6)
    }

    // ---- full-area states -----------------------------------------------------------

    /**
     * The supported way to pair a full-area state with pull-to-refresh: the state is the
     * scrollable. Wrapped in a scrolling column instead, it loses its centring and sits at the
     * top — which is what the gallery itself used to show.
     */
    @Test
    fun `a full-area state given a scrollState is scrollable and still centred`() {
        compose.setContent {
            Host {
                Box(Modifier.size(360.dp, 300.dp).testTag("parent")) {
                    KansoEmptyState(
                        icon = ErrorIcon,
                        title = "Nothing here yet",
                        scrollState = rememberScrollState(),
                    )
                }
            }
        }

        compose.onNode(hasScrollAction()).assertExists()
        val parent = compose.onNodeWithTag("parent").fetchSemanticsNode().boundsInRoot
        val title = placed("Nothing here yet").single().boundsInRoot
        // The icon sits above the title, so a centred block puts the title below the middle;
        // top-aligned, it is well above it.
        assertTrue("the state must be centred in its parent", title.center.y > parent.center.y)
    }

    // ---- strings --------------------------------------------------------------------

    /** The default retry label is drawn on screen; in a Japanese app it used to read "Retry". */
    @Test
    @Config(qualifiers = "ja-w360dp-h640dp-xhdpi")
    fun `the built-in strings follow a Japanese locale`() {
        compose.setContent {
            Host {
                Column {
                    KansoErrorState(
                        title = "接続できません",
                        modifier = Modifier.height(
                            300.dp,
                        ),
                        onRetry = {
                        },
                    )
                    KansoBackButton(onBack = {})
                }
            }
        }

        compose.onNodeWithText("再試行").assertIsDisplayed()
        compose.onNodeWithContentDescription("戻る").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }

    // ---- touch targets --------------------------------------------------------------

    /**
     * Padding alone brings a one-line row to 48dp only from a font scale of 1.0 up; at the
     * "Small" setting it is 44.4dp. The explicit floor is what holds it there — and at the two
     * scales the goldens are taken at, removing the floor changes nothing they can see.
     */
    @Test
    fun `tappable rows keep the 48dp floor at a small font scale`() {
        compose.setContent {
            Host(fontScale = 0.85f) {
                Column {
                    KansoListItem("Peers", Modifier.testTag("list"), onClick = {})
                    KansoCheckboxRow(
                        "Send diagnostics",
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("checkbox"),
                    )
                    KansoRadioRow(
                        "Metric",
                        selected = true,
                        onSelect = {},
                        modifier = Modifier.testTag("radio"),
                    )
                }
            }
        }

        val floor = 48 * compose.density.density
        listOf("list", "checkbox", "radio").forEach { tag ->
            val height = compose.onNodeWithTag(tag).fetchSemanticsNode().size.height
            assertTrue("the $tag row is ${height}px tall, under the 48dp floor", height >= floor)
        }
    }

    /**
     * The regression that gave the full-area states their `scrollState`: at 200% in a short
     * parent the call-to-action is below the fold, and it has to be reachable.
     */
    @Test
    fun `an empty state's action can be scrolled into view at a large font scale`() {
        compose.setContent {
            Host(fontScale = 2f) {
                Box(Modifier.size(360.dp, 420.dp)) {
                    KansoEmptyState(
                        icon = ErrorIcon,
                        title = "Nothing here yet",
                        description = "Empty, loading and error states share one visual " +
                            "language so every screen feels the same.",
                        scrollState = rememberScrollState(),
                        action = { KansoButton("Add a peer", onClick = {}) },
                    )
                }
            }
        }

        compose.onNodeWithText("Add a peer").performScrollTo().assertIsDisplayed()
    }

    // ---- rows: content padding ------------------------------------------------------

    /**
     * Padding on the modifier insets the pressed area along with the text. `contentPadding`
     * insets the text alone — the difference between a highlight that runs to the edges of a
     * card and one that floats inside it.
     */
    @Test
    fun `row content padding moves the content and leaves the pressed area full width`() {
        val inset = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        compose.setContent {
            Host {
                Column {
                    KansoListItem("Plain", Modifier.testTag("plain"), onClick = {})
                    KansoListItem(
                        "Inset",
                        Modifier.testTag("inset"),
                        onClick = {},
                        contentPadding = inset,
                    )
                    KansoSwitchRow(
                        "Switch",
                        checked = true,
                        onCheckedChange = {},
                        modifier = Modifier.testTag("switch"),
                        contentPadding = inset,
                    )
                }
            }
        }
        val sixteen = with(compose.density) { 16.dp.toPx() }
        val screen = with(compose.density) { 360.dp.toPx() }

        fun row(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

        fun textLeft(text: String) = placed(text).single().boundsInRoot.left

        assertEquals("by default a row pads only above and below", 0f, textLeft("Plain"), 0.5f)
        listOf("inset" to "Inset", "switch" to "Switch").forEach { (tag, text) ->
            assertEquals("$tag: the pressed area starts at the edge", 0f, row(tag).left, 0.5f)
            assertEquals("$tag: and runs the full width", screen, row(tag).width, 0.5f)
            assertEquals("$tag: while the content is inset", sixteen, textLeft(text), 0.5f)
        }
    }

    // ---- content container ----------------------------------------------------------

    /** `AbsoluteAlignment.Left`, `.Right` and any bias used to fall through to "centre". */
    @Test
    fun `the content container honours an absolute or a biased alignment`() {
        compose.setContent {
            Host {
                Column {
                    KansoContentContainer(maxWidth = 100.dp, alignment = AbsoluteAlignment.Right) {
                        Text("right")
                    }
                    KansoContentContainer(maxWidth = 100.dp, alignment = AbsoluteAlignment.Left) {
                        Text("left")
                    }
                    KansoContentContainer(
                        maxWidth = 100.dp,
                        alignment = BiasAlignment.Horizontal(0.5f),
                    ) {
                        Text("biased")
                    }
                    KansoContentContainer(maxWidth = 100.dp) { Text("centred") }
                }
            }
        }
        val density = compose.density.density

        fun left(text: String) = placed(text).single().boundsInRoot.left

        // 260dp of slack on a 360dp screen: all of it, none of it, three quarters, half.
        assertEquals(260f * density, left("right"), 1f)
        assertEquals(0f, left("left"), 1f)
        assertEquals(195f * density, left("biased"), 1f)
        assertEquals(130f * density, left("centred"), 1f)
    }

    // ---- banner ---------------------------------------------------------------------

    /**
     * The banner's icon is an icon: a theme that resizes its icons resizes this one too. It was
     * sized by a spacing token that happens to hold the same 24dp.
     */
    @Test
    fun `the banner icon follows the icon size token`() {
        compose.setContent {
            KansoTheme(dynamicColor = false, sizing = KansoSizing(icon = 40.dp)) {
                KansoInfoBanner("Cached", Modifier.testTag("banner"), icon = ErrorIcon)
            }
        }
        val banner = compose.onNodeWithTag("banner").fetchSemanticsNode()
        // The icon is the tallest thing in the row: 40dp, plus the banner's 12dp above and below.
        val expected = with(compose.density) { 64.dp.roundToPx() }
        assertEquals(expected, banner.size.height)
    }
}
