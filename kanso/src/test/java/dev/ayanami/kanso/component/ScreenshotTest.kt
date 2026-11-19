/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dev.ayanami.kanso.theme.Kanso
import dev.ayanami.kanso.theme.KansoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images for the components whose bugs have all been text-layout bugs.
 *
 * Scoped deliberately. Not a component x theme matrix — that is unreviewable in a pull-request
 * diff and churns on every BOM bump. These are the components whose regressions actually
 * happened and show up as moved pixels: a section header that ellipsised, a status row that
 * starved its label, a list row, a card, the setting rows and the info banner — each captured
 * at font scale 1.0 and 2.0, because the second is where they broke. Two more are about colour
 * rather than text and are captured once: a skeleton and a neutral status, each on the bare
 * surface and inside a card, where both used to vanish.
 *
 * What a golden cannot say is why something is wrong — that a row lost its 48dp floor, or that
 * an empty state's call-to-action cannot be reached. Those are measured in
 * `LayoutContractTest`; behaviour belongs in `ComponentBehaviourTest`, and visual review in the
 * previews. What a golden adds is catching the change nobody looked at.
 *
 * Re-record after an intentional visual change:
 *
 * ```
 * ./gradlew :kanso:recordRoborazziDebug
 * ```
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun golden(
        name: String,
        scale: Float,
        frozen: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        // A component that animates for ever never lets the composition go idle; with the clock
        // stopped it is captured at the first frame of its animation instead.
        if (frozen) compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = scale,
                ),
            ) {
                KansoTheme(dynamicColor = false) {
                    Surface(color = Kanso.colors.surface) {
                        Column(Modifier.width(360.dp).padding(Kanso.spacing.lg)) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name-font$scale.png")
    }

    private fun atDefaultScale(name: String, content: @Composable () -> Unit) {
        golden(name, 1.0f, content = content)
    }

    @Test
    fun sectionHeader() = atDefaultScale("section-header") {
        KansoSectionHeader("Encrypted transport", supporting = "Applies to every channel.")
        KansoSectionHeader("Encrypted transport configuration and peer verification")
    }

    @Test
    fun sectionHeaderLargeFont() = golden("section-header", 2.0f) {
        KansoSectionHeader("Encrypted transport", supporting = "Applies to every channel.")
        KansoSectionHeader("Encrypted transport configuration and peer verification")
    }

    @Test
    fun statusRow() = atDefaultScale("status-row") {
        KansoStatusRow("Last sync", "2 minutes ago")
        KansoStatusRow("Fingerprint", "SHA256:pOxq7l9mNc5vVQr2wZ8kT1hYbJ4uE6aD0fGsX3iL")
    }

    @Test
    fun statusRowLargeFont() = golden("status-row", 2.0f) {
        KansoStatusRow("Last sync", "2 minutes ago")
        KansoStatusRow("Fingerprint", "SHA256:pOxq7l9mNc5vVQr2wZ8kT1hYbJ4uE6aD0fGsX3iL")
    }

    @Test
    fun listItem() = atDefaultScale("list-item") {
        KansoListItem(
            "Search peers",
            supporting = "Discover devices on the local network",
            onClick = {},
        )
        KansoListItem("No supporting line")
    }

    @Test
    fun listItemLargeFont() = golden("list-item", 2.0f) {
        KansoListItem(
            "Search peers",
            supporting = "Discover devices on the local network",
            onClick = {},
        )
        KansoListItem("No supporting line")
    }

    @Test
    fun card() = atDefaultScale("card") {
        KansoCard(title = "Connection", subtitle = "Encrypted transport status") {
            KansoStatusRow("Protocol", "TLS 1.3")
        }
    }

    @Test
    fun cardLargeFont() = golden("card", 2.0f) {
        KansoCard(title = "Connection", subtitle = "Encrypted transport status") {
            KansoStatusRow("Protocol", "TLS 1.3")
        }
    }

    /**
     * A setting row is the same class of hazard: two lines of text, a leading icon and a
     * control competing for one row, against a 48dp floor that only bites at a small scale
     * and a control that only collides with the text at a large one.
     */
    @Test
    fun settingRows() = atDefaultScale("setting-rows") {
        KansoSwitchRow(
            headline = "Background sync",
            supporting = "Keeps peer status up to date while the app is closed",
            checked = true,
            onCheckedChange = {},
        )
        KansoCheckboxRow(headline = "Send diagnostics", checked = false, onCheckedChange = {})
    }

    @Test
    fun settingRowsLargeFont() = golden("setting-rows", 2.0f) {
        KansoSwitchRow(
            headline = "Background sync",
            supporting = "Keeps peer status up to date while the app is closed",
            checked = true,
            onCheckedChange = {},
        )
        KansoCheckboxRow(headline = "Send diagnostics", checked = false, onCheckedChange = {})
    }

    /** The banner's action is what a long message pushes off the row — the empty-state bug. */
    @Test
    fun infoBanner() = atDefaultScale("info-banner") {
        KansoInfoBanner(
            text = "This device has not synced since 12 March.",
            status = KansoStatus.Warning,
            action = { KansoButton("Sync", onClick = {}, style = KansoButtonStyle.Text) },
        )
    }

    @Test
    fun infoBannerLargeFont() = golden("info-banner", 2.0f) {
        KansoInfoBanner(
            text = "This device has not synced since 12 March.",
            status = KansoStatus.Warning,
            action = { KansoButton("Sync", onClick = {}, style = KansoButtonStyle.Text) },
        )
    }

    /**
     * The bars are drawn relative to what is under them, and that is as often a card as the
     * bare surface. Between two surface tones they were the card's own colour: this image had a
     * blank where the second skeleton is.
     */
    @Test
    fun skeleton() = golden("skeleton", 1.0f, frozen = true) {
        KansoSkeleton(rows = 2, icon = true)
        KansoCard(title = "In a card") {
            KansoSkeleton(rows = 2, icon = true)
        }
    }

    /** The same hazard for the one status that has no hue of its own to be seen by. */
    @Test
    fun neutralStatus() = golden("neutral-status", 1.0f) {
        KansoListItem(
            "new.example.com",
            supporting = "Never checked",
            trailing = { KansoStatusBadge("Unknown", KansoStatus.Neutral) },
        )
        KansoCard(title = "In a card") {
            KansoListItem(
                "new.example.com",
                supporting = "Never checked",
                trailing = { KansoStatusBadge("Unknown", KansoStatus.Neutral) },
            )
            KansoInfoBanner("Nothing has been checked yet.", status = KansoStatus.Neutral)
        }
    }
}
