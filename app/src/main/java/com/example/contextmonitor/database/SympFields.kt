package com.example.contextmonitor.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_data")
data class SympFields(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int,
    val respiratoryRate: Int,
    val fever: Int = 0,
    val cough: Int = 0,
    val shortnessOfBreath: Int = 0,
    val fatigue: Int = 0,
    val muscleAches: Int = 0,
    val headache: Int = 0,
    val soreThroat: Int = 0,
    val lossOfTasteOrSmell: Int = 0,
    val congestion: Int = 0,
    val nausea: Int = 0
)