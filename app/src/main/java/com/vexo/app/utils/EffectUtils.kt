package com.vexo.app.utils

import android.animation.*
import android.view.View

object EffectUtils {

    // ─────────────────────────────────────────────
    //  20 EFFECTS  (applied as View animations)
    // ─────────────────────────────────────────────

    fun applyGlow(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun applyMotionBlur(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 6f, 0f).apply {
            duration = 100; repeatCount = 5
        }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun applyZoom(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 600; repeatCount = ValueAnimator.INFINITE
        }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 600; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun apply3DZoom(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.25f, 1f).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE
        }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.25f, 1f).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE
        }
        val rX = ObjectAnimator.ofFloat(view, "rotationX", 0f, 8f, 0f).apply {
            duration = 800; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(sx, sy, rX); it.start() }
    }

    fun applyShake(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -12f, 12f, -8f, 8f, 0f).apply {
            duration = 500; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun applyFlash(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f).apply {
            duration = 300; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun applyGlitch(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 8f, -8f, 0f).apply {
            duration = 150; repeatCount = ValueAnimator.INFINITE
        }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f).apply {
            duration = 150; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(tx, alpha); it.start() }
    }

    fun applyRGBSplit(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 5f, -5f, 0f).apply {
            duration = 200; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun applyChromatic(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, 3f, -3f, 0f).apply {
            duration = 250; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(ty); it.start() }
    }

    fun applyLensFlare(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 1f).apply {
            duration = 1200; repeatCount = ValueAnimator.INFINITE
        }
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f).apply {
            duration = 1200; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(alpha, sx); it.start() }
    }

    fun applyLightLeak(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.8f, 1f).apply {
            duration = 1500; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun applyFilmGrain(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.92f, 1f).apply {
            duration = 80; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun applyVignette(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.85f, 1f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun applyBlur(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.02f, 1f).apply {
            duration = 400; repeatCount = ValueAnimator.INFINITE
        }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.02f, 1f).apply {
            duration = 400; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun applyPixelate(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.9f, 1f).apply {
            duration = 300; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun applyNoise(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 2f, -2f, 0f).apply {
            duration = 100; repeatCount = ValueAnimator.INFINITE
        }
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, 2f, -2f, 0f).apply {
            duration = 100; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(tx, ty); it.start() }
    }

    fun applySmoke(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -15f, 0f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE
        }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 1f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(ty, alpha); it.start() }
    }

    fun applyFire(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -8f, 0f).apply {
            duration = 300; repeatCount = ValueAnimator.INFINITE
        }
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.03f, 0.97f, 1f).apply {
            duration = 300; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(ty, sx); it.start() }
    }

    fun applySpark(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.3f, 1f).apply {
            duration = 200; repeatCount = ValueAnimator.INFINITE
        }
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f).apply {
            duration = 200; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(alpha, sx); it.start() }
    }

    fun applyAura(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE
        }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE
        }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.playTogether(sx, sy, alpha); it.start() }
    }

    // ─────────────────────────────────────────────
    //  20 ANIMATIONS
    // ─────────────────────────────────────────────

    fun animFadeIn(view: View): AnimatorSet {
        view.alpha = 0f
        val a = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply { duration = 600 }
        return AnimatorSet().also { it.play(a); it.start() }
    }

    fun animFadeOut(view: View): AnimatorSet {
        val a = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply { duration = 600 }
        return AnimatorSet().also { it.play(a); it.start() }
    }

    fun animZoomIn(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).apply { duration = 500 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f).apply { duration = 500 }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun animZoomOut(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f).apply { duration = 500 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f).apply { duration = 500 }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun animPopUp(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1.2f, 1f).apply { duration = 400 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.2f, 1f).apply { duration = 400 }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun animBounce(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f, -15f, 0f).apply {
            duration = 600
        }
        return AnimatorSet().also { it.play(ty); it.start() }
    }

    fun animSlideLeft(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 300f, 0f).apply { duration = 400 }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun animSlideRight(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", -300f, 0f).apply { duration = 400 }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun animSlideUp(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 300f, 0f).apply { duration = 400 }
        return AnimatorSet().also { it.play(ty); it.start() }
    }

    fun animSlideDown(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", -300f, 0f).apply { duration = 400 }
        return AnimatorSet().also { it.play(ty); it.start() }
    }

    fun animSpin(view: View): AnimatorSet {
        val r = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply { duration = 600 }
        return AnimatorSet().also { it.play(r); it.start() }
    }

    fun animSwing(view: View): AnimatorSet {
        val r = ObjectAnimator.ofFloat(view, "rotation", -20f, 20f, -10f, 10f, 0f).apply {
            duration = 700
        }
        return AnimatorSet().also { it.play(r); it.start() }
    }

    fun animShake(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -10f, 10f, -10f, 0f).apply {
            duration = 400
        }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun animWobble(view: View): AnimatorSet {
        val r = ObjectAnimator.ofFloat(view, "rotation", 0f, -5f, 5f, -3f, 3f, 0f).apply {
            duration = 600
        }
        return AnimatorSet().also { it.play(r); it.start() }
    }

    fun animPulse(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 500; repeatCount = 2
        }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 500; repeatCount = 2
        }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun animFloat(view: View): AnimatorSet {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -10f, 0f).apply {
            duration = 1500; repeatCount = ValueAnimator.INFINITE
        }
        return AnimatorSet().also { it.play(ty); it.start() }
    }

    fun animTypewriter(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply { duration = 800 }
        val tx = ObjectAnimator.ofFloat(view, "translationX", -20f, 0f).apply { duration = 800 }
        return AnimatorSet().also { it.playTogether(alpha, tx); it.start() }
    }

    fun animElastic(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1.3f, 0.9f, 1.1f, 1f).apply {
            duration = 700
        }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1.3f, 0.9f, 1.1f, 1f).apply {
            duration = 700
        }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun animFlip(view: View): AnimatorSet {
        val rY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 180f).apply { duration = 500 }
        return AnimatorSet().also { it.play(rY); it.start() }
    }

    fun anim3DRotate(view: View): AnimatorSet {
        val rX = ObjectAnimator.ofFloat(view, "rotationX", 0f, 360f).apply { duration = 700 }
        val rY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f).apply { duration = 700 }
        return AnimatorSet().also { it.playTogether(rX, rY); it.start() }
    }

    // ─────────────────────────────────────────────
    //  20 VIDEO EFFECTS
    // ─────────────────────────────────────────────

    fun videoVelocity(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 20f, 0f).apply { duration = 300 }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun videoSlowMo(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.8f, 1f).apply { duration = 2000 }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun videoSpeedRamp(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply { duration = 800 }
        return AnimatorSet().also { it.play(sx); it.start() }
    }

    fun videoBeatShake(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -15f, 15f, 0f).apply {
            duration = 200; repeatCount = 3
        }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun videoFlashBeat(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f, 1f).apply {
            duration = 150; repeatCount = 3
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun videoGlitchTransition(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 12f, -12f, 0f).apply {
            duration = 120; repeatCount = 4
        }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f).apply {
            duration = 120; repeatCount = 4
        }
        return AnimatorSet().also { it.playTogether(tx, alpha); it.start() }
    }

    fun videoRGBGlitch(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 8f, -8f, 0f).apply {
            duration = 100; repeatCount = 5
        }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun videoMotionTrail(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.4f, 1f).apply { duration = 600 }
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, 0f).apply { duration = 600 }
        return AnimatorSet().also { it.playTogether(alpha, tx); it.start() }
    }

    fun videoCameraShake(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -8f, 8f, -5f, 5f, 0f).apply {
            duration = 400; repeatCount = 3
        }
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -5f, 5f, 0f).apply {
            duration = 400; repeatCount = 3
        }
        return AnimatorSet().also { it.playTogether(tx, ty); it.start() }
    }

    fun videoDynamicZoom(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f).apply { duration = 800 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f).apply { duration = 800 }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun videoSpinTransition(view: View): AnimatorSet {
        val r = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply { duration = 600 }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply { duration = 600 }
        return AnimatorSet().also { it.playTogether(r, alpha); it.start() }
    }

    fun videoWhipPan(view: View): AnimatorSet {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 300f).apply { duration = 200 }
        return AnimatorSet().also { it.play(tx); it.start() }
    }

    fun videoLightSweep(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.7f, 1f, 0.7f).apply { duration = 800 }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun videoLensFlare(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f).apply { duration = 900 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f).apply { duration = 900 }
        return AnimatorSet().also { it.playTogether(sx, sy); it.start() }
    }

    fun videoFilmBurn(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.3f, 1f).apply { duration = 700 }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun videoFlashback(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply { duration = 1000 }
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f).apply { duration = 1000 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f).apply { duration = 1000 }
        return AnimatorSet().also { it.playTogether(alpha, sx, sy); it.start() }
    }

    fun videoFreezeFrame(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.95f, 1f).apply {
            duration = 100; repeatCount = 10
        }
        return AnimatorSet().also { it.play(alpha); it.start() }
    }

    fun videoEchoTrail(view: View): AnimatorSet {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f).apply {
            duration = 400; repeatCount = 3
        }
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 10f, 0f).apply {
            duration = 400; repeatCount = 3
        }
        return AnimatorSet().also { it.playTogether(alpha, tx); it.start() }
    }

    fun videoBlurTransition(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f).apply { duration = 600 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f).apply { duration = 600 }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 1f).apply { duration = 600 }
        return AnimatorSet().also { it.playTogether(sx, sy, alpha); it.start() }
    }

    fun videoParticleBurst(view: View): AnimatorSet {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.5f, 0f).apply { duration = 500 }
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.5f, 0f).apply { duration = 500 }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply { duration = 500 }
        return AnimatorSet().also { it.playTogether(sx, sy, alpha); it.start() }
    }
}
