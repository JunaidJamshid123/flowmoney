package com.example.flowmoney.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.flowmoney.MainActivity
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/**
 * Helper class to manage and send notifications
 */
class NotificationHelper(private val context: Application) {

    companion object {
        private const val CHANNEL_ID = "flow_money_notifications"
        private const val CHANNEL_NAME = "FlowMoney Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for FlowMoney app"
        
        private const val NOTIFICATION_GROUP_TRANSACTIONS = "group_transactions"
        private const val NOTIFICATION_GROUP_BUDGETS = "group_budgets"
    }

    init {
        // Create notification channel for Android O and above
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ (Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Send local notification for new transaction
     */
    fun notifyTransactionAdded(transaction: Transaction) {
        val type = transaction.type.replaceFirstChar { it.uppercase() }
        val amount = formatCurrency(transaction.amount)
        
        val title = "New $type Added"
        val message = "You added a new $type of $amount"
        
        sendLocalNotification(title, message, NOTIFICATION_GROUP_TRANSACTIONS)
    }

    /**
     * Send notification for new account
     */
    fun notifyAccountAdded(accountName: String, accountType: String) {
        val title = "New Account Added"
        val message = "You added a new $accountType account: $accountName"
        
        sendLocalNotification(title, message, NOTIFICATION_GROUP_TRANSACTIONS)
    }

    /**
     * Send notification for new category
     */
    fun notifyCategoryAdded(categoryName: String) {
        val title = "New Category Added"
        val message = "You added a new category: $categoryName"
        
        sendLocalNotification(title, message, NOTIFICATION_GROUP_TRANSACTIONS)
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
        
        sendLocalNotification(title, message, NOTIFICATION_GROUP_BUDGETS)
    }

    /**
     * Send local notification using standard Android notification system
     */
    private fun sendLocalNotification(
        title: String, 
        message: String,
        group: String
    ) {
        try {
            // Create intent for when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build the notification
            val notificationId = System.currentTimeMillis().toInt()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_camera) // Use appropriate icon from your resources
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(group)

            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                // Check for notification permission (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                            android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notify(notificationId, builder.build())
                    }
                } else {
                    // For earlier Android versions, no runtime permission needed
                    notify(notificationId, builder.build())
                }
            }
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