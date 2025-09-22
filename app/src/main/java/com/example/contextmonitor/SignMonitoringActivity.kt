package com.example.contextmonitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SignMonitoringActivity : AppCompatActivity() {
    private lateinit var healthRepository: HealthRepository
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

        // Initialize database components
        val healthDataDao = AppDatabase.getDatabase(application).healthDataDao()
        healthRepository = HealthRepository(healthDataDao)

        heartRateButton.setOnClickListener {
            heartRateButton.isEnabled = false
            lifecycleScope.launch {
                try {
                    // For emulator: use provided video file
                    val videoUri = copyVideoFromAssetsToStorage("heart_rate_measuring.mp4")
                    if (videoUri != null) {
                        Log.d("HeartRate", "Video URI: $videoUri")
                        heartRateValue = heartRateCalculator(videoUri, contentResolver)

                        if (heartRateValue == -1) {
                            Toast.makeText(this@SignMonitoringActivity, "Failed to calculate heart rate. Video might be too short or invalid.", Toast.LENGTH_LONG).show()
                            heartRateStatus.text = "Measurement failed"
                        } else {
                            heartRateMeasured = true
                            heartRateStatus.text = "Measured: $heartRateValue bpm"
                        }
                    } else {
                        Toast.makeText(this@SignMonitoringActivity, "Failed to load video. Check Logcat for details.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("HeartRate", "Error measuring heart rate: ${e.message}", e)
                    Toast.makeText(this@SignMonitoringActivity, "Error measuring heart rate: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    heartRateButton.isEnabled = true
                    checkNextButton(nextButton)
                }
            }
        }

        respiratoryRateButton.setOnClickListener {
            respiratoryRateButton.isEnabled = false
            lifecycleScope.launch {
                try {
                    // Read CSV files from assets
                    val accelX = readCSVFromAssets("CSVBreatheX.csv")
                    val accelY = readCSVFromAssets("CSVBreatheY.csv")
                    val accelZ = readCSVFromAssets("CSVBreatheZ.csv")

                    // Try the alternative algorithm
                    respiratoryRateValue = alternativeRespiratoryRateCalculator(accelY)

                    respiratoryRateMeasured = true
                    respiratoryRateStatus.text = "Measured: $respiratoryRateValue breaths/min"
                } catch (e: Exception) {
                    Toast.makeText(this@SignMonitoringActivity, "Error measuring respiratory rate: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    respiratoryRateButton.isEnabled = true
                    checkNextButton(nextButton)
                }
            }
        }

        nextButton.setOnClickListener {
            lifecycleScope.launch {
                val healthData = HealthData(
                    heartRate = heartRateValue,
                    respiratoryRate = respiratoryRateValue
                    // Symptoms will be 0 initially, updated in SymptomActivity
                )
                healthRepository.insert(healthData)

                val intent = Intent(this@SignMonitoringActivity, SymptomActivity::class.java).apply {
                    putExtra("HEART_RATE", heartRateValue)
                    putExtra("RESPIRATORY_RATE", respiratoryRateValue)
                }
                startActivity(intent)
            }
        }
    }

    private suspend fun copyVideoFromAssetsToStorage(fileName: String): Uri? = withContext(Dispatchers.IO) {
        val file = File(filesDir, fileName) // filesDir is available in Activity context
        try {
            // Check if asset exists
            val assetExists = try {
                assets.open(fileName).close() // assets is available in Activity context
                true
            } catch (e: IOException) {
                false
            }

            if (!assetExists) {
                Log.e("AssetLoading", "Asset $fileName not found in assets folder")
                return@withContext null
            }

            if (!file.exists()) {
                assets.open(fileName).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream) // This should work now
                    }
                }
                Log.d("AssetLoading", "Copied $fileName from assets to ${file.absolutePath}")
            } else {
                Log.d("AssetLoading", "File already exists at ${file.absolutePath}")
            }

            return@withContext Uri.fromFile(file)
        } catch (e: IOException) {
            Log.e("AssetLoading", "Error copying file: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun readCSVFromAssets(fileName: String): MutableList<Float> {
        val data = mutableListOf<Float>()
        try {
            assets.open(fileName).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    line.toFloatOrNull()?.let { data.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    private fun checkNextButton(nextButton: Button) {
        nextButton.isEnabled = heartRateMeasured && respiratoryRateMeasured
    }
}