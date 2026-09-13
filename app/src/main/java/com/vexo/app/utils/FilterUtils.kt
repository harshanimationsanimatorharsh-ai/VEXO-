package com.vexo.app.utils

import android.graphics.ColorMatrix

object FilterUtils {

    fun none() = ColorMatrix().apply {
        set(floatArrayOf(
            1f,0f,0f,0f,0f,
            0f,1f,0f,0f,0f,
            0f,0f,1f,0f,0f,
            0f,0f,0f,1f,0f))
    }

    fun cinematic() = ColorMatrix().apply {
        set(floatArrayOf(
            0.6f,0.3f,0.1f,0f,-10f,
            0.1f,0.7f,0.2f,0f,-5f,
            0.05f,0.15f,0.8f,0f,10f,
            0f,0f,0f,1f,0f))
    }

    fun hdr() = ColorMatrix().apply {
        set(floatArrayOf(
            1.4f,0f,0f,0f,-30f,
            0f,1.4f,0f,0f,-30f,
            0f,0f,1.4f,0f,-30f,
            0f,0f,0f,1f,0f))
    }

    fun aesthetic() = ColorMatrix().apply {
        set(floatArrayOf(
            1.1f,0.05f,0f,0f,10f,
            0f,1.0f,0.05f,0f,5f,
            0.05f,0f,1.1f,0f,15f,
            0f,0f,0f,1f,0f))
    }

    fun warmGlow() = ColorMatrix().apply {
        set(floatArrayOf(
            1.3f,0.05f,0f,0f,25f,
            0.05f,1.1f,0f,0f,10f,
            0f,0f,0.75f,0f,-15f,
            0f,0f,0f,1f,0f))
    }

    fun coolTone() = ColorMatrix().apply {
        set(floatArrayOf(
            0.75f,0f,0.05f,0f,-15f,
            0f,1.0f,0.05f,0f,5f,
            0.05f,0.05f,1.3f,0f,25f,
            0f,0f,0f,1f,0f))
    }

    fun vintageFilm() = ColorMatrix().apply {
        set(floatArrayOf(
            0.9f,0.1f,0.05f,0f,20f,
            0.1f,0.85f,0.1f,0f,15f,
            0.05f,0.1f,0.75f,0f,-5f,
            0f,0f,0f,1f,0f))
    }

    fun retro() = ColorMatrix().apply {
        set(floatArrayOf(
            1.1f,0.2f,0f,0f,10f,
            0.1f,0.9f,0.1f,0f,5f,
            0f,0.1f,0.7f,0f,-10f,
            0f,0f,0f,1f,0f))
    }

    fun y2k() = ColorMatrix().apply {
        set(floatArrayOf(
            1.2f,0.1f,0.1f,0f,15f,
            0.1f,1.0f,0.2f,0f,10f,
            0.1f,0.2f,1.2f,0f,20f,
            0f,0f,0f,1f,0f))
    }

    fun vhs() = ColorMatrix().apply {
        set(floatArrayOf(
            0.8f,0.2f,0f,0f,10f,
            0f,0.8f,0.2f,0f,5f,
            0.2f,0f,0.8f,0f,8f,
            0f,0f,0f,1f,0f))
    }

    fun bwNoir() = ColorMatrix().apply {
        setSaturation(0f)
        val c = ColorMatrix().apply {
            set(floatArrayOf(
                1.2f,0f,0f,0f,-20f,
                0f,1.2f,0f,0f,-20f,
                0f,0f,1.2f,0f,-20f,
                0f,0f,0f,1f,0f))
        }
        postConcat(c)
    }

    fun glamour() = ColorMatrix().apply {
        set(floatArrayOf(
            1.15f,0.05f,0.05f,0f,20f,
            0.05f,1.1f,0.05f,0f,15f,
            0.05f,0.05f,1.05f,0f,10f,
            0f,0f,0f,1f,0f))
    }

    fun nightScene() = ColorMatrix().apply {
        set(floatArrayOf(
            0.5f,0.1f,0.2f,0f,-20f,
            0.05f,0.6f,0.15f,0f,-15f,
            0.1f,0.2f,1.0f,0f,10f,
            0f,0f,0f,1f,0f))
    }

    fun movie() = ColorMatrix().apply {
        set(floatArrayOf(
            0.7f,0.2f,0.1f,0f,-5f,
            0.1f,0.75f,0.15f,0f,-5f,
            0.05f,0.1f,0.85f,0f,5f,
            0f,0f,0f,1f,0f))
    }

    fun colorist() = ColorMatrix().apply {
        set(floatArrayOf(
            1.05f,0.1f,0.05f,0f,5f,
            0.05f,1.0f,0.1f,0f,5f,
            0.1f,0.05f,1.05f,0f,10f,
            0f,0f,0f,1f,0f))
    }

    fun neon() = ColorMatrix().apply {
        set(floatArrayOf(
            1.5f,0f,0.3f,0f,20f,
            0f,1.3f,0.3f,0f,10f,
            0.3f,0.3f,1.5f,0f,30f,
            0f,0f,0f,1f,0f))
    }

    fun dreamy() = ColorMatrix().apply {
        set(floatArrayOf(
            1.0f,0.1f,0.1f,0f,30f,
            0.1f,1.0f,0.1f,0f,20f,
            0.1f,0.1f,1.1f,0f,30f,
            0f,0f,0f,0.95f,0f))
    }

    fun darkMood() = ColorMatrix().apply {
        set(floatArrayOf(
            0.7f,0.1f,0.05f,0f,-30f,
            0.05f,0.7f,0.1f,0f,-25f,
            0.05f,0.05f,0.8f,0f,-20f,
            0f,0f,0f,1f,0f))
    }

    fun fadedFilm() = ColorMatrix().apply {
        set(floatArrayOf(
            0.8f,0.1f,0.05f,0f,40f,
            0.05f,0.8f,0.1f,0f,35f,
            0.05f,0.05f,0.8f,0f,30f,
            0f,0f,0f,1f,0f))
    }

    fun cartoonAi() = ColorMatrix().apply {
        set(floatArrayOf(
            2.0f,0f,0f,0f,-100f,
            0f,2.0f,0f,0f,-100f,
            0f,0f,2.0f,0f,-100f,
            0f,0f,0f,1f,0f))
    }

    fun barbiePink() = ColorMatrix().apply {
        set(floatArrayOf(
            1.3f,0.1f,0.3f,0f,30f,
            0f,0.8f,0.2f,0f,10f,
            0.1f,0f,0.9f,0f,15f,
            0f,0f,0f,1f,0f))
    }
}
