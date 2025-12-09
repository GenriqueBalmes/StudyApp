package com.mobcomp.studyx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId("activity_login"))

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<TextInputEditText>(getViewId("etEmail"))
        val etPassword = findViewById<TextInputEditText>(getViewId("etPassword"))
        val btnLogin = findViewById<Button>(getViewId("btnLogin"))
        val tvRegisterLink = findViewById<TextView>(getViewId("tvRegisterLink"))

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInput(email, password, etEmail, etPassword)) {
                loginUser(email, password, btnLogin)
            }
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(
        email: String,
        password: String,
        etEmail: TextInputEditText,
        etPassword: TextInputEditText
    ): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String, btnLogin: Button) {
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainNavActivity::class.java))  // ← CHANGED!
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun getLayoutId(name: String): Int {
        return resources.getIdentifier(name, "layout", packageName)
    }

    private fun getViewId(name: String): Int {
        return resources.getIdentifier(name, "id", packageName)
    }
}