package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class VoiceRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f) // 0.0f to 1.0f
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private var currentFile: File? = null
    private var recordingStartTime = 0L

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun startRecording(takeName: String): File? {
        if (_isRecording.value) return null

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: SecurityException) {
            return null
        } catch (e: Exception) {
            return null
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return null
        }

        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()

        val safeTakeName = takeName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(recordingsDir, "${safeTakeName}_${System.currentTimeMillis()}.wav")
        currentFile = outputFile

        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            audioRecord?.release()
            audioRecord = null
            return null
        }

        _isRecording.value = true
        recordingStartTime = System.currentTimeMillis()
        _recordingDurationMs.value = 0L

        recordJob = scope.launch {
            val pcmData = ByteArray(bufferSize)
            val fos = FileOutputStream(outputFile)
            // Write placeholder 44-byte WAV header
            val headerPlaceholder = ByteArray(44)
            fos.write(headerPlaceholder)

            var totalAudioLen: Long = 0

            try {
                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(pcmData, 0, bufferSize) ?: -1
                    if (read > 0) {
                        fos.write(pcmData, 0, read)
                        totalAudioLen += read

                        // Compute peak amplitude for meter and waveform
                        var sumSquare = 0.0
                        var maxSample = 0
                        for (i in 0 until read step 2) {
                            if (i + 1 < read) {
                                val sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
                                val sampleShort = sample.toShort()
                                val absVal = abs(sampleShort.toInt())
                                if (absVal > maxSample) maxSample = absVal
                                sumSquare += (sampleShort * sampleShort).toDouble()
                            }
                        }
                        val sampleCount = read / 2
                        val rms = if (sampleCount > 0) sqrt(sumSquare / sampleCount) else 0.0
                        val normRms = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        _currentAmplitude.value = normRms
                        _recordingDurationMs.value = System.currentTimeMillis() - recordingStartTime
                    }
                }
            } catch (e: Exception) {
                // handle safely
            } finally {
                fos.flush()
                fos.close()
                // Update WAV header with accurate length
                writeWavHeader(outputFile, totalAudioLen, SAMPLE_RATE, 1, 16)
            }
        }

        return outputFile
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        _isRecording.value = false
        _currentAmplitude.value = 0f

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord?.release()
        audioRecord = null

        recordJob?.cancel()
        recordJob = null

        return currentFile
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        if (!file.exists() || file.length() < 44) return

        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen.toInt())
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // 'fmt ' chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1.toShort()) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate.toInt())
        buffer.putShort(((channels * bitsPerSample) / 8).toShort()) // BlockAlign
        buffer.putShort(bitsPerSample.toShort())

        // 'data' chunk
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
}
