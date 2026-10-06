package com.example.model

enum class ProjectStatus {
    RECORDING,
    EDITING,
    MIXING,
    COMPLETED
}

enum class TrackType {
    INSTRUMENTAL,
    VOCAL_LEAD,
    VOCAL_BACKING,
    VOCAL_ADLIB,
    CUSTOM
}

data class AudioClip(
    val id: String,
    val name: String,
    val filePath: String,
    val startOffsetMs: Long = 0L,     // Offset in timeline
    val sourceStartMs: Long = 0L,     // Trim in source audio
    val durationMs: Long = 0L,        // Trimmed duration to play
    val gain: Float = 1.0f,           // 0.0f to 2.0f
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
)

data class EffectConfig(
    val gain: Float = 1.0f,
    val highPassEnabled: Boolean = false,
    val highPassHz: Float = 80f,
    val noiseGateEnabled: Boolean = false,
    val noiseGateThreshold: Float = -40f, // in dB
    val compressorEnabled: Boolean = false,
    val compressorRatio: Float = 3.0f,
    val compressorThreshold: Float = -18f,
    val eqBass: Float = 0f,    // -12dB to +12dB
    val eqMid: Float = 0f,
    val eqTreble: Float = 0f,
    val reverbEnabled: Boolean = false,
    val reverbDecay: Float = 0.3f, // 0.0 to 1.0
    val limiterEnabled: Boolean = true
)

data class AudioTrack(
    val id: String,
    val name: String,
    val type: TrackType,
    val volume: Float = 1.0f, // 0.0f to 1.5f
    val pan: Float = 0.0f,    // -1.0f (Left) to +1.0f (Right)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val clips: List<AudioClip> = emptyList(),
    val effects: EffectConfig = EffectConfig()
)

data class LyricLine(
    val id: String,
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 0L
)

enum class CoverTemplate {
    MINIMAL,
    RAP,
    DARK,
    NEON,
    CLEAN,
    CINEMATIC
}

data class CoverSettings(
    val template: CoverTemplate = CoverTemplate.NEON,
    val title: String = "",
    val artist: String = "",
    val subtitle: String = "",
    val primaryColorHex: String = "#00E5FF",
    val secondaryColorHex: String = "#8B5CF6",
    val backgroundImageUri: String? = null,
    val blur: Float = 0f,
    val darkness: Float = 0.3f,
    val aspectRatio: String = "16:9" // "16:9", "1:1", "9:16"
)

data class Project(
    val id: String,
    val name: String,
    val artistName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val bpm: Int = 120,
    val musicalKey: String = "C Minor",
    val status: ProjectStatus = ProjectStatus.RECORDING,
    val tracks: List<AudioTrack> = emptyList(),
    val lyrics: List<LyricLine> = emptyList(),
    val coverSettings: CoverSettings = CoverSettings(),
    val notes: String = "",
    val masterVolume: Float = 1.0f
) {
    val totalDurationMs: Long
        get() {
            var maxEnd = 0L
            for (track in tracks) {
                for (clip in track.clips) {
                    val end = clip.startOffsetMs + clip.durationMs
                    if (end > maxEnd) maxEnd = end
                }
            }
            return maxEnd
        }
}
