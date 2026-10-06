package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.model.AudioTrack
import java.io.File

class MultitrackAudioEngine(private val context: Context) {

    private val players = mutableMapOf<String, MediaPlayer>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Peak levels for master and tracks
    private val _masterPeak = MutableStateFlow(0f)
    val masterPeak: StateFlow<Float> = _masterPeak.asStateFlow()

    private var currentTracks: List<AudioTrack> = emptyList()
    private var masterVolume: Float = 1.0f

    fun prepareTracks(tracks: List<AudioTrack>, masterVol: Float = 1.0f) {
        stop()
        releasePlayers()
        currentTracks = tracks
        masterVolume = masterVol

        var maxDuration = 0L

        // Check if any track is soloed
        val hasSolo = tracks.any { it.isSolo }

        tracks.forEach { track ->
            val isTrackMuted = track.isMuted || (hasSolo && !track.isSolo)
            val effectiveVol = if (isTrackMuted) 0f else (track.volume * masterVol).coerceIn(0f, 1.5f)

            // Pan calculation: pan is -1.0 (left) to 1.0 (right)
            val leftVol = effectiveVol * ((1.0f - track.pan) / 2.0f).coerceIn(0f, 1f) * 2f
            val rightVol = effectiveVol * ((1.0f + track.pan) / 2.0f).coerceIn(0f, 1f) * 2f

            track.clips.forEach { clip ->
                val file = File(clip.filePath)
                if (file.exists()) {
                    val clipEnd = clip.startOffsetMs + clip.durationMs
                    if (clipEnd > maxDuration) maxDuration = clipEnd

                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(context, Uri.fromFile(file))
                            setVolume(leftVol.coerceIn(0f, 1f), rightVol.coerceIn(0f, 1f))
                            prepare()
                        }
                        players[clip.id] = mp
                    } catch (e: Exception) {
                        // File could be corrupted or unreadable
                    }
                }
            }
        }

        _durationMs.value = maxDuration
    }

    fun play() {
        if (_isPlaying.value) return
        if (players.isEmpty()) return

        val startPos = _currentPositionMs.value

        players.values.forEach { mp ->
            try {
                if (startPos < mp.duration) {
                    mp.seekTo(startPos.toInt())
                    mp.start()
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        _isPlaying.value = true
        startProgressTracking()
    }

    fun pause() {
        if (!_isPlaying.value) return
        players.values.forEach { mp ->
            try {
                if (mp.isPlaying) mp.pause()
            } catch (e: Exception) {
                // ignore
            }
        }
        _isPlaying.value = false
        progressJob?.cancel()
        _masterPeak.value = 0f
    }

    fun stop() {
        pause()
        _currentPositionMs.value = 0L
        players.values.forEach { mp ->
            try {
                mp.seekTo(0)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        players.values.forEach { mp ->
            try {
                mp.seekTo(positionMs.toInt())
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun updateTrackVolumes(tracks: List<AudioTrack>, masterVol: Float) {
        currentTracks = tracks
        masterVolume = masterVol
        val hasSolo = tracks.any { it.isSolo }

        tracks.forEach { track ->
            val isTrackMuted = track.isMuted || (hasSolo && !track.isSolo)
            val effectiveVol = if (isTrackMuted) 0f else (track.volume * masterVol).coerceIn(0f, 1.5f)
            val leftVol = (effectiveVol * ((1.0f - track.pan) / 2.0f).coerceIn(0f, 1f) * 2f).coerceIn(0f, 1f)
            val rightVol = (effectiveVol * ((1.0f + track.pan) / 2.0f).coerceIn(0f, 1f) * 2f).coerceIn(0f, 1f)

            track.clips.forEach { clip ->
                players[clip.id]?.let { mp ->
                    try {
                        mp.setVolume(leftVol, rightVol)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                // Determine current position from primary playing player
                val anyActivePlayer = players.values.firstOrNull { it.isPlaying }
                if (anyActivePlayer != null) {
                    val currentPos = anyActivePlayer.currentPosition.toLong()
                    _currentPositionMs.value = currentPos
                    // Simulate dynamic audio peak meter based on playback status & volume
                    val baseAmp = if (anyActivePlayer.isPlaying) 0.65f * masterVolume.coerceIn(0.2f, 1.2f) else 0f
                    val jitter = ((System.currentTimeMillis() % 17) / 100f)
                    _masterPeak.value = (baseAmp + jitter).coerceIn(0f, 1.1f)

                    if (_durationMs.value > 0 && currentPos >= _durationMs.value) {
                        stop()
                        break
                    }
                } else {
                    stop()
                    break
                }
                delay(30)
            }
        }
    }

    fun release() {
        stop()
        releasePlayers()
    }

    private fun releasePlayers() {
        players.values.forEach {
            try {
                it.release()
            } catch (e: Exception) {
                // ignore
            }
        }
        players.clear()
    }
}
