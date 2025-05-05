package com.example.flowmoney.Activities.Authentication

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
import com.example.flowmoney.Models.User
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
        initializeViews()
        setupClickListeners()

        // Check if coming from logout
        if (intent.getBooleanExtra("FROM_LOGOUT", false)) {
            Toast.makeText(this, "You have been logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btLogin = findViewById(R.id.btLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        cvGoogle = findViewById(R.id.cvGoogle)
        cvFacebook = findViewById(R.id.cvFacebook)
        cvApple = findViewById(R.id.cvApple)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
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
            handleForgotPassword()
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

    private fun handleForgotPassword() {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            etEmail.error = "Email is required"
            etEmail.requestFocus()
        } else if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            etEmail.error = "Invalid email format"
            etEmail.requestFocus()
        } else {
            sendPasswordResetEmail(email)
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

        // Show progress bar and disable login button
        progressBar.visibility = View.VISIBLE
        btLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    handleSuccessfulLogin()
                } else {
                    // If sign in fails, display a message to the user
                    handleFailedLogin(task.exception?.message)
                }
            }
    }

    private fun handleSuccessfulLogin() {
        // Sign in success, update UI with the signed-in user's information
        Log.d(TAG, "signInWithEmail:success")

        // Update last login time and reset logout flag
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .update(
                    mapOf(
                        "last_login_at" to System.currentTimeMillis(),
                        "is_logged_out" to false
                    )
                )
                .addOnCompleteListener { updateTask ->
                    // Hide progress bar
                    progressBar.visibility = View.GONE
                    btLogin.isEnabled = true

                    if (!updateTask.isSuccessful) {
                        Log.w(TAG, "Error updating last login time", updateTask.exception)
                    }

                    // Navigate to main activity
                    navigateToMainActivity()
                }
        } else {
            // Hide progress bar
            progressBar.visibility = View.GONE
            btLogin.isEnabled = true
            navigateToMainActivity()
        }
    }

    private fun handleFailedLogin(errorMessage: String?) {
        // Hide progress bar and enable login button
        progressBar.visibility = View.GONE
        btLogin.isEnabled = true

        Log.w(TAG, "signInWithEmail:failure", Exception(errorMessage))

        // Display a user-friendly error message
        val message = when {
            errorMessage?.contains("password is invalid") == true -> "Incorrect password. Please try again."
            errorMessage?.contains("no user record") == true -> "No account found with this email. Please sign up."
            errorMessage?.contains("blocked all requests") == true -> "Too many login attempts. Please try again later."
            else -> "Authentication failed: $errorMessage"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendPasswordResetEmail(email: String) {
        progressBar.visibility = View.VISIBLE
        btLogin.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                btLogin.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        progressBar.visibility = View.VISIBLE
        btLogin.isEnabled = false
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
                progressBar.visibility = View.GONE
                btLogin.isEnabled = true

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
            progressBar.visibility = View.GONE
            btLogin.isEnabled = true

            Log.w(TAG, "Google sign in failed", e)
            Toast.makeText(this, "Google Sign In failed: Error code ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, check if user exists in database
                    Log.d(TAG, "signInWithCredential:success")
                    checkGoogleUserInDatabase()
                } else {
                    // If sign in fails, display a message to the user.
                    progressBar.visibility = View.GONE
                    btLogin.isEnabled = true

                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkGoogleUserInDatabase() {
        val firebaseUser = auth.currentUser

        if (firebaseUser != null) {
            // Check if this is a new user
            db.collection("users").document(firebaseUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Existing user - update login time and reset logout flag
                        document.reference.update(
                            mapOf(
                                "last_login_at" to System.currentTimeMillis(),
                                "is_logged_out" to false
                            )
                        )
                            .addOnSuccessListener {
                                // Navigate to main activity
                                progressBar.visibility = View.GONE
                                btLogin.isEnabled = true
                                navigateToMainActivity()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btLogin.isEnabled = true
                                Log.w(TAG, "Error updating user login time", e)
                                navigateToMainActivity() // Still navigate even if update fails
                            }
                    } else {
                        // New user - redirect to Signup activity to complete profile
                        progressBar.visibility = View.GONE
                        btLogin.isEnabled = true

                        val intent = Intent(this, Signup::class.java)
                        intent.putExtra("GOOGLE_SIGN_IN", true)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btLogin.isEnabled = true

                    Log.w(TAG, "Error checking user existence", e)
                    Toast.makeText(this, "Failed to process sign in", Toast.LENGTH_SHORT).show()
                }
        } else {
            progressBar.visibility = View.GONE
            btLogin.isEnabled = true
            Toast.makeText(this, "Failed to get user information", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}