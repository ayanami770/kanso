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
// longer brings even material-icons-core transitively. But KansoPasswordField's reveal toggle
// is part of the component's behaviour, not decoration a caller supplies, so these two live
// here rather than becoming a required parameter or a dependency every consumer inherits.
//
// Filled with SolidColor(Black) as a placeholder: Icon() applies its own tint over the whole
// vector, so the colour here never reaches the screen.

private const val ViewportSize = 24f
private val IconSizeDp = 24.dp
private const val StrokeWidth = 1.8f

private fun kansoIcon(
    name: String,
    build: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = IconSizeDp,
    defaultHeight = IconSizeDp,
    viewportWidth = ViewportSize,
    viewportHeight = ViewportSize,
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
