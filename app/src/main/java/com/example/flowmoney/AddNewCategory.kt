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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Adapters.CategoryIconAdapter
import com.example.flowmoney.Models.Category
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.RadioButton
import android.widget.RadioGroup
import java.io.ByteArrayOutputStream
import java.util.UUID

class AddNewCategory : AppCompatActivity() {
    companion object {
        private const val TAG = "AddNewCategory"
    }

    private lateinit var etName: TextInputEditText
    private lateinit var rvCategoryIcons: RecyclerView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var radioGroupCategoryType: RadioGroup
    private lateinit var radioBtnExpense: RadioButton
    private lateinit var radioBtnIncome: RadioButton
    private lateinit var radioBtnSaving: RadioButton
    private lateinit var progressBar: View

    private var selectedIconResId: Int = R.drawable.cash // Default icon
    private var categoryType: String = "expense" // Default to expense category

    // Firebase components
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_category)

        try {
            // Initialize Firebase
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            // Check if user is logged in
            if (auth.currentUser == null) {
                Log.e(TAG, "No user is logged in, redirecting")
                Toast.makeText(this, "Please log in to add a category", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Initialize views
            etName = findViewById(R.id.etName)
            rvCategoryIcons = findViewById(R.id.rvCategoryIcons)
            btnCancel = findViewById(R.id.btnCancel)
            btnSave = findViewById(R.id.btnSave)
            radioGroupCategoryType = findViewById(R.id.radioGroupCategoryType)
            radioBtnExpense = findViewById(R.id.radioBtnExpense)
            radioBtnIncome = findViewById(R.id.radioBtnIncome)
            radioBtnSaving = findViewById(R.id.radioBtnSaving)
            progressBar = findViewById(R.id.progressBar)

            // Set up radio group listener
            radioGroupCategoryType.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.radioBtnExpense -> categoryType = "expense"
                    R.id.radioBtnIncome -> categoryType = "income"
                    R.id.radioBtnSaving -> categoryType = "saving"
                }
                Log.d(TAG, "Category type selected: $categoryType")
            }
            
            // Set default selection
            radioBtnExpense.isChecked = true

            // Setup RecyclerView with layout manager
            val layoutManager = GridLayoutManager(this, 4) // 4 columns
            rvCategoryIcons.layoutManager = layoutManager

            // Set fixed size to improve performance
            rvCategoryIcons.setHasFixedSize(true)

            // Then setup adapter
            setupIconsRecyclerView()
            setupButtons()

        } catch (e: Exception) {
            val errorMsg = "Error initializing AddNewCategory: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupIconsRecyclerView() {
        try {
            // List of all category icons - Add all your drawable resources here
            val categoryIcons = listOf(
                R.drawable.cash,
                R.drawable.onlineshopping,
                R.drawable.restaurantt,
                R.drawable.income,
                R.drawable.shoppingg,
                R.drawable.wallet,
                R.drawable.saving,
                R.drawable.savinggg,
                R.drawable.gas,
                R.drawable.wine,
                R.drawable.won
            )

            Log.d(TAG, "Setting up RecyclerView with ${categoryIcons.size} icons")

            // Create adapter with proper selection handling
            val adapter = CategoryIconAdapter(categoryIcons) { iconResId ->
                // Track the selected icon
                selectedIconResId = iconResId
                Log.d(TAG, "Icon selected: $iconResId")
                
                // Show visual feedback (we'll update the adapter to highlight selected icon)
                (rvCategoryIcons.adapter as CategoryIconAdapter).setSelectedIcon(iconResId)
            }

            // Set adapter
            rvCategoryIcons.adapter = adapter

            // Log success
            Log.d(TAG, "RecyclerView adapter successfully set")

        } catch (e: Exception) {
            val errorMsg = "Error setting up RecyclerView: ${e.message}"
            Log.e(TAG, errorMsg, e)
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val categoryName = etName.text.toString().trim()
            if (categoryName.isEmpty()) {
                etName.error = "Please enter a category name"
                return@setOnClickListener
            }

            // Show progress
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false
            btnCancel.isEnabled = false

            try {
                // Get current user ID
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Toast.makeText(this, "Please log in to add categories", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Convert selected icon to Base64
                val iconBase64 = getIconAsBase64(this, selectedIconResId)

                // Create category object
                val categoryId = UUID.randomUUID().toString()
                val category = Category(
                    categoryId = categoryId,
                    userId = userId,
                    name = categoryName,
                    iconBase64 = iconBase64,
                    isIncome = categoryType == "income"
                )

                // Save to Firestore
                firestore.collection("categories")
                    .document(categoryId)
                    .set(category)
                    .addOnSuccessListener {
                        Log.d(TAG, "Category added successfully")
                        
                        // Send notification for new category
                        (application as FlowMoneyApplication).notificationHelper.notifyCategoryAdded(categoryName)
                        
                        Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error adding category", e)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                        btnCancel.isEnabled = true
                        progressBar.visibility = View.GONE
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving category", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun getIconAsBase64(context: Context, resourceId: Int): String {
        try {
            Log.d(TAG, "Getting drawable resource for ID: $resourceId")

            // Get the drawable safely
            val drawable = ContextCompat.getDrawable(context, resourceId)
                ?: throw IllegalStateException("Could not load drawable for resource ID: $resourceId")

            // Convert drawable to bitmap
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
}