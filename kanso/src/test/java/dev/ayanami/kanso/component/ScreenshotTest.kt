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
 * diff and churns on every BOM bump. These four are the ones whose regressions actually
 * happened: a section header that ellipsised, a status row that starved its label, a list row
 * that lost its touch target, an empty state whose call-to-action fell off the screen. Each is
 * captured at font scale 1.0 and 2.0, because the second is where they broke.
 *
 * Behaviour belongs in `ComponentBehaviourTest` and visual review belongs in the previews;
 * what a golden adds is catching the change nobody looked at.
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

    private fun golden(name: String, scale: Float, content: @Composable () -> Unit) {
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

    private fun bothScales(name: String, content: @Composable () -> Unit) {
        golden(name, 1.0f, content)
    }

    @Test
    fun sectionHeader() = bothScales("section-header") {
        KansoSectionHeader("Encrypted transport", supporting = "Applies to every channel.")
        KansoSectionHeader("Encrypted transport configuration and peer verification")
    }

    @Test
    fun sectionHeaderLargeFont() = golden("section-header", 2.0f) {
        KansoSectionHeader("Encrypted transport", supporting = "Applies to every channel.")
        KansoSectionHeader("Encrypted transport configuration and peer verification")
    }

    @Test
    fun statusRow() = bothScales("status-row") {
        KansoStatusRow("Last sync", "2 minutes ago")
        KansoStatusRow("Fingerprint", "SHA256:pOxq7l9mNc5vVQr2wZ8kT1hYbJ4uE6aD0fGsX3iL")
    }

    @Test
    fun statusRowLargeFont() = golden("status-row", 2.0f) {
        KansoStatusRow("Last sync", "2 minutes ago")
        KansoStatusRow("Fingerprint", "SHA256:pOxq7l9mNc5vVQr2wZ8kT1hYbJ4uE6aD0fGsX3iL")
    }

    @Test
    fun listItem() = bothScales("list-item") {
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
    fun card() = bothScales("card") {
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
    fun settingRows() = bothScales("setting-rows") {
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
    fun infoBanner() = bothScales("info-banner") {
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
}
