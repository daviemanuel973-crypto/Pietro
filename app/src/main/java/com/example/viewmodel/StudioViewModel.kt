package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.audio.AudioMixerRenderer
import com.example.audio.MultitrackAudioEngine
import com.example.audio.VoiceRecorder
import com.example.data.ProjectRepository
import com.example.model.AudioClip
import com.example.model.AudioTrack
import com.example.model.CoverSettings
import com.example.model.CoverTemplate
import com.example.model.EffectConfig
import com.example.model.LyricLine
import com.example.model.Project
import com.example.model.ProjectStatus
import com.example.model.TrackType
import com.example.visual.CoverArtGenerator
import com.example.visual.VideoEncoderEngine
import java.io.File
import java.io.FileOutputStream

enum class AppScreen {
    PROJECTS_LIST,
    STUDIO_TIMELINE,
    RECORDING_STUDIO,
    MIXER_PANEL,
    LYRICS_SYNC,
    VISUAL_COVER,
    EXPORT_STUDIO,
    YOUTUBE_PUBLISH
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    val voiceRecorder = VoiceRecorder(application)
    val audioEngine = MultitrackAudioEngine(application)

    val projects = repository.projects

    private val _currentScreen = MutableStateFlow(AppScreen.PROJECTS_LIST)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Export progress and result states
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow("")
    val exportStatusMessage: StateFlow<String> = _exportStatusMessage.asStateFlow()

    private val _exportedAudioFile = MutableStateFlow<File?>(null)
    val exportedAudioFile: StateFlow<File?> = _exportedAudioFile.asStateFlow()

    private val _exportedVideoFile = MutableStateFlow<File?>(null)
    val exportedVideoFile: StateFlow<File?> = _exportedVideoFile.asStateFlow()

    private val _previewCoverBitmap = MutableStateFlow<Bitmap?>(null)
    val previewCoverBitmap: StateFlow<Bitmap?> = _previewCoverBitmap.asStateFlow()

    // Monitoring toggle
    private val _isMonitoringEnabled = MutableStateFlow(false)
    val isMonitoringEnabled: StateFlow<Boolean> = _isMonitoringEnabled.asStateFlow()

    // Selected track for FX/details
    private val _selectedTrackId = MutableStateFlow<String?>(null)
    val selectedTrackId: StateFlow<String?> = _selectedTrackId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadAllProjects()
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleMonitoring() {
        _isMonitoringEnabled.value = !_isMonitoringEnabled.value
    }

    fun selectTrack(trackId: String?) {
        _selectedTrackId.value = trackId
    }

    fun openProject(project: Project) {
        _activeProject.value = project
        _currentScreen.value = AppScreen.STUDIO_TIMELINE
        audioEngine.prepareTracks(project.tracks, project.masterVolume)
        refreshCoverPreview(project.coverSettings)
    }

