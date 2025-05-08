package com.example.flowmoney.Adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying transactions in the Analysis screen
 */
class AnalysisTransactionAdapter(
    private var transactions: List<Transaction> = emptyList(),
    private var categories: Map<String, Category> = emptyMap(),
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<AnalysisTransactionAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "AnalysisTransactionAdapter"
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryIcon: ImageView = itemView.findViewById(R.id.image_category)
        val transactionTitle: TextView = itemView.findViewById(R.id.text_transaction_title)
        val transactionCategory: TextView = itemView.findViewById(R.id.text_transaction_category)
        val transactionDate: TextView = itemView.findViewById(R.id.text_transaction_date)
        val transactionAmount: TextView = itemView.findViewById(R.id.text_transaction_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val category = categories[transaction.categoryId]
        
        // Set transaction title (using category name or notes if available)
        val title = if (!transaction.notes.isNullOrEmpty()) {
            transaction.notes
        } else {
            category?.name ?: "Unknown"
        }
        holder.transactionTitle.text = title
        
        // Set category name
        holder.transactionCategory.text = category?.name ?: "Unknown Category"
        
        // Set date in "Today, 2:30 PM" format
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(transaction.getDateAsDate())
        holder.transactionDate.text = formattedDate
        
        // Set amount with sign and color
        val amount = transaction.amount
        val amountText = when (transaction.type) {
            "expense" -> "-$${String.format("%.2f", amount)}"
            "income" -> "+$${String.format("%.2f", amount)}"
            "saving" -> "-$${String.format("%.2f", amount)}"
            else -> "$${String.format("%.2f", amount)}"
        }
        holder.transactionAmount.text = amountText
        
        // Set amount text color based on transaction type
        val context = holder.itemView.context
        val colorRes = when (transaction.type) {
            "income" -> R.color.income_green
            "expense" -> R.color.expense_red
            "saving" -> R.color.saving_blue
            else -> R.color.black
        }
        holder.transactionAmount.setTextColor(context.getColor(colorRes))
        
        // Set category color for the text and icon background
        val cardView = holder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.category_icon_card)
        if (cardView != null) {
            // Apply background color based on transaction type
            val bgColor = when (transaction.type) {
                "income" -> context.getColor(R.color.income_green_light)
                "expense" -> context.getColor(R.color.expense_red_light)
                "saving" -> context.getColor(R.color.saving_blue_light)
                else -> context.getColor(R.color.gray_light)
            }
            cardView.setCardBackgroundColor(bgColor)
            
            // Apply the same color to category name
            holder.transactionCategory.setTextColor(context.getColor(colorRes))
        }
        
        // Set category icon from base64 if available, otherwise use default icon
        if (category != null && !category.iconBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(category.iconBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.categoryIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding category icon", e)
                setDefaultIcon(holder.categoryIcon, transaction.type)
            }
        } else {
            setDefaultIcon(holder.categoryIcon, transaction.type)
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }
    
    override fun getItemCount(): Int = transactions.size
    
    /**
     * Update the adapter with new data
     */
    fun updateData(newTransactions: List<Transaction>, newCategories: Map<String, Category>) {
        transactions = newTransactions
        categories = newCategories
        notifyDataSetChanged()
    }
    
    /**
     * Set default icon based on transaction type
     */
    private fun setDefaultIcon(imageView: ImageView, transactionType: String) {
        val iconResource = when (transactionType) {
            "income" -> R.drawable.income
            "expense" -> R.drawable.shoppingg
            "saving" -> R.drawable.saving
            else -> R.drawable.cash
        }
        imageView.setImageResource(iconResource)
    }
    
    /**
     * Filter transactions by specified transaction type
     */
    fun filterByType(type: String) {
        // We don't change transactions here - this should be done at the fragment level
        // by reloading the data with the new filter
        notifyDataSetChanged()
    }
    
    /**
     * Sort transactions by amount (descending)
     */
    fun sortByAmount() {
        transactions = transactions.sortedByDescending { it.amount }
        notifyDataSetChanged()
    }
    
    /**
     * Sort transactions by date (newest first)
     */
    fun sortByDate() {
        transactions = transactions.sortedByDescending { it.date.seconds }
        notifyDataSetChanged()
    }
} 