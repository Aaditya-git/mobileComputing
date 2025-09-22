package com.example.contextmonitor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs

suspend fun heartRateCalculator(uri: Uri, contentResolver: ContentResolver): Int {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            val frameList = ArrayList<Bitmap>()

            // Correct way to set data source from URI
            try {
                // For content URIs, we need to use a different approach
                contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
                    retriever.setDataSource(parcelFileDescriptor.fileDescriptor)
                }
            } catch (e: Exception) {
                // If that fails, try with the file path
                val path = getPathFromUri(uri, contentResolver)
                if (path != null) {
                    retriever.setDataSource(path)
                } else {
                    Log.e("HeartRate", "Failed to get path from URI: $uri")
                    return@withContext -1
                }
            }

            // Try to extract duration using different metadata keys
            var duration = 0
            try {
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt() ?: 0
            } catch (e: Exception) {
                try {
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                    // Estimate frame count assuming 30fps
                    duration = (durationMs / 1000 * 30).toInt()
                } catch (e: Exception) {
                    Log.e("HeartRate", "Failed to get video duration")
                    return@withContext -1
                }
            }

            if (duration < 20) {
                Log.e("HeartRate", "Video too short for analysis: $duration frames")
                return@withContext -1
            }

            // Try to extract frames at different intervals
            val frameInterval = if (duration > 100) 15 else 5
            var i = 5 // Start a bit earlier
            val maxFrames = min(duration, 500)

            while (i < maxFrames && frameList.size < 50) {
                try {
                    val bitmap = retriever.getFrameAtIndex(i)
                    if (bitmap != null) {
                        frameList.add(bitmap)
                    }
                    i += frameInterval
                } catch (e: Exception) {
                    Log.w("HeartRate", "Failed to get frame at index $i: ${e.message}")
                    i += frameInterval
                }
            }

            retriever.release()

            if (frameList.isEmpty()) {
                Log.e("HeartRate", "No frames extracted from video after multiple attempts")
                return@withContext -1
            }

            // Process the extracted frames
            var redBucket: Long
            var pixelCount: Long = 0
            val a = mutableListOf<Long>()

            for (bitmap in frameList) {
                redBucket = 0
                // Use safe bounds checking to avoid out of bounds errors
                val width = bitmap.width
                val height = bitmap.height
                val startX = min(350, width - 100)
                val startY = min(350, height - 100)
                val endX = min(450, width)
                val endY = min(450, height)

                for (y in startY until endY) {
                    for (x in startX until endX) {
                        val c: Int = bitmap.getPixel(x, y)
                        pixelCount++
                        redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                    }
                }
                a.add(redBucket)
            }

            // Check if we have enough data points
            if (a.size < 10) {
                Log.e("HeartRate", "Not enough data points: ${a.size}")
                return@withContext -1
            }

            val b = mutableListOf<Long>()
            for (i in 0 until a.lastIndex - 5) {
                val temp = (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2)
                        + a.elementAt(i + 3) + a.elementAt(i + 4)) / 4
                b.add(temp)
            }

            // Check if we have enough smoothed data points
            if (b.isEmpty()) {
                Log.e("HeartRate", "No smoothed data points available")
                return@withContext -1
            }

            var x = b.elementAt(0)
            var count = 0
            for (i in 1 until b.lastIndex) {
                val p = b.elementAt(i)
                if ((p - x) > 3500) {
                    count += 1
                }
                x = b.elementAt(i)
            }

            val rate = ((count.toFloat()) * 60).toInt()
            val result = (rate / 4)

            Log.d("HeartRate", "Heart rate calculation complete: $result bpm")
            return@withContext result

        } catch (e: Exception) {
            Log.e("HeartRate", "Unexpected error in heart rate calculation: ${e.message}", e)
            return@withContext -1
        }
    }
}

// Helper function to get file path from URI
private fun getPathFromUri(uri: Uri, contentResolver: ContentResolver): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    val projection = arrayOf(MediaStore.Images.Media.DATA)
    return try {
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        }
    } catch (e: Exception) {
        null
    }
}

// Respiratory rate calculator function
fun respiratoryRateCalculator(
    accelValuesX: MutableList<Float>,
    accelValuesY: MutableList<Float>,
    accelValuesZ: MutableList<Float>,
): Int {
    Log.d("RespRate", "Input sizes - X: ${accelValuesX.size}, Y: ${accelValuesY.size}, Z: ${accelValuesZ.size}")

    // Check if we have enough data
    if (accelValuesX.size < 20 || accelValuesY.size < 20 || accelValuesZ.size < 20) {
        Log.e("RespRate", "Not enough data points for respiratory rate calculation")
        return 0
    }

    var previousValue: Float
    var currentValue: Float
    previousValue = 10f
    var k = 0

    for (i in 11 until minOf(accelValuesX.size, accelValuesY.size, accelValuesZ.size)) {
        currentValue = sqrt(
            accelValuesZ[i].toDouble().pow(2.0) +
                    accelValuesX[i].toDouble().pow(2.0) +
                    accelValuesY[i].toDouble().pow(2.0)
        ).toFloat()

        val diff = abs(previousValue - currentValue)
        if (diff > 0.15) {
            k++
        }

        previousValue = currentValue
    }

    val ret = (k.toDouble() / 45.00)
    val result = (ret * 30).toInt()

    Log.d("RespRate", "Respiratory rate calculation complete: $result breaths/min")
    return result
}

// Alternative respiratory rate calculator that focuses on periodic patterns
fun alternativeRespiratoryRateCalculator(
    accelValuesY: MutableList<Float>, // Focus on Y-axis (up-down movement)
): Int {
    if (accelValuesY.size < 100) {
        Log.e("RespRate", "Not enough data for alternative algorithm")
        return 0
    }

    // Simple peak detection algorithm
    val windowSize = 5
    var peakCount = 0
    var lastPeakIndex = -10 // Ensure we don't count peaks too close together

    for (i in windowSize until accelValuesY.size - windowSize) {
        val window = accelValuesY.subList(i - windowSize, i + windowSize)
        val current = accelValuesY[i]

        // Check if current value is a local maximum
        if (current == window.max() && i - lastPeakIndex > 10) {
            peakCount++
            lastPeakIndex = i
        }
    }

    // Calculate breaths per minute (assuming 45 seconds of data)
    val breathsPerMinute = (peakCount * 60 / 45).toInt()

    Log.d("RespRate", "Alternative algorithm result: $breathsPerMinute breaths/min")
    return breathsPerMinute
}