    fun createNewProject(name: String, artist: String = "", bpm: Int = 120, musicalKey: String = "C Minor") {
        viewModelScope.launch {
            val newProj = Project(
                id = "proj_${System.currentTimeMillis()}",
                name = if (name.isNotBlank()) name else "Novo Beat",
                artistName = artist,
                bpm = bpm,
                musicalKey = musicalKey,
                status = ProjectStatus.RECORDING,
                tracks = listOf(
                    AudioTrack(
                        id = "track_inst_${System.currentTimeMillis()}",
                        name = "Instrumental",
                        type = TrackType.INSTRUMENTAL
                    ),
                    AudioTrack(
                        id = "track_lead_${System.currentTimeMillis()}",
                        name = "Voz Principal",
                        type = TrackType.VOCAL_LEAD
                    ),
                    AudioTrack(
                        id = "track_back_${System.currentTimeMillis()}",
                        name = "Backing Vocal",
                        type = TrackType.VOCAL_BACKING
                    )
                ),
                coverSettings = CoverSettings(
                    title = name,
                    artist = artist,
                    template = CoverTemplate.NEON
                )
            )
            repository.saveProject(newProj)
            openProject(newProj)
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_activeProject.value?.id == projectId) {
                audioEngine.stop()
                _activeProject.value = null
                _currentScreen.value = AppScreen.PROJECTS_LIST
            }
        }
    }

    fun addTrack(type: TrackType, name: String) {
        val current = _activeProject.value ?: return
        val newTrack = AudioTrack(
            id = "track_${System.currentTimeMillis()}",
            name = if (name.isNotBlank()) name else "Nova Faixa",
            type = type
        )
        val updatedTracks = current.tracks + newTrack
        val updatedProj = current.copy(tracks = updatedTracks)
        updateAndSaveProject(updatedProj)
    }

    fun updateTrackVolume(trackId: String, volume: Float) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map {
            if (it.id == trackId) it.copy(volume = volume) else it
        }
        val updated = current.copy(tracks = updatedTracks)
        _activeProject.value = updated
        audioEngine.updateTrackVolumes(updatedTracks, updated.masterVolume)
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun updateTrackPan(trackId: String, pan: Float) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map {
            if (it.id == trackId) it.copy(pan = pan) else it
        }
        val updated = current.copy(tracks = updatedTracks)
        _activeProject.value = updated
        audioEngine.updateTrackVolumes(updatedTracks, updated.masterVolume)
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun toggleTrackMute(trackId: String) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map {
            if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
        }
        val updated = current.copy(tracks = updatedTracks)
        _activeProject.value = updated
        audioEngine.updateTrackVolumes(updatedTracks, updated.masterVolume)
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun toggleTrackSolo(trackId: String) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map {
            if (it.id == trackId) it.copy(isSolo = !it.isSolo) else it
        }
        val updated = current.copy(tracks = updatedTracks)
        _activeProject.value = updated
        audioEngine.updateTrackVolumes(updatedTracks, updated.masterVolume)
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun updateMasterVolume(vol: Float) {
        val current = _activeProject.value ?: return
        val updated = current.copy(masterVolume = vol)
        _activeProject.value = updated
        audioEngine.updateTrackVolumes(updated.tracks, vol)
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun updateTrackEffects(trackId: String, effects: EffectConfig) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map {
            if (it.id == trackId) it.copy(effects = effects) else it
        }
        val updated = current.copy(tracks = updatedTracks)
        updateAndSaveProject(updated)
    }

    fun importAudioFile(uri: Uri, targetTrackType: TrackType = TrackType.INSTRUMENTAL) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val importsDir = File(context.filesDir, "imports").apply { if (!exists()) mkdirs() }
                val importedFile = File(importsDir, "import_${System.currentTimeMillis()}.wav")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(importedFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (importedFile.exists() && importedFile.length() > 0) {
                    val durationMs = 180_000L // Default estimate if header parsing requires ExoPlayer
                    val clip = AudioClip(
                        id = "clip_${System.currentTimeMillis()}",
                        name = "Beat Importado",
                        filePath = importedFile.absolutePath,
                        startOffsetMs = 0L,
                        durationMs = durationMs
                    )

                    val current = _activeProject.value ?: return@launch
                    // Check if instrumental track exists
                    val existingTrack = current.tracks.firstOrNull { it.type == targetTrackType }
                    val updatedTracks = if (existingTrack != null) {
                        current.tracks.map {
                            if (it.id == existingTrack.id) it.copy(clips = it.clips + clip) else it
                        }
                    } else {
                        current.tracks + AudioTrack(
                            id = "track_${System.currentTimeMillis()}",
                            name = "Instrumental Beat",
                            type = targetTrackType,
                            clips = listOf(clip)
                        )
                    }

                    val updated = current.copy(tracks = updatedTracks, status = ProjectStatus.EDITING)
                    updateAndSaveProject(updated)
                    audioEngine.prepareTracks(updatedTracks, updated.masterVolume)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun startVocalRecording(targetTrackId: String? = null) {
        val proj = _activeProject.value ?: return
        val takeCount = proj.tracks.flatMap { it.clips }.count { it.name.startsWith("Voz Take") } + 1
        val takeName = "Voz Take $takeCount"

        val file = voiceRecorder.startRecording(takeName)
        if (file != null) {
            // If backing playback is desired during recording
            audioEngine.play()
        }
    }

    fun stopVocalRecording(targetTrackId: String? = null) {
        val recordedFile = voiceRecorder.stopRecording()
        audioEngine.pause()

        if (recordedFile != null && recordedFile.exists()) {
            val proj = _activeProject.value ?: return
            val takeCount = proj.tracks.flatMap { it.clips }.count { it.name.startsWith("Voz Take") } + 1
            val clipName = "Voz Take $takeCount"
            val durationMs = voiceRecorder.recordingDurationMs.value.coerceAtLeast(1000L)

            val newClip = AudioClip(
                id = "clip_${System.currentTimeMillis()}",
                name = clipName,
                filePath = recordedFile.absolutePath,
                startOffsetMs = 0L,
                durationMs = durationMs
            )

            // Find or select target track (lead vocal by default)
            val trackToUse = if (targetTrackId != null) {
                proj.tracks.firstOrNull { it.id == targetTrackId }
            } else {
                proj.tracks.firstOrNull { it.type == TrackType.VOCAL_LEAD } ?: proj.tracks.firstOrNull()
            }

            if (trackToUse != null) {
                val updatedTracks = proj.tracks.map {
                    if (it.id == trackToUse.id) it.copy(clips = it.clips + newClip) else it
                }
                val updated = proj.copy(tracks = updatedTracks, status = ProjectStatus.EDITING)
                updateAndSaveProject(updated)
                audioEngine.prepareTracks(updatedTracks, updated.masterVolume)
            }
        }
    }

    fun deleteClip(trackId: String, clipId: String) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.filter { it.id != clipId })
            } else track
        }
        val updated = current.copy(tracks = updatedTracks)
        updateAndSaveProject(updated)
        audioEngine.prepareTracks(updatedTracks, updated.masterVolume)
    }

    fun splitClipAtPosition(trackId: String, clipId: String, splitPositionMs: Long) {
        val current = _activeProject.value ?: return
        val updatedTracks = current.tracks.map { track ->
            if (track.id == trackId) {
                val clip = track.clips.firstOrNull { it.id == clipId }
                if (clip != null && splitPositionMs > clip.startOffsetMs && splitPositionMs < clip.startOffsetMs + clip.durationMs) {
                    val firstDur = splitPositionMs - clip.startOffsetMs
                    val secondDur = clip.durationMs - firstDur
                    val clip1 = clip.copy(id = "clip_${System.currentTimeMillis()}_1", durationMs = firstDur)
                    val clip2 = clip.copy(
                        id = "clip_${System.currentTimeMillis()}_2",
                        startOffsetMs = splitPositionMs,
                        sourceStartMs = clip.sourceStartMs + firstDur,
                        durationMs = secondDur
                    )
                    val newClips = track.clips.filter { it.id != clipId } + listOf(clip1, clip2)
                    track.copy(clips = newClips)
                } else track
            } else track
        }
        val updated = current.copy(tracks = updatedTracks)
        updateAndSaveProject(updated)
        audioEngine.prepareTracks(updatedTracks, updated.masterVolume)
    }

    // Lyrics operations
    fun updateLyrics(lyrics: List<LyricLine>) {
        val current = _activeProject.value ?: return
        val updated = current.copy(lyrics = lyrics)
        updateAndSaveProject(updated)
    }

    fun addLyricLine(text: String, startMs: Long = 0L, endMs: Long = 0L) {
        val current = _activeProject.value ?: return
        val newLine = LyricLine(
            id = "lyric_${System.currentTimeMillis()}",
            text = text,
            startMs = startMs,
            endMs = endMs
        )
        val updated = current.copy(lyrics = current.lyrics + newLine)
        updateAndSaveProject(updated)
    }

    fun syncCurrentLyricLine(lineId: String, timestampMs: Long) {
        val current = _activeProject.value ?: return
        val updatedLyrics = current.lyrics.map {
            if (it.id == lineId) {
                it.copy(startMs = timestampMs, endMs = timestampMs + 3000L)
            } else it
        }
        val updated = current.copy(lyrics = updatedLyrics)
        updateAndSaveProject(updated)
    }

    // Cover Visual settings
    fun updateCoverSettings(settings: CoverSettings) {
        val current = _activeProject.value ?: return
        val updated = current.copy(coverSettings = settings)
        _activeProject.value = updated
        viewModelScope.launch {
            repository.saveProject(updated)
            refreshCoverPreview(settings)
        }
    }

    private fun refreshCoverPreview(settings: CoverSettings) {
        viewModelScope.launch {
            val bitmap = CoverArtGenerator.generateCoverBitmap(
                context = getApplication(),
                settings = settings,
                targetWidth = 640,
                targetHeight = 360
            )
            _previewCoverBitmap.value = bitmap
        }
    }

    // Export Mixdown WAV
    fun renderAudioMixdown(onComplete: (File?) -> Unit = {}) {
        val current = _activeProject.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportStatusMessage.value = "Renderizando mixagem de áudio..."

            val mixFile = AudioMixerRenderer.renderMixdown(
                context = getApplication(),
                tracks = current.tracks,
                masterGain = current.masterVolume,
                outputFileName = "${current.name.replace(" ", "_")}_mixdown.wav",
                onProgress = { progress ->
                    _exportProgress.value = progress
                }
            )

            _exportedAudioFile.value = mixFile
            _isExporting.value = false
            _exportStatusMessage.value = if (mixFile != null) "Mixagem concluída com sucesso!" else "Erro na renderização"
            onComplete(mixFile)
        }
    }

    // Export Full MP4 Video
    fun renderVideoExport(aspectRatio: String = "16:9", onComplete: (File?) -> Unit = {}) {
        val current = _activeProject.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportStatusMessage.value = "Gerando capa em alta resolução..."

            val width = if (aspectRatio == "1:1") 1080 else 1920
            val height = if (aspectRatio == "1:1") 1080 else 1080

            val coverBitmap = CoverArtGenerator.generateCoverBitmap(
                context = getApplication(),
                settings = current.coverSettings,
                targetWidth = width,
                targetHeight = height
            )

            _exportStatusMessage.value = "Renderizando mixagem e faixas..."
            val mixFile = AudioMixerRenderer.renderMixdown(
                context = getApplication(),
                tracks = current.tracks,
                masterGain = current.masterVolume,
                outputFileName = "temp_mix_${System.currentTimeMillis()}.wav",
                onProgress = { p -> _exportProgress.value = p * 0.25f }
            )

            if (mixFile == null) {
                _isExporting.value = false
                _exportStatusMessage.value = "Erro: adicione áudio ao projeto para renderizar o vídeo"
                return@launch
            }

            _exportStatusMessage.value = "Codificando vídeo MP4 H.264 + AAC..."
            val videoFile = VideoEncoderEngine.renderVideo(
                context = getApplication(),
                audioFile = mixFile,
                coverBitmap = coverBitmap,
                lyrics = current.lyrics,
                width = width,
                height = height,
                outputFileName = "${current.name.replace(" ", "_")}_VoxFrame.mp4",
                onProgress = { p ->
                    _exportProgress.value = 0.25f + (p * 0.75f)
                }
            )

            _exportedVideoFile.value = videoFile
            _isExporting.value = false
            _exportStatusMessage.value = if (videoFile != null) "Vídeo exportado com sucesso!" else "Erro ao codificar vídeo"
            onComplete(videoFile)
        }
    }

    private fun updateAndSaveProject(updated: Project) {
        _activeProject.value = updated
        viewModelScope.launch {
            repository.saveProject(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
