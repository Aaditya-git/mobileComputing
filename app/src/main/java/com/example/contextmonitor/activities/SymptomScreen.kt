package com.example.contextmonitor.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.contextmonitor.R
import com.example.contextmonitor.database.AppDatabase
import com.example.contextmonitor.database.DataManager
import kotlinx.coroutines.launch

class SymptomScreen : AppCompatActivity() {
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom)

        // Initialize database
        val database = AppDatabase.getDatabase(application)
        dataManager = DataManager(database.healthDataDao())

        val uploadButton = findViewById<Button>(R.id.uploadButton)

        uploadButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Get the latest health record
                    val latestRecord = dataManager.getLatestHealthData()

                    if (latestRecord != null) {
                        // Update with symptom ratings
                        val updatedRecord = latestRecord.copy(
                            fever = getRating(R.id.feverRating),
                            cough = getRating(R.id.coughRating),
                            shortnessOfBreath = getRating(R.id.shortnessOfBreathRating),
                            fatigue = getRating(R.id.fatigueRating),
                            muscleAches = getRating(R.id.muscleAchesRating),
                            headache = getRating(R.id.headacheRating),
                            soreThroat = getRating(R.id.soreThroatRating),
                            lossOfTasteOrSmell = getRating(R.id.lossOfTasteOrSmellRating),
                            congestion = getRating(R.id.congestionRating),
                            nausea = getRating(R.id.nauseaRating)
                        )

                        // Update the record
//                        healthRepository.deleteAll()
                        dataManager.insert(updatedRecord)
//                        healthRepository.update(updatedRecord)

                        Toast.makeText(this@SymptomScreen,
                            "All health data saved successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SymptomScreen,
                            "No health data found to update", Toast.LENGTH_SHORT).show()
                    }

                    // Return to main activity
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@SymptomScreen,
                        "Error saving symptom data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getRating(ratingBarId: Int): Int {
        return findViewById<RatingBar>(ratingBarId).rating.toInt()
    }
}