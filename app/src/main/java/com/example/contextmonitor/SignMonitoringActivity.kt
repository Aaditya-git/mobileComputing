package com.example.contextmonitor

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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

        // Initialize repository
        val healthDataDao = AppDatabase.getDatabase(application).healthDataDao()
        healthRepository = HealthRepository(healthDataDao)

        // ---- Heart Rate Button ----
        heartRateButton.setOnClickListener {
            heartRateButton.isEnabled = false
            lifecycleScope.launch {
                try {
                    val videoUri = copyVideoFromAssetsToStorage("heart_rate_measuring.mp4")
                    if (videoUri != null) {
                        heartRateValue = heartRateCalculator(videoUri, contentResolver)
                        if (heartRateValue <= 0) {
                            heartRateStatus.text = "Measurement failed"
                            Toast.makeText(
                                this@SignMonitoringActivity,
                                "Failed to calculate heart rate.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            heartRateMeasured = true
                            heartRateStatus.text = "Measured: $heartRateValue bpm"
                        }
                    } else {
                        Toast.makeText(
                            this@SignMonitoringActivity,
                            "Video not found in assets.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("HeartRate", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@SignMonitoringActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    heartRateButton.isEnabled = true
                    checkNextButton(nextButton)
                }
            }
        }

        // ---- Respiratory Rate Button ----
        respiratoryRateButton.setOnClickListener {
            respiratoryRateButton.isEnabled = false
            lifecycleScope.launch {
                try {
                    val accelX = readCSVFromAssets("CSVBreatheX.csv")
                    val accelY = readCSVFromAssets("CSVBreatheY.csv")
                    val accelZ = readCSVFromAssets("CSVBreatheZ.csv")

                    respiratoryRateValue = respiratoryRateCalculator(accelX, accelY, accelZ)
                    if (respiratoryRateValue <= 0) {
                        respiratoryRateStatus.text = "Measurement failed"
                        Toast.makeText(
                            this@SignMonitoringActivity,
                            "Failed to calculate respiratory rate.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        respiratoryRateMeasured = true
                        respiratoryRateStatus.text =
                            "Measured: $respiratoryRateValue breaths/min"
                    }
                } catch (e: Exception) {
                    Log.e("RespRate", "Error: ${e.message}", e)
                    Toast.makeText(
                        this@SignMonitoringActivity,
                        "Error measuring respiratory rate: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    respiratoryRateButton.isEnabled = true
                    checkNextButton(nextButton)
                }
            }
        }

        // ---- Next Button ----
        nextButton.setOnClickListener {
            lifecycleScope.launch {
                val healthData = HealthData(
                    heartRate = heartRateValue,
                    respiratoryRate = respiratoryRateValue
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

    // ------------------- Helpers -------------------

    private suspend fun copyVideoFromAssetsToStorage(fileName: String): Uri? =
        withContext(Dispatchers.IO) {
            val file = File(filesDir, fileName)
            try {
                val assetExists = try {
                    assets.open(fileName).close()
                    true
                } catch (e: IOException) {
                    false
                }
                if (!assetExists) return@withContext null

                if (!file.exists()) {
                    assets.open(fileName).use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                Uri.fromFile(file)
            } catch (e: IOException) {
                Log.e("AssetLoading", "Error copying file: ${e.message}")
                null
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

    // ------------------- Heart Rate -------------------

    private suspend fun heartRateCalculator(uri: Uri, contentResolver: ContentResolver): Int {
        return withContext(Dispatchers.IO) {
            var bpm = -1
            val retriever = MediaMetadataRetriever()
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = contentResolver.openFileDescriptor(uri, "r")
                pfd?.let { retriever.setDataSource(it.fileDescriptor) }

                val frameCountStr =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                val frameCount = frameCountStr?.toIntOrNull() ?: 0
                if (frameCount <= 0) return@withContext -1

                val redValues = mutableListOf<Double>()

                // sample every 10th frame to save work
                for (i in 0 until frameCount step 10) {
                    val bmp: Bitmap? = try {
                        retriever.getFrameAtIndex(i)
                    } catch (e: Exception) {
                        null
                    }
                    bmp?.let { frame ->
                        val step = max(1, min(frame.width / 50, frame.height / 50))
                        var sum = 0L
                        var count = 0
                        for (y in 0 until frame.height step step) {
                            for (x in 0 until frame.width step step) {
                                val p = frame.getPixel(x, y)
                                val red = (p shr 16) and 0xFF
                                sum += red
                                count++
                            }
                        }
                        redValues.add(sum.toDouble() / max(1, count))
                        frame.recycle() // free memory immediately
                    }
                }

                if (redValues.size < 3) return@withContext -1

                val smoothed = movingAverageDouble(redValues, 7)
                val mean = smoothed.average()
                val std = kotlin.math.sqrt(smoothed.map { (it - mean) * (it - mean) }.average())
                val threshold = mean + 0.5 * std

                var peaks = 0
                var lastPeakIndex = -10
                for (i in 1 until smoothed.size - 1) {
                    val isLocalMax =
                        smoothed[i] > smoothed[i - 1] && smoothed[i] > smoothed[i + 1]
                    if (isLocalMax && smoothed[i] > threshold) {
                        if (i - lastPeakIndex >= 10) {
                            peaks++
                            lastPeakIndex = i
                        }
                    }
                }

                val fps = 30.0
                val seconds = frameCount / fps
                bpm = if (seconds > 0) ((peaks / seconds) * 60.0).toInt() else -1

                Log.d("HeartRate", "frames=$frameCount peaks=$peaks bpm=$bpm")
            } catch (e: Exception) {
                Log.e("HeartRate", "Exception: ${e.message}", e)
            } finally {
                try {
                    retriever.release()
                    pfd?.close()
                } catch (_: Exception) {
                }
            }
            bpm
        }
    }

    private fun movingAverageDouble(data: List<Double>, window: Int): List<Double> {
        if (data.size < window) return data
        val result = mutableListOf<Double>()
        val half = window / 2
        for (i in data.indices) {
            val start = max(0, i - half)
            val end = min(data.size - 1, i + half)
            result.add(data.subList(start, end + 1).average())
        }
        return result
    }

    // ------------------- Respiratory Rate -------------------

    private fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>
    ): Int {
        if (accelValuesX.isEmpty() || accelValuesY.isEmpty() || accelValuesZ.isEmpty()) return -1

        val measurementSeconds = 45.0
        val sampleRate = accelValuesY.size / measurementSeconds
        val sr = if (sampleRate < 1.0) 1.0 else sampleRate

        // Combine into magnitude
        val magnitudes = mutableListOf<Float>()
        for (i in accelValuesY.indices) {
            val x = accelValuesX.getOrNull(i) ?: 0f
            val y = accelValuesY.getOrNull(i) ?: 0f
            val z = accelValuesZ.getOrNull(i) ?: 0f
            magnitudes.add(sqrt(x * x + y * y + z * z))
        }

        val smoothWindow = max(3, (sr * 0.5).toInt())
        val smoothed = smoothSignal(magnitudes, smoothWindow)

        val mean = smoothed.average()
        val std = kotlin.math.sqrt(smoothed.map { (it - mean) * (it - mean) }.average())
        val threshold = (mean + 0.3 * std).toFloat()

        val minPeakDistFrames = max(1, (sr * 1.2).toInt())
        var peaks = 0
        var lastPeakIndex = -minPeakDistFrames
        for (i in 1 until smoothed.size - 1) {
            val isLocalMax = smoothed[i] > smoothed[i - 1] && smoothed[i] > smoothed[i + 1]
            if (isLocalMax && smoothed[i] > threshold) {
                if (i - lastPeakIndex >= minPeakDistFrames) {
                    peaks++
                    lastPeakIndex = i
                }
            }
        }

        if (peaks == 0) return -1
        val breathsPerMinute = (peaks / measurementSeconds * 60.0).toInt()
        Log.d("RespRate", "peaks=$peaks sr=$sr breaths/min=$breathsPerMinute")
        return breathsPerMinute
    }

    private fun smoothSignal(values: List<Float>, window: Int = 5): List<Float> {
        if (values.size < window) return values
        val result = mutableListOf<Float>()
        val half = window / 2
        for (i in values.indices) {
            val start = max(0, i - half)
            val end = min(values.size - 1, i + half)
            val avg = values.subList(start, end + 1).average().toFloat()
            result.add(avg)
        }
        return result
    }
}
