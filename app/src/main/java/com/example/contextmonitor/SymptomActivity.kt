package com.example.contextmonitor

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SymptomActivity : AppCompatActivity() {
    private var heartRate: Int = 0
    private var respiratoryRate: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom)

        // Get values from intent
        heartRate = intent.getIntExtra("HEART_RATE", 0)
        respiratoryRate = intent.getIntExtra("RESPIRATORY_RATE", 0)

        val uploadButton = findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            // Get ratings from all RatingBars
            val feverRating = findViewById<RatingBar>(R.id.feverRating).rating.toInt()
            val coughRating = findViewById<RatingBar>(R.id.coughRating).rating.toInt()
            val shortnessOfBreathRating = findViewById<RatingBar>(R.id.shortnessOfBreathRating).rating.toInt()
            val fatigueRating = findViewById<RatingBar>(R.id.fatigueRating).rating.toInt()
            val muscleAchesRating = findViewById<RatingBar>(R.id.muscleAchesRating).rating.toInt()
            val headacheRating = findViewById<RatingBar>(R.id.headacheRating).rating.toInt()
            val soreThroatRating = findViewById<RatingBar>(R.id.soreThroatRating).rating.toInt()
            val lossOfTasteOrSmellRating = findViewById<RatingBar>(R.id.lossOfTasteOrSmellRating).rating.toInt()
            val congestionRating = findViewById<RatingBar>(R.id.congestionRating).rating.toInt()
            val nauseaRating = findViewById<RatingBar>(R.id.nauseaRating).rating.toInt()

            // TODO: Store these ratings in the database along with heart rate and respiratory rate
            // For now, show a toast with all data
            Toast.makeText(this, "Heart Rate: $heartRate, Respiratory Rate: $respiratoryRate, Symptoms uploaded", Toast.LENGTH_SHORT).show()

            // Finish the activity and go back to MainActivity
            finish()
        }
    }
}