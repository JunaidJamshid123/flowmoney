package com.example.flowmoney.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Models.Budget
import com.example.flowmoney.Models.Category
import com.example.flowmoney.R
import com.example.flowmoney.SetBudget
import de.hdodenhof.circleimageview.CircleImageView
import java.text.NumberFormat
import java.util.Locale

class BudgetAdapter(
    private val context: Context,
    private var budgets: List<Budget> = listOf(),
    private var categories: Map<String, Category> = mapOf()
) : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val budgetName: TextView = itemView.findViewById(R.id.budget_name)
        val budgetLimit: TextView = itemView.findViewById(R.id.budget_limit)
        val budgetSpent: TextView = itemView.findViewById(R.id.budget_spent)
        val budgetRemaining: TextView = itemView.findViewById(R.id.budget_remaining)
        val budgetProgress: ProgressBar = itemView.findViewById(R.id.budget_progress)
        val setBudgetText: TextView = itemView.findViewById(R.id.set_budget_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = budgets.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val budget = budgets[position]
        val category = categories[budget.categoryId]
        
        // Set category name
        holder.budgetName.text = category?.name ?: "Unknown Category"
        
        // Set budget values with proper formatting
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        holder.budgetLimit.text = currencyFormat.format(budget.limit)
        holder.budgetSpent.text = currencyFormat.format(budget.spent)
        holder.budgetRemaining.text = currencyFormat.format(budget.getRemaining())
        
        // Set progress
        holder.budgetProgress.progress = budget.getProgress()
        
        // Set colors based on remaining amount
        if (budget.isExceeded()) {
            holder.budgetRemaining.setTextColor(context.getColor(R.color.expense_red))
        } else {
            holder.budgetRemaining.setTextColor(context.getColor(R.color.income_green))
        }
        
        // Set category icon if available
        if (category != null && category.iconBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(category.iconBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.profileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.default_profile)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.default_profile)
        }
        
        // Set click listener for set budget text
        holder.setBudgetText.setOnClickListener {
            if (category != null) {
                val intent = Intent(context, SetBudget::class.java)
                intent.putExtra("category_id", category.categoryId)
                intent.putExtra("category_name", category.name)
                intent.putExtra("category_icon", category.iconBase64)
                intent.putExtra("is_income", category.isIncome)
                intent.putExtra("budget_id", budget.budgetId)
                intent.putExtra("budget_limit", budget.limit)
                intent.putExtra("edit_mode", true)
                context.startActivity(intent)
            }
        }
        
        // Set click listener for the whole item (same as set budget text)
        holder.itemView.setOnClickListener {
            holder.setBudgetText.performClick()
        }
    }
    
    fun updateData(newBudgets: List<Budget>, newCategories: Map<String, Category>) {
        budgets = newBudgets
        categories = newCategories
        notifyDataSetChanged()
    }
} 