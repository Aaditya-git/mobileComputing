package com.example.contextmonitor.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object FileUtils {

    suspend fun copyFileFromAssetsToStorage(context: Context, fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, fileName)
                if (!file.exists()) {
                    context.assets.open(fileName).use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                Uri.fromFile(file)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun readCSVFromAssets(context: Context, fileName: String): MutableList<Float> {
        val data = mutableListOf<Float>()
        try {
            context.assets.open(fileName).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    line.toFloatOrNull()?.let { data.add(it) }
                }
            }
        } catch (e: Exception) {
            for (i in 0 until 500) {
                data.add((sin(i * 0.1) * 0.5).toFloat())
            }
        }
        return data
    }

    fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>
    ): Int {
        if (accelValuesY.isEmpty()) return -1
        var previousValue = 10f
        var k = 0
        val limit = accelValuesY.size
        for (i in 11 until limit) {
            val currentValue = sqrt(
                accelValuesZ[i].toDouble().pow(2.0) +
                        accelValuesX[i].toDouble().pow(2.0) +
                        accelValuesY[i].toDouble().pow(2.0)
            ).toFloat()
            if (abs(previousValue - currentValue) > 0.15f) {
                k++
            }
            previousValue = currentValue
        }
        val ret = k.toDouble() / 45.0
        return (ret * 30).toInt()
    }

    suspend fun heartRateCalculator(uri: Uri, contentResolver: ContentResolver): Int {
        return withContext(Dispatchers.IO) {
            var result = -1
            val retriever = MediaMetadataRetriever()
            try {
                var path: String? = null
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                val cursor = contentResolver.query(uri, proj, null, null, null)
                cursor?.use {
                    try {
                        val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        if (it.moveToFirst()) path = it.getString(columnIndex)
                    } catch (_: Exception) {}
                }
                if (path != null) {
                    try {
                        retriever.setDataSource(path)
                    } catch (_: Exception) {}
                } else {
                    try {
                        val pfd = contentResolver.openFileDescriptor(uri, "r")
                        pfd?.use { retriever.setDataSource(it.fileDescriptor) }
                    } catch (_: Exception) {}
                }

                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                val frameDuration = min(durationStr?.toIntOrNull() ?: 0, 425)
                val frameList = ArrayList<Bitmap>()
                var i = 10
                while (i < frameDuration) {
                    val bitmap = try {
                        retriever.getFrameAtIndex(i)
                    } catch (e: Exception) {
                        null
                    }
                    bitmap?.let { frameList.add(it) }
                    i += 15
                }

                if (frameList.isNotEmpty()) {
                    val a = mutableListOf<Long>()
                    for (frame in frameList) {
                        var redBucket = 0L
                        val startY = 350
                        val endY = 450
                        val startX = 350
                        val endX = 450
                        val maxY = min(frame.height - 1, endY)
                        val maxX = min(frame.width - 1, endX)
                        var pixelCount = 0L
                        for (y in startY..maxY) {
                            for (x in startX..maxX) {
                                val c: Int = frame.getPixel(x, y)
                                redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                                pixelCount++
                            }
                        }
                        a.add(redBucket)
                        frame.recycle()
                    }
                    if (a.size >= 6) {
                        val b = mutableListOf<Long>()
                        for (j in 0 until a.size - 5) {
                            val temp = (a[j] + a[j + 1] + a[j + 2] + a[j + 3] + a[j + 4]) / 4
                            b.add(temp)
                        }
                        if (b.isNotEmpty()) {
                            var x = b[0]
                            var count = 0
                            for (j in 1 until b.size) {
                                val p = b[j]
                                if ((p - x) > 3500) {
                                    count++
                                }
                                x = p
                            }
                            val rate = (count.toFloat() * 60).toInt()
                            result = rate / 4
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("MediaPath", "convertMediaUriToPath: ${e.stackTrace}")
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
            result
        }
    }
}
