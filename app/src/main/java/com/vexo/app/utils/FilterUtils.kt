package com.vexo.app.utils

import android.graphics.ColorMatrix

object FilterUtils {

    fun cinematic(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.1f,  0f,    0f,    0f, -20f,
        0f,    1.0f,  0f,    0f, -10f,
        0f,    0f,    0.9f,  0f,   0f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun hdr(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.3f,  0f,    0f,    0f, -30f,
        0f,    1.3f,  0f,    0f, -30f,
        0f,    0f,    1.3f,  0f, -30f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun aesthetic(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.0f,  0.05f, 0f,    0f,  10f,
        0f,    1.0f,  0.05f, 0f,  10f,
        0.05f, 0f,    1.1f,  0f,  15f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun warmGlow(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.2f,  0f,    0f,    0f,  20f,
        0f,    1.05f, 0f,    0f,  10f,
        0f,    0f,    0.8f,  0f, -10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun coolTone(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.9f,  0f,    0f,    0f, -10f,
        0f,    0.95f, 0f,    0f,   0f,
        0f,    0f,    1.2f,  0f,  20f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun vintageFilm(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.9f,  0.05f, 0f,    0f,  20f,
        0.05f, 0.85f, 0f,    0f,  10f,
        0f,    0f,    0.7f,  0f,  20f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun retro(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.1f,  0.1f,  0f,    0f,  30f,
        0f,    0.9f,  0.1f,  0f,  20f,
        0f,    0.1f,  0.8f,  0f,  10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun y2k(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.2f,  0f,    0.1f,  0f,  10f,
        0f,    1.1f,  0f,    0f,  10f,
        0.1f,  0f,    1.3f,  0f,  20f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun vhs(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.8f,  0.1f,  0f,    0f,  20f,
        0.05f, 0.75f, 0.1f,  0f,  15f,
        0f,    0.1f,  0.7f,  0f,  10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun bwNoir(): ColorMatrix = ColorMatrix().also { it.setSaturation(0f) }

    fun glamour(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.15f, 0f,    0f,    0f,  15f,
        0f,    1.0f,  0f,    0f,  10f,
        0f,    0f,    1.1f,  0f,  20f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun nightScene(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.7f,  0f,    0f,    0f, -20f,
        0f,    0.7f,  0f,    0f, -10f,
        0f,    0f,    1.1f,  0f,  10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun movie(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.0f,  0f,    0f,    0f, -15f,
        0f,    0.95f, 0f,    0f, -10f,
        0f,    0f,    0.85f, 0f,  10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun colorist(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.05f, 0.02f, 0f,    0f,   5f,
        0f,    1.05f, 0.02f, 0f,   5f,
        0.02f, 0f,    1.05f, 0f,   5f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun neon(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.4f,  0f,    0.2f,  0f, -20f,
        0f,    1.2f,  0f,    0f, -20f,
        0.2f,  0f,    1.5f,  0f, -10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun dreamy(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.0f,  0.05f, 0.05f, 0f,  20f,
        0.05f, 1.0f,  0.05f, 0f,  20f,
        0.05f, 0.05f, 1.1f,  0f,  30f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun darkMood(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.75f, 0f,    0f,    0f, -30f,
        0f,    0.75f, 0f,    0f, -30f,
        0f,    0f,    0.85f, 0f, -10f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun fadedFilm(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.85f, 0f,    0f,    0f,  40f,
        0f,    0.80f, 0f,    0f,  30f,
        0f,    0f,    0.75f, 0f,  30f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun cartoonAI(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.5f,  0f,    0f,    0f, -40f,
        0f,    1.5f,  0f,    0f, -40f,
        0f,    0f,    1.5f,  0f, -40f,
        0f,    0f,    0f,    1f,   0f
    ))

    fun barbiePink(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.3f,  0f,    0.1f,  0f,  30f,
        0f,    0.7f,  0.1f,  0f,  10f,
        0.1f,  0f,    0.9f,  0f,  20f,
        0f,    0f,    0f,    1f,   0f
    ))
}
