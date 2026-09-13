package com.vexo.app.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.animation.*

object EffectUtils {

    // ─── EFFECTS ───────────────────────────────────────────

    fun applyGlow(view: View) {
        view.animate()
            .scaleX(1.02f).scaleY(1.02f)
            .setDuration(800)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(800).start()
            }.start()
    }

    fun applyMotionBlur(view: View) {
        val blur = ObjectAnimator.ofFloat(view, "translationX", 0f, 8f, -8f, 4f, -4f, 0f)
        blur.duration = 300
        blur.repeatCount = 2
        blur.start()
    }

    fun applyZoom(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 600
            start()
        }
    }

    fun apply3DZoom(view: View) {
        val rotY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 15f, -15f, 0f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        AnimatorSet().apply {
            playTogether(rotY, scaleX, scaleY)
            duration = 800
            start()
        }
    }

    fun applyShake(view: View) {
        val shake = ObjectAnimator.ofFloat(
            view, "translationX",
            0f, -20f, 20f, -15f, 15f, -10f, 10f, -5f, 5f, 0f
        )
        shake.duration = 600
        shake.start()
    }

    fun applyFlash(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.WHITE)
        overlayView.alpha = 0.8f
        overlayView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                overlayView.visibility = View.GONE
            }.start()
    }

    fun applyGlitch(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, -15f, 8f, -8f, 0f)
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, 5f, -5f, 3f, -3f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f, 0.8f, 1f)
        AnimatorSet().apply {
            playTogether(tx, ty, alpha)
            duration = 400
            start()
        }
    }

    fun applyRGBSplit(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 8f, -8f, 0f)
        val rotZ = ObjectAnimator.ofFloat(view, "rotation", 0f, 1f, -1f, 0f)
        AnimatorSet().apply {
            playTogether(tx, rotZ)
            duration = 300
            repeatCount = 3
            start()
        }
    }

    fun applyChromaticAberration(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 5f, -5f, 0f)
        tx.duration = 200
        tx.repeatCount = 4
        tx.start()
    }

    fun applyLensFlare(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#30FFFFFF"))
        overlayView.alpha = 0f
        overlayView.animate()
            .alpha(0.6f).setDuration(150)
            .withEndAction {
                overlayView.animate().alpha(0f).setDuration(300)
                    .withEndAction { overlayView.visibility = View.GONE }.start()
            }.start()
    }

    fun applyLightLeak(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#40FF8800"))
        overlayView.alpha = 0f
        overlayView.animate()
            .alpha(0.5f).setDuration(500)
            .withEndAction {
                overlayView.animate().alpha(0f).setDuration(800)
                    .withEndAction { overlayView.visibility = View.GONE }.start()
            }.start()
    }

    fun applyFilmGrain(view: View) {
        val anim = ValueAnimator.ofFloat(0.95f, 1.0f)
        anim.duration = 100
        anim.repeatCount = 10
        anim.repeatMode = ValueAnimator.REVERSE
        anim.addUpdateListener { view.alpha = it.animatedValue as Float }
        anim.start()
    }

    fun applyVignette(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#60000000"))
        overlayView.alpha = 0f
        overlayView.animate().alpha(1f).setDuration(300).start()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            overlayView.animate().alpha(0f).setDuration(500)
                .withEndAction { overlayView.visibility = View.GONE }.start()
        }, 2000)
    }

    fun applyBlur(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 0.95f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.85f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, alpha)
            duration = 500
            start()
        }
    }

    fun applyPixelate(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.1f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun applyNoise(view: View) {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 0.9f, 0.75f, 1f)
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 2f, -2f, 1f, -1f, 0f)
        AnimatorSet().apply {
            playTogether(alpha, tx)
            duration = 300
            repeatCount = 3
            start()
        }
    }

    fun applySmoke(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#50AAAAAA"))
        overlayView.alpha = 0f
        overlayView.animate().alpha(0.4f).setDuration(1000)
            .withEndAction {
                overlayView.animate().alpha(0f).setDuration(1500)
                    .withEndAction { overlayView.visibility = View.GONE }.start()
            }.start()
    }

    fun applyFire(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#60FF4500"))
        val anim = ValueAnimator.ofFloat(0f, 0.5f, 0.3f, 0.5f, 0f)
        anim.duration = 1000
        anim.addUpdateListener { overlayView.alpha = it.animatedValue as Float }
        anim.withEndAction { overlayView.visibility = View.GONE }
        anim.start()
    }

    fun applySpark(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 0.95f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 0.95f, 1.05f, 1f)
        val rotZ = ObjectAnimator.ofFloat(view, "rotation", 0f, 3f, -3f, 1f, 0f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, rotZ)
            duration = 600
            start()
        }
    }

    fun applyAura(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.85f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    // ─── ANIMATIONS ────────────────────────────────────────

    fun animFadeIn(view: View) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate().alpha(1f).setDuration(600).start()
    }

    fun animFadeOut(view: View) {
        view.animate().alpha(0f).setDuration(600)
            .withEndAction { view.visibility = View.GONE }.start()
    }

    fun animZoomIn(view: View) {
        view.scaleX = 0f; view.scaleY = 0f
        view.visibility = View.VISIBLE
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }

    fun animZoomOut(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 500
            withEndAction { view.visibility = View.GONE }
            start()
        }
    }

    fun animPopUp(view: View) {
        view.scaleX = 0.5f; view.scaleY = 0.5f; view.alpha = 0f
        view.visibility = View.VISIBLE
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1.1f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.1f, 1f)
        val a = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy, a)
            duration = 400
            start()
        }
    }

    fun animBounce(view: View) {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f)
        ty.duration = 600
        ty.interpolator = BounceInterpolator()
        ty.repeatCount = 2
        ty.start()
    }

    fun animSlideLeft(view: View) {
        view.translationX = 500f; view.visibility = View.VISIBLE
        view.animate().translationX(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    fun animSlideRight(view: View) {
        view.translationX = -500f; view.visibility = View.VISIBLE
        view.animate().translationX(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    fun animSlideUp(view: View) {
        view.translationY = 300f; view.visibility = View.VISIBLE
        view.animate().translationY(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    fun animSlideDown(view: View) {
        view.translationY = -300f; view.visibility = View.VISIBLE
        view.animate().translationY(0f).setDuration(400)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    fun animSpin(view: View) {
        val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        rot.duration = 800
        rot.interpolator = LinearInterpolator()
        rot.start()
    }

    fun animSwing(view: View) {
        val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, 20f, -20f, 15f, -15f, 10f, -10f, 0f)
        rot.duration = 800
        rot.start()
    }

    fun animShake(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 20f, -20f, 15f, -15f, 0f)
        tx.duration = 600
        tx.start()
    }

    fun animWobble(view: View) {
        val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, 10f, -8f, 6f, -4f, 2f, -1f, 0f)
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 0.95f, 1.05f, 1f)
        AnimatorSet().apply {
            playTogether(rot, sx)
            duration = 700
            start()
        }
    }

    fun animPulse(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 500
            repeatCount = 3
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    fun animFloat(view: View) {
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -15f, 0f)
        ty.duration = 1200
        ty.repeatCount = ValueAnimator.INFINITE
        ty.repeatMode = ValueAnimator.REVERSE
        ty.interpolator = AccelerateDecelerateInterpolator()
        ty.start()
    }

    fun animElastic(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.4f, 0.8f, 1.2f, 0.9f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f, 1.4f, 0.9f, 1.1f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 700
            start()
        }
    }

    fun animFlip(view: View) {
        val rotY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f)
        rotY.duration = 200
        rotY.withEndAction {
            view.scaleX = -1f
            val rotY2 = ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f)
            rotY2.duration = 200
            rotY2.start()
        }
        rotY.start()
    }

    fun anim3DRotate(view: View) {
        val rotX = ObjectAnimator.ofFloat(view, "rotationX", 0f, 360f)
        val rotY = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f)
        AnimatorSet().apply {
            playTogether(rotX, rotY)
            duration = 1000
            start()
        }
    }

    // ─── VIDEO EFFECTS ─────────────────────────────────────

    fun applyVelocity(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -30f, 30f, -20f, 20f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.8f, 1f)
        AnimatorSet().apply {
            playTogether(tx, alpha)
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun applyBeatShake(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 10f, -10f, 8f, -8f, 0f)
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, 5f, -5f, 3f, -3f, 0f)
        AnimatorSet().apply {
            playTogether(tx, ty)
            duration = 300
            repeatCount = 2
            start()
        }
    }

    fun applyFlashBeat(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.WHITE)
        val anim = ValueAnimator.ofFloat(0f, 0.7f, 0f, 0.5f, 0f)
        anim.duration = 400
        anim.addUpdateListener { overlayView.alpha = it.animatedValue as Float }
        anim.withEndAction { overlayView.visibility = View.GONE }
        anim.start()
    }

    fun applyGlitchTransition(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 20f, -20f, 15f, -15f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f, 0.7f, 1f)
        val rotZ = ObjectAnimator.ofFloat(view, "rotation", 0f, 2f, -2f, 1f, 0f)
        AnimatorSet().apply {
            playTogether(tx, alpha, rotZ)
            duration = 500
            start()
        }
    }

    fun applyRGBGlitch(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 10f, -10f, 5f, -5f, 0f)
        val rotZ = ObjectAnimator.ofFloat(view, "rotation", 0f, 3f, -3f, 1f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 0.9f, 0.7f, 1f)
        AnimatorSet().apply {
            playTogether(tx, rotZ, alpha)
            duration = 400
            repeatCount = 2
            start()
        }
    }

    fun applyMotionTrail(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 30f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 1f)
        AnimatorSet().apply {
            playTogether(tx, alpha)
            duration = 600
            start()
        }
    }

    fun applyCameraShake(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -15f, 15f, -12f, 12f, -8f, 8f, 0f)
        val ty = ObjectAnimator.ofFloat(view, "translationY", 0f, -8f, 8f, -6f, 6f, -4f, 4f, 0f)
        AnimatorSet().apply {
            playTogether(tx, ty)
            duration = 500
            start()
        }
    }

    fun applyDynamicZoom(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun applySpinTransition(view: View) {
        val rotZ = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.5f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.5f, 1f)
        AnimatorSet().apply {
            playTogether(rotZ, sx, sy)
            duration = 700
            start()
        }
    }

    fun applyWhipPan(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -200f, 0f)
        tx.duration = 300
        tx.interpolator = AccelerateDecelerateInterpolator()
        tx.start()
    }

    fun applyLightSweep(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#40FFFFFF"))
        val tx = ObjectAnimator.ofFloat(overlayView, "translationX", -500f, 500f)
        val alpha = ObjectAnimator.ofFloat(overlayView, "alpha", 0f, 0.5f, 0f)
        AnimatorSet().apply {
            playTogether(tx, alpha)
            duration = 800
            withEndAction { overlayView.visibility = View.GONE }
            start()
        }
    }

    fun applyFilmBurn(overlayView: View) {
        overlayView.visibility = View.VISIBLE
        overlayView.setBackgroundColor(Color.parseColor("#80FF6600"))
        val anim = ValueAnimator.ofFloat(0f, 0.8f, 0.4f, 0.9f, 0f)
        anim.duration = 600
        anim.addUpdateListener { overlayView.alpha = it.animatedValue as Float }
        anim.withEndAction { overlayView.visibility = View.GONE }
        anim.start()
    }

    fun applyFlashback(view: View) {
        val anim = ValueAnimator.ofFloat(1f, 0f, 1f, 0f, 1f)
        anim.duration = 600
        anim.addUpdateListener { view.alpha = it.animatedValue as Float }
        anim.start()
    }

    fun applyEcho(view: View) {
        val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, 0f, 10f, 0f, 5f, 0f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 0.9f, 0.8f, 1f)
        AnimatorSet().apply {
            playTogether(tx, alpha)
            duration = 700
            start()
        }
    }

    fun applyBlurTransition(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f)
        AnimatorSet().apply {
            playTogether(sx, sy, alpha)
            duration = 500
            start()
        }
    }

    fun applyParticleBurst(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 0.8f, 1.1f, 1f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 0.8f, 1.1f, 1f)
        val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, 5f, -5f, 2f, 0f)
        AnimatorSet().apply {
            playTogether(sx, sy, rot)
            duration = 600
            start()
        }
    }

    // Helper extension
    private fun ValueAnimator.withEndAction(action: () -> Unit): ValueAnimator {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) { action() }
        })
        return this
    }

    private fun AnimatorSet.withEndAction(action: () -> Unit): AnimatorSet {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) { action() }
        })
        return this
    }

    private fun ObjectAnimator.withEndAction(action: () -> Unit): ObjectAnimator {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) { action() }
        })
        return this
    }
}
