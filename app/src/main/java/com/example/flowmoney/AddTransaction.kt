package com.example.flowmoney

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.flowmoney.Models.Account
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.flowmoney.utlities.BudgetUtils

class AddTransaction : AppCompatActivity() {
    companion object {
        private const val TAG = "AddTransaction"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB max
    }

    // UI Components
    private lateinit var btnClose: ImageButton
    private lateinit var categorySpinner: Spinner
    private lateinit var accountSpinner: Spinner
    private lateinit var radioExpense: RadioButton
    private lateinit var radioIncome: RadioButton
    private lateinit var radioSaving: RadioButton
    private lateinit var amountEditText: TextInputEditText
    private lateinit var clearAmountBtn: TextView
    private lateinit var dateTextView: TextView
    private lateinit var addInvoiceBtn: TextView
    private lateinit var notesEditText: TextInputEditText
    private lateinit var addTransactionBtn: MaterialButton

    // Data
    private var selectedDate: Calendar = Calendar.getInstance()
    private var invoiceUri: Uri? = null
    private val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Data collections
    private val accounts = mutableListOf<Account>()
    private val categories = mutableListOf<Category>()
    private val filteredCategories = mutableListOf<Category>()

    // Invoice launcher
    private val getInvoice = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            invoiceUri = it
            addInvoiceBtn.text = "Invoice Added"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please log in to add a transaction", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupUIListeners()
        
