package com.aistudio.sharhdarmatn.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PuzzleRepository(private val context: Context, private val puzzleDao: PuzzleDao) {

    val allProgress: Flow<List<PuzzleProgressEntity>> = puzzleDao.getAllPuzzleProgress()

    fun getPuzzleProgress(puzzleId: String): Flow<PuzzleProgressEntity?> {
        return puzzleDao.getPuzzleProgress(puzzleId)
    }

    suspend fun saveProgress(
        puzzleId: String,
        userInput: String,
        isCompleted: Boolean,
        timeSpentSeconds: Long,
        score: Int,
        hintsUsed: Int
    ) {
        val entity = PuzzleProgressEntity(
            id = puzzleId,
            userInput = userInput,
            isCompleted = isCompleted,
            timeSpentSeconds = timeSpentSeconds,
            score = score,
            hintsUsed = hintsUsed,
            lastPlayedTime = System.currentTimeMillis()
        )
        puzzleDao.insertOrUpdate(entity)
    }

    suspend fun clearAllProgress() {
        puzzleDao.clearAllProgress()
    }

    // Shared preferences for setting state
    private val prefs = context.getSharedPreferences("sharh_dm_prefs", Context.MODE_PRIVATE)

    fun isInstantCheckEnabled(): Boolean = prefs.getBoolean("instant_check", true)
    fun setInstantCheckEnabled(enabled: Boolean) = prefs.edit().putBoolean("instant_check", enabled).apply()

    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean("sound_enabled", enabled).apply()

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean("dark_mode", false)
    fun setDarkModeEnabled(enabled: Boolean) = prefs.edit().putBoolean("dark_mode", enabled).apply()

    fun getClueFontSize(): Float = prefs.getFloat("clue_font_size", 16f)
    fun setClueFontSize(size: Float) = prefs.edit().putFloat("clue_font_size", size).apply()

    fun hasShownOnboarding(): Boolean = prefs.getBoolean("shown_onboarding", false)
    fun setShownOnboarding(shown: Boolean) = prefs.edit().putBoolean("shown_onboarding", shown).apply()
}
