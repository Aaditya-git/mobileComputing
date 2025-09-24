package com.example.contextmonitor
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

class HealthRepository(private val healthDataDao: HealthDataDao) {
    //added a change
    // Use Flow for reactive updates
    val allHealthData: Flow<List<HealthData>> = healthDataDao.getAllHealthData()

    // For LiveData if you prefer
    val allHealthDataLiveData = healthDataDao.getAllHealthData().asLiveData()

    suspend fun insert(healthData: HealthData) {
        healthDataDao.insert(healthData)
    }

    suspend fun getLatestHealthData(): HealthData? {
        return healthDataDao.getLatestHealthData()
    }

    suspend fun deleteAll() {
        healthDataDao.deleteAll()
    }

    suspend fun update(healthData: HealthData){
        healthDataDao.update(healthData)
    }
}