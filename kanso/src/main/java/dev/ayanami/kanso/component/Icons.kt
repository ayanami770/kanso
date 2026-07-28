/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// The only icons kanso draws itself.
//
// The library deliberately depends on no icon set — an app picks its own, and material3 no
// longer brings even material-icons-core transitively. These exist because each is part of a
// component's own behaviour rather than decoration a caller supplies: the password reveal
// toggle, the error state's glyph, the scaffold's back affordance. Everything else is the
// consumer's to pass in.
//
// Filled with SolidColor(Black) as a placeholder: Icon() applies its own tint over the whole
// vector, so the colour here never reaches the screen.

private const val ViewportSize = 24f
private val IconSizeDp = 24.dp
private const val StrokeWidth = 1.8f

private fun kansoIcon(
    name: String,
    autoMirror: Boolean = false,
    build: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = IconSizeDp,
    defaultHeight = IconSizeDp,
    viewportWidth = ViewportSize,
    viewportHeight = ViewportSize,
    autoMirror = autoMirror,
).apply(build).build()

/** An open eye — the password is hidden, tapping reveals it. */
internal val EyeIcon: ImageVector = kansoIcon("kanso_eye") {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = StrokeWidth) {
        moveTo(12f, 5f)
        curveTo(7f, 5f, 2.7f, 8.1f, 1f, 12f)
        curveTo(2.7f, 15.9f, 7f, 19f, 12f, 19f)
        curveTo(17f, 19f, 21.3f, 15.9f, 23f, 12f)
        curveTo(21.3f, 8.1f, 17f, 5f, 12f, 5f)
        close()
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 9.4f)
        curveTo(13.44f, 9.4f, 14.6f, 10.56f, 14.6f, 12f)
        curveTo(14.6f, 13.44f, 13.44f, 14.6f, 12f, 14.6f)
        curveTo(10.56f, 14.6f, 9.4f, 13.44f, 9.4f, 12f)
        curveTo(9.4f, 10.56f, 10.56f, 9.4f, 12f, 9.4f)
        close()
    }
}

/** A struck-through eye — the password is revealed, tapping hides it again. */
internal val EyeOffIcon: ImageVector = kansoIcon("kanso_eye_off") {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = StrokeWidth) {
        moveTo(12f, 5f)
        curveTo(7f, 5f, 2.7f, 8.1f, 1f, 12f)
        curveTo(2.7f, 15.9f, 7f, 19f, 12f, 19f)
        curveTo(17f, 19f, 21.3f, 15.9f, 23f, 12f)
        curveTo(21.3f, 8.1f, 17f, 5f, 12f, 5f)
        close()
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 9.4f)
        curveTo(13.44f, 9.4f, 14.6f, 10.56f, 14.6f, 12f)
        curveTo(14.6f, 13.44f, 13.44f, 14.6f, 12f, 14.6f)
        curveTo(10.56f, 14.6f, 9.4f, 13.44f, 9.4f, 12f)
        curveTo(9.4f, 10.56f, 10.56f, 9.4f, 12f, 9.4f)
        close()
    }
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(3.5f, 3.5f)
        lineTo(20.5f, 20.5f)
    }
}

/**
 * A circled exclamation — [KansoErrorState]'s default glyph.
 *
 * A shape rather than a colour, on purpose: the error state renders it in the same muted tint
 * as [KansoEmptyState]'s icon, so "this failed" is legible to a reader who cannot separate red
 * from grey, and to a screen reader that gets the title text either way.
 */
internal val ErrorIcon: ImageVector = kansoIcon("kanso_error") {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = StrokeWidth) {
        moveTo(12f, 2.6f)
        curveTo(17.19f, 2.6f, 21.4f, 6.81f, 21.4f, 12f)
        curveTo(21.4f, 17.19f, 17.19f, 21.4f, 12f, 21.4f)
        curveTo(6.81f, 21.4f, 2.6f, 17.19f, 2.6f, 12f)
        curveTo(2.6f, 6.81f, 6.81f, 2.6f, 12f, 2.6f)
        close()
    }
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(12f, 7f)
        lineTo(12f, 13f)
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 15.4f)
        curveTo(12.77f, 15.4f, 13.4f, 16.03f, 13.4f, 16.8f)
        curveTo(13.4f, 17.57f, 12.77f, 18.2f, 12f, 18.2f)
        curveTo(11.23f, 18.2f, 10.6f, 17.57f, 10.6f, 16.8f)
        curveTo(10.6f, 16.03f, 11.23f, 15.4f, 12f, 15.4f)
        close()
    }
}

/**
 * A back arrow for [KansoBackButton].
 *
 * `autoMirror` is the whole reason this is not four lines inline: "back" points the other way
 * in an RTL layout, and a hand-drawn vector without the flag would send a right-to-left reader
 * forwards.
 */
internal val ArrowBackIcon: ImageVector = kansoIcon("kanso_arrow_back", autoMirror = true) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(20f, 12f)
        lineTo(4.5f, 12f)
        moveTo(11f, 4.5f)
        lineTo(3.5f, 12f)
        lineTo(11f, 19.5f)
    }
}
