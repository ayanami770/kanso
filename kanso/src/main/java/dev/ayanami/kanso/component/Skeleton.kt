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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.ayanami.kanso.R
import dev.ayanami.kanso.theme.Kanso

private val IconPlaceholderSize = 24.dp
private val HeadlineBarHeight = 16.dp
private val SupportingBarHeight = 12.dp
private val RowMinHeight = 48.dp

/** The proportion of the row's width each placeholder bar takes. */
private const val HeadlineWidthFraction = 0.55f
private const val SupportingWidthFraction = 0.8f

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
 * standing in for. Match [rows] and [icon] to the list being loaded.
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
    // Between two adjacent surface tones, not between a colour and transparency: the pulse has
    // to stay legible against a card and against the bare background, and an alpha ramp only
    // works over one of them.
    val barColor = lerp(
        Kanso.colors.surfaceContainerHighest,
        Kanso.colors.surfaceContainerHigh,
        progress,
    )
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

@Composable
private fun SkeletonRow(barColor: Color, icon: Boolean, supporting: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (supporting) RowMinHeight + Kanso.spacing.xl else RowMinHeight)
            .padding(vertical = Kanso.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) {
            Bar(barColor, Modifier.size(IconPlaceholderSize), CircleShape)
            Spacer(Modifier.size(Kanso.spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Bar(
                barColor,
                Modifier.fillMaxWidth(HeadlineWidthFraction).height(HeadlineBarHeight),
            )
            if (supporting) {
                Spacer(Modifier.size(Kanso.spacing.sm))
                Bar(
                    barColor,
                    Modifier.fillMaxWidth(SupportingWidthFraction).height(SupportingBarHeight),
                )
            }
        }
    }
}

@Composable
private fun Bar(
    color: Color,
    modifier: Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(percent = 50),
) {
    Spacer(modifier.background(color, shape))
}
