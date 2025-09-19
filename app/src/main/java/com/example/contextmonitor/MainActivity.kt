package com.example.contextmonitor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // This will look for activity_main.xml

        // Find the buttons by their IDs
        val recordButton = findViewById<Button>(R.id.recordButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        // Set click listener for "Record health data" button
        recordButton.setOnClickListener {
            // Start SignMonitoringActivity
            val intent = Intent(this, SignMonitoringActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for "Delete all recorded data" button
        deleteButton.setOnClickListener {
            // For now, show a message; we'll add database deletion later
            Toast.makeText(this, "Data deletion will be implemented later", Toast.LENGTH_SHORT).show()
        }
    }
}