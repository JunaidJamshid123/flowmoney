package com.example.flowmoney.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore

class SplashScreen : AppCompatActivity() {
    private val splashTimeout = 2500L  // 2.5 seconds
    private val TAG = "SplashScreen"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
            // User is logged in, but check if it's a valid session by confirming in Firestore
            // This handles the case where user logged out in a previous session but Firebase Auth
            // still has a cached authentication state
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Valid user session exists, check if any logout flag is set
                        val isLoggedOut = document.getBoolean("is_logged_out") ?: false

                        if (isLoggedOut) {
                            // User explicitly logged out in previous session
                            // Clear Firebase Auth state and navigate to login
                            auth.signOut()
                            navigateToLogin()
                        } else {
                            // User is properly logged in, update login time and proceed to main
                            updateLastLoginTime(currentUser.uid)
                            navigateToMain()
                        }
                    } else {
                        // User doesn't exist in Firestore despite Auth token - potential issue
                        // This might happen if user was deleted from backend but local auth persisted
                        auth.signOut()
                        navigateToLogin()
                    }
                }
                .addOnFailureListener { e ->
                    // Failed to check user in Firestore - network issue likely
                    Log.e(TAG, "Error checking user session", e)
                    // Still navigate to main if we have a local auth token - can verify again later
                    navigateToMain()
                }
        } else {
            // No user is logged in, show onboarding or login screens
            navigateToOnboarding()
        }
    }

    private fun updateLastLoginTime(userId: String) {
        db.collection("users").document(userId)
            .update("last_login_at", System.currentTimeMillis(), "is_logged_out", false)
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating last login time", e)
            }
    }

    private fun navigateToMain() {
        Log.d(TAG, "User logged in, navigating to MainActivity: ${auth.currentUser?.uid}")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        Log.d(TAG, "No active session, navigating to Login")
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToOnboarding() {
        Log.d(TAG, "First time user, navigating to onboarding")
        val intent = Intent(this, Fram1::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}