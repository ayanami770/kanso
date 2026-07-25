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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Layers
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import dev.ayanami.kanso.component.KansoButton
import dev.ayanami.kanso.component.KansoButtonStyle
import dev.ayanami.kanso.component.KansoCard
import dev.ayanami.kanso.component.KansoEmptyState
import dev.ayanami.kanso.component.KansoListItem
import dev.ayanami.kanso.component.KansoLoadingState
import dev.ayanami.kanso.component.KansoScaffold
import dev.ayanami.kanso.component.KansoSectionHeader
import dev.ayanami.kanso.component.KansoStatusRow
import dev.ayanami.kanso.component.KansoTextField
import dev.ayanami.kanso.theme.Kanso
import dev.ayanami.kanso.theme.KansoBrand
import dev.ayanami.kanso.theme.KansoDefaultBrand
import dev.ayanami.kanso.theme.KansoTheme

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
        when (tab) {
            Tab.Gallery -> GalleryScreen(inner, brandName, dark, dynamic, onToggleDynamic)
            Tab.Forms -> FormsScreen(inner)
            Tab.States -> StatesScreen(inner)
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
            KansoListItem("Encrypted ClientHello", supporting = "kid hidden from the relay",
                icon = Icons.Filled.Lock)
            KansoListItem("Fast path", supporting = "session pool + SSE", icon = Icons.Filled.Bolt)
        }
    }
}

@Composable
private fun FormsScreen(inner: PaddingValues) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val pinError = pin.isNotEmpty() && pin.length < 6

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
                KansoTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = "Control PIN",
                    keyboardType = KeyboardType.NumberPassword,
                    // NumberPassword only picks the keyboard; Compose still draws the glyphs.
                    visualTransformation = PasswordVisualTransformation(),
                    isError = pinError,
                    errorText = "PIN must be at least 6 digits.",
                    supporting = "Pairs this device with the proxy.",
                )
            }
            KansoButton(
                "Save",
                onClick = {},
                enabled = name.isNotBlank() && pin.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Kanso.spacing.lg),
            )
        }
    }
}

@Composable
private fun StatesScreen(inner: PaddingValues) {
    var loading by remember { mutableStateOf(false) }

    Column(screenColumn(inner)) {
        if (loading) {
            KansoLoadingState(label = "Establishing session…")
        } else {
            KansoEmptyState(
                icon = Icons.Filled.Inbox,
                title = "Nothing here yet",
                description = "Empty, loading and error states share one visual language so every screen feels the same.",
                action = {
                    KansoButton("Simulate loading", onClick = { loading = true })
                },
            )
        }
    }
}
