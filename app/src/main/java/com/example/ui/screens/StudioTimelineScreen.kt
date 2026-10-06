package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioClip
import com.example.model.AudioTrack
import com.example.model.Project
import com.example.model.TrackType
import com.example.ui.components.AudioPeakMeter
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackColorAdlibs
import com.example.ui.theme.TrackColorBacking
import com.example.ui.theme.TrackColorCustom
import com.example.ui.theme.TrackColorInstrumental
import com.example.ui.theme.TrackColorVocalLead
import com.example.ui.theme.WaveformGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTimelineScreen(viewModel: StudioViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.audioEngine.durationMs.collectAsState()
    val masterPeak by viewModel.audioEngine.masterPeak.collectAsState()

    val isRecording by viewModel.voiceRecorder.isRecording.collectAsState()
    val recordDurationMs by viewModel.voiceRecorder.recordingDurationMs.collectAsState()
    val micAmplitude by viewModel.voiceRecorder.currentAmplitude.collectAsState()
    val isMonitoring by viewModel.isMonitoringEnabled.collectAsState()

    var timelineZoom by remember { mutableFloatStateOf(1.0f) }
    var selectedClipForEdit by remember { mutableStateOf<Pair<AudioTrack, AudioClip>?>(null) }
    var showAddTrackDialog by remember { mutableStateOf(false) }

    // Launcher for file import
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(uri, TrackType.INSTRUMENTAL)
        }
    }

    // Permission launcher for Recording
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVocalRecording()
        }
    }

    BackHandler {
        viewModel.navigateTo(AppScreen.PROJECTS_LIST)
    }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum projeto ativo", color = TextPrimary)
        }
        return
    }

    val activeProj = project!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeProj.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${activeProj.bpm} BPM • ${activeProj.musicalKey}",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                            if (isRecording) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• GRAVANDO [${formatTime(recordDurationMs)}]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RecordingRed
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.PROJECTS_LIST) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                actions = {
                    // Quick Navigation tabs: Mixer, Letra, Capa, Exportar
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.MIXER_PANEL) }) {
                        Text("MIXER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.LYRICS_SYNC) }) {
                        Text("LETRA", color = ElectricViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.VISUAL_COVER) }) {
                        Text("VISUAL", color = StudioAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.navigateTo(AppScreen.EXPORT_STUDIO) }) {
                        Text("EXPORTAR", color = WaveformGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            // Main Transport & Studio Control Bar
            StudioTransportBar(
                isPlaying = isPlaying,
                isRecording = isRecording,
                isMonitoring = isMonitoring,
                currentPosMs = currentPositionMs,
                totalDurMs = durationMs,
                masterPeak = masterPeak,
                micAmplitude = micAmplitude,
                timelineZoom = timelineZoom,
                onPlayPause = {
                    if (isPlaying) viewModel.audioEngine.pause() else viewModel.audioEngine.play()
                },
                onStop = {
                    viewModel.audioEngine.stop()
                },
                onRecord = {
                    if (isRecording) {
                        viewModel.stopVocalRecording()
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onToggleMonitoring = { viewModel.toggleMonitoring() },
                onZoomIn = { timelineZoom = (timelineZoom + 0.25f).coerceAtMost(3.0f) },
                onZoomOut = { timelineZoom = (timelineZoom - 0.25f).coerceAtLeast(0.5f) },
                onImportBeat = { audioPickerLauncher.launch("audio/*") }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Timeline Tools Row: Add Track, Import Instrumental, Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { showAddTrackDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Faixa", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar Beat", fontSize = 12.sp)
                    }
                }

                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            // Timeline Tracks LazyColumn
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(DarkBackground)
            ) {
                val horizontalScrollState = rememberScrollState()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeProj.tracks, key = { it.id }) { track ->
                        TimelineTrackRow(
                            track = track,
                            currentPosMs = currentPositionMs,
                            durationMs = durationMs.coerceAtLeast(60_000L),
                            zoom = timelineZoom,
                            scrollState = horizontalScrollState,
                            onMute = { viewModel.toggleTrackMute(track.id) },
                            onSolo = { viewModel.toggleTrackSolo(track.id) },
                            onSelectClip = { clip ->
                                selectedClipForEdit = Pair(track, clip)
                            }
                        )
                    }
                }
            }
        }
    }

    // Clip Editor Modal / Dialog
    selectedClipForEdit?.let { (track, clip) ->
        ClipEditorDialog(
            track = track,
            clip = clip,
            currentCursorMs = currentPositionMs,
            onDismiss = { selectedClipForEdit = null },
            onDelete = {
                viewModel.deleteClip(track.id, clip.id)
                selectedClipForEdit = null
            },
            onSplitAtCursor = {
                viewModel.splitClipAtPosition(track.id, clip.id, currentPositionMs)
                selectedClipForEdit = null
            }
        )
    }

    // Add Track Dialog
    if (showAddTrackDialog) {
        var trackName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(TrackType.VOCAL_BACKING) }

        AlertDialog(
            onDismissRequest = { showAddTrackDialog = false },
            containerColor = DarkSurface,
            title = { Text("Adicionar Nova Faixa", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedType == TrackType.VOCAL_BACKING,
                            onClick = { selectedType = TrackType.VOCAL_BACKING },
                            label = { Text("Backing") }
                        )
                        FilterChip(
                            selected = selectedType == TrackType.VOCAL_ADLIB,
                            onClick = { selectedType = TrackType.VOCAL_ADLIB },
                            label = { Text("Adlibs") }
                        )
                        FilterChip(
                            selected = selectedType == TrackType.VOCAL_LEAD,
                            onClick = { selectedType = TrackType.VOCAL_LEAD },
                            label = { Text("Voz Lead") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (trackName.isNotBlank()) trackName else when (selectedType) {
                            TrackType.VOCAL_BACKING -> "Backing Vocal"
                            TrackType.VOCAL_ADLIB -> "Adlibs"
                            TrackType.VOCAL_LEAD -> "Voz Principal 2"
                            else -> "Faixa"
                        }
                        viewModel.addTrack(selectedType, finalName)
                        showAddTrackDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrackDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun TimelineTrackRow(
    track: AudioTrack,
    currentPosMs: Long,
    durationMs: Long,
    zoom: Float,
    scrollState: androidx.compose.foundation.ScrollState,
    onMute: () -> Unit,
    onSolo: () -> Unit,
    onSelectClip: (AudioClip) -> Unit
) {
    val trackColor = when (track.type) {
        TrackType.INSTRUMENTAL -> TrackColorInstrumental
        TrackType.VOCAL_LEAD -> TrackColorVocalLead
        TrackType.VOCAL_BACKING -> TrackColorBacking
        TrackType.VOCAL_ADLIB -> TrackColorAdlibs
        TrackType.CUSTOM -> TrackColorCustom
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(DarkSurface)
    ) {
        // Track Header (Sticky Left Panel)
        Column(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .background(DarkSurfaceVariant)
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = track.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = trackColor,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = if (track.isMuted) RecordingRed else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .clickable { onMute() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("M", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Surface(
                    color = if (track.isSolo) StudioAmber else Color(0xFF1E293B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .clickable { onSolo() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("S", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (track.isSolo) Color.Black else Color.White)
                }
            }
        }

        // Timeline Track Body (Horizontally Scrollable Clips)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .background(Color(0xFF111319))
        ) {
            val totalTimelineWidth = (1200 * zoom).dp

            Box(modifier = Modifier.width(totalTimelineWidth).fillMaxHeight()) {
                // Background grid lines (bars)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidthPx = 80.dp.toPx() * zoom
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = Color(0xFF1E222D),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += barWidthPx
                    }

                    // Playhead indicator
                    val playheadX = (currentPosMs.toFloat() / durationMs.toFloat()) * size.width
                    drawLine(
                        color = NeonCyan,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, size.height),
                        strokeWidth = 3f
                    )
                }

                // Render Clips
                track.clips.forEach { clip ->
                    val clipStartFrac = (clip.startOffsetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    val clipDurFrac = (clip.durationMs.toFloat() / durationMs.toFloat()).coerceIn(0.01f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(trackColor.copy(alpha = 0.85f))
                            .clickable { onSelectClip(clip) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = clip.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1
                            )
                            Text(
                                text = formatTime(clip.durationMs),
                                fontSize = 9.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioTransportBar(
    isPlaying: Boolean,
    isRecording: Boolean,
    isMonitoring: Boolean,
    currentPosMs: Long,
    totalDurMs: Long,
    masterPeak: Float,
    micAmplitude: Float,
    timelineZoom: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onRecord: () -> Unit,
    onToggleMonitoring: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onImportBeat: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 8.dp,
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top Row: Peak Meter, Time, Zoom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Peak Meter + Monitoring status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AudioPeakMeter(
                        peak = if (isRecording) micAmplitude * 1.2f else masterPeak,
                        height = 36.dp,
                        width = 8.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onToggleMonitoring,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Monitoramento",
                            tint = if (isMonitoring) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isMonitoring) {
                        Text("MONITOR ATIVO", fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }

                // Zoom Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Text("${(timelineZoom * 100).toInt()}%", fontSize = 11.sp, color = TextMuted)
                    IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Buttons Row: Stop, Play/Pause, Record Big Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TextPrimary)
                }

                // Play / Pause Button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .testTag("btn_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Record Vocal Take Button
                IconButton(
                    onClick = onRecord,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) RecordingRed else Color(0xFF4A101D))
                        .testTag("btn_record_vocal")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Gravar Voz",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClipEditorDialog(
    track: AudioTrack,
    clip: AudioClip,
    currentCursorMs: Long,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSplitAtCursor: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Editar Clip: ${clip.name}", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Faixa: ${track.name}", fontSize = 13.sp, color = NeonCyan)
                Text("Duração: ${formatTime(clip.durationMs)}", fontSize = 12.sp, color = TextSecondary)
                Text("Offset Início: ${formatTime(clip.startOffsetMs)}", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSplitAtCursor,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dividir no Cursor (${formatTime(currentCursorMs)})")
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = RecordingRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir Take/Clip")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TextPrimary)
            }
        }
    )
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val millis = (ms % 1000) / 100
    return String.format("%02d:%02d.%01d", min, sec, millis)
}
