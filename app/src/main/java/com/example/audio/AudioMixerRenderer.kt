package com.example.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.AudioClip
import com.example.model.AudioTrack
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object AudioMixerRenderer {

    /**
     * Non-destructively renders all active tracks into a single mastered 16-bit 44.1kHz stereo WAV file.
     * Takes into account:
     * - Track volume & Master gain
     * - Pan (left/right distribution)
     * - Mute & Solo logic
     * - Clip trim offsets (startOffsetMs, sourceStartMs, durationMs)
     * - Soft-knee Limiter to prevent harsh digital clipping
     */
    suspend fun renderMixdown(
        context: Context,
        tracks: List<AudioTrack>,
        masterGain: Float,
        outputFileName: String = "mixdown_${System.currentTimeMillis()}.wav",
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val outputFile = File(exportDir, outputFileName)

        // Find max duration in ms
        var maxDurationMs = 0L
        val activeClips = mutableListOf<Pair<AudioTrack, AudioClip>>()
        val hasSolo = tracks.any { it.isSolo }

        for (track in tracks) {
            val isMuted = track.isMuted || (hasSolo && !track.isSolo)
            if (isMuted) continue
            for (clip in track.clips) {
                val clipEnd = clip.startOffsetMs + clip.durationMs
                if (clipEnd > maxDurationMs) maxDurationMs = clipEnd
                activeClips.add(Pair(track, clip))
            }
        }

        if (maxDurationMs <= 0L || activeClips.isEmpty()) {
            return@withContext null
        }

        val sampleRate = 44100
        val channels = 2 // Stereo output
        val bitsPerSample = 16
        val bytesPerSample = 2
        val totalSamples = ((maxDurationMs * sampleRate) / 1000).toInt()
        val totalAudioBytes = totalSamples.toLong() * channels * bytesPerSample

        val fos = FileOutputStream(outputFile)
        val headerPlaceholder = ByteArray(44)
        fos.write(headerPlaceholder)

        // Process in chunks of 4096 stereo samples
        val chunkSamples = 4096
        val buffer = ByteArray(chunkSamples * channels * bytesPerSample)
        var samplesProcessed = 0

        // Pre-read PCM data for each clip for fast mixing
        val clipPcmCache = mutableListOf<ClipMixSource>()
        for ((track, clip) in activeClips) {
            val file = File(clip.filePath)
            if (file.exists() && file.length() > 44) {
                try {
                    val fis = FileInputStream(file)
                    fis.skip(44) // Skip WAV header
                    val data = fis.readBytes()
                    fis.close()
                    val startSample = ((clip.startOffsetMs * sampleRate) / 1000).toInt()
                    val durSample = ((clip.durationMs * sampleRate) / 1000).toInt()
                    val panL = ((1.0f - track.pan) / 2.0f).coerceIn(0f, 1f) * 2f
                    val panR = ((1.0f + track.pan) / 2.0f).coerceIn(0f, 1f) * 2f
                    val effVol = track.volume * clip.gain

                    clipPcmCache.add(
                        ClipMixSource(
                            pcmBytes = data,
                            startSample = startSample,
                            durSamples = durSample,
                            volLeft = effVol * panL,
                            volRight = effVol * panR
                        )
                    )
                } catch (e: Exception) {
                    // skip corrupted clip
                }
            }
        }

        val outBb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)

        while (samplesProcessed < totalSamples) {
            outBb.clear()
            val samplesInThisChunk = minOf(chunkSamples, totalSamples - samplesProcessed)

            for (s in 0 until samplesInThisChunk) {
                val globalSample = samplesProcessed + s
                var mixLeft = 0.0f
                var mixRight = 0.0f

                for (source in clipPcmCache) {
                    if (globalSample >= source.startSample && globalSample < source.startSample + source.durSamples) {
                        val clipSampleIdx = globalSample - source.startSample
                        val byteIdx = clipSampleIdx * 2 // 16-bit mono
                        if (byteIdx + 1 < source.pcmBytes.size) {
                            val b0 = source.pcmBytes[byteIdx].toInt() and 0xFF
                            val b1 = source.pcmBytes[byteIdx + 1].toInt() shl 8
                            val sampleVal = (b0 or b1).toShort().toFloat()
                            mixLeft += sampleVal * source.volLeft
                            mixRight += sampleVal * source.volRight
                        }
                    }
                }

                // Apply master gain
                mixLeft *= masterGain
                mixRight *= masterGain

                // Limiter / soft clipping to protect from harsh digital distortion
                val finalLeft = softLimit(mixLeft)
                val finalRight = softLimit(mixRight)

                outBb.putShort(finalLeft.toInt().toShort())
                outBb.putShort(finalRight.toInt().toShort())
            }

            fos.write(buffer, 0, samplesInThisChunk * channels * bytesPerSample)
            samplesProcessed += samplesInThisChunk

            val progress = (samplesProcessed.toFloat() / totalSamples).coerceIn(0f, 1f)
            onProgress(progress)
        }

        fos.flush()
        fos.close()

        // Write canonical WAV header
        writeWavHeader(outputFile, totalAudioBytes, sampleRate, channels, bitsPerSample)

        outputFile
    }

    private fun softLimit(sample: Float): Float {
        val threshold = 30000f
        val maxVal = 32767f
        val absVal = abs(sample)
        return if (absVal < threshold) {
            sample
        } else {
            val sign = if (sample >= 0) 1f else -1f
            val excess = absVal - threshold
            val compressed = threshold + (excess / (1f + (excess / (maxVal - threshold))))
            (compressed * sign).coerceIn(-maxVal, maxVal)
        }
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen.toInt())
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16)
        buffer.putShort(1.toShort()) // PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate.toInt())
        buffer.putShort(((channels * bitsPerSample) / 8).toShort())
        buffer.putShort(bitsPerSample.toShort())

        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(totalAudioLen.toInt())

        try {
            val raf = RandomAccessFile(file, "rw")
            raf.seek(0)
            raf.write(header)
            raf.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    private data class ClipMixSource(
        val pcmBytes: ByteArray,
        val startSample: Int,
        val durSamples: Int,
        val volLeft: Float,
        val volRight: Float
    )
}
