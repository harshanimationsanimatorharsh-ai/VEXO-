package com.vexo.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.R

class PhoneAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)

        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnVerify = findViewById<Button>(R.id.btnVerify)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        btnVerify.setOnClickListener {
            val phone = etPhone.text.toString().trim()

            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("vexo_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_name", "User")
                .putString("user_email", "$phone@vexo.app")
                .apply()

            Toast.makeText(this, "Welcome to VEXO!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
