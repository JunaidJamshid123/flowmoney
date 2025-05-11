package com.example.flowmoney.utlities

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.flowmoney.R
import com.example.flowmoney.viewmodels.NetworkStatusViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Helper class to display and manage offline status indicator
 */
class OfflineStatusHelper(
    private val context: Context,
    private val viewModel: NetworkStatusViewModel,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private var offlineSnackbar: Snackbar? = null
    
    /**
     * Initialize the offline status helper
     * Should be called in the activity/fragment's onCreate or onStart
     */
    fun initialize(rootView: View) {
        // Create a Snackbar to show when offline
        offlineSnackbar = Snackbar.make(
            rootView,
            "You're offline. The app will sync when connection is restored.",
            Snackbar.LENGTH_INDEFINITE
        )
        
        // Monitor network status changes
        viewModel.getNetworkStatus().observe(lifecycleOwner) { isOnline ->
            if (isOnline) {
                offlineSnackbar?.dismiss()
            } else {
                offlineSnackbar?.show()
            }
        }
    }
    
    /**
     * Create a view to show the offline status in a list when empty
     */
    fun createOfflineEmptyView(parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.view_offline_empty, parent, false)
        val messageTextView = view.findViewById<TextView>(R.id.offlineMessageText)
        
        viewModel.getNetworkStatus().observe(lifecycleOwner) { isOnline ->
            if (isOnline) {
                messageTextView.text = "No data available. Pull to refresh."
            } else {
                messageTextView.text = "You're offline. Data will sync when connection is restored."
            }
        }
        
        return view
    }
    
    /**
     * Clean up resources
     * Should be called in onDestroy
     */
    fun cleanup() {
        offlineSnackbar?.dismiss()
        offlineSnackbar = null
    }
    
    companion object {
        /**
         * Create an offline status helper for an activity
         */
        fun with(activity: Activity, viewModel: NetworkStatusViewModel): OfflineStatusHelper {
            return OfflineStatusHelper(activity, viewModel, activity as LifecycleOwner)
        }
    }
} 