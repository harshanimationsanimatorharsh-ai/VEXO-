package com.vexo.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vexo.app.databinding.ActivityPhoneAuthBinding

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener { finish() }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.length < 10) {
                binding.etPhone.error = "Enter valid number"
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            // Simulate OTP sent
            Handler(Looper.getMainLooper()).postDelayed({
                binding.progressBar.visibility = View.GONE
                binding.layoutOtp.visibility = View.VISIBLE
                Toast.makeText(this, "OTP Sent!", Toast.LENGTH_SHORT).show()
            }, 1500)
        }

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6) {
                binding.etOtp.error = "Enter 6 digit OTP"
                return@setOnClickListener
            }
            binding.progressBar.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                binding.progressBar.visibility = View.GONE
                // Save login
                getSharedPreferences("vexo_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_name", "VEXO User")
                    .putString("user_email",
                        binding.etPhone.text.toString())
                    .apply()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }, 1500)
        }
    }
}
