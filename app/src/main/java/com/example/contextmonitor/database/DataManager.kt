package com.example.contextmonitor.database

import kotlinx.coroutines.flow.Flow

class DataManager(private val crudOps: CrudOps) {

    val allSympFields: Flow<List<SympFields>> = crudOps.getAllHealthData()

    suspend fun insert(sympFields: SympFields): Long {
        return crudOps.insert(sympFields)
    }

    suspend fun getLatestHealthData(): SympFields? {
        return crudOps.getLatestHealthData()
    }

    suspend fun deleteAll() {
        crudOps.deleteAll()
    }

    suspend fun getRecordCount(): Int {
        return crudOps.getRecordCount()
    }
}