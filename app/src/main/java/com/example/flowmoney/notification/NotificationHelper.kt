package com.example.flowmoney.notification

import android.app.Application
import android.content.Context
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationReceivedEvent
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/**
 * Helper class to manage and send notifications
 */
class NotificationHelper(private val context: Application) {

    init {
        // Set notification received handler for OneSignal version 5.0+
        OneSignal.Notifications.addNotificationReceivedHandler { notificationReceivedEvent ->
            handleNotificationReceived(notificationReceivedEvent)
        }
    }

    /**
     * Handle incoming notifications
     */
    private fun handleNotificationReceived(event: INotificationReceivedEvent) {
        // Process received notification
        val notification = event.notification
    }

    /**
     * Send local notification for new transaction
     */
    fun notifyTransactionAdded(transaction: Transaction) {
        val type = transaction.type.replaceFirstChar { it.uppercase() }
        val amount = formatCurrency(transaction.amount)
        
        val title = "New $type Added"
        val message = "You added a new $type of $amount"
        
        sendLocalNotification(title, message)
    }

    /**
     * Send notification for new account
     */
    fun notifyAccountAdded(accountName: String, accountType: String) {
        val title = "New Account Added"
        val message = "You added a new $accountType account: $accountName"
        
        sendLocalNotification(title, message)
    }

    /**
     * Send notification for new category
     */
    fun notifyCategoryAdded(categoryName: String) {
        val title = "New Category Added"
        val message = "You added a new category: $categoryName"
        
        sendLocalNotification(title, message)
    }

    /**
     * Send notification for budget limit exceeded
     */
    fun notifyBudgetExceeded(categoryName: String, budgetLimit: Double, currentAmount: Double) {
        val limitFormatted = formatCurrency(budgetLimit)
        val currentFormatted = formatCurrency(currentAmount)
        val overAmount = formatCurrency(currentAmount - budgetLimit)
        
        val title = "Budget Limit Exceeded"
        val message = "Your $categoryName budget of $limitFormatted has been exceeded by $overAmount"
        
        // Use different notification ID for budget alerts to ensure they're not overwritten
        sendLocalNotification(title, message, additionalData = mapOf(
            "type" to "budget_exceeded",
            "category" to categoryName,
            "limit" to budgetLimit,
            "current" to currentAmount
        ))
    }

    /**
     * Send local notification (doesn't use OneSignal dashboard)
     */
    private fun sendLocalNotification(
        title: String, 
        message: String,
        additionalData: Map<String, Any>? = null
    ) {
        try {
            // Create notification content
            val notificationJson = JSONObject()
            notificationJson.put("title", title)
            notificationJson.put("body", message)
            
            // Add additional data if provided
            if (additionalData != null) {
                val additionalDataJson = JSONObject()
                for ((key, value) in additionalData) {
                    additionalDataJson.put(key, value)
                }
                notificationJson.put("additionalData", additionalDataJson)
            }
            
            // Generate a unique ID for the notification
            val notificationId = UUID.randomUUID().toString()
            
            // Create an in-app notification (compatible with OneSignal 5.0+)
            OneSignal.Notifications.postNotification(notificationJson, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Format currency value
     */
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }
} 