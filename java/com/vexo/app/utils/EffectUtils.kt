package com.vexo.app.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

object EffectUtils {

    // ─── 20 EFFECTS ───
    fun applyEffect(view: View, effectName: String) {
        when (effectName) {
            "Shake" -> shake(view)
            "Pulse" -> pulse(view)
            "Flash" -> flash(view)
            "Bounce" -> bounce(view)
            "Spin" -> spin(view)
            "Flip" -> flip(view)
            "Zoom" -> zoom(view)
            "Slide Left" -> slideLeft(view)
            "Slide Right" -> slideRight(view)
            "Slide Up" -> slideUp(view)
            "Slide Down" -> slideDown(view)
            "Fade In" -> fadeIn(view)
            "Fade Out" -> fadeOut(view)
            "Rubber" -> rubber(view)
            "Swing" -> swing(view)
            "Wobble" -> wobble(view)
            "Tada" -> tada(view)
            "Jello" -> jello(view)
            "HeartBeat" -> heartBeat(view)
            "Roll" -> roll(view)
        }
    }

    fun getEffectNames(): List<String> = listOf(
        "Shake", "Pulse", "Flash", "Bounce", "Spin",
        "Flip", "Zoom", "Slide Left", "Slide Right", "Slide Up",
        "Slide Down", "Fade In", "Fade Out", "Rubber", "Swing",
        "Wobble", "Tada", "Jello", "HeartBeat", "Roll"
    )

    private fun shake(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationX", 0f, 20f, -20f, 10f, -10f, 0f)
        anim.duration = 500
        anim.start()
    }

    private fun pulse(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        scaleX.duration = 500
        scaleY.duration = 500
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
    }

    private fun flash(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f, 0f, 1f)
        anim.duration = 600
        anim.start()
    }

    private fun bounce(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f, -15f, 0f)
        anim.duration = 600
        anim.start()
    }

    private fun spin(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        anim.duration = 600
        anim.start()
    }

    private fun flip(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "rotationY", 0f, 180f, 0f)
        anim.duration = 600
        anim.start()
    }

    private fun zoom(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.5f, 1f)
        scaleX.duration = 500
        scaleY.duration = 500
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
    }

    private fun slideLeft(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationX", 200f, 0f)
        anim.duration = 500
        anim.start()
    }

    private fun slideRight(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationX", -200f, 0f)
        anim.duration = 500
        anim.start()
    }

    private fun slideUp(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationY", 200f, 0f)
        anim.duration = 500
        anim.start()
    }

    private fun slideDown(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationY", -200f, 0f)
        anim.duration = 500
        anim.start()
    }

    private fun fadeIn(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        anim.duration = 600
        anim.start()
    }

    private fun fadeOut(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        anim.duration = 600
        anim.start()
    }

    private fun rubber(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 0.8f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f, 1.3f, 0.9f, 1f)
        scaleX.duration = 600
        scaleY.duration = 600
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
    }

    private fun swing(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "rotation", 0f, 15f, -10f, 5f, -5f, 0f)
        anim.duration = 600
        anim.start()
    }

    private fun wobble(view: View) {
        val transX = ObjectAnimator.ofFloat(view, "translationX", 0f, -20f, 15f, -10f, 5f, 0f)
        val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, -5f, 3f, -3f, 2f, 0f)
        transX.duration = 600
        rot.duration = 600
        val set = AnimatorSet()
        set.playTogether(transX, rot)
        set.start()
    }

    private fun tada(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1.1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1.1f, 1.1f, 1f)
        val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, -3f, 3f, -3f, 0f)
        scaleX.duration = 600
        scaleY.duration = 600
        rot.duration = 600
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY, rot)
        set.start()
    }

    private fun jello(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 0.9f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1.2f, 0.95f, 1f)
        scaleX.duration = 700
        scaleY.duration = 700
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
    }

    private fun heartBeat(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 1f, 1.3f, 1f)
        scaleX.duration = 700
        scaleY.duration = 700
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
    }

    private fun roll(view: View) {
        val transX = ObjectAnimator.ofFloat(view, "translationX", -200f, 0f)
        val rot = ObjectAnimator.ofFloat(view, "rotation", -120f, 0f)
        transX.duration = 600
        rot.duration = 600
        val set = AnimatorSet()
        set.playTogether(transX, rot)
        set.start()
    }

    // ─── ANIMATION NAMES ───
    fun getAnimationNames(): List<String> = listOf(
        "FadeIn", "FadeOut", "SlideUp", "SlideDown", "SlideLeft",
        "SlideRight", "ZoomIn", "ZoomOut", "RotateIn", "RotateOut",
        "BounceIn", "FlipX", "FlipY", "RollIn", "LightIn",
        "LightOut", "SwingIn", "DropIn", "Spiral", "Unfold"
    )

    // ─── VIDEO EFFECT NAMES ───
    fun getVideoEffectNames(): List<String> = listOf(
        "Glitch", "VHS", "Neon", "Matrix", "Blur",
        "Sharpen", "Vignette", "Grain", "Pixelate", "Mirror",
        "Fisheye", "Tilt-Shift", "Chromatic", "Scanlines", "Hologram",
        "Duotone", "Posterize", "Solarize", "Crosshatch", "Emboss"
    )
}
