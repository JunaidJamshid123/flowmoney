package com.example.flowmoney.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.flowmoney.R

class AnalysisFragment : Fragment() {
    private lateinit var spinnerFilter: Spinner

    // Time period buttons
    private lateinit var btnDay: Button
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private lateinit var btnYear: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)

        // Initialize views
        spinnerFilter = view.findViewById(R.id.spinner_filter)

        btnDay = view.findViewById(R.id.btn_day)
        btnWeek = view.findViewById(R.id.btn_week)
        btnMonth = view.findViewById(R.id.btn_month)
        btnYear = view.findViewById(R.id.btn_year)

        // Setup time period buttons
        setupTimePeriodButtons()

        // Setup filter spinner
        setupFilterSpinner()

        return view
    }

    private fun setupTimePeriodButtons() {
        val buttons = listOf(btnDay, btnWeek, btnMonth, btnYear)

        buttons.forEach { button ->
            button.setOnClickListener {
                // Reset all buttons
                buttons.forEach { btn ->
                    btn.setBackgroundResource(R.drawable.bg_button_unselected)
                    btn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }

                // Set selected button
                button.setBackgroundResource(R.drawable.bg_button_selected)
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("Expense", "Income", "Savings", "Investment")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        // Set default selection to "Expense"
        spinnerFilter.setSelection(0)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AnalysisFragment()
    }
}