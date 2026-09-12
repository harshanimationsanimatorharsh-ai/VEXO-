package com.vexo.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.vexo.app.R
import com.vexo.app.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private var isSignIn = true
    private val RC_GOOGLE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupGoogle()
        setupClicks()
        updateMode()
    }

    private fun setupGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClicks() {
        binding.btnGoogle.setOnClickListener {
            startActivityForResult(googleClient.signInIntent, RC_GOOGLE)
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
            loading(true)
            if (isSignIn) signIn(email, pass) else signUp(email, pass)
        }

        binding.tvToggle.setOnClickListener {
            isSignIn = !isSignIn
            updateMode()
        }

        binding.tvForgot.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Reset email sent!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnPhone.setOnClickListener {
            startActivity(Intent(this, PhoneAuthActivity::class.java))
        }
    }

    private fun signIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { loading(false); goMain() }
            .addOnFailureListener { loading(false)
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
    }

    private fun signUp(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { loading(false); goMain() }
            .addOnFailureListener { loading(false)
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                loading(true)
                val cred = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(cred)
                    .addOnSuccessListener { loading(false); goMain() }
                    .addOnFailureListener { loading(false)
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
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

    private fun loading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnEmailAction.isEnabled = !show
        binding.btnGoogle.isEnabled = !show
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }
}
