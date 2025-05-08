package com.example.flowmoney.Adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import java.text.SimpleDateFormat
import java.util.Locale

class SearchAdapter(
    private val context: Context,
    private var transactions: List<Transaction> = listOf(),
    private var categories: Map<String, Category> = mapOf(),
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconBackground: CardView = itemView.findViewById(R.id.icon_background)
        val itemIcon: ImageView = itemView.findViewById(R.id.item_icon)
        val itemTitle: TextView = itemView.findViewById(R.id.item_title)
        val itemTime: TextView = itemView.findViewById(R.id.item_time)
        val itemCheck: ImageView = itemView.findViewById(R.id.item_check)
        val itemPrice: TextView = itemView.findViewById(R.id.item_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.search_result_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = transactions[position]
        val category = categories[transaction.categoryId]
        
        // Set category title
        holder.itemTitle.text = category?.name ?: "Unknown Category"
        
        // Set transaction time
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        holder.itemTime.text = dateFormat.format(transaction.getDateAsDate())
        
        // Set transaction amount with appropriate sign
        val amount = transaction.amount
        val isIncome = transaction.type == "income"
        val amountText = String.format("$%,.2f", amount)
        holder.itemPrice.text = amountText
        
        // Set color and icon based on transaction type
        when (transaction.type) {
            "income" -> {
                holder.iconBackground.setCardBackgroundColor(context.getColor(R.color.income_green))
                holder.itemCheck.setImageResource(R.drawable.up_arrow)
                holder.itemPrice.setTextColor(context.getColor(R.color.income_green))
            }
            "expense" -> {
                holder.iconBackground.setCardBackgroundColor(context.getColor(R.color.expense_red))
                holder.itemCheck.setImageResource(R.drawable.down)
                holder.itemPrice.setTextColor(context.getColor(R.color.expense_red))
            }
            "saving" -> {
                holder.iconBackground.setCardBackgroundColor(context.getColor(R.color.saving_blue))
                holder.itemCheck.setImageResource(R.drawable.down)
                holder.itemPrice.setTextColor(context.getColor(R.color.saving_blue))
            }
            else -> {
                holder.iconBackground.setCardBackgroundColor(context.getColor(R.color.gray_text))
                holder.itemCheck.setImageResource(R.drawable.mobilebanking)
                holder.itemPrice.setTextColor(context.getColor(R.color.gray_text))
            }
        }
        
        // Set category icon if available
        if (category != null && category.iconBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(category.iconBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.itemIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                setDefaultIcon(holder.itemIcon, transaction.type)
            }
        } else {
            setDefaultIcon(holder.itemIcon, transaction.type)
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }
    
    private fun setDefaultIcon(imageView: ImageView, transactionType: String) {
        val iconResource = when (transactionType) {
            "income" -> R.drawable.income
            "expense" -> R.drawable.shoppingg
            "saving" -> R.drawable.saving
            else -> R.drawable.mobilebanking
        }
        imageView.setImageResource(iconResource)
    }
    
    fun updateData(newTransactions: List<Transaction>, newCategories: Map<String, Category>) {
        transactions = newTransactions
        categories = newCategories
        notifyDataSetChanged()
    }
    
    fun filterTransactions(query: String) {
        if (query.isEmpty()) {
            // No filter, show all transactions
            notifyDataSetChanged()
            return
        }
        
        val filteredList = transactions.filter { transaction ->
            val category = categories[transaction.categoryId]
            val categoryName = category?.name?.lowercase() ?: ""
            val notes = transaction.notes.lowercase()
            val amount = transaction.amount.toString()
            
            categoryName.contains(query.lowercase()) || 
            notes.contains(query.lowercase()) ||
            amount.contains(query)
        }
        
        updateData(filteredList, categories)
    }
} 