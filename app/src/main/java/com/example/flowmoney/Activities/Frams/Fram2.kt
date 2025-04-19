package com.example.flowmoney.Activities.Frams

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.flowmoney.Activities.Authentication.Login
import com.example.flowmoney.MainActivity
import com.example.flowmoney.R

class Fram2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fram2)

        val btnNext2: Button = findViewById(R.id.btnNext2)
        val btnSkip: TextView = findViewById(R.id.btnSkip)

        // Move to Fram3 when Next is clicked
        btnNext2.setOnClickListener {
            val intent = Intent(this, Fram3::class.java)
            startActivity(intent)
        }

        // Skip to MainActivity (or any target activity)
        btnSkip.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Optional: finish onboarding flow
        }
    }
}
