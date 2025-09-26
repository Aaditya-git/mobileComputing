package com.example.contextmonitor.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [SympFields::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDataDao(): CrudOps

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}