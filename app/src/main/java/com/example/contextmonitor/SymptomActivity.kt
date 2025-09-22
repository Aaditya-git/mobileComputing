package com.example.contextmonitor

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private lateinit var healthRepository: HealthRepository
class SymptomActivity : AppCompatActivity() {
    private lateinit var healthRepository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom)

        // Initialize repository
        val healthDataDao = AppDatabase.getDatabase(application).healthDataDao()
        healthRepository = HealthRepository(healthDataDao)

        val uploadButton = findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            lifecycleScope.launch {
                // Get the latest health data record
                val latestData = healthRepository.getLatestHealthData()

                if (latestData != null) {
                    val updatedData = latestData.copy(
                        fever = findViewById<RatingBar>(R.id.feverRating).rating.toInt(),
                        cough = findViewById<RatingBar>(R.id.coughRating).rating.toInt(),
                        shortnessOfBreath = findViewById<RatingBar>(R.id.shortnessOfBreathRating).rating.toInt(),
                        fatigue = findViewById<RatingBar>(R.id.fatigueRating).rating.toInt(),
                        muscleAches = findViewById<RatingBar>(R.id.muscleAchesRating).rating.toInt(),
                        headache = findViewById<RatingBar>(R.id.headacheRating).rating.toInt(),
                        soreThroat = findViewById<RatingBar>(R.id.soreThroatRating).rating.toInt(),
                        lossOfTasteOrSmell = findViewById<RatingBar>(R.id.lossOfTasteOrSmellRating).rating.toInt(),
                        congestion = findViewById<RatingBar>(R.id.congestionRating).rating.toInt(),
                        nausea = findViewById<RatingBar>(R.id.nauseaRating).rating.toInt()
                    )

                    // Delete the old record and insert the updated one
                    healthRepository.deleteAll()
                    healthRepository.insert(updatedData)

                    Toast.makeText(this@SymptomActivity, "Health data saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SymptomActivity, "No health data found to update", Toast.LENGTH_SHORT).show()
                }

                finish()
            }
        }
    }
}//added a change