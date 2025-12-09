package com.mobcomp.studyx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId("activity_home"))

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvUserName = findViewById<TextView>(getViewId("tvUserName"))
        val tvEmail = findViewById<TextView>(getViewId("tvEmail"))
        val tvStudentLevel = findViewById<TextView>(getViewId("tvStudentLevel"))
        val btnLogout = findViewById<Button>(getViewId("btnLogout"))

        loadUserData(tvUserName, tvEmail, tvStudentLevel)

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadUserData(
        tvUserName: TextView,
        tvEmail: TextView,
        tvStudentLevel: TextView
    ) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: "User"
                        val email = document.getString("email") ?: "N/A"
                        val studentLevel = document.getString("studentLevel") ?: "N/A"

                        tvUserName.text = fullName
                        tvEmail.text = "Email: $email"
                        tvStudentLevel.text = "Level: $studentLevel"
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to load user data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun getLayoutId(name: String): Int {
        return resources.getIdentifier(name, "layout", packageName)
    }

    private fun getViewId(name: String): Int {
        return resources.getIdentifier(name, "id", packageName)
    }
}