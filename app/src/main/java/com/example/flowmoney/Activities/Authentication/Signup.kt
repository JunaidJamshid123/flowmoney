package com.example.flowmoney.Activities.Authentication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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

class Signup : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btSignup: Button
    private lateinit var tvLogin: TextView
    private lateinit var cvGoogle: CardView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private val TAG = "SignupActivity"
    private val RC_SIGN_IN = 9001

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

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
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btSignup = findViewById(R.id.btSignup)
        tvLogin = findViewById(R.id.tvLogin)
        cvGoogle = findViewById(R.id.cvGoogle)

        // Initialize progress bar - make sure to add this to your layout XML
        progressBar = findViewById(R.id.progressBar)
        if (progressBar == null) {
            Log.w(TAG, "ProgressBar not found in layout")
        }

        // Set click listeners
        btSignup.setOnClickListener {
            if (validateInputs()) {
                signUpWithEmail()
            }
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        cvGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Check if we're coming from a Google Sign In in the Login activity
        if (intent.getBooleanExtra("GOOGLE_SIGN_IN", false)) {
            // Auto-fill fields with Google account info if available
            val currentUser = auth.currentUser
            if (currentUser != null) {
                etFullName.setText(currentUser.displayName ?: "")
                etEmail.setText(currentUser.email ?: "")
                etEmail.isEnabled = false // Don't allow changing email if coming from Google

                // Generate a username suggestion from email
                val emailPrefix = currentUser.email?.substringBefore("@")?.replace(".", "_") ?: ""
                etUsername.setText(emailPrefix)
            }
        }
    }

    private fun validateInputs(): Boolean {
        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Check if fields are empty
        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            etFullName.requestFocus()
            return false
        }

        if (username.isEmpty()) {
            etUsername.error = "Username is required"
            etUsername.requestFocus()
            return false
        }

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

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Please confirm your password"
            etConfirmPassword.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return false
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please accept the Terms & Conditions", Toast.LENGTH_SHORT).show()
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

    private fun signUpWithEmail() {
        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Show progress bar
        progressBar.visibility = View.VISIBLE

        // Disable signup button to prevent multiple clicks
        btSignup.isEnabled = false

        // First check if username already exists
        checkIfUsernameExists(username) { usernameExists ->
            if (usernameExists) {
                progressBar.visibility = View.GONE
                btSignup.isEnabled = true
                etUsername.error = "Username already taken"
                etUsername.requestFocus()
            } else {
                // Handle Google Sign In case differently
                if (intent.getBooleanExtra("GOOGLE_SIGN_IN", false) && auth.currentUser != null) {
                    // User is already authenticated with Google, just save profile
                    val firebaseUser = auth.currentUser

                    if (firebaseUser != null) {
                        // Create User object
                        val user = User(
                            userId = firebaseUser.uid,
                            fullName = fullName,
                            username = username,
                            email = email,
                            profileImageUrl = firebaseUser.photoUrl?.toString()
                        ).createFromSocialLogin("google")

                        // Save user to Firestore
                        saveUserToFirestore(user)
                    }
                } else {
                    // Create user with email and password
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success")
                                val firebaseUser = auth.currentUser

                                if (firebaseUser != null) {
                                    // Create User object
                                    val user = User(
                                        userId = firebaseUser.uid,
                                        fullName = fullName,
                                        username = username,
                                        email = email
                                    )

                                    // Save user to Firestore
                                    saveUserToFirestore(user)
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                progressBar.visibility = View.GONE
                                btSignup.isEnabled = true
                                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                                Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }
    }

    private fun checkIfUsernameExists(username: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error checking username", e)
                // If there's an error, assume username doesn't exist to allow signup attempt
                callback(false)
            }
    }

    private fun saveUserToFirestore(user: User) {
        db.collection("users").document(user.userId)
            .set(user.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "User profile created for ${user.userId}")

                // Hide progress bar
                progressBar.visibility = View.GONE
                btSignup.isEnabled = true

                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()

                // Navigate to main activity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                // Hide progress bar
                progressBar.visibility = View.GONE
                btSignup.isEnabled = true

                Log.w(TAG, "Error adding user to Firestore", e)
                Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signInWithGoogle() {
        progressBar.visibility = View.VISIBLE
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
            Log.w(TAG, "Google sign in failed: code=${e.statusCode}", e)
            Toast.makeText(this, "Google Sign In failed: Status code ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val firebaseUser = auth.currentUser

                    if (firebaseUser != null) {
                        // Check if this is a new user
                        db.collection("users").document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                progressBar.visibility = View.GONE

                                if (!document.exists()) {
                                    // New user - Auto-fill fields with Google account info
                                    etFullName.setText(firebaseUser.displayName ?: "")
                                    etEmail.setText(firebaseUser.email ?: "")
                                    etEmail.isEnabled = false // Don't allow changing email from Google

                                    // Generate a username suggestion from email
                                    val emailPrefix = firebaseUser.email?.substringBefore("@")?.replace(".", "_") ?: ""
                                    etUsername.setText(emailPrefix)

                                    Toast.makeText(this, "Please complete your profile", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Existing user - just update login time and redirect to main
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
                                progressBar.visibility = View.GONE
                                Log.w(TAG, "Error checking user existence", e)
                                Toast.makeText(this, "Failed to process sign in", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    progressBar.visibility = View.GONE
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}