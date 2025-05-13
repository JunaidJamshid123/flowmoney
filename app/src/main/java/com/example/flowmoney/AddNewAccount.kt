package com.example.flowmoney

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.flowmoney.Models.Account
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.util.UUID

class AddNewAccount : AppCompatActivity() {
    // Define TAG for logging
    companion object {
        private const val TAG = "AddNewAccount"
    }

    private lateinit var initialAmountEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button

    private var selectedIconContainer: FrameLayout? = null
    private var selectedIconId: String = "mobilebanking" // Default selected icon

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Map to store drawable resources for each icon ID
    private val iconResourceMap = mapOf(
        "mobilebanking" to R.drawable.mobilebanking,
        "creditcard" to R.drawable.creditcard,
        "onlinewallet" to R.drawable.onlinewallet,
        "bitcoin" to R.drawable.bitcoin,
        "smartphone" to R.drawable.smartphone,
        "bank" to R.drawable.bank,
        "cash" to R.drawable.cash
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_account)

        // Initialize Firebase components
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            Log.e(TAG, "No user is logged in, redirecting to login")
            Toast.makeText(this, "Please log in to add an account", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Firebase initialized. User logged in: ${auth.currentUser?.uid}")

        // Initialize views
        initialAmountEditText = findViewById(R.id.etInitialAmount)
        nameEditText = findViewById(R.id.etName)
        cancelButton = findViewById(R.id.btnCancel)
        saveButton = findViewById(R.id.btnSave)

        // Set up the initial selected icon
        selectedIconContainer = findViewById(R.id.mobileBankingContainer)
        selectedIconContainer?.isSelected = true

        // Set up click listeners for all icon containers
        setupIconClickListeners()

        // Set up button click listeners
        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            saveAccount()
        }
    }

    private fun setupIconClickListeners() {
        val iconContainers = listOf(
            findViewById<FrameLayout>(R.id.mobileBankingContainer),
            findViewById<FrameLayout>(R.id.creditCardContainer),
            findViewById<FrameLayout>(R.id.onlineWalletContainer),
            findViewById<FrameLayout>(R.id.bitcoinContainer),
            findViewById<FrameLayout>(R.id.smartphoneContainer),
            findViewById<FrameLayout>(R.id.bankContainer),
            findViewById<FrameLayout>(R.id.cashContainer)
        )

        val iconIds = listOf(
            "mobilebanking",
            "creditcard",
            "onlinewallet",
            "bitcoin",
            "smartphone",
            "bank",
            "cash"
        )

        val selectedIndicators = listOf(
            findViewById<ImageView>(R.id.ivMobileBankingSelected),
            findViewById<ImageView>(R.id.ivCreditCardSelected),
            findViewById<ImageView>(R.id.ivOnlineWalletSelected),
            findViewById<ImageView>(R.id.ivBitcoinSelected),
            findViewById<ImageView>(R.id.ivSmartphoneSelected),
            findViewById<ImageView>(R.id.ivBankSelected),
            findViewById<ImageView>(R.id.ivCashSelected)
        )

        // Initially hide all selected indicators except the default one
        selectedIndicators.forEachIndexed { index, indicator ->
            indicator.visibility = if (index == 0) View.VISIBLE else View.GONE
        }

        // Set up click listeners for each container
        iconContainers.forEachIndexed { index, container ->
            container.setOnClickListener {
                // Deselect previous selection
                selectedIconContainer?.isSelected = false
                selectedIndicators.forEach { it.visibility = View.GONE }

                // Update selection
                selectedIconContainer = container
                selectedIconId = iconIds[index]
                container.isSelected = true
                selectedIndicators[index].visibility = View.VISIBLE

                // Log selection for debugging
                Log.d(TAG, "Selected icon: $selectedIconId")
            }
        }
    }

    private fun saveAccount() {
        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No user is currently logged in")
            Toast.makeText(this, "You must be logged in to create an account", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Current user ID: ${currentUser.uid}")

        // Validate input
        val initialAmountText = initialAmountEditText.text.toString().trim()
        if (initialAmountText.isEmpty()) {
            initialAmountEditText.error = "Please enter an amount"
            return
        }

        val initialAmount = initialAmountText.toDoubleOrNull()
        if (initialAmount == null) {
            initialAmountEditText.error = "Please enter a valid amount"
            return
        }

        // Get account name, defaulting to "Untitled" if empty
        val accountName = nameEditText.text.toString().trim().takeIf { it.isNotEmpty() } ?: "Untitled"

        // Show loading indicator or disable button
        saveButton.isEnabled = false
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

        try {
            // Get resource ID for the selected icon
            val resourceId = iconResourceMap[selectedIconId]
            if (resourceId == null) {
                Log.e(TAG, "Resource ID not found for icon: $selectedIconId")
                Toast.makeText(this, "Error: Icon not found", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = true
                return
            }

            // Convert icon to Base64 string
            val iconBase64 = getIconAsBase64(this, resourceId)
            Log.d(TAG, "Icon converted to Base64 successfully")

            // Create a unique ID for the account
            val accountId = UUID.randomUUID().toString()
            Log.d(TAG, "Generated account ID: $accountId")

            // Create account object
            val account = Account(
                accountId = accountId,
                userId = currentUser.uid,
                accountName = accountName,
                balance = initialAmount,
                accountType = getAccountTypeFromIcon(selectedIconId),
                accountImageUrl = iconBase64,
                note = "Created on ${android.text.format.DateFormat.format("MMM dd, yyyy", System.currentTimeMillis())}"
            )

            // Set timestamps explicitly
            val currentTime = System.currentTimeMillis()
            account.createdAt = currentTime
            account.updatedAt = currentTime

            // Log account details before saving
            Log.d(TAG, "Account to be saved: ${account.toString()}")

            // Save to Firestore
            saveAccountToFirestore(account)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing account data", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            saveButton.isEnabled = true
        }
    }

    private fun getAccountTypeFromIcon(iconId: String): String {
        return when (iconId) {
            "mobilebanking" -> "Mobile Banking"
            "creditcard" -> "Credit Card"
            "onlinewallet" -> "E-Wallet"
            "bitcoin" -> "Cryptocurrency"
            "smartphone" -> "Mobile Money"
            "bank" -> "Bank Account"
            "cash" -> "Cash"
            else -> "Other"
        }
    }

    private fun getIconAsBase64(context: Context, resourceId: Int): String {
        try {
            Log.d(TAG, "Getting drawable resource for ID: $resourceId")

            // Get the drawable safely
            val drawable = ContextCompat.getDrawable(context, resourceId)
                ?: throw IllegalStateException("Could not load drawable for resource ID: $resourceId")

            // Log drawable dimensions
            Log.d(TAG, "Drawable dimensions: ${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")

            // Convert drawable to bitmap with proper error handling
            val bitmap = drawableToBitmap(drawable)

            // Convert bitmap to Base64 string
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d(TAG, "Base64 string length: ${base64String.length}")

            return base64String
        } catch (e: Exception) {
            Log.e(TAG, "Error converting drawable to Base64", e)
            throw e
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        // Create a bitmap with appropriate dimensions
        val width = if (drawable.intrinsicWidth <= 0) 1 else drawable.intrinsicWidth
        val height = if (drawable.intrinsicHeight <= 0) 1 else drawable.intrinsicHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private fun saveAccountToFirestore(account: Account) {
        // Find the progress bar safely
        val progressBar = findViewById<View>(R.id.progressBar)
        
        // Show loading indicator if available
        progressBar?.visibility = View.VISIBLE
        
        // Generate account ID if not provided
        if (account.accountId.isBlank()) {
            account.accountId = UUID.randomUUID().toString()
        }
        
        // Reference to the account document in Firestore
        val accountRef = firestore.collection("accounts").document(account.accountId)
        
        // Convert Account object to a map for Firestore
        val accountData = account.toMap()
        
        // Set the data to Firestore
        accountRef.set(accountData)
            .addOnSuccessListener {
                Log.d(TAG, "Account saved successfully!")
                
                // Send notification for new account
                (application as FlowMoneyApplication).notificationHelper.notifyAccountAdded(
                    account.accountName, 
                    account.accountType
                )
                
                // Hide loading indicator if available
                progressBar?.visibility = View.GONE
                
                // Set result and finish
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving account", e)
                Toast.makeText(this, "Failed to save account: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Hide loading indicator if available
                progressBar?.visibility = View.GONE
            }
    }
}