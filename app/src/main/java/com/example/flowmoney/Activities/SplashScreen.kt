package com.example.flowmoney.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.flowmoney.Activities.Authentication.Login
import com.example.flowmoney.Activities.Frams.Fram1
import com.example.flowmoney.MainActivity
import com.example.flowmoney.R
import com.google.firebase.auth.FirebaseAuth

class SplashScreen : AppCompatActivity() {
    private val splashTimeout = 2500L  // 2.5 seconds
    private val TAG = "SplashScreen"
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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

        // Navigate to appropriate activity after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, splashTimeout)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is already logged in
            Log.d(TAG, "User already logged in: ${currentUser.uid}")

            // Check if email is verified if you want to enforce this
            if (currentUser.isEmailVerified || isUserFromSocialLogin()) {
                // Navigate to main activity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Navigate to login and suggest verification
                val intent = Intent(this, Login::class.java)
                intent.putExtra("SHOW_VERIFICATION_REMINDER", true)
                startActivity(intent)
                finish()
            }
        } else {
            // User is not logged in
            Log.d(TAG, "No user logged in, showing onboarding")

            // Navigate to onboarding/intro frame
            val intent = Intent(this, Fram1::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isUserFromSocialLogin(): Boolean {
        // Check if the user signed in with a provider other than email/password
        val currentUser = auth.currentUser ?: return false
        val providerData = currentUser.providerData

        for (profile in providerData) {
            val providerId = profile.providerId
            if (providerId == "google.com" ||
                providerId == "facebook.com" ||
                providerId == "apple.com"
            ) {
                return true
            }
        }

        return false
    }
}