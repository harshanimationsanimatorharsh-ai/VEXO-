package com.vexo.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.vexo.app.databinding.ActivityPhoneAuthBinding
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.ivBack.setOnClickListener { finish() }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.length < 10) {
                binding.etPhone.error = "Enter valid number"
                return@setOnClickListener
            }
            sendOtp("+91$phone")
        }

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6) {
                binding.etOtp.error = "Enter 6 digit OTP"
                return@setOnClickListener
            }
            verify(otp)
        }
    }

    private fun sendOtp(phone: String) {
        loading(true)
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(c: PhoneAuthCredential) {
                    signIn(c)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    loading(false)
                    Toast.makeText(this@PhoneAuthActivity,
                        e.message, Toast.LENGTH_SHORT).show()
                }
                override fun onCodeSent(id: String,
                    token: PhoneAuthProvider.ForceResendingToken) {
                    loading(false)
                    verificationId = id
                    binding.layoutOtp.visibility = View.VISIBLE
                    Toast.makeText(this@PhoneAuthActivity,
                        "OTP Sent!", Toast.LENGTH_SHORT).show()
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verify(otp: String) {
        loading(true)
        signIn(PhoneAuthProvider.getCredential(verificationId, otp))
    }

    private fun signIn(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                loading(false)
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener {
                loading(false)
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
