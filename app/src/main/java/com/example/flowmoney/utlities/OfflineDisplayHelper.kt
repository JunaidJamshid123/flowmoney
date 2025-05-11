package com.example.flowmoney.utlities

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.R
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.viewmodels.NetworkStatusViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class to assist with displaying data in offline mode
 */
class OfflineDisplayHelper(
    private val context: Context,
    private val networkStatusViewModel: NetworkStatusViewModel
) {
    /**
     * Show proper message when no data is available based on network status
     */
    fun showNoDataMessage(
        emptyStateView: View,
        recyclerView: RecyclerView,
        messageTextView: TextView,
        isDataEmpty: Boolean
    ) {
        if (isDataEmpty) {
            // Check network status
            val isOnline = networkStatusViewModel.getNetworkStatus().value ?: false
            
            // Set appropriate message
            val message = if (isOnline) {
                "No data available. Pull to refresh."
            } else {
                "You're offline. Data will be displayed when available."
            }
            
            // Update UI
            messageTextView.text = message
            emptyStateView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // Data is available, show the recycler view
            emptyStateView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    /**
     * Calculate and format financial summary values
     */
    fun calculateSummary(
        transactions: List<Transaction>
    ): Triple<Double, Double, Double> {
        val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        val savings = transactions.filter { it.type == "saving" }.sumOf { it.amount }
        
        return Triple(income, expense, savings)
    }
    
    /**
     * Format currency values for display
     */
    fun formatCurrency(amount: Double): String {
        return String.format("$%.2f", amount)
    }
} 