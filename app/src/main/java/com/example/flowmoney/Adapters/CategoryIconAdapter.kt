package com.example.flowmoney.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.R
import com.google.android.material.card.MaterialCardView

class CategoryIconAdapter(
    private val icons: List<Int>,
    private var selectedPosition: Int = 0,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<CategoryIconAdapter.IconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val icon = icons[position]
        holder.bind(icon, position == selectedPosition)

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition

            // Update previous and new selected items
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)

            // Callback with the selected icon resource ID
            onIconSelected(icons[selectedPosition])
        }
    }

    override fun getItemCount() = icons.size

    fun setSelectedPosition(position: Int) {
        if (position in 0 until itemCount) {
            val previousSelected = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
        }
    }

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardIcon)
        private val iconImageView: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(iconResId: Int, isSelected: Boolean) {
            iconImageView.setImageResource(iconResId)

            // Update the card appearance based on selection state
            if (isSelected) {
                cardView.strokeColor = itemView.context.getColor(R.color.green_primary) // Use your app's primary color
                cardView.strokeWidth = 2
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.green_light)) // Light variant of primary color
                iconImageView.setColorFilter(itemView.context.getColor(R.color.green_primary))
            } else {
                cardView.strokeColor = itemView.context.getColor(R.color.light_gray) // Light gray for unselected
                cardView.strokeWidth = 1
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.light_gray))
                iconImageView.setColorFilter(itemView.context.getColor(R.color.gray_text))
            }
        }
    }
}