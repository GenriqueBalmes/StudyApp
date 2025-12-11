package com.mobcomp.studyx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId("activity_register"))

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etFullName = findViewById<TextInputEditText>(getViewId("etFullName"))
        val etEmail = findViewById<TextInputEditText>(getViewId("etEmail"))
        val etStudentLevel = findViewById<TextInputEditText>(getViewId("etStudentLevel"))
        val etPassword = findViewById<TextInputEditText>(getViewId("etPassword"))
        val etConfirmPassword = findViewById<TextInputEditText>(getViewId("etConfirmPassword"))
        val btnRegister = findViewById<Button>(getViewId("btnRegister"))
        val tvLoginLink = findViewById<TextView>(getViewId("tvLoginLink"))

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val studentLevel = etStudentLevel.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInput(
                    fullName, email, studentLevel, password, confirmPassword,
                    etFullName, etEmail, etStudentLevel, etPassword, etConfirmPassword
                )) {
                registerUser(fullName, email, studentLevel, password, btnRegister)
            }
        }

        tvLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInput(
        fullName: String,
        email: String,
        studentLevel: String,
        password: String,
        confirmPassword: String,
        etFullName: TextInputEditText,
        etEmail: TextInputEditText,
        etStudentLevel: TextInputEditText,
        etPassword: TextInputEditText,
        etConfirmPassword: TextInputEditText
    ): Boolean {
        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            etFullName.requestFocus()
            return false
        }

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

        if (studentLevel.isEmpty()) {
            etStudentLevel.error = "Student level is required"
            etStudentLevel.requestFocus()
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

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun registerUser(
        fullName: String,
        email: String,
        studentLevel: String,
        password: String,
        btnRegister: Button
    ) {
        btnRegister.isEnabled = false
        btnRegister.text = "Registering..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    val userData = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "studentLevel" to studentLevel,
                        "createdAt" to System.currentTimeMillis()
                    )

                    userId?.let {
                        db.collection("users").document(it)
                            .set(userData)
                            .addOnSuccessListener {
                                btnRegister.isEnabled = true
                                btnRegister.text = "Register"
                                Toast.makeText(
                                    this,
                                    "Registration successful! Please log in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                btnRegister.isEnabled = true
                                btnRegister.text = "Register"
                                Toast.makeText(
                                    this,
                                    "Failed to save user data: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                } else {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
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