package com.example.game.ui.components

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object GameAudioSynthesizer {
    var isSoundEnabled: Boolean = true

    fun playChime(frequencies: List<Float>, durationMs: Long = 150) {
        if (!isSoundEnabled) return
        kotlin.concurrent.thread {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val sampleCount = (sampleRate * (durationMs / 1000.0f)).toInt()
                val totalSamples = sampleCount * frequencies.size
                val samples = ShortArray(totalSamples)

                for (fIdx in frequencies.indices) {
                    val freq = frequencies[fIdx]
                    val startIdx = fIdx * sampleCount
                    for (i in 0 until sampleCount) {
                        val t = i.toFloat() / sampleRate
                        val angle = 2.0 * Math.PI * freq * t
                        val envelope = if (i > sampleCount * 0.7f) {
                            (sampleCount - i).toFloat() / (sampleCount * 0.3f)
                        } else {
                            1.0f
                        }
                        samples[startIdx + i] = (Math.sin(angle) * Short.MAX_VALUE * 0.4f * envelope).toInt().toShort()
                    }
                }

                val bufferSize = maxOf(minBufferSize, totalSamples * 2)
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )

                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.play()
                    audioTrack.write(samples, 0, samples.size)
                    Thread.sleep((durationMs * frequencies.size) + 100)
                    try { audioTrack.stop() } catch (t: Throwable) {}
                    try { audioTrack.release() } catch (t: Throwable) {}
                }
            } catch (e: Throwable) {
                // Completely safe from crashing the thread/JVM
            }
        }
    }

    fun playNormalPlay() {
        // Double soft ping (D5 -> A5)
        playChime(listOf(587.33f, 880.00f), durationMs = 100)
    }

    fun playHighScorePlay() {
        // Energetic major arpeggio (C5 -> E5 -> G5 -> C6)
        playChime(listOf(523.25f, 659.25f, 783.99f, 1046.50f), durationMs = 125)
    }

    fun playMegaScorePlay() {
        // Sparkling cascading melody (C5 -> E5 -> G5 -> C6 -> E6 -> G6 -> C7)
        playChime(listOf(523.25f, 659.25f, 783.99f, 1046.50f, 1318.51f, 1567.98f, 2093.00f), durationMs = 95)
    }
}
