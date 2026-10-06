package com.example.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object WaveformExtractor {

    /**
     * Extracts normalized waveform amplitude samples (0.0f to 1.0f) from a WAV or PCM file.
     * @param file The audio file
     * @param targetPoints The number of amplitude bars to return (e.g., 60 or 100)
     */
    suspend fun extractAmplitudes(file: File, targetPoints: Int = 80): List<Float> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() < 44) {
            return@withContext List(targetPoints) { 0.15f }
        }

        try {
            val fis = FileInputStream(file)
            val header = ByteArray(44)
            fis.read(header)

            val totalBytes = file.length() - 44
            val sampleCount = totalBytes / 2 // 16-bit PCM
            if (sampleCount <= 0) {
                fis.close()
                return@withContext List(targetPoints) { 0.1f }
            }

            val samplesPerPoint = (sampleCount / targetPoints).toInt().coerceAtLeast(1)
            val stepBytes = samplesPerPoint * 2
            val buffer = ByteArray(stepBytes.coerceAtMost(8192))

            val amplitudes = mutableListOf<Float>()

            for (i in 0 until targetPoints) {
                var maxSample = 0
                val bytesToRead = stepBytes.coerceAtMost(buffer.size)
                val read = fis.read(buffer, 0, bytesToRead)
                if (read > 0) {
                    val bb = ByteBuffer.wrap(buffer, 0, read).order(ByteOrder.LITTLE_ENDIAN)
                    while (bb.remaining() >= 2) {
                        val sample = abs(bb.short.toInt())
                        if (sample > maxSample) maxSample = sample
                    }
                    val normalized = (maxSample / 32768.0f).coerceIn(0.05f, 1.0f)
                    amplitudes.add(normalized)
                } else {
                    amplitudes.add(0.05f)
                }

                // Skip remainder of the window if window was larger than buffer
                val remainingToSkip = (stepBytes - bytesToRead).toLong()
                if (remainingToSkip > 0) {
                    fis.skip(remainingToSkip)
                }
            }
            fis.close()
            amplitudes
        } catch (e: Exception) {
            List(targetPoints) { 0.2f }
        }
    }
}
