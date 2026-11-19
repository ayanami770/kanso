/*
 * Copyright 2026 ayanami770
 * Licensed under the Apache License, Version 2.0.
 */
package dev.ayanami.kanso.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A scheme has to be the colour its seed is.
 *
 * Contrast was never what this guards: tone is luminance, and every ratio held while the hue
 * went wrong. The engine once held CIELAB's hue angle from tone to tone, and CIELAB's hue
 * lines are not perceptually straight through the blues — so a pure blue seed produced a
 * violet primary, 31° of OKLab hue away from it, and lighter roles 39° away. Measured over the
 * 72 seeds below, 15 had a primary role more than 12° from the seed.
 *
 * Everything here goes through the public scheme builders and measures hue with its own OKLab
 * arithmetic, so it holds whatever the engine does inside.
 */
class HueFidelityTest {

    private class Measured(val chroma: Double, val hue: Double)

    /** OKLab chroma and hue (degrees), worked out here rather than asked of the engine. */
    private fun oklch(color: Color): Measured {
        fun linear(channel: Float): Double {
            val u = channel.toDouble()
            return if (u <= 0.04045) u / 12.92 else ((u + 0.055) / 1.055).pow(2.4)
        }
        val r = linear(color.red)
        val g = linear(color.green)
        val b = linear(color.blue)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bAxis = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        return Measured(
            chroma = sqrt(a * a + bAxis * bAxis),
            hue = (atan2(bAxis, a) * 180.0 / PI + 360.0) % 360.0,
        )
    }

    private fun hueDistance(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return min(d, 360.0 - d)
    }

    /** Fully saturated seeds five degrees of HSV hue apart — the circle the drift was found on. */
    private val seeds: List<Color> = (0 until 360 step 5).map { Color.hsv(it.toFloat(), 1f, 1f) }

    private fun hex(color: Color): String = "#%06X".format(
        ((color.red * 255f + 0.5f).toInt() shl 16) or
            ((color.green * 255f + 0.5f).toInt() shl 8) or
            (color.blue * 255f + 0.5f).toInt(),
    )

    /**
     * The four roles that carry a brand, in both schemes. What is left of the old drift is the
     * rounding of a colour into eight bits a channel: 1.5° at worst, in the palest of them.
     */
    @Test
    fun `the primary roles keep the seed's hue round the whole circle`() {
        val drifted = mutableListOf<String>()
        var worst = 0.0
        seeds.forEach { seed ->
            val light = kansoLightColorScheme(seed)
            val dark = kansoDarkColorScheme(seed)
            listOf(
                "light primary" to light.primary,
                "light primaryContainer" to light.primaryContainer,
                "dark primary" to dark.primary,
                "dark primaryContainer" to dark.primaryContainer,
            ).forEach { (role, color) ->
                val distance = hueDistance(oklch(seed).hue, oklch(color).hue)
                worst = maxOf(worst, distance)
                if (distance > 3.0) {
                    drifted += "seed ${hex(seed)} $role ${hex(color)} is " +
                        "${"%.1f".format(distance)}° off"
                }
            }
        }
        assertTrue(
            "${drifted.size} primary role(s) more than 3° of OKLab hue from their seed; the " +
                "worst is ${"%.1f".format(worst)}°. First 5:\n" +
                drifted.take(5).joinToString("\n"),
            drifted.isEmpty(),
        )
    }

    /** The seed the drift was worst for, and the colour the README says it gives. */
    @Test
    fun `a pure blue seed gives a blue primary`() {
        val seed = Color(0xFF0000FF)
        val primary = kansoLightColorScheme(seed).primary
        assertTrue(
            "#0000FF gave ${hex(primary)}, ${hueDistance(oklch(seed).hue, oklch(primary).hue)}° " +
                "of hue from its seed",
            hueDistance(oklch(seed).hue, oklch(primary).hue) <= 3.0,
        )
        assertEquals("the README names this colour", Color(0xFF335AB4), primary)
    }

    /**
     * Every role with a colour to speak of, not only the four above: the secondary palette at
     * the seed's hue, the tertiary one sixty degrees on. Roles too near grey to have a hue at
     * eight bits a channel are left out — there, one step of one channel is tens of degrees.
     */
    @Test
    fun `every coloured role keeps the hue its palette was given`() {
        fun roles(s: ColorScheme): List<Triple<String, Color, Double>> = listOf(
            Triple("primary", s.primary, 0.0),
            Triple("primaryContainer", s.primaryContainer, 0.0),
            Triple("onPrimaryContainer", s.onPrimaryContainer, 0.0),
            Triple("inversePrimary", s.inversePrimary, 0.0),
            Triple("secondary", s.secondary, 0.0),
            Triple("secondaryContainer", s.secondaryContainer, 0.0),
            Triple("onSecondaryContainer", s.onSecondaryContainer, 0.0),
            Triple("tertiary", s.tertiary, 60.0),
            Triple("tertiaryContainer", s.tertiaryContainer, 60.0),
            Triple("onTertiaryContainer", s.onTertiaryContainer, 60.0),
        )
        val drifted = mutableListOf<String>()
        seeds.forEach { seed ->
            listOf(
                "light" to kansoLightColorScheme(seed),
                "dark" to kansoDarkColorScheme(seed),
            ).forEach { (mode, scheme) ->
                roles(scheme).forEach { (role, color, shift) ->
                    val measured = oklch(color)
                    val wanted = (oklch(seed).hue + shift) % 360.0
                    val distance = hueDistance(wanted, measured.hue)
                    if (measured.chroma >= 0.02 && distance > 4.0) {
                        drifted += "seed ${hex(seed)} $mode $role ${hex(color)} is " +
                            "${"%.1f".format(distance)}° off"
                    }
                }
            }
        }
        assertTrue(
            "${drifted.size} role(s) more than 4° of OKLab hue from where their palette was " +
                "put. First 5:\n" + drifted.take(5).joinToString("\n"),
            drifted.isEmpty(),
        )
    }
}
