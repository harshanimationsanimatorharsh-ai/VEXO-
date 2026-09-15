package com.vexo.app.utils

import android.graphics.ColorMatrix

object FilterUtils {

    fun normal(): ColorMatrix = ColorMatrix()

    fun blackAndWhite(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        return matrix
    }

    fun vintage(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.9f, 0.1f, 0.1f, 0f, 20f,
        0.1f, 0.8f, 0.1f, 0f, 10f,
        0.1f, 0.1f, 0.6f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun cool(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.8f, 0f, 0f, 0f, 0f,
        0f, 0.9f, 0f, 0f, 0f,
        0f, 0f, 1.2f, 0f, 20f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun warm(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.2f, 0f, 0f, 0f, 20f,
        0f, 1.0f, 0f, 0f, 10f,
        0f, 0f, 0.8f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun bright(): ColorMatrix = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, 50f,
        0f, 1f, 0f, 0f, 50f,
        0f, 0f, 1f, 0f, 50f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun dark(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.7f, 0f, 0f, 0f, 0f,
        0f, 0.7f, 0f, 0f, 0f,
        0f, 0f, 0.7f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun contrast(): ColorMatrix {
        val scale = 1.5f
        val translate = (-0.5f * scale + 0.5f) * 255f
        return ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun fade(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.8f, 0f, 0f, 0f, 30f,
        0f, 0.8f, 0f, 0f, 30f,
        0f, 0f, 0.8f, 0f, 30f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun sepia(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun invert(): ColorMatrix = ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun saturate(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(2.5f)
        return matrix
    }

    fun desaturate(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(0.3f)
        return matrix
    }

    fun redBoost(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.5f, 0f, 0f, 0f, 0f,
        0f, 0.9f, 0f, 0f, 0f,
        0f, 0f, 0.9f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun greenBoost(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.9f, 0f, 0f, 0f, 0f,
        0f, 1.5f, 0f, 0f, 0f,
        0f, 0f, 0.9f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun blueBoost(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.9f, 0f, 0f, 0f, 0f,
        0f, 0.9f, 0f, 0f, 0f,
        0f, 0f, 1.5f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun dramatic(): ColorMatrix {
        val scale = 1.8f
        val translate = (-0.5f * scale + 0.5f) * 255f
        return ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale * 0.9f, 0f, 0f, translate,
            0f, 0f, scale * 0.8f, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun matte(): ColorMatrix = ColorMatrix(floatArrayOf(
        0.9f, 0f, 0f, 0f, 20f,
        0f, 0.85f, 0f, 0f, 20f,
        0f, 0f, 0.8f, 0f, 30f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun cartoonAI(): ColorMatrix {
        val matrix = ColorMatrix()
        matrix.setSaturation(3f)
        return matrix
    }

    fun barbiePink(): ColorMatrix = ColorMatrix(floatArrayOf(
        1.2f, 0f, 0f, 0f, 30f,
        0f, 0.7f, 0f, 0f, 0f,
        0f, 0f, 0.8f, 0f, 20f,
        0f, 0f, 0f, 1f, 0f
    ))

    fun getFilterNames(): List<String> = listOf(
        "Normal", "B&W", "Vintage", "Cool", "Warm",
        "Bright", "Dark", "Contrast", "Fade", "Sepia",
        "Invert", "Saturate", "Desaturate", "Red", "Green",
        "Blue", "Dramatic", "Matte", "CartoonAI", "Barbie"
    )

    fun getFilter(name: String): ColorMatrix = when (name) {
        "Normal" -> normal()
        "B&W" -> blackAndWhite()
        "Vintage" -> vintage()
        "Cool" -> cool()
        "Warm" -> warm()
        "Bright" -> bright()
        "Dark" -> dark()
        "Contrast" -> contrast()
        "Fade" -> fade()
        "Sepia" -> sepia()
        "Invert" -> invert()
        "Saturate" -> saturate()
        "Desaturate" -> desaturate()
        "Red" -> redBoost()
        "Green" -> greenBoost()
        "Blue" -> blueBoost()
        "Dramatic" -> dramatic()
        "Matte" -> matte()
        "CartoonAI" -> cartoonAI()
        "Barbie" -> barbiePink()
        else -> normal()
    }
}
