package com.example.contextmonitor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.lifecycle.LiveData
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow


@Dao
interface HealthDataDao {
    @Insert
    suspend fun insert(healthData: HealthData)

    @Query("SELECT * FROM health_data ORDER BY timestamp DESC")
    fun getAllHealthData(): Flow<List<HealthData>>

    @Query("SELECT * FROM health_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHealthData(): HealthData?

    @Query("DELETE FROM health_data")
    suspend fun deleteAll()
}