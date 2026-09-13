package com.vexo.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.R

class PhoneAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)

        val etPhone   = findViewById<EditText>(R.id.etPhone)
        val etOtp     = findViewById<EditText>(R.id.etOtp)
        val btnSendOtp= findViewById<Button>(R.id.btnSendOtp)
        val btnVerify = findViewById<Button>(R.id.btnVerifyOtp)
        val btnBack   = findViewById<ImageButton>(R.id.btnBack)

        btnBack?.setOnClickListener { finish() }

        btnSendOtp?.setOnClickListener {
            val phone = etPhone?.text.toString().trim()
            if (phone.length < 10) {
                Toast.makeText(this, "Enter valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "OTP sent to $phone (demo: 123456)", Toast.LENGTH_LONG).show()
        }

        btnVerify?.setOnClickListener {
            val otp = etOtp?.text.toString().trim()
            if (otp == "123456") {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    val prefs = getSharedPreferences("vexo_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("user_name", "Phone User")
                        .putString("user_email", "${etPhone?.text}@phone.vexo")
                        .apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }, 800)
            } else {
                Toast.makeText(this, "Wrong OTP! Use 123456", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
