package com.vexo.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isSignIn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateMode()
        setupClicks()
    }

    private fun setupClicks() {

        binding.btnGoogle.setOnClickListener {
            // Save login state
            saveLogin("Google User", "google@vexo.app")
            goMain()
        }

        binding.btnEmailAction.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter valid email"
                return@setOnClickListener
            }
            if (pass.length < 6) {
                binding.etPassword.error = "Min 6 characters"
                return@setOnClickListener
            }

            saveLogin(email.substringBefore("@"), email)
            Toast.makeText(this,
                if (isSignIn) "Welcome back!" else "Account created!",
                Toast.LENGTH_SHORT).show()
            goMain()
        }

        binding.tvToggle.setOnClickListener {
            isSignIn = !isSignIn
            updateMode()
        }

        binding.tvForgot.setOnClickListener {
            Toast.makeText(this,
                "Reset link sent to your email!",
                Toast.LENGTH_LONG).show()
        }

        binding.btnPhone.setOnClickListener {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }
    }

    private fun updateMode() {
        if (isSignIn) {
            binding.tvTitle.text = "Welcome Back"
            binding.btnEmailAction.text = "Sign In"
            binding.tvToggle.text = "Don't have an account? Sign Up"
            binding.tvForgot.visibility = View.VISIBLE
        } else {
            binding.tvTitle.text = "Create Account"
            binding.btnEmailAction.text = "Sign Up"
            binding.tvToggle.text = "Already have an account? Sign In"
            binding.tvForgot.visibility = View.GONE
        }
    }

    private fun saveLogin(name: String, email: String) {
        getSharedPreferences("vexo_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", true)
            .putString("user_name", name)
            .putString("user_email", email)
            .apply()
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }
}
