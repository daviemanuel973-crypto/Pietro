package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.model.AudioClip
import com.example.model.AudioTrack
import com.example.model.CoverSettings
import com.example.model.CoverTemplate
import com.example.model.EffectConfig
import com.example.model.LyricLine
import com.example.model.Project
import com.example.model.ProjectStatus
import com.example.model.TrackType
import java.io.File

class ProjectRepository(private val context: Context) {

    private val storageDir = File(context.filesDir, "projects").apply {
        if (!exists()) mkdirs()
    }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    suspend fun loadAllProjects() = withContext(Dispatchers.IO) {
        val list = mutableListOf<Project>()
        val files = storageDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()

        for (file in files) {
            try {
                val jsonStr = file.readText()
                val proj = parseProject(JSONObject(jsonStr))
                list.add(proj)
            } catch (e: Exception) {
                // corrupted file
            }
        }

        // If list is empty on first install, create a starter demo project for smooth exploration
        if (list.isEmpty()) {
            val sampleProj = createSampleProject()
            saveProject(sampleProj)
            list.add(sampleProj)
        }

        _projects.value = list.sortedByDescending { it.updatedAt }
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val file = File(storageDir, "${updated.id}.json")
        val json = serializeProject(updated)
        file.writeText(json.toString(2))

        val currentList = _projects.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            currentList[index] = updated
        } else {
            currentList.add(0, updated)
        }
        _projects.value = currentList.sortedByDescending { it.updatedAt }
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val file = File(storageDir, "$projectId.json")
        if (file.exists()) file.delete()
        _projects.value = _projects.value.filter { it.id != projectId }
    }

    suspend fun duplicateProject(projectId: String): Project? = withContext(Dispatchers.IO) {
        val existing = _projects.value.firstOrNull { it.id == projectId } ?: return@withContext null
        val newProj = existing.copy(
            id = "proj_${System.currentTimeMillis()}",
            name = "${existing.name} (Cópia)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(newProj)
        newProj
    }

    fun getProject(id: String): Project? {
        return _projects.value.firstOrNull { it.id == id }
    }

    private fun createSampleProject(): Project {
        val id = "proj_demo_1"
        return Project(
            id = id,
            name = "Rhythm & Voice Vol. 1",
            artistName = "MC Studio",
            bpm = 135,
            musicalKey = "D Minor",
            status = ProjectStatus.RECORDING,
            tracks = listOf(
                AudioTrack(
                    id = "track_inst",
                    name = "Instrumental Beat",
                    type = TrackType.INSTRUMENTAL,
                    volume = 0.9f
                ),
                AudioTrack(
                    id = "track_lead",
                    name = "Voz Principal",
                    type = TrackType.VOCAL_LEAD,
                    volume = 1.1f
                ),
                AudioTrack(
                    id = "track_back",
                    name = "Backing Vocal",
                    type = TrackType.VOCAL_BACKING,
                    volume = 0.8f
                ),
                AudioTrack(
                    id = "track_adlib",
                    name = "Adlibs & FX",
                    type = TrackType.VOCAL_ADLIB,
                    volume = 0.75f
                )
            ),
            lyrics = listOf(
                LyricLine(id = "l1", text = "Gravei no VoxFrame do celular", startMs = 0, endMs = 3000),
                LyricLine(id = "l2", text = "Mixei a voz sem sair do lugar", startMs = 3200, endMs = 6500),
                LyricLine(id = "l3", text = "Capa estilosa e lyric visual", startMs = 6800, endMs = 10000),
                LyricLine(id = "l4", text = "Estúdio de bolso profissional", startMs = 10200, endMs = 14000)
            ),
            coverSettings = CoverSettings(
                template = CoverTemplate.NEON,
                title = "Rhythm & Voice Vol. 1",
                artist = "MC Studio",
                subtitle = "Single Oficial • 2026",
                primaryColorHex = "#00E5FF"
            )
        )
    }

    private fun serializeProject(p: Project): JSONObject {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("artistName", p.artistName)
        obj.put("createdAt", p.createdAt)
        obj.put("updatedAt", p.updatedAt)
        obj.put("bpm", p.bpm)
        obj.put("musicalKey", p.musicalKey)
        obj.put("status", p.status.name)
        obj.put("notes", p.notes)
        obj.put("masterVolume", p.masterVolume.toDouble())

        // Tracks
        val tracksArr = JSONArray()
        p.tracks.forEach { t ->
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("name", t.name)
            tObj.put("type", t.type.name)
            tObj.put("volume", t.volume.toDouble())
            tObj.put("pan", t.pan.toDouble())
            tObj.put("isMuted", t.isMuted)
            tObj.put("isSolo", t.isSolo)

            val clipsArr = JSONArray()
            t.clips.forEach { c ->
                val cObj = JSONObject()
                cObj.put("id", c.id)
                cObj.put("name", c.name)
                cObj.put("filePath", c.filePath)
                cObj.put("startOffsetMs", c.startOffsetMs)
                cObj.put("sourceStartMs", c.sourceStartMs)
                cObj.put("durationMs", c.durationMs)
                cObj.put("gain", c.gain.toDouble())
                cObj.put("fadeInMs", c.fadeInMs)
                cObj.put("fadeOutMs", c.fadeOutMs)
                clipsArr.put(cObj)
            }
            tObj.put("clips", clipsArr)

            // Effects
            val effObj = JSONObject()
            effObj.put("gain", t.effects.gain.toDouble())
            effObj.put("highPassEnabled", t.effects.highPassEnabled)
            effObj.put("highPassHz", t.effects.highPassHz.toDouble())
            effObj.put("noiseGateEnabled", t.effects.noiseGateEnabled)
            effObj.put("compressorEnabled", t.effects.compressorEnabled)
            effObj.put("eqBass", t.effects.eqBass.toDouble())
            effObj.put("eqMid", t.effects.eqMid.toDouble())
            effObj.put("eqTreble", t.effects.eqTreble.toDouble())
            effObj.put("reverbEnabled", t.effects.reverbEnabled)
            tObj.put("effects", effObj)

            tracksArr.put(tObj)
        }
        obj.put("tracks", tracksArr)

        // Lyrics
        val lyricsArr = JSONArray()
        p.lyrics.forEach { l ->
            val lObj = JSONObject()
            lObj.put("id", l.id)
            lObj.put("text", l.text)
            lObj.put("startMs", l.startMs)
            lObj.put("endMs", l.endMs)
            lyricsArr.put(lObj)
        }
        obj.put("lyrics", lyricsArr)

        // CoverSettings
        val coverObj = JSONObject()
        coverObj.put("template", p.coverSettings.template.name)
        coverObj.put("title", p.coverSettings.title)
        coverObj.put("artist", p.coverSettings.artist)
        coverObj.put("subtitle", p.coverSettings.subtitle)
        coverObj.put("primaryColorHex", p.coverSettings.primaryColorHex)
        coverObj.put("aspectRatio", p.coverSettings.aspectRatio)
        coverObj.put("darkness", p.coverSettings.darkness.toDouble())
        obj.put("coverSettings", coverObj)

        return obj
    }

    private fun parseProject(obj: JSONObject): Project {
        val tracksList = mutableListOf<AudioTrack>()
        val tracksArr = obj.optJSONArray("tracks")
        if (tracksArr != null) {
            for (i in 0 until tracksArr.length()) {
                val tObj = tracksArr.getJSONObject(i)
                val clipsList = mutableListOf<AudioClip>()
                val clipsArr = tObj.optJSONArray("clips")
                if (clipsArr != null) {
                    for (j in 0 until clipsArr.length()) {
                        val cObj = clipsArr.getJSONObject(j)
                        clipsList.add(
                            AudioClip(
                                id = cObj.getString("id"),
                                name = cObj.getString("name"),
                                filePath = cObj.getString("filePath"),
                                startOffsetMs = cObj.optLong("startOffsetMs", 0L),
                                sourceStartMs = cObj.optLong("sourceStartMs", 0L),
                                durationMs = cObj.optLong("durationMs", 0L),
                                gain = cObj.optDouble("gain", 1.0).toFloat(),
                                fadeInMs = cObj.optLong("fadeInMs", 0L),
                                fadeOutMs = cObj.optLong("fadeOutMs", 0L)
                            )
                        )
                    }
                }

                var eff = EffectConfig()
                val effObj = tObj.optJSONObject("effects")
                if (effObj != null) {
                    eff = EffectConfig(
                        gain = effObj.optDouble("gain", 1.0).toFloat(),
                        highPassEnabled = effObj.optBoolean("highPassEnabled", false),
                        noiseGateEnabled = effObj.optBoolean("noiseGateEnabled", false),
                        compressorEnabled = effObj.optBoolean("compressorEnabled", false),
                        eqBass = effObj.optDouble("eqBass", 0.0).toFloat(),
                        eqMid = effObj.optDouble("eqMid", 0.0).toFloat(),
                        eqTreble = effObj.optDouble("eqTreble", 0.0).toFloat(),
                        reverbEnabled = effObj.optBoolean("reverbEnabled", false)
                    )
                }

                tracksList.add(
                    AudioTrack(
                        id = tObj.getString("id"),
                        name = tObj.getString("name"),
                        type = try { TrackType.valueOf(tObj.getString("type")) } catch (e: Exception) { TrackType.CUSTOM },
                        volume = tObj.optDouble("volume", 1.0).toFloat(),
                        pan = tObj.optDouble("pan", 0.0).toFloat(),
                        isMuted = tObj.optBoolean("isMuted", false),
                        isSolo = tObj.optBoolean("isSolo", false),
                        clips = clipsList,
                        effects = eff
                    )
                )
            }
        }

        val lyricsList = mutableListOf<LyricLine>()
        val lyricsArr = obj.optJSONArray("lyrics")
        if (lyricsArr != null) {
            for (i in 0 until lyricsArr.length()) {
                val lObj = lyricsArr.getJSONObject(i)
                lyricsList.add(
                    LyricLine(
                        id = lObj.getString("id"),
                        text = lObj.getString("text"),
                        startMs = lObj.optLong("startMs", 0L),
                        endMs = lObj.optLong("endMs", 0L)
                    )
                )
            }
        }

        val coverObj = obj.optJSONObject("coverSettings")
        val coverSettings = if (coverObj != null) {
            CoverSettings(
                template = try { CoverTemplate.valueOf(coverObj.optString("template", "NEON")) } catch (e: Exception) { CoverTemplate.NEON },
                title = coverObj.optString("title", ""),
                artist = coverObj.optString("artist", ""),
                subtitle = coverObj.optString("subtitle", ""),
                primaryColorHex = coverObj.optString("primaryColorHex", "#00E5FF"),
                aspectRatio = coverObj.optString("aspectRatio", "16:9"),
                darkness = coverObj.optDouble("darkness", 0.3).toFloat()
            )
        } else {
            CoverSettings()
        }

        return Project(
            id = obj.getString("id"),
            name = obj.getString("name"),
            artistName = obj.optString("artistName", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            bpm = obj.optInt("bpm", 120),
            musicalKey = obj.optString("musicalKey", "C Minor"),
            status = try { ProjectStatus.valueOf(obj.optString("status", "RECORDING")) } catch (e: Exception) { ProjectStatus.RECORDING },
            tracks = tracksList,
            lyrics = lyricsList,
            coverSettings = coverSettings,
            notes = obj.optString("notes", ""),
            masterVolume = obj.optDouble("masterVolume", 1.0).toFloat()
        )
    }
}
