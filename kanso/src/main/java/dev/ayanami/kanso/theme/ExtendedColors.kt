/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The semantic colour roles Material 3 does not define, in the shape it uses for `error`.
 *
 * Every app needs pass/fail, in-range/out-of-range, valid/expiring **by name**. Without these
 * each one invents its own `Color(0xFF2E7D32)` outside the design system — exactly the token
 * drift a design system exists to prevent.
 *
 * These are fixed literals rather than seed-derived, for the same reason Material 3 fixes the
 * error palette: "success" must read as success under every brand, and a hue rotation must not
 * be able to turn a warning green. They are also the reason the roles are provided *after* the
 * colour scheme is resolved — anything computed inside the seed builders would vanish under
 * dynamic colour, a device-dependent failure that never shows up on the developer's phone.
 *
 * Read them through [Kanso.extendedColors]. Contrast for every on-pair is pinned by
 * `ColorContrastTest`.
 */
@Immutable
data class KansoExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

/** Extended roles for a light scheme. */
val KansoLightExtendedColors = KansoExtendedColors(
    success = Color(0xFF2E6B34),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFB1F1AE),
    onSuccessContainer = Color(0xFF002204),
    warning = Color(0xFF7A5900),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDF9B),
    onWarningContainer = Color(0xFF261A00),
    info = Color(0xFF0B5FA5),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFD3E4FF),
    onInfoContainer = Color(0xFF001C38),
)

/** Extended roles for a dark scheme. */
val KansoDarkExtendedColors = KansoExtendedColors(
    success = Color(0xFF96D593),
    onSuccess = Color(0xFF00390B),
    successContainer = Color(0xFF14521E),
    onSuccessContainer = Color(0xFFB1F1AE),
    warning = Color(0xFFF2C047),
    onWarning = Color(0xFF402D00),
    warningContainer = Color(0xFF5C4200),
    onWarningContainer = Color(0xFFFFDF9B),
    info = Color(0xFFA3C9FF),
    onInfo = Color(0xFF00325A),
    infoContainer = Color(0xFF00497F),
    onInfoContainer = Color(0xFFD3E4FF),
)

val LocalKansoExtendedColors = staticCompositionLocalOf { KansoLightExtendedColors }
