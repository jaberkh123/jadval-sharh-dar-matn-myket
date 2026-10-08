package com.aistudio.sharhdarmatn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    @Query("SELECT * FROM puzzle_progress WHERE id = :id")
    fun getPuzzleProgress(id: String): Flow<PuzzleProgressEntity?>

    @Query("SELECT * FROM puzzle_progress")
    fun getAllPuzzleProgress(): Flow<List<PuzzleProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: PuzzleProgressEntity)

    @Query("DELETE FROM puzzle_progress")
    suspend fun clearAllProgress()
}
