package com.hostelops

import android.content.Context
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

        // Set version
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvAppVersion.text = "Version ${pInfo.versionName}"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "Version 1.0.0"
        }

        // Set random quote
        val quotes = arrayOf(
            "\"Success is not final, failure is not fatal: it is the courage to continue that counts.\" - Winston Churchill",
            "\"The only way to do great work is to love what you do.\" - Steve Jobs",
            "\"Believe you can and you're halfway there.\" - Theodore Roosevelt",
            "\"Your time is limited, don't waste it living someone else's life.\" - Steve Jobs",
            "\"Hardships often prepare ordinary people for an extraordinary destiny.\" - C.S. Lewis",
            "\"The best way to predict the future is to create it.\" - Peter Drucker",
            "\"Everything you've ever wanted is on the other side of fear.\" - George Addair",
            "\"The journey of a thousand miles begins with one step.\" - Lao Tzu"
        )
        binding.tvQuote.text = quotes.random()

        checkSessionAndNavigate()
    }

    private fun checkSessionAndNavigate() {
        val prefs = getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
        val lastOpened = prefs.getLong("last_opened", 0L)
        val currentTime = System.currentTimeMillis()
        val twentyDaysInMillis = 20L * 24 * 60 * 60 * 1000

        if (lastOpened != 0L && (currentTime - lastOpened) > twentyDaysInMillis) {
            auth.signOut()
        }

        // Update last opened time
        prefs.edit().putLong("last_opened", currentTime).apply()

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
