package com.example.contextmonitor.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.contextmonitor.R
import com.example.contextmonitor.database.AppDatabase
import com.example.contextmonitor.database.SympFields
import com.example.contextmonitor.database.DataManager
import com.example.contextmonitor.utils.FileUtils
import com.example.contextmonitor.utils.FileUtils.respiratoryRateCalculator
import com.example.contextmonitor.utils.FileUtils.heartRateCalculator
import kotlinx.coroutines.launch
//import com.example.contextmonitor.utils.heartRateCalculator
//import com.example.contextmonitor.utils.respiratoryRateCalculator


class measurementScreen : AppCompatActivity() {
    private lateinit var dataManager: DataManager
    private var heartRateMeasured = false
    private var respiratoryRateMeasured = false
    private var heartRateValue: Int = 0
    private var respiratoryRateValue: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_monitoring)

        val heartRateButton = findViewById<Button>(R.id.heartRateButton)
        val respiratoryRateButton = findViewById<Button>(R.id.respiratoryRateButton)
        val nextButton = findViewById<Button>(R.id.nextButton)
        val heartRateStatus = findViewById<TextView>(R.id.heartRateStatus)
        val respiratoryRateStatus = findViewById<TextView>(R.id.respiratoryRateStatus)

        val database = AppDatabase.getDatabase(application)
        dataManager = DataManager(database.healthDataDao())

        heartRateButton.setOnClickListener {
            heartRateButton.isEnabled = false
            heartRateStatus.text = "Measuring heart rate..."

            lifecycleScope.launch {
                val videoUri = FileUtils.copyFileFromAssetsToStorage(
                    this@measurementScreen,
                    "heart_rate_measuring.mp4"
                )
                if (videoUri != null) {
                    val bpm = heartRateCalculator(videoUri, contentResolver)
                    if (bpm > 0) {
                        heartRateValue = bpm
                        heartRateMeasured = true
                        heartRateStatus.text = "Heart Rate: $heartRateValue BPM"
                        Toast.makeText(
                            this@measurementScreen,
                            "Heart rate measured: $heartRateValue BPM",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        heartRateStatus.text = "Measurement failed"
                        Toast.makeText(
                            this@measurementScreen,
                            "Heart rate measurement failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    heartRateStatus.text = "Measurement failed"
                    Toast.makeText(
                        this@measurementScreen,
                        "Video file not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                heartRateButton.isEnabled = true
                checkNextButton(nextButton)
            }
        }

        respiratoryRateButton.setOnClickListener {
            respiratoryRateButton.isEnabled = false
            respiratoryRateStatus.text = "Measuring respiratory rate..."

            lifecycleScope.launch {
                val accelX = FileUtils.readCSVFromAssets(this@measurementScreen, "CSVBreatheX.csv")
                val accelY = FileUtils.readCSVFromAssets(this@measurementScreen, "CSVBreatheY.csv")
                val accelZ = FileUtils.readCSVFromAssets(this@measurementScreen, "CSVBreatheZ.csv")

                if (accelX.isNotEmpty() && accelY.isNotEmpty() && accelZ.isNotEmpty()) {
                    val breaths = respiratoryRateCalculator(accelX, accelY, accelZ)
                    if (breaths > 0) {
                        respiratoryRateValue = breaths
                        respiratoryRateMeasured = true
                        respiratoryRateStatus.text = "Respiratory Rate: $respiratoryRateValue breaths/min"
                        Toast.makeText(
                            this@measurementScreen,
                            "Respiratory rate measured: $respiratoryRateValue breaths/min",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        respiratoryRateStatus.text = "Measurement failed"
                        Toast.makeText(
                            this@measurementScreen,
                            "Respiratory rate measurement failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    respiratoryRateStatus.text = "Measurement failed"
                    Toast.makeText(
                        this@measurementScreen,
                        "CSV data missing",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                respiratoryRateButton.isEnabled = true
                checkNextButton(nextButton)
            }
        }

        nextButton.setOnClickListener {
            lifecycleScope.launch {
                val sympFields = SympFields(
                    heartRate = heartRateValue,
                    respiratoryRate = respiratoryRateValue
                )
                dataManager.insert(sympFields)
                val intent = Intent(this@measurementScreen, SymptomScreen::class.java)
                startActivity(intent)
            }
        }
    }

    private fun checkNextButton(button: Button) {
        button.isEnabled = heartRateMeasured && respiratoryRateMeasured
    }
}
