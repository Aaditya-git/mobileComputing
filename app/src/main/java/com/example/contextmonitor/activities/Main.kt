package com.example.contextmonitor.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.contextmonitor.R
import com.example.contextmonitor.database.AppDatabase
import com.example.contextmonitor.database.DataManager
import kotlinx.coroutines.launch

class Main : AppCompatActivity() {
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        val database = AppDatabase.getDatabase(application)
        dataManager = DataManager(database.healthDataDao())

        val recordButton = findViewById<Button>(R.id.recordButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        recordButton.setOnClickListener {
            val intent = Intent(this, measurementScreen::class.java)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                dataManager.deleteAll()
                Toast.makeText(this@Main,
                    "All health data deleted successfully", Toast.LENGTH_SHORT).show()
            }
        }

        // Show current record count
        lifecycleScope.launch {
            val count = dataManager.getRecordCount()
            if (count > 0) {
                Toast.makeText(this@Main,
                    "Database contains $count health records", Toast.LENGTH_SHORT).show()
            }
        }
    }
}