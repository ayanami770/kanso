/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.ayanami.kanso.component.KansoAlertDialog
import dev.ayanami.kanso.component.KansoBottomSheet
import dev.ayanami.kanso.component.KansoButton
import dev.ayanami.kanso.component.KansoButtonStyle
import dev.ayanami.kanso.component.KansoCard
import dev.ayanami.kanso.component.KansoCheckboxRow
import dev.ayanami.kanso.component.KansoContentContainer
import dev.ayanami.kanso.component.KansoDivider
import dev.ayanami.kanso.component.KansoEmptyState
import dev.ayanami.kanso.component.KansoErrorState
import dev.ayanami.kanso.component.KansoInfoBanner
import dev.ayanami.kanso.component.KansoListItem
import dev.ayanami.kanso.component.KansoLoadingState
import dev.ayanami.kanso.component.KansoPasswordField
import dev.ayanami.kanso.component.KansoRadioRow
import dev.ayanami.kanso.component.KansoRefreshBox
import dev.ayanami.kanso.component.KansoScaffold
import dev.ayanami.kanso.component.KansoSectionHeader
import dev.ayanami.kanso.component.KansoSelectField
import dev.ayanami.kanso.component.KansoSkeleton
import dev.ayanami.kanso.component.KansoStatus
import dev.ayanami.kanso.component.KansoStatusBadge
import dev.ayanami.kanso.component.KansoStatusRow
import dev.ayanami.kanso.component.KansoSwitchRow
import dev.ayanami.kanso.component.KansoTextField
import dev.ayanami.kanso.theme.Kanso
import dev.ayanami.kanso.theme.KansoBrand
import dev.ayanami.kanso.theme.KansoDefaultBrand
import dev.ayanami.kanso.theme.KansoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DemoApp() }
    }
}

/**
 * Sample seeds for the brand switcher — deliberately the gallery's own data rather than the
 * library's. A design system should not ship a list of its consumers; an app defines its brand
 * where it defines everything else about itself. These five are here to show that one seed
 * really does produce a whole coherent scheme, across hues that behave very differently.
 */
private val Brands = listOf(
    KansoDefaultBrand,
    KansoBrand("LMSA", Color(0xFF006A60)),          // secure teal
    KansoBrand("CertWatch", Color(0xFF3F5AA6)),     // trust blue
    KansoBrand("Semicon News", Color(0xFF8A4F00)),  // amber/silicon
    KansoBrand("medcal", Color(0xFF386A20)),        // clinical green
)

private enum class Tab(val label: String, val icon: ImageVector) {
    Gallery("Components", Icons.Outlined.Widgets),
    Forms("Forms", Icons.Outlined.Edit),
    States("States", Icons.Outlined.Layers),
    Settings("Settings", Icons.Outlined.Tune),
}

@Composable
private fun DemoApp() {
    var brandIndex by remember { mutableIntStateOf(0) }
    var dark by remember { mutableStateOf(false) }
    var dynamic by remember { mutableStateOf(false) }
    val brand: KansoBrand = Brands[brandIndex]

    KansoTheme(brand = brand, darkTheme = dark, dynamicColor = dynamic) {
        DemoShell(
            brandName = brand.name,
            dark = dark,
            dynamic = dynamic,
            onPickBrand = { brandIndex = it },
            onToggleDark = { dark = !dark },
            onToggleDynamic = { dynamic = !dynamic },
        )
    }
}

@Composable
private fun DemoShell(
    brandName: String,
    dark: Boolean,
    dynamic: Boolean,
    onPickBrand: (Int) -> Unit,
    onToggleDark: () -> Unit,
    onToggleDynamic: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.Gallery) }
    var menuOpen by remember { mutableStateOf(false) }

    // The gallery runs on KansoScaffold rather than a hand-rolled Scaffold on purpose: if the
    // showcase cannot use the library's own screen shell, no real app with bottom navigation
    // can either. Note there is no @OptIn here — KansoScaffold absorbs the experimental
    // Material 3 opt-in so consumers never inherit it.
    KansoScaffold(
        title = "kanso",
        centeredTitle = false,
        // Settings is the one tab that starts a hierarchy rather than sitting inside one, so
        // it gets the tall collapsing header — which is exactly the rule largeTopBar states.
        largeTopBar = tab == Tab.Settings,
        actions = {
            // Brand accent switcher — shows the "shared system + per-app accent" model live.
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.Palette, contentDescription = "Brand")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Brands.forEachIndexed { i, b ->
                    DropdownMenuItem(
                        text = { Text(b.name) },
                        onClick = { onPickBrand(i); menuOpen = false },
                    )
                }
            }
            IconButton(onClick = onToggleDark) {
                Icon(
                    if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle dark mode",
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { inner ->
        // Caps content width for the whole gallery. A no-op on a phone; on a tablet or a
        // resized window it is the difference between readable prose and a 160-character line.
        // No component needed changing for this to work — each one applies the caller's
        // modifier first, so this outer constraint is what their fillMaxWidth() resolves to.
        KansoContentContainer {
            when (tab) {
                Tab.Gallery -> GalleryScreen(inner, brandName, dark, dynamic, onToggleDynamic)
                Tab.Forms -> FormsScreen(inner)
                Tab.States -> StatesScreen(inner)
                Tab.Settings -> SettingsScreen(inner)
            }
        }
    }
}

@Composable
private fun screenColumn(inner: PaddingValues): Modifier =
    Modifier
        .fillMaxSize()
        .padding(inner)
        .verticalScroll(rememberScrollState())


@Composable
private fun GalleryScreen(
    inner: PaddingValues,
    brandName: String,
    dark: Boolean,
    dynamic: Boolean,
    onToggleDynamic: () -> Unit,
) {
    Column(
        screenColumn(inner).padding(horizontal = Kanso.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Kanso.spacing.section),
    ) {
        KansoSectionHeader(
            "Buttons",
            supporting = "One emphasis system, five weights.",
            modifier = Modifier.padding(top = Kanso.spacing.sm),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Kanso.spacing.sm),
        ) {
            KansoButton("Filled", onClick = {}, modifier = Modifier.weight(1f))
            KansoButton("Tonal", onClick = {}, style = KansoButtonStyle.Tonal, modifier = Modifier.weight(1f))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Kanso.spacing.sm),
        ) {
            KansoButton("Outlined", onClick = {}, style = KansoButtonStyle.Outlined, modifier = Modifier.weight(1f))
            KansoButton("Text", onClick = {}, style = KansoButtonStyle.Text, modifier = Modifier.weight(1f))
        }
        KansoButton("Working…", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
        KansoButton("Unlock", onClick = {}, icon = Icons.Filled.Lock, style = KansoButtonStyle.Tonal)

        KansoCard(title = "Cards", subtitle = "Grouped content on a filled surface.") {
            KansoStatusRow("Brand accent", brandName)
            KansoStatusRow("Theme", if (dark) "Dark" else "Light")
            KansoStatusRow("Dynamic color", if (dynamic) "On (Material You)" else "Off")
            KansoButton(
                if (dynamic) "Use brand accent" else "Use wallpaper color",
                onClick = onToggleDynamic,
                style = KansoButtonStyle.Text,
                modifier = Modifier.padding(top = Kanso.spacing.sm),
            )
        }

        KansoCard(title = "List items") {
            KansoListItem("Envelope-sealed transport", supporting = "cnsa2-e2ee-v3 · P-384 + ML-KEM-1024",
                icon = Icons.Filled.CheckCircle)
            KansoDivider(inset = true)
            KansoListItem("Encrypted ClientHello", supporting = "kid hidden from the relay",
                icon = Icons.Filled.Lock)
            KansoDivider(inset = true)
            KansoListItem("Fast path", supporting = "session pool + SSE", icon = Icons.Filled.Bolt)
        }

        KansoSectionHeader(
            "Status",
            supporting = "The roles Material 3 leaves to you — and never colour alone.",
        )
        KansoInfoBanner(
            text = "Showing cached results from 12 March.",
            icon = Icons.Filled.Info,
            action = { KansoButton("Refresh", onClick = {}, style = KansoButtonStyle.Text) },
        )
        KansoCard(title = "Certificates", subtitle = "Every badge states its meaning in words.") {
            KansoListItem(
                "api.example.com",
                supporting = "Expires in 63 days",
                trailing = { KansoStatusBadge("Valid", KansoStatus.Success) },
            )
            KansoDivider()
            KansoListItem(
                "cdn.example.com",
                supporting = "Expires in 4 days",
                trailing = { KansoStatusBadge("Expiring", KansoStatus.Warning) },
            )
            KansoDivider()
            KansoListItem(
                "old.example.com",
                supporting = "Expired 8 days ago",
                trailing = { KansoStatusBadge("Failed", KansoStatus.Error) },
            )
            KansoDivider()
            KansoListItem(
                "new.example.com",
                supporting = "Never checked",
                trailing = { KansoStatusBadge("Unknown", KansoStatus.Neutral) },
            )
        }
    }
}

/** A domain type for the picker — the point being that it is not a String. */
private enum class Transport(val label: String) {
    Direct("Direct"),
    Relay("Relay"),
    Onion("Onion"),
}

