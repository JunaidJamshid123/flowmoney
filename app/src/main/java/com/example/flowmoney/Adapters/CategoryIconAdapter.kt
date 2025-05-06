package com.example.flowmoney.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.R
import com.google.android.material.card.MaterialCardView

class CategoryIconAdapter(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<CategoryIconAdapter.IconViewHolder>() {

    // Track selected position, -1 means no selection
    private var selectedPosition = 0 // Default to first item as selected

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardIcon: MaterialCardView = itemView.findViewById(R.id.cardIcon)
        val iconImageView: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(iconResId: Int, position: Int) {
            // Set the icon
            iconImageView.setImageResource(iconResId)

            // Set selected state
            val isSelected = position == selectedPosition
            val accentColor = ContextCompat.getColor(itemView.context, R.color.accent_green)
            val defaultColor = ContextCompat.getColor(itemView.context, R.color.light_gray)

            // Update card appearance based on selection state
            with(cardIcon) {
                strokeColor = if (isSelected) accentColor else defaultColor
                strokeWidth = if (isSelected) 2 else 1
                setCardBackgroundColor(
                    if (isSelected)
                        ContextCompat.getColor(itemView.context, R.color.very_light_green)
                    else
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                )
            }

            // Set click listener
            itemView.setOnClickListener {
                // Update selection if different
                if (selectedPosition != position) {
                    val oldPosition = selectedPosition
                    selectedPosition = position

                    // Notify adapter about changes to refresh both items
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)

                    // Notify listener about selection
                    onIconSelected(iconResId)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position], position)
    }

    override fun getItemCount(): Int = icons.size

    // Method to get currently selected icon resource ID
    fun getSelectedIconResId(): Int = icons[selectedPosition]
}