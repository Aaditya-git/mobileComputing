package com.example.contextmonitor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
private lateinit var healthRepository: HealthRepository
class MainActivity : AppCompatActivity() {
    private lateinit var healthRepository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize repository
        val healthDataDao = AppDatabase.getDatabase(application).healthDataDao()
        healthRepository = HealthRepository(healthDataDao)

        val recordButton = findViewById<Button>(R.id.recordButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        recordButton.setOnClickListener {
            val intent = Intent(this, SignMonitoringActivity::class.java)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                healthRepository.deleteAll()
                Toast.makeText(this@MainActivity, "All data deleted successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }
}