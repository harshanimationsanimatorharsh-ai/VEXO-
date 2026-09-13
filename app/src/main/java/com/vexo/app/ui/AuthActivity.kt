package com.vexo.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.R

class AuthActivity : AppCompatActivity() {

    private var isSignIn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val tvTitle      = findViewById<TextView>(R.id.tvAuthTitle)
        val etEmail      = findViewById<EditText>(R.id.etEmail)
        val etPassword   = findViewById<EditText>(R.id.etPassword)
        val etName       = findViewById<EditText>(R.id.etName)
        val btnAuth      = findViewById<Button>(R.id.btnAuth)
        val tvToggle     = findViewById<TextView>(R.id.tvToggleAuth)
        val btnGoogle    = findViewById<Button>(R.id.btnGoogle)
        val btnPhone     = findViewById<Button>(R.id.btnPhone)

        fun updateUI() {
            tvTitle?.text  = if (isSignIn) "Sign In" else "Sign Up"
            btnAuth?.text  = if (isSignIn) "Sign In" else "Sign Up"
            tvToggle?.text = if (isSignIn) "Don't have an account? Sign Up" else "Already have an account? Sign In"
            etName?.visibility = if (isSignIn) android.view.View.GONE else android.view.View.VISIBLE
        }
        updateUI()

        tvToggle?.setOnClickListener {
            isSignIn = !isSignIn
            updateUI()
        }

        btnAuth?.setOnClickListener {
            val email = etEmail?.text.toString().trim()
            val pass  = etPassword?.text.toString().trim()
            val name  = etName?.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isSignIn && name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("vexo_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_email", email)
                .putString("user_name", if (isSignIn) email.substringBefore("@") else name)
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnGoogle?.setOnClickListener {
            val prefs = getSharedPreferences("vexo_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_email", "user@gmail.com")
                .putString("user_name", "Google User")
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnPhone?.setOnClickListener {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }
    }
}
