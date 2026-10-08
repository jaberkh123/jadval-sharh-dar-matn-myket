package com.aistudio.sharhdarmatn.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PuzzleProgressEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sharh_dm_database_v1"
                )
                // ⚠️ NO fallbackToDestructiveMigration() — it would silently WIPE all user
                // progress on any future schema version bump. If the schema ever changes,
                // write a proper Migration() instead. Keep the DB name stable across updates.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
