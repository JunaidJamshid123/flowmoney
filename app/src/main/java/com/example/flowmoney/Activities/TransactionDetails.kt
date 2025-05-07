package com.example.flowmoney.Activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.flowmoney.Models.Account
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TransactionDetails : AppCompatActivity() {
    companion object {
        private const val TAG = "TransactionDetails"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
    }

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var profileImage: CircleImageView
    private lateinit var transactionTypeText: TextView
    private lateinit var amountText: TextView
    private lateinit var statusText: TextView
    private lateinit var fromText: TextView
    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var earningsText: TextView
    private lateinit var feeText: TextView
    private lateinit var totalText: TextView
    private lateinit var downloadReceiptButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var invoiceContainer: LinearLayout
    private lateinit var invoiceImage: ImageView

    // Data
    private var transaction: Transaction? = null
    private var account: Account? = null
    private var category: Category? = null

    // Firestore
    private lateinit var firestore: FirebaseFirestore

    // Request permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            generateAndSavePdf()
        } else {
            Toast.makeText(this, "Storage permission is required to save PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_details)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initViews()

        // Setup click listeners
        setupClickListeners()

        // Get transaction ID from intent
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        if (transactionId.isNullOrEmpty()) {
            Toast.makeText(this, "Transaction details not available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading state
        showLoading(true)

        // Load transaction details
        loadTransactionDetails(transactionId)
    }

    private fun initViews() {
        backButton = findViewById(R.id.btn_back)
        profileImage = findViewById(R.id.profile_image)
        transactionTypeText = findViewById(R.id.tv_transaction_type)
        amountText = findViewById(R.id.tv_amount)
        
        // Get status value (this is a nested TextView)
        val detailsContainer = findViewById<LinearLayout>(R.id.transaction_details_container)
        statusText = detailsContainer.findViewById(R.id.tv_status_value)
        fromText = detailsContainer.findViewById(R.id.tv_from_value)
        timeText = detailsContainer.findViewById(R.id.tv_time_value)
        dateText = detailsContainer.findViewById(R.id.tv_date_value)
        
        // Get earnings values (also nested)
        val earningsContainer = findViewById<LinearLayout>(R.id.earnings_container)
        earningsText = earningsContainer.findViewById(R.id.tv_earnings_value)
        feeText = earningsContainer.findViewById(R.id.tv_fee_value)
        totalText = earningsContainer.findViewById(R.id.tv_total_value)
        
        // Get invoice views
        invoiceContainer = findViewById(R.id.invoice_container)
        invoiceImage = findViewById(R.id.invoice_image)
        
        downloadReceiptButton = findViewById(R.id.btn_download_receipt)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        downloadReceiptButton.setOnClickListener {
            checkPermissionAndGeneratePdf()
        }
    }

    private fun loadTransactionDetails(transactionId: String) {
        firestore.collection("transactions")
            .document(transactionId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        // Create transaction object from document
                        val data = document.data ?: return@addOnSuccessListener
                        transaction = Transaction.fromMap(data, document.id)
                        
                        // Load associated account and category
                        loadAccountDetails(transaction?.accountId ?: "")
                        loadCategoryDetails(transaction?.categoryId ?: "")
                        
                        // Update UI with transaction details
                        updateTransactionUI()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                        Toast.makeText(this, "Failed to load transaction details", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                    }
                } else {
                    Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading transaction", e)
                Toast.makeText(this, "Failed to load transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
                finish()
            }
    }

    private fun loadAccountDetails(accountId: String) {
        if (accountId.isEmpty()) {
            // Skip loading if no account ID
            return
        }

        firestore.collection("accounts")
            .document(accountId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        account = Account()
                        account?.accountId = document.getString("account_id") ?: ""
                        account?.userId = document.getString("user_id") ?: ""
                        account?.accountName = document.getString("account_name") ?: ""
                        account?.balance = document.getDouble("balance") ?: 0.0
                        account?.accountType = document.getString("account_type") ?: ""
                        account?.accountImageUrl = document.getString("account_image_url")
                        account?.note = document.getString("note")
                        
                        // Update UI with account details
                        updateAccountUI()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing account", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading account", e)
            }
    }

    private fun loadCategoryDetails(categoryId: String) {
        if (categoryId.isEmpty()) {
            // Skip loading if no category ID
            return
        }

        firestore.collection("categories")
            .document(categoryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        category = Category()
                        category?.categoryId = document.getString("category_id") ?: ""
                        category?.userId = document.getString("user_id") ?: ""
                        category?.name = document.getString("name") ?: ""
                        category?.iconBase64 = document.getString("icon_base64") ?: ""
                        category?.isIncome = document.getBoolean("is_income") ?: false
                        
                        // Update UI with category details
                        updateCategoryUI()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category", e)
                    }
                }
                
                // Hide loading regardless of success (this is the last data to load)
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading category", e)
                showLoading(false)
            }
    }

    private fun updateTransactionUI() {
        transaction?.let { tx ->
            // Set transaction type with appropriate color
            val typeText = tx.type.capitalize(Locale.getDefault())
            transactionTypeText.text = typeText
            
            // Set color based on transaction type
            val typeColor = when (tx.type) {
                "income" -> ContextCompat.getColor(this, R.color.income_green)
                "expense" -> ContextCompat.getColor(this, R.color.expense_red)
                "saving" -> ContextCompat.getColor(this, R.color.saving_blue)
                else -> ContextCompat.getColor(this, R.color.black)
            }
            transactionTypeText.setTextColor(typeColor)
            statusText.setTextColor(typeColor)
            statusText.text = typeText
            
            // Set transaction amount
            val formattedAmount = String.format("$%,.2f", tx.amount)
            amountText.text = formattedAmount
            
            // Set date and time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = tx.getDateAsDate()
            
            dateText.text = dateFormat.format(date)
            timeText.text = timeFormat.format(date)
            
            // Set earnings/fees section (simplify for now)
            val fee = 0.0  // Placeholder, could be calculated based on business logic
            earningsText.text = String.format("$%,.2f", tx.amount)
            feeText.text = String.format("- $%,.2f", fee)
            totalText.text = String.format("$%,.2f", tx.amount - fee)
            
            // Display invoice if available
            if (tx.hasInvoice()) {
                if (!tx.invoiceBase64.isNullOrEmpty()) {
                    // Display base64 image
                    try {
                        val decodedBytes = Base64.decode(tx.invoiceBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        invoiceImage.setImageBitmap(bitmap)
                        invoiceContainer.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Log.e(TAG, "Error displaying invoice image", e)
                        invoiceContainer.visibility = View.GONE
                    }
                } else if (!tx.invoiceUrl.isNullOrEmpty()) {
                    // For URL-based images (legacy), we could add Glide or Picasso implementation here
                    // This is a placeholder for future implementation
                    Toast.makeText(this, "URL-based invoices not supported in this version", Toast.LENGTH_SHORT).show()
                    invoiceContainer.visibility = View.GONE
                }
            } else {
                invoiceContainer.visibility = View.GONE
            }
        }
    }

    private fun updateAccountUI() {
        account?.let { acc ->
            // Set account name as source/destination
            fromText.text = acc.accountName
        }
    }

    private fun updateCategoryUI() {
        category?.let { cat ->
            // Set category image if available
            if (!cat.iconBase64.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(cat.iconBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding category icon", e)
                    setDefaultProfileImage()
                }
            } else {
                setDefaultProfileImage()
            }
        }
    }

    private fun setDefaultProfileImage() {
        // Set default profile image based on transaction type
        val iconResource = when (transaction?.type) {
            "income" -> R.drawable.income
            "expense" -> R.drawable.shoppingg
            "saving" -> R.drawable.saving
            else -> R.drawable.cash
        }
        profileImage.setImageResource(iconResource)
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun checkPermissionAndGeneratePdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    generateAndSavePdf()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        } else {
            generateAndSavePdf()
        }
    }

    private fun generateAndSavePdf() {
        if (transaction == null) {
            Toast.makeText(this, "Transaction details not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create PDF document
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw receipt header
            val paint = android.graphics.Paint().apply {
                color = Color.BLACK
                textSize = 15f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            canvas.drawText("FLOW MONEY", pageInfo.pageWidth / 2f, 80f, paint)
            
            paint.textSize = 12f
            canvas.drawText("TRANSACTION RECEIPT", pageInfo.pageWidth / 2f, 100f, paint)

            // Draw line
            val linePaint = android.graphics.Paint().apply {
                color = Color.GRAY
                strokeWidth = 1f
            }
            canvas.drawLine(50f, 120f, pageInfo.pageWidth - 50f, 120f, linePaint)

            // Draw transaction details
            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.textSize = 12f
            
            val tx = transaction!!
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            
            var y = 160f
            val leftMargin = 70f
            val columnWidth = 200f
            
            // Draw each detail row
            drawPdfRow(canvas, "Transaction Type:", tx.type.capitalize(Locale.getDefault()), leftMargin, y, paint)
            y += 25f
            
            drawPdfRow(canvas, "Amount:", String.format("$%,.2f", tx.amount), leftMargin, y, paint)
            y += 25f
            
            drawPdfRow(canvas, "Date:", dateFormat.format(tx.getDateAsDate()), leftMargin, y, paint)
            y += 25f
            
            drawPdfRow(canvas, "Account:", account?.accountName ?: "Unknown", leftMargin, y, paint)
            y += 25f
            
            drawPdfRow(canvas, "Category:", category?.name ?: "Unknown", leftMargin, y, paint)
            y += 25f
            
            if (!tx.notes.isNullOrEmpty()) {
                drawPdfRow(canvas, "Notes:", tx.notes, leftMargin, y, paint)
                y += 25f
            }
            
            drawPdfRow(canvas, "Transaction ID:", tx.transactionId, leftMargin, y, paint)
            y += 40f
            
            // Add invoice image if available
            if (tx.hasInvoice() && !tx.invoiceBase64.isNullOrEmpty()) {
                try {
                    canvas.drawText("Invoice:", leftMargin, y, paint)
                    y += 15f
                    
                    val decodedBytes = Base64.decode(tx.invoiceBase64, Base64.DEFAULT)
                    val invoiceBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    
                    // Scale the bitmap to fit the PDF
                    val maxWidth = pageInfo.pageWidth - (leftMargin * 2)
                    val maxHeight = 200f // Maximum height for the invoice image
                    
                    val originalWidth = invoiceBitmap.width
                    val originalHeight = invoiceBitmap.height
                    
                    val ratio = Math.min(maxWidth / originalWidth, maxHeight / originalHeight)
                    val scaledWidth = originalWidth * ratio
                    val scaledHeight = originalHeight * ratio
                    
                    // Center the image horizontally
                    val left = (pageInfo.pageWidth - scaledWidth) / 2
                    
                    // Draw the image
                    canvas.drawBitmap(
                        invoiceBitmap,
                        android.graphics.Rect(0, 0, originalWidth, originalHeight),
                        android.graphics.RectF(left, y, left + scaledWidth, y + scaledHeight),
                        null
                    )
                    
                    y += scaledHeight + 25f
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding invoice to PDF", e)
                    // Skip invoice but continue with the PDF
                    y += 10f
                }
            }
            
            // Draw footer
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("Thank you for using Flow Money", pageInfo.pageWidth / 2f, y, paint)
            
            // Finish drawing
            pdfDocument.finishPage(page)

            // Save PDF
            val fileName = "FlowMoney_Receipt_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + fileName
            val file = File(filePath)
            
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            
            Toast.makeText(this, "Receipt saved to Downloads folder", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            Toast.makeText(this, "Failed to generate receipt: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawPdfRow(canvas: android.graphics.Canvas, label: String, value: String, x: Float, y: Float, paint: android.graphics.Paint) {
        val labelPaint = android.graphics.Paint(paint).apply {
            isFakeBoldText = true
        }
        
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x + 200f, y, paint)
    }
}