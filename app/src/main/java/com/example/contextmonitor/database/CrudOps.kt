package com.example.contextmonitor.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CrudOps {
    @Insert
    suspend fun insert(sympFields: SympFields): Long

    @Query("SELECT * FROM health_data ORDER BY timestamp DESC")
    fun getAllHealthData(): Flow<List<SympFields>>

    @Update
    suspend fun update(sympFields: SympFields)
    @Query("SELECT * FROM health_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHealthData(): SympFields?

    @Query("DELETE FROM health_data")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM health_data")
    suspend fun getRecordCount(): Int
}