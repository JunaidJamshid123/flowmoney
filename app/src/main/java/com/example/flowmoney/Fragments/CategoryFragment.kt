package com.example.flowmoney.Fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Adapters.CategoryAdapter
import com.example.flowmoney.Adapters.CategoryIconAdapter
import com.example.flowmoney.Models.Category
import com.example.flowmoney.R
import com.example.flowmoney.utlities.FirestoreUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import android.widget.RadioGroup
import android.widget.RadioButton

class CategoryFragment : Fragment() {
    private val TAG = "CategoryFragment"
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var balanceTextView: TextView
    private lateinit var addCategoryButton: FloatingActionButton
    private lateinit var backButton: CardView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()
    
    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is logged in
        if (auth.currentUser == null) {
            Log.e(TAG, "No user is logged in")
            Toast.makeText(context, "Please log in to view categories", Toast.LENGTH_SHORT).show()
            activity?.onBackPressed()
            return view
        }

        // Initialize views
        recyclerView = view.findViewById(R.id.rv_categories)
        balanceTextView = view.findViewById(R.id.tv_balance)
        addCategoryButton = view.findViewById(R.id.fab_add_category)
        backButton = view.findViewById(R.id.cv_back_button)


        // Set up RecyclerView
        setupRecyclerView()

        // Set click listener for add category button
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }

        // Set click listener for back button
        backButton.setOnClickListener {
            activity?.onBackPressed()
        }

        // Load categories from Firestore
        loadCategories()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        categoryAdapter = CategoryAdapter(categories) { category ->
            // Handle category click - can be implemented later
            Toast.makeText(context, "Selected: ${category.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = categoryAdapter
    }

    private fun loadCategories() {
        // Get current user ID
        val userId = auth.currentUser?.uid ?: return
        
        FirestoreUtils.getAllCategories(
            userId = userId,
            onSuccess = { fetchedCategories ->
                categories.clear()
                categories.addAll(fetchedCategories)
                categoryAdapter.notifyDataSetChanged()

                if (categories.isEmpty()) {
                    // Show empty state if needed
                    Log.d(TAG, "No categories found")
                }
            },
            onFailure = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showAddCategoryDialog() {
        try {
            // Get current user ID
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "You must be logged in to add categories", Toast.LENGTH_SHORT).show()
                return
            }
            
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.activity_add_new_category)

            // Make dialog background transparent
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Set dialog width to match parent
            dialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            // Set gravity to center
            dialog.window?.setGravity(Gravity.CENTER)

            // Set animation
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Initialize views in the dialog
            val etName = dialog.findViewById<TextInputEditText>(R.id.etName)
            val rvCategoryIcons = dialog.findViewById<RecyclerView>(R.id.rvCategoryIcons)
            val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
            val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSave)
            val radioGroupCategoryType = dialog.findViewById<RadioGroup>(R.id.radioGroupCategoryType)
            val radioBtnExpense = dialog.findViewById<RadioButton>(R.id.radioBtnExpense)
            val radioBtnIncome = dialog.findViewById<RadioButton>(R.id.radioBtnIncome)
            val radioBtnSaving = dialog.findViewById<RadioButton>(R.id.radioBtnSaving)
            
            // Default category type
            var categoryType = "expense"
            
            // Radio button listener for category type
            radioGroupCategoryType.setOnCheckedChangeListener { group, checkedId ->
                categoryType = when (checkedId) {
                    R.id.radioBtnExpense -> "expense"
                    R.id.radioBtnIncome -> "income"
                    R.id.radioBtnSaving -> "saving"
                    else -> "expense"
                }
                Log.d(TAG, "Selected category type: $categoryType")
            }
            
            // Set default selection
            radioBtnExpense.isChecked = true

            // Variable to store selected icon
            var selectedIconResId = R.drawable.cash // Default icon

            // Setup icons RecyclerView
            setupIconsRecyclerView(dialog, rvCategoryIcons) { iconId ->
                selectedIconResId = iconId
            }

            // Set click listeners for buttons
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val categoryName = etName.text.toString().trim()

                if (categoryName.isEmpty()) {
                    etName.error = "Please enter a category name"
                    return@setOnClickListener
                }

                // Show progress indicator
                btnSave.isEnabled = false
                btnCancel.isEnabled = false

                // Save category to Firestore
                context?.let { ctx ->
                    FirestoreUtils.saveCategory(
                        context = ctx,
                        name = categoryName,
                        iconResourceId = selectedIconResId,
                        isIncome = categoryType == "income",
                        categoryType = categoryType,
                        userId = userId,
                        onSuccess = { newCategory ->
                            // Add to local list and notify adapter
                            categories.add(newCategory)
                            categoryAdapter.notifyItemInserted(categories.size - 1)

                            // Show confirmation
                            Toast.makeText(context, "Category saved: ${newCategory.name}", Toast.LENGTH_SHORT).show()

                            // Close dialog
                            dialog.dismiss()
                        },
                        onFailure = { errorMessage ->
                            btnSave.isEnabled = true
                            btnCancel.isEnabled = true

                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupIconsRecyclerView(dialog: Dialog, recyclerView: RecyclerView, onIconSelected: (Int) -> Unit) {
        try {
            // List of all category icons
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

            // Create and set layout manager
            val layoutManager = GridLayoutManager(dialog.context, 4)
            recyclerView.layoutManager = layoutManager

            // Set fixed size for performance
            recyclerView.setHasFixedSize(true)

            // Create and set adapter
            val adapter = CategoryIconAdapter(categoryIcons) { iconResId ->
                onIconSelected(iconResId)
                Log.d(TAG, "Icon selected: $iconResId")
            }

            // Set adapter
            recyclerView.adapter = adapter

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up icons RecyclerView: ${e.message}", e)
            Toast.makeText(context, "Error loading icons: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CategoryFragment()
    }
}