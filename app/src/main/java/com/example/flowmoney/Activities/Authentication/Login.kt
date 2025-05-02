package com.example.flowmoney.Activities.Authentication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.flowmoney.MainActivity
import com.example.flowmoney.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class Login : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var cvGoogle: CardView
    private lateinit var cvFacebook: CardView
    private lateinit var cvApple: CardView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val TAG = "LoginActivity"
    private val RC_SIGN_IN = 9001

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize UI elements
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btLogin = findViewById(R.id.btLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        cvGoogle = findViewById(R.id.cvGoogle)
        cvFacebook = findViewById(R.id.cvFacebook)
        cvApple = findViewById(R.id.cvApple)

        // Add progress bar to layout if not already there
        progressBar = findViewById(R.id.progressBar)
        if (progressBar == null) {
            // You'll need to add a ProgressBar to your layout XML
            Log.w(TAG, "ProgressBar not found in layout")
        }

        // Set click listeners
        btLogin.setOnClickListener {
            if (validateInputs()) {
                loginWithEmail()
            }
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            } else {
                sendPasswordResetEmail(email)
            }
        }

        cvGoogle.setOnClickListener {
            signInWithGoogle()
        }

        cvFacebook.setOnClickListener {
            // Facebook login implementation would go here
            Toast.makeText(this, "Facebook login not implemented yet", Toast.LENGTH_SHORT).show()
        }

        cvApple.setOnClickListener {
            // Apple login implementation would go here
            Toast.makeText(this, "Apple login not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(): Boolean {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Check if fields are empty
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return false
        }

        if (!isValidEmail(email)) {
            etEmail.error = "Please enter a valid email address"
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        return emailPattern.matcher(email).matches()
    }

    private fun loginWithEmail() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Show progress bar
        progressBar?.visibility = View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Hide progress bar
                progressBar?.visibility = View.GONE

                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")

                    // Update last login time
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        db.collection("users").document(userId)
                            .update("last_login_at", System.currentTimeMillis())
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error updating last login time", e)
                            }
                    }

                    // Navigate to main activity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        progressBar?.visibility = View.VISIBLE

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar?.visibility = View.GONE

                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        progressBar?.visibility = View.VISIBLE
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent()
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleGoogleSignInResult(task)
            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                Log.e(TAG, "Google Sign In error", e)
                Toast.makeText(this, "Google Sign In failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign In successful, account ID: ${account.id}")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            progressBar?.visibility = View.GONE
            Log.w(TAG, "Google sign in failed", e)
            Toast.makeText(this, "Google Sign In failed: ${e.statusCode} - ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                progressBar?.visibility = View.GONE

                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val firebaseUser = auth.currentUser

                    if (firebaseUser != null) {
                        // Check if this is a new user
                        db.collection("users").document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // New user - redirect to Signup activity to complete profile
                                    val intent = Intent(this, Signup::class.java)
                                    intent.putExtra("GOOGLE_SIGN_IN", true)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Existing user - just update login time
                                    db.collection("users").document(firebaseUser.uid)
                                        .update("last_login_at", System.currentTimeMillis())
                                        .addOnSuccessListener {
                                            // Navigate to main activity
                                            val intent = Intent(this, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error checking user existence", e)
                                Toast.makeText(this, "Failed to process sign in", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}