package com.example.flowmoney

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup

class AddNewAccount : AppCompatActivity() {

    private lateinit var initialAmountEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var iconsRadioGroup: RadioGroup
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_account)

        // Initialize views
        initialAmountEditText = findViewById(R.id.etInitialAmount)
        nameEditText = findViewById(R.id.etName)
        iconsRadioGroup = findViewById(R.id.rgIcons)
        cancelButton = findViewById(R.id.btnCancel)
        saveButton = findViewById(R.id.btnSave)

        // Set click listeners
        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            saveAccount()
        }
    }

    private fun saveAccount() {
        // Get user input
        val initialAmount = initialAmountEditText.text.toString().toFloatOrNull() ?: 0f
        val accountName = nameEditText.text.toString().takeIf { it.isNotEmpty() } ?: "Untitled"
        val selectedIconId = iconsRadioGroup.checkedRadioButtonId

        // Create account object and save it
        // This would depend on your data model and storage implementation

        // Close activity
        finish()
    }
}