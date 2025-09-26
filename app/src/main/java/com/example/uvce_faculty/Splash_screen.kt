package com.example.uvce_faculty

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uvce_faculty.databinding.ActivitySplashScreenBinding
import com.google.firebase.auth.FirebaseAuth

class Splash_screen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate logo: fade + scale
        binding.logo.apply {
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1500).start()
        }

        // Animate app name: fade in after logo
        binding.appName.apply {
            alpha = 0f
            animate().alpha(1f).setStartDelay(500).setDuration(1500).start()
        }

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val nextIntent = if (auth.currentUser != null) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, GetIn::class.java)
            }
            startActivity(nextIntent)
            finish()
        }, 2500)
    }
}
