package com.aistudio.sharhdarmatn.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.sin
import kotlin.math.PI

object SoundManager {
    private var isEnabled = true
    
    private var clickTrack: AudioTrack? = null
    private var cellSelectTrack: AudioTrack? = null
    private var typeTrack: AudioTrack? = null
    private var deleteTrack: AudioTrack? = null
    private var checkTrack: AudioTrack? = null
    private var successTrack: AudioTrack? = null
    private var scoreGainTrack: AudioTrack? = null
    private var scoreLossTrack: AudioTrack? = null

    init {
        try {
            initTracks()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initTracks() {
        val sampleRate = 22050
        
        // 1. Click Track: 480 Hz, 60 ms
        clickTrack = createSineTrack(480.0, 0.06, 12000, sampleRate)
        
        // 2. Cell Select Track: 620 Hz, 40 ms
        cellSelectTrack = createSineTrack(620.0, 0.04, 10000, sampleRate)
        
        // 3. Type Track: 700 Hz, 30 ms (crisp quick pop)
        typeTrack = createSineTrack(700.0, 0.03, 8000, sampleRate)
        
        // 4. Delete Track: 380 Hz, 80 ms
        deleteTrack = createSineTrack(380.0, 0.08, 10000, sampleRate)
        
        // 5. Check Track: Two tones combined (440 Hz & 554 Hz), 120 ms
        checkTrack = createDoubleToneTrack(440.0, 554.0, 0.12, 9000, sampleRate)
        
        // 6. Success Track: Ascending arpeggio (C major: 523 Hz -> 659 Hz -> 784 Hz), 350 ms total
        successTrack = createArpeggioTrack(listOf(523.25, 659.25, 783.99), 0.35, 11000, sampleRate)
        
        // 7. Score Gain Track: Sparkly arpeggio, 250 ms
        scoreGainTrack = createArpeggioTrack(listOf(587.33, 739.99, 880.0), 0.25, 11000, sampleRate)
        
        // 8. Score Loss Track: Descending soft glide (350 Hz down to 220 Hz), 200 ms
        scoreLossTrack = createGlideTrack(350.0, 220.0, 0.20, 10000, sampleRate)
    }

    private fun createSineTrack(freq: Double, durationSecs: Double, maxAmp: Short, sampleRate: Int): AudioTrack? {
        try {
            val numSamples = (sampleRate * durationSecs).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = Math.pow(1.0 - i.toDouble() / numSamples, 2.5)
                val angle = 2.0 * PI * freq * t
                buffer[i] = (sin(angle) * maxAmp * env).toInt().toShort()
            }
            return buildStaticTrack(buffer, sampleRate)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun createDoubleToneTrack(freq1: Double, freq2: Double, durationSecs: Double, maxAmp: Short, sampleRate: Int): AudioTrack? {
        try {
            val numSamples = (sampleRate * durationSecs).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = Math.pow(1.0 - i.toDouble() / numSamples, 2.0)
                val angle1 = 2.0 * PI * freq1 * t
                val angle2 = 2.0 * PI * freq2 * t
                val mixed = (sin(angle1) + sin(angle2)) * 0.5
                buffer[i] = (mixed * maxAmp * env).toInt().toShort()
            }
            return buildStaticTrack(buffer, sampleRate)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun createArpeggioTrack(freqs: List<Double>, durationSecs: Double, maxAmp: Short, sampleRate: Int): AudioTrack? {
        try {
            val numSamples = (sampleRate * durationSecs).toInt()
            val buffer = ShortArray(numSamples)
            val stepSize = numSamples / freqs.size
            for (i in 0 until numSamples) {
                val stepIdx = (i / stepSize).coerceAtMost(freqs.size - 1)
                val freq = freqs[stepIdx]
                val t = i.toDouble() / sampleRate
                val env = Math.pow(1.0 - i.toDouble() / numSamples, 1.8)
                val stepSampleIdx = i % stepSize
                val stepEnv = Math.pow(1.0 - stepSampleIdx.toDouble() / stepSize, 1.2)
                
                val angle = 2.0 * PI * freq * t
                buffer[i] = (sin(angle) * maxAmp * env * stepEnv).toInt().toShort()
            }
            return buildStaticTrack(buffer, sampleRate)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun createGlideTrack(startFreq: Double, endFreq: Double, durationSecs: Double, maxAmp: Short, sampleRate: Int): AudioTrack? {
        try {
            val numSamples = (sampleRate * durationSecs).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val ratio = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * ratio
                val t = i.toDouble() / sampleRate
                val env = Math.pow(1.0 - ratio, 2.0)
                val angle = 2.0 * PI * currentFreq * t
                buffer[i] = (sin(angle) * maxAmp * env).toInt().toShort()
            }
            return buildStaticTrack(buffer, sampleRate)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun buildStaticTrack(buffer: ShortArray, sampleRate: Int): AudioTrack {
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * 2,
            AudioTrack.MODE_STATIC
        )
        audioTrack.write(buffer, 0, buffer.size)
        return audioTrack
    }

    fun setSoundEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    private fun playTrack(track: AudioTrack?) {
        if (!isEnabled || track == null) return
        try {
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClick() {
        playTrack(clickTrack)
    }

    fun playCellSelect() {
        playTrack(cellSelectTrack)
    }

    fun playType() {
        playTrack(typeTrack)
    }

    fun playDelete() {
        playTrack(deleteTrack)
    }

    fun playCheck() {
        playTrack(checkTrack)
    }

    fun playSuccess() {
        playTrack(successTrack)
    }

    fun playScoreGain() {
        playTrack(scoreGainTrack)
    }

    fun playScoreLoss() {
        playTrack(scoreLossTrack)
    }
}