@Composable
private fun FormsScreen(inner: PaddingValues) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf<Transport?>(null) }
    var confirming by remember { mutableStateOf(false) }
    val pinError = pin.isNotEmpty() && pin.length < 6

    if (confirming) {
        KansoAlertDialog(
            title = "Forget this device?",
            text = "The pairing and its saved PIN are removed. This cannot be undone.",
            confirmText = "Forget",
            dismissText = "Cancel",
            destructive = true,
            onConfirm = { confirming = false; name = ""; pin = ""; transport = null },
            onDismiss = { confirming = false },
        )
    }

    Column(
        screenColumn(inner).padding(horizontal = Kanso.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Kanso.spacing.section),
    ) {
        KansoSectionHeader(
            "Text fields",
            supporting = "Label, helper text and a live error state.",
            modifier = Modifier.padding(top = Kanso.spacing.sm),
        )
        KansoCard {
            KansoTextField(
                value = name,
                onValueChange = { name = it },
                label = "Display name",
                supporting = "Shown to peers on the control channel.",
            )
            Column(Modifier.padding(top = Kanso.spacing.md)) {
                KansoPasswordField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = "Control PIN",
                    numeric = true,
                    isError = pinError,
                    errorText = "PIN must be at least 6 digits.",
                    supporting = "Pairs this device with the proxy.",
                )
            }
            Column(Modifier.padding(top = Kanso.spacing.md)) {
                KansoSelectField(
                    value = transport,
                    options = Transport.entries,
                    onSelect = { transport = it },
                    label = "Transport",
                    optionLabel = { it.label },
                    supporting = "The picker hands back the enum, not its label.",
                )
            }
            KansoButton(
                "Save",
                onClick = {},
                enabled = name.isNotBlank() && pin.length >= 6 && transport != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Kanso.spacing.lg),
            )
            KansoButton(
                "Forget this device",
                onClick = { confirming = true },
                style = KansoButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The four things a list can be showing when it is not showing a list. */
private enum class DemoState { Empty, Loading, Skeleton, Error }

@Composable
private fun StatesScreen(inner: PaddingValues) {
    var state by remember { mutableStateOf(DemoState.Empty) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Pull down anywhere on this screen to see the refresh indicator in the brand colour.
    KansoRefreshBox(
        refreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                delay(1500)
                refreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().padding(inner),
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (state) {
                DemoState.Empty -> KansoEmptyState(
                    icon = Icons.Filled.Inbox,
                    title = "Nothing here yet",
                    description = "Empty, loading, skeleton and error states share one visual " +
                        "language so every screen feels the same.",
                    action = {
                        KansoButton("Next state", onClick = { state = DemoState.Loading })
                    },
                )
                DemoState.Loading -> KansoLoadingState(label = "Establishing session…")
                // A skeleton promises the shape of what is coming; a spinner promises nothing.
                DemoState.Skeleton -> Column(Modifier.padding(Kanso.spacing.screen)) {
                    KansoSkeleton(rows = 5, icon = true)
                }
                DemoState.Error -> KansoErrorState(
                    title = "Could not reach the relay",
                    description = "The demo does not actually connect to anything — this is " +
                        "the state a failed load lands in.",
                    onRetry = { state = DemoState.Empty },
                )
            }
            if (state == DemoState.Loading || state == DemoState.Skeleton) {
                KansoButton(
                    "Next state",
                    onClick = {
                        state = if (state == DemoState.Loading) {
                            DemoState.Skeleton
                        } else {
                            DemoState.Error
                        }
                    },
                    style = KansoButtonStyle.Text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Kanso.spacing.screen),
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(inner: PaddingValues) {
    var sync by remember { mutableStateOf(true) }
    var diagnostics by remember { mutableStateOf(false) }
    var units by remember { mutableStateOf("Metric") }
    var sheetOpen by remember { mutableStateOf(false) }

    if (sheetOpen) {
        KansoBottomSheet(onDismiss = { sheetOpen = false }) {
            Column(Modifier.padding(Kanso.spacing.screen)) {
                KansoSectionHeader("About", supporting = "A sheet with the kanso container colour.")
                KansoStatusRow("Version", "demo")
                KansoStatusRow("Licence", "Apache-2.0")
                KansoButton(
                    "Close",
                    onClick = { sheetOpen = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Kanso.spacing.lg),
                )
            }
        }
    }

    Column(
        screenColumn(inner).padding(horizontal = Kanso.spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Kanso.spacing.section),
    ) {
        KansoSectionHeader(
            "Preferences",
            supporting = "Tap anywhere on a row — the row is the control, not the switch.",
            modifier = Modifier.padding(top = Kanso.spacing.sm),
        )
        KansoCard(contentPadding = PaddingValues(horizontal = Kanso.spacing.lg)) {
            KansoSwitchRow(
                headline = "Background sync",
                supporting = "Keeps peer status up to date while the app is closed",
                checked = sync,
                onCheckedChange = { sync = it },
                icon = Icons.Filled.Bolt,
            )
            KansoDivider()
            KansoCheckboxRow(
                headline = "Send diagnostics",
                supporting = "Anonymous crash reports only",
                checked = diagnostics,
                onCheckedChange = { diagnostics = it },
            )
        }

        KansoSectionHeader("Units")
        // selectableGroup is what makes a screen reader announce "1 of 2" — a radio row on its
        // own cannot know how many siblings it has.
        KansoCard(contentPadding = PaddingValues(horizontal = Kanso.spacing.lg)) {
            Column(Modifier.selectableGroup()) {
                listOf("Metric", "Imperial").forEach { option ->
                    KansoRadioRow(
                        headline = option,
                        selected = units == option,
                        onSelect = { units = option },
                    )
                }
            }
        }

        KansoButton(
            "About kanso",
            onClick = { sheetOpen = true },
            style = KansoButtonStyle.Tonal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
