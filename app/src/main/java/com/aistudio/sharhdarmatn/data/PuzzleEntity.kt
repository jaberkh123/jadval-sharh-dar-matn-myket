package com.aistudio.sharhdarmatn.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val id: String,
    val userInput: String, // 100 characters representing the 10x10 grid (space for empty)
    val isCompleted: Boolean,
    val timeSpentSeconds: Long,
    val score: Int,
    val hintsUsed: Int,
    val lastPlayedTime: Long = System.currentTimeMillis()
)
