package com.vexo.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.ivLogo)
        val tagline = findViewById<TextView>(R.id.tvTagline)
        val credit = findViewById<TextView>(R.id.tvCredit)

        logo?.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
        tagline?.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
        credit?.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("vexo_prefs", MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            if (isLoggedIn) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, AuthActivity::class.java))
            }
            finish()
        }, 2500)
    }
}
