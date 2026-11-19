/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import dev.ayanami.kanso.R
import dev.ayanami.kanso.theme.Kanso
import dev.ayanami.kanso.theme.KansoMinTouchTarget
import kotlin.math.ceil

/** A bar's thickness as a share of the text line it stands in for — 16dp and 12dp by default. */
private const val HeadlineBarFraction = 2f / 3f
private const val SupportingBarFraction = 0.6f

/** The proportion of the row's width each placeholder bar takes. */
private const val HeadlineWidthFraction = 0.55f
private const val SupportingWidthFraction = 0.8f

/** How strongly the content colour is laid over the surface at the two ends of a pulse. */
private const val BarAlphaStrong = 0.14f
private const val BarAlphaFaint = 0.09f

/** What a text style's line occupies when the style does not say. */
private const val FallbackLineHeightMultiplier = 1.2f

// The font scale above which Compose keeps a text's line height in proportion to its font size
// rather than converting it from sp by itself.
private const val NonLinearFontScale = 1.05f

/**
 * The shape of a list, drawn as bars, while the list itself is still loading.
 *
 * Preferred over [KansoLoadingState] for a first load that fills a whole screen: a centred
 * spinner says only "wait", whereas this says what is about to arrive and how much of it, so
 * the layout does not jump when the data lands. Keep using [KansoLoadingState] for a modal or
 * a short wait where there is no shape to promise.
 *
 * Deliberately one component and not a `Modifier.kansoSkeleton()`. A general-purpose shimmer
 * modifier gets applied to whatever is convenient, and the result is a screen that pulses in
 * six unrelated rhythms; a single row skeleton keeps the placeholder honest about what it is
 * standing in for. Match [rows] and [icon] to the list being loaded. A row is exactly as tall as
 * the [KansoListItem] it replaces, at any font scale, so nothing below it moves when the data
 * arrives.
 *
 * The whole block is announced as one "loading content" node. The bars carry no meaning to read
 * out, and left alone a screen reader would find nothing here at all.
 *
 * The shimmer runs forever by construction, so a test that waits for the composition to go idle
 * will hang on it. Drive the clock manually (`mainClock.autoAdvance = false`) if you need to
 * assert against a screen showing one.
 */
@Composable
public fun KansoSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 3,
    icon: Boolean = false,
    supporting: Boolean = true,
) {
    val loadingLabel = stringResource(R.string.kanso_loading_content)
    val transition = rememberInfiniteTransition(label = "kanso_skeleton")
    // One transition for the whole block rather than one per row: the bars must pulse together,
    // or the placeholder reads as content that is arriving rather than content that is absent.
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Kanso.motion.shimmer, easing = Kanso.motion.easing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kanso_skeleton_progress",
    )
    // The content colour laid over whatever is underneath, not a pair of surface tones. A tone
    // only shows against a different tone, and the natural pair for this — the two highest
    // surface containers — is what a filled card is painted in: a skeleton inside a KansoCard
    // pulsed between the card's own colour and almost that. An overlay darkens a light surface
    // and lightens a dark one, whichever surface it lands on.
    val barColor = skeletonBarColor(Kanso.colors.onSurface, progress)
    Column(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = loadingLabel },
    ) {
        repeat(rows) {
            SkeletonRow(barColor = barColor, icon = icon, supporting = supporting)
        }
    }
}

/** The bar colour at [progress] of one pulse: [onSurface] at an alpha between the two ends. */
internal fun skeletonBarColor(onSurface: Color, progress: Float): Color =
    onSurface.copy(alpha = BarAlphaStrong + (BarAlphaFaint - BarAlphaStrong) * progress)

@Composable
private fun SkeletonRow(barColor: Color, icon: Boolean, supporting: Boolean) {
    // The row is built the way the list row is — the same padding around the same line heights
    // — rather than given a height in dp. A fixed height matched a two-line row only
    // approximately at the default font scale and not at all at any other, which is the layout
    // jump a skeleton exists to prevent.
    val density = LocalDensity.current
    val headlineLine = Kanso.typography.bodyLarge.lineHeightIn(density)
    val supportingLine = Kanso.typography.bodyMedium.lineHeightIn(density)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = KansoMinTouchTarget)
            .padding(vertical = Kanso.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) {
            Bar(barColor, Modifier.size(Kanso.sizing.icon), CircleShape)
            Spacer(Modifier.size(Kanso.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            BarLine(barColor, headlineLine, HeadlineBarFraction, HeadlineWidthFraction)
            if (supporting) {
                BarLine(barColor, supportingLine, SupportingBarFraction, SupportingWidthFraction)
            }
        }
    }
}

/** One line of text's worth of height, with a bar centred in it. */
@Composable
private fun BarLine(color: Color, lineHeight: Dp, barFraction: Float, widthFraction: Float) {
    Box(Modifier.fillMaxWidth().height(lineHeight), contentAlignment = Alignment.CenterStart) {
        Bar(color, Modifier.fillMaxWidth(widthFraction).height(lineHeight * barFraction))
    }
}

/**
 * What one line of this style occupies, in dp at the current font scale — worked out the way the
 * text layout works it out, because a pixel of difference per line is a row that jumps when the
 * content replaces its placeholder.
 */
private fun TextStyle.lineHeightIn(density: Density): Dp = with(density) {
    val fontPx = if (fontSize.isSp) fontSize.toPx() else Float.NaN
    val px = when {
        // At an ordinary font scale a line height in sp is converted on its own. Above that the
        // system's font-scale curve is not linear, and text keeps the line height in proportion
        // to the font size instead: converted separately, the placeholder's line comes out
        // shorter than the one the text is drawn on.
        lineHeight.isSp && fontSize.isSp && fontScale > NonLinearFontScale ->
            lineHeight.value / fontPx.toSp().value * fontPx

        lineHeight.isSp -> lineHeight.toPx()

        lineHeight.isEm && fontSize.isSp -> fontPx * lineHeight.value

        fontSize.isSp -> fontPx * FallbackLineHeightMultiplier

        else -> (KansoMinTouchTarget / 2).toPx()
    }
    // A line is rounded up to a whole pixel, not to the nearest one.
    ceil(px).toDp()
}

@Composable
private fun Bar(
    color: Color,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(percent = 50),
) {
    Spacer(modifier.background(color, shape))
}