        // Load real data from Firestore
        fetchAccountsAndCategories()
    }

    private fun initializeViews() {
        btnClose = findViewById(R.id.btn_close)
        categorySpinner = findViewById(R.id.category_spinner)
        accountSpinner = findViewById(R.id.account_spinner)
        radioExpense = findViewById(R.id.radio_expense)
        radioIncome = findViewById(R.id.radio_income)
        radioSaving = findViewById(R.id.radio_saving)
        amountEditText = findViewById(R.id.amount_edit_text)
        clearAmountBtn = findViewById(R.id.clear_amount_btn)
        dateTextView = findViewById(R.id.date_text_view)
        addInvoiceBtn = findViewById(R.id.add_invoice_btn)
        notesEditText = findViewById(R.id.notes_edit_text)
        addTransactionBtn = findViewById(R.id.add_transaction_btn)

        dateTextView.text = dateFormatter.format(selectedDate.time)
        
        // Set default radio button
        radioExpense.isChecked = true
    }

    private fun setupUIListeners() {
        btnClose.setOnClickListener { finish() }
        clearAmountBtn.setOnClickListener { amountEditText.setText("") }
        addInvoiceBtn.setOnClickListener { getInvoice.launch("image/*") }
        addTransactionBtn.setOnClickListener { saveTransaction() }

        radioExpense.setOnClickListener { filterCategories(false) }
        radioIncome.setOnClickListener { filterCategories(true) }
        radioSaving.setOnClickListener { filterCategories(false) } // Savings typically use expense categories
        
        setupDatePicker()
    }

    private fun setupDatePicker() {
        dateTextView.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    dateTextView.text = dateFormatter.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun fetchAccountsAndCategories() {
        showLoading(true)
        
        val userId = auth.currentUser?.uid ?: return
        
        // Fetch accounts
        firestore.collection("accounts")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                accounts.clear()
                for (document in documents) {
                    try {
                        val account = Account()
                        account.accountId = document.getString("account_id") ?: ""
                        account.userId = document.getString("user_id") ?: ""
                        account.accountName = document.getString("account_name") ?: ""
                        account.balance = document.getDouble("balance") ?: 0.0
                        account.accountType = document.getString("account_type") ?: ""
                        account.accountImageUrl = document.getString("account_image_url")
                        account.note = document.getString("note")
                        account.createdAt = document.getLong("created_at") ?: 0L
                        account.updatedAt = document.getLong("updated_at") ?: 0L
                        
                        accounts.add(account)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing account document", e)
                    }
                }
                
                // Setup account spinner
                setupAccountSpinner()
                
                // After accounts are loaded, fetch categories
                fetchCategories(userId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching accounts", e)
                Toast.makeText(this, "Failed to load accounts: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    private fun fetchCategories(userId: String) {
        firestore.collection("categories")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                categories.clear()
                for (document in documents) {
                    try {
                        val category = Category()
                        category.categoryId = document.getString("category_id") ?: ""
                        category.userId = document.getString("user_id") ?: ""
                        category.name = document.getString("name") ?: ""
                        category.iconBase64 = document.getString("icon_base64") ?: ""
                        category.isIncome = document.getBoolean("is_income") ?: false
                        category.createdAt = document.getLong("created_at") ?: 0L
                        category.updatedAt = document.getLong("updated_at") ?: 0L
                        
                        categories.add(category)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category document", e)
                    }
                }
                
                // Filter categories based on currently selected transaction type
                filterCategories(radioIncome.isChecked)
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching categories", e)
                Toast.makeText(this, "Failed to load categories: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    private fun setupAccountSpinner() {
        if (accounts.isEmpty()) {
            Toast.makeText(this, "You need to create an account first", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accounts.map { it.accountName }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        accountSpinner.adapter = accountAdapter
    }
    
    private fun setupCategorySpinner() {
        if (filteredCategories.isEmpty()) {
            Toast.makeText(this, "You need to create categories first", Toast.LENGTH_LONG).show()
            // Don't finish, allow user to continue but warn them
        }
        
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filteredCategories.map { it.name }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = categoryAdapter
    }

    private fun filterCategories(isIncome: Boolean) {
        filteredCategories.clear()
        filteredCategories.addAll(categories.filter { it.isIncome == isIncome })
        setupCategorySpinner()
    }

    private fun showLoading(show: Boolean) {
        // Use button text to indicate loading state
        addTransactionBtn.isEnabled = !show
        addTransactionBtn.text = if (show) "Loading..." else "Add Transaction"
    }

    private fun saveTransaction() {
        val amountStr = amountEditText.text.toString()
        val amount = amountStr.toDoubleOrNull()

        if (amountStr.isEmpty() || amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val accountIndex = accountSpinner.selectedItemPosition
        val categoryIndex = categorySpinner.selectedItemPosition

        if (accountIndex < 0 || accountIndex >= accounts.size) {
            Toast.makeText(this, "Please select a valid account", Toast.LENGTH_SHORT).show()
            return
        }

        if (filteredCategories.isEmpty()) {
            Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryIndex < 0 || categoryIndex >= filteredCategories.size) {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedAccount = accounts[accountIndex]
        val selectedCategory = filteredCategories[categoryIndex]
        val notes = notesEditText.text.toString()
        
        // Determine transaction type
        val transactionType = when {
            radioExpense.isChecked -> "expense"
            radioIncome.isChecked -> "income"
            radioSaving.isChecked -> "saving"
            else -> "expense"
        }

        showLoading(true)
        
        // If there's an invoice, process it
        if (invoiceUri != null) {
            processInvoiceAndCreateTransaction(
                selectedAccount, 
                selectedCategory, 
                amount, 
                transactionType, 
                notes
            )
        } else {
            // No invoice, create transaction directly
            createTransaction(
                selectedAccount, 
                selectedCategory, 
                amount, 
                transactionType, 
                notes, 
                null
            )
        }
    }

    private fun processInvoiceAndCreateTransaction(
        account: Account,
        category: Category,
        amount: Double,
        type: String,
        notes: String
    ) {
        // Show loading indicator
        showLoading(true)
        
        // Make sure we have a valid URI
        if (invoiceUri == null) {
            Log.e(TAG, "Invoice URI is null")
            Toast.makeText(this, "Invalid invoice image", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }
        
        try {
            // Convert the image to a base64 string
            val base64Image = convertUriToBase64(invoiceUri!!)
            
            if (base64Image == null) {
                Toast.makeText(this, "Failed to process the invoice image", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return
            }
            
            // Create the transaction with the base64 image string
            createTransaction(account, category, amount, type, notes, base64Image)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing invoice", e)
            Toast.makeText(this, "Error processing invoice: ${e.message}", Toast.LENGTH_SHORT).show()
            showLoading(false)
        }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            // Open input stream from URI
            val inputStream = contentResolver.openInputStream(uri)
            
            // Read the input stream into a bitmap
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Resize the bitmap to reduce file size
            val resizedBitmap = resizeBitmap(originalBitmap, 800, 800)
            
            // Convert bitmap to base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            
            // Check if the size is within limits (1MB)
            if (byteArray.size > MAX_IMAGE_SIZE) {
                Toast.makeText(this, "Image is too large (max 1MB)", Toast.LENGTH_SHORT).show()
                return null
            }
            
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d(TAG, "Converted image to base64 string of length: ${base64String.length}")
            
            base64String
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to base64", e)
            null
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        
        val scale = scaleWidth.coerceAtMost(scaleHeight)
        
        val matrix = android.graphics.Matrix()
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun createTransaction(
        account: Account,
        category: Category,
        amount: Double,
        type: String,
        notes: String,
        invoiceBase64: String?
    ) {
        val userId = auth.currentUser?.uid ?: return
        val transactionId = UUID.randomUUID().toString()
        
        // Create transaction data map with all fields
        val transactionData = mutableMapOf(
            "transaction_id" to transactionId,
            "user_id" to userId,
            "account_id" to account.accountId,
            "category_id" to category.categoryId,
            "type" to type,
            "amount" to amount,
            "date" to Timestamp(selectedDate.time),
            "created_at" to Timestamp.now(),
            "updated_at" to Timestamp.now(),
            "notes" to notes,
            "is_deleted" to false
        )
        
        // Add invoice if available
        if (!invoiceBase64.isNullOrEmpty()) {
            transactionData["invoice_base64"] = invoiceBase64
        }
        
        // Save to Firestore
        firestore.collection("transactions")
            .document(transactionId)
            .set(transactionData)
            .addOnSuccessListener {
                // Update account balance
                updateAccountBalance(account, amount, type)
                
                // Update budget if this is an expense transaction
                if (type == "expense") {
                    BudgetUtils.updateBudgetSpending(userId, category.categoryId, amount)
                }
                
                // Send notification for new transaction
                val transaction = Transaction(
                    transactionId = transactionId,
                    userId = userId,
                    accountId = account.accountId,
                    categoryId = category.categoryId,
                    type = type,
                    amount = amount,
                    date = Timestamp(selectedDate.time),
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    notes = notes,
                    isDeleted = false
                )
                (application as FlowMoneyApplication).notificationHelper.notifyTransactionAdded(transaction)
                
                // Check if the transaction caused any budget to be exceeded
                if (type == "expense") {
                    checkBudgetLimits(category.categoryId, amount)
                }
                
                Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving transaction", e)
                Toast.makeText(this, "Failed to save transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    private fun updateAccountBalance(account: Account, amount: Double, type: String) {
        val changeAmount = when (type) {
            "income" -> amount
            "expense", "saving" -> -amount
            else -> 0.0
        }
        
        val newBalance = account.balance + changeAmount
        
        firestore.collection("accounts")
            .document(account.accountId)
            .update(
                mapOf(
                    "balance" to newBalance,
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating account balance", e)
                // Don't show error to user, transaction was still saved
            }
    }

    /**
     * Check if the transaction has caused any budget limit to be exceeded
     */
    private fun checkBudgetLimits(categoryId: String, amount: Double) {
        val userId = auth.currentUser?.uid ?: return
        
        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Check if there's a budget for this category and if it's exceeded
        firestore.collection("budgets")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("category_id", categoryId)
            .whereEqualTo("month", currentMonth)
            .whereEqualTo("year", currentYear)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Found a budget, check if it's exceeded
                    val budget = documents.documents[0]
                    val limit = budget.getDouble("limit") ?: 0.0
                    val spent = budget.getDouble("spent") ?: 0.0
                    
                    // If budget is exceeded, send notification
                    if (spent > limit) {
                        // Get category name
                        firestore.collection("categories")
                            .document(categoryId)
                            .get()
                            .addOnSuccessListener { categoryDoc ->
                                if (categoryDoc.exists()) {
                                    val categoryName = categoryDoc.getString("name") ?: "Unknown"
                                    
                                    // Send notification
                                    (application as FlowMoneyApplication).notificationHelper
                                        .notifyBudgetExceeded(categoryName, limit, spent)
                                    
                                    Log.d(TAG, "Budget exceeded notification sent for $categoryName")
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking budget limits", e)
            }
    }
}