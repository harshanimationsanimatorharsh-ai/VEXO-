package com.vexo.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginAndNavigate()
        }, 3000)
    }

    private fun startAnimations() {
        val scale = ScaleAnimation(
            0.3f, 1f, 0.3f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply { duration = 900 }

        val fade = AlphaAnimation(0f, 1f).apply {
            duration = 900
        }

        val logoAnim = AnimationSet(true).apply {
            addAnimation(scale)
            addAnimation(fade)
        }
        binding.ivLogo.startAnimation(logoAnim)

        val textFade = AlphaAnimation(0f, 1f).apply {
            duration = 800
            startOffset = 700
        }
        binding.tvAppName.startAnimation(textFade)
        binding.tvTagline.startAnimation(textFade)
        binding.tvMadeBy.startAnimation(textFade)
    }

    private fun checkLoginAndNavigate() {
        val prefs = getSharedPreferences("vexo_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, AuthActivity::class.java))
        }
        finish()
    }
}
