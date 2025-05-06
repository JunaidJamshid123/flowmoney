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
import com.example.flowmoney.Adapters.CategoryIconAdapter
import com.example.flowmoney.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class CategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var balanceTextView: TextView
    private lateinit var addCategoryButton: FloatingActionButton
    private lateinit var backButton: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.rv_categories)
        balanceTextView = view.findViewById(R.id.tv_balance)
        addCategoryButton = view.findViewById(R.id.fab_add_category)
        backButton = view.findViewById(R.id.cv_back_button)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        // You would set your adapter here later
        // recyclerView.adapter = yourAdapter

        // Set click listener for add category button
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }

        // Set click listener for back button
        backButton.setOnClickListener {
            activity?.onBackPressed()
        }

        return view
    }

    private fun showAddCategoryDialog() {
        try {
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

                // Save the category with the selected icon
                saveCategory(categoryName, false, selectedIconResId)

                // Show confirmation
                Toast.makeText(context, "Category saved: $categoryName", Toast.LENGTH_SHORT).show()

                // Close dialog
                dialog.dismiss()

                // Refresh categories list
                refreshCategoriesList()
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e("CategoryFragment", "Error showing dialog: ${e.message}", e)
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
                Log.d("CategoryFragment", "Icon selected: $iconResId")
            }

            // Set adapter
            recyclerView.adapter = adapter

        } catch (e: Exception) {
            Log.e("CategoryFragment", "Error setting up icons RecyclerView: ${e.message}", e)
            Toast.makeText(context, "Error loading icons: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCategory(name: String, isIncome: Boolean, iconId: Int) {
        // TODO: Implement saving the category to your data source (Firebase, etc.)
        Log.d("CategoryFragment", "Saving category: $name with icon: $iconId")
    }

    private fun refreshCategoriesList() {
        // TODO: Refresh the RecyclerView with updated data
        Log.d("CategoryFragment", "Refreshing categories list")
    }

    companion object {
        @JvmStatic
        fun newInstance() = CategoryFragment()
    }
}