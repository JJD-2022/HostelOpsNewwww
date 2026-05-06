package com.hostelops

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hostelops.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAndNavigate()
        }, 2000)
    }

    private fun checkUserAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("ROLE", role)
                        startActivity(intent)
                    } else {
                        // User exists in Auth but not in Firestore, take to role selection
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
