package com.example.flowmoney.Activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.flowmoney.Models.User
import com.example.flowmoney.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class ProfileActivity : AppCompatActivity() {

    // UI elements
    private lateinit var profileImage: ImageView
    private lateinit var editProfileImageButton: CardView
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var progressBar: ProgressBar

    // Fields
    private lateinit var fullNameValue: EditText
    private lateinit var usernameValue: EditText
    private lateinit var emailValue: EditText
    private lateinit var phoneNumberValue: EditText
    private lateinit var addressValue: EditText

    // Edit buttons
    private lateinit var btnEditFullName: ImageView
    private lateinit var btnEditUsername: ImageView
    private lateinit var btnEditEmail: ImageView
    private lateinit var btnEditPhoneNumber: ImageView
    private lateinit var btnEditAddress: ImageView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // User data
    private lateinit var currentUser: User
    private var imageUri: Uri? = null
    private var isImageChanged = false
    private var isDataChanged = false
    private var base64Image: String? = null

    // Permissions
    private val PERMISSION_REQUEST_CODE = 100
    private val TAG = "ProfileActivity"

    // Maximum image size for Firestore (0.9MB to be safe with Firestore's 1MB document limit)
    private val MAX_IMAGE_SIZE_BYTES = 900 * 1024 // 900KB

    // Activity result launcher for image picking
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                convertImageToBase64(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        initializeViews()
        setupClickListeners()

        // Load user data
        loadUserData()
    }

    private fun initializeViews() {
        // Profile image and edit button
        profileImage = findViewById(R.id.profileImage)
        editProfileImageButton = findViewById(R.id.editProfileImageButton)

        // Navigation buttons
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        // Fields
        fullNameValue = findViewById(R.id.fullNameValue)
        usernameValue = findViewById(R.id.usernameValue)
        emailValue = findViewById(R.id.emailValue)
        phoneNumberValue = findViewById(R.id.phoneNumberValue)
        addressValue = findViewById(R.id.addressValue)

        // Edit buttons
        btnEditFullName = findViewById(R.id.btnEditFullName)
        btnEditUsername = findViewById(R.id.btnEditUsername)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        btnEditPhoneNumber = findViewById(R.id.btnEditPhoneNumber)
        btnEditAddress = findViewById(R.id.btnEditAddress)
    }

    private fun setupClickListeners() {
        // Back button
        btnBack.setOnClickListener {
            if (isDataChanged) {
                showUnsavedChangesDialog()
            } else {
                finish()
            }
        }

        // Save button
        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveUserData()
            }
        }

        // Edit profile image button
        editProfileImageButton.setOnClickListener {
            if (checkPermissions()) {
                openImagePicker()
            } else {
                requestPermissions()
            }
        }

        // Setup field edit buttons
        setupFieldEditButtons()
    }

    private fun setupFieldEditButtons() {
        btnEditFullName.setOnClickListener {
            toggleFieldEdit(fullNameValue)
            isDataChanged = true
        }

        btnEditUsername.setOnClickListener {
            toggleFieldEdit(usernameValue)
            isDataChanged = true
        }

        btnEditEmail.setOnClickListener {
            if (!currentUser.isSocialLogin) {
                toggleFieldEdit(emailValue)
                isDataChanged = true
            } else {
                Toast.makeText(this, "Cannot edit email for social login accounts", Toast.LENGTH_SHORT).show()
            }
        }

        btnEditPhoneNumber.setOnClickListener {
            toggleFieldEdit(phoneNumberValue)
            isDataChanged = true
        }

        btnEditAddress.setOnClickListener {
            toggleFieldEdit(addressValue)
            isDataChanged = true
        }
    }

    private fun toggleFieldEdit(field: EditText) {
        field.isEnabled = !field.isEnabled
        if (field.isEnabled) {
            field.requestFocus()
            field.setSelection(field.text.length)
        }
    }

    private fun loadUserData() {
        val currentFirebaseUser = auth.currentUser
        if (currentFirebaseUser == null) {
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Get user data from Firestore
        db.collection("users").document(currentFirebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE

                if (document != null && document.exists()) {
                    try {
                        // Map Firestore document to User model
                        currentUser = User()
                        currentUser.userId = document.id
                        currentUser.fullName = document.getString("full_name") ?: ""
                        currentUser.username = document.getString("username") ?: ""
                        currentUser.email = document.getString("email") ?: ""
                        currentUser.phoneNumber = document.getString("phone_number")
                        currentUser.profileImageUrl = document.getString("profile_image_url")
                        currentUser.address = document.getString("address")
                        currentUser.isSocialLogin = document.getBoolean("is_social_login") ?: false
                        currentUser.socialLoginType = document.getString("social_login_type")

                        // Populate UI with user data
                        populateUserData()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping document to User", e)
                        Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error getting user document", e)
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun populateUserData() {
        // Set basic user information
        fullNameValue.setText(currentUser.fullName)
        usernameValue.setText(currentUser.username)
        emailValue.setText(currentUser.email)
        phoneNumberValue.setText(currentUser.phoneNumber ?: "")

        // Set address from the User model
        addressValue.setText(currentUser.address ?: "")

        // Disable email editing for social login accounts
        if (currentUser.isSocialLogin) {
            emailValue.isEnabled = false
            btnEditEmail.alpha = 0.5f
        }

        // Load profile image from Base64 string
        if (!currentUser.profileImageUrl.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(currentUser.profileImageUrl, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                Glide.with(this)
                    .load(decodedImage)
                    .placeholder(R.drawable.default_profile)
                    .circleCrop()
                    .into(profileImage)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding profile image", e)
                // Load default image if decoding fails
                Glide.with(this)
                    .load(R.drawable.default_profile)
                    .circleCrop()
                    .into(profileImage)
            }
        } else {
            // Load default image if no profile image exists
            Glide.with(this)
                .load(R.drawable.default_profile)
                .circleCrop()
                .into(profileImage)
        }
    }

    private fun convertImageToBase64(uri: Uri) {
        try {
            // Show progress while processing image
            progressBar.visibility = View.VISIBLE

            // Process in a background thread
            Thread {
                try {
                    val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return@Thread

                    // Read the image into a bitmap
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    // Resize the bitmap to ensure it fits within Firestore's document size limit
                    val resizedBitmap = resizeBitmapIfNeeded(originalBitmap)

                    // Convert bitmap to base64 string
                    val outputStream = ByteArrayOutputStream()
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val imageBytes = outputStream.toByteArray()

                    // Check if the image is still too large
                    if (imageBytes.size > MAX_IMAGE_SIZE_BYTES) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@ProfileActivity,
                                "Image is too large. Please select a smaller image.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@Thread
                    }

                    base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                    isImageChanged = true
                    isDataChanged = true

                    // Update UI on the main thread
                    runOnUiThread {
                        progressBar.visibility = View.GONE

                        // Load the selected image into the profile image view
                        Glide.with(this@ProfileActivity)
                            .load(resizedBitmap)
                            .circleCrop()
                            .into(profileImage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting image to base64", e)

                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ProfileActivity,
                            "Error processing image. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Log.e(TAG, "Error processing image", e)
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        // Calculate the current size of the bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val currentSize = outputStream.toByteArray().size

        // If the image is already small enough, return the original
        if (currentSize <= MAX_IMAGE_SIZE_BYTES) {
            return bitmap
        }

        // Calculate the scaling factor needed
        val scaleFactor = Math.sqrt(MAX_IMAGE_SIZE_BYTES.toDouble() / currentSize.toDouble())

        // Calculate new dimensions
        val newWidth = (bitmap.width * scaleFactor).toInt()
        val newHeight = (bitmap.height * scaleFactor).toInt()

        // Create and return the resized bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun validateInputs(): Boolean {
        // Validate full name
        if (fullNameValue.text.toString().trim().isEmpty()) {
            fullNameValue.error = "Full name is required"
            fullNameValue.requestFocus()
            return false
        }

        // Validate username
        if (usernameValue.text.toString().trim().isEmpty()) {
            usernameValue.error = "Username is required"
            usernameValue.requestFocus()
            return false
        }

        // Validate email
        val email = emailValue.text.toString().trim()
        if (email.isEmpty()) {
            emailValue.error = "Email is required"
            emailValue.requestFocus()
            return false
        }

        if (!isValidEmail(email)) {
            emailValue.error = "Please enter a valid email address"
            emailValue.requestFocus()
            return false
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        return emailPattern.matcher(email).matches()
    }

    private fun saveUserData() {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        // Check if username is changed and already exists
        val newUsername = usernameValue.text.toString().trim()
        if (newUsername != currentUser.username) {
            checkIfUsernameExists(newUsername) { usernameExists ->
                if (usernameExists) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    usernameValue.error = "Username already exists"
                    usernameValue.requestFocus()
                } else {
                    // Continue with saving data
                    updateUserDataInFirestore()
                }
            }
        } else {
            // Username not changed, continue with saving data
            updateUserDataInFirestore()
        }
    }

    private fun checkIfUsernameExists(username: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                val exists = documents.documents.any { it.id != auth.currentUser?.uid }
                callback(exists)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking username", e)
                callback(false) // Assume username doesn't exist on error
            }
    }

    private fun updateUserDataInFirestore() {
        // Update user data from UI
        currentUser.fullName = fullNameValue.text.toString().trim()
        currentUser.username = usernameValue.text.toString().trim()

        // Only update email if not a social login
        if (!currentUser.isSocialLogin) {
            currentUser.email = emailValue.text.toString().trim()
        }

        currentUser.phoneNumber = phoneNumberValue.text.toString().trim()
        currentUser.address = addressValue.text.toString().trim()

        // Update profile image if changed
        if (isImageChanged && base64Image != null) {
            currentUser.profileImageUrl = base64Image
        }

        // Create update map
        val updates = mutableMapOf<String, Any?>()
        updates["full_name"] = currentUser.fullName
        updates["username"] = currentUser.username
        updates["email"] = currentUser.email
        updates["phone_number"] = currentUser.phoneNumber
        updates["address"] = currentUser.address

        // Only update the image if it changed
        if (isImageChanged && base64Image != null) {
            updates["profile_image_url"] = base64Image
        }

        // Update last login time
        currentUser.updateLoginTime()
        updates["last_login_at"] = currentUser.lastLoginAt

        // Update user document in Firestore
        db.collection("users").document(currentUser.userId)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                isDataChanged = false
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Log.e(TAG, "Error updating user data", e)
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Cannot select profile image.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them before leaving?")
            .setPositiveButton("Save") { _, _ ->
                if (validateInputs()) {
                    saveUserData()
                }
            }
            .setNegativeButton("Discard") { _, _ ->
                finish()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    // Handle back button press
    override fun onBackPressed() {
        if (isDataChanged) {
            showUnsavedChangesDialog()
        } else {
            super.onBackPressed()
        }
    }
}