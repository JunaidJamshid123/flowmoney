package com.example.flowmoney.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.flowmoney.Activities.Authentication.Login
import com.example.flowmoney.Activities.Authentication.Signup
import com.example.flowmoney.Activities.Frams.Fram1
import com.example.flowmoney.Activities.Frams.Fram2
import com.example.flowmoney.Activities.Frams.Fram3
import com.example.flowmoney.MainActivity
import com.example.flowmoney.R

class SplashScreen : AppCompatActivity() {
    private val splashTimeout = 5000L  // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)



        // Initialize views
        val logoContainer = findViewById<CardView>(R.id.logo_container)
        val appName = findViewById<TextView>(R.id.app_name)
        val appTagline = findViewById<TextView>(R.id.app_tagline)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        // Apply animations with delay
        Handler(Looper.getMainLooper()).postDelayed({
            logoContainer.startAnimation(fadeIn)
        }, 300)

        Handler(Looper.getMainLooper()).postDelayed({
            appName.startAnimation(fadeIn)
        }, 800)

        Handler(Looper.getMainLooper()).postDelayed({
            appTagline.startAnimation(fadeIn)
        }, 1200)

        // Navigate to main activity after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            val mainIntent = Intent(this, Fram1::class.java)
            startActivity(mainIntent)
            finish()
        }, splashTimeout)
    }
}