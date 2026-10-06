package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrack
import com.example.model.EffectConfig
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
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerPanelScreen(viewModel: StudioViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val masterPeak by viewModel.audioEngine.masterPeak.collectAsState()

    var editingFxTrackId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateTo(AppScreen.STUDIO_TIMELINE)
    }

    if (project == null) return
    val activeProj = project!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MIXER & PROCESSAMENTO", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text(activeProj.name, fontSize = 11.sp, color = NeonCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.STUDIO_TIMELINE) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Horizontal scrollable channel strips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Master Channel Strip (Fixed prominent)
                MasterChannelStrip(
                    masterVolume = activeProj.masterVolume,
                    peak = masterPeak,
                    onVolumeChange = { viewModel.updateMasterVolume(it) }
                )

                // Track Channel Strips
                activeProj.tracks.forEach { track ->
                    TrackChannelStrip(
                        track = track,
                        onVolumeChange = { vol -> viewModel.updateTrackVolume(track.id, vol) },
                        onPanChange = { pan -> viewModel.updateTrackPan(track.id, pan) },
                        onMuteToggle = { viewModel.toggleTrackMute(track.id) },
                        onSoloToggle = { viewModel.toggleTrackSolo(track.id) },
                        onOpenFx = { editingFxTrackId = track.id }
                    )
                }
            }

            // FX Rack Section if track selected
            editingFxTrackId?.let { trackId ->
                val track = activeProj.tracks.firstOrNull { it.id == trackId }
                if (track != null) {
                    TrackFxRack(
                        track = track,
                        onUpdateEffects = { eff -> viewModel.updateTrackEffects(track.id, eff) },
                        onClose = { editingFxTrackId = null }
                    )
                }
            }
        }
    }
}

@Composable
fun MasterChannelStrip(
    masterVolume: Float,
    peak: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1824)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricViolet))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("MASTER", fontSize = 12.sp, fontWeight = FontWeight.Black, color = ElectricViolet)

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Peak Meter
                AudioPeakMeter(peak = peak, height = 180.dp, width = 8.dp)

                Spacer(modifier = Modifier.width(8.dp))

                // Fader Slider
                Slider(
                    value = masterVolume,
                    onValueChange = onVolumeChange,
                    valueRange = 0.0f..1.5f,
                    modifier = Modifier.width(48.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricViolet,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Text(
                text = "${(masterVolume * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun TrackChannelStrip(
    track: AudioTrack,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onOpenFx: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = track.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                maxLines = 1
            )

            // Pan Slider
            Text("PAN: ${if (track.pan < 0) "L${(-track.pan*50).toInt()}" else if (track.pan > 0) "R${(track.pan*50).toInt()}" else "C"}", fontSize = 9.sp, color = TextMuted)
            Slider(
                value = track.pan,
                onValueChange = onPanChange,
                valueRange = -1.0f..1.0f,
                modifier = Modifier.fillMaxWidth().height(24.dp),
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )

            // Volume Fader
            Slider(
                value = track.volume,
                onValueChange = onVolumeChange,
                valueRange = 0.0f..1.5f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )

            Text("${(track.volume * 100).toInt()}%", fontSize = 10.sp, color = TextSecondary)

            // FX Button
            TextButton(
                onClick = onOpenFx,
                modifier = Modifier.height(28.dp)
            ) {
                Text("FX RACK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioAmber)
            }

            // Mute / Solo buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = if (track.isMuted) RecordingRed else DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    TextButton(onClick = onMuteToggle, modifier = Modifier.size(32.dp)) {
                        Text("M", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    color = if (track.isSolo) StudioAmber else DarkSurfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    TextButton(onClick = onSoloToggle, modifier = Modifier.size(32.dp)) {
                        Text("S", fontSize = 10.sp, color = if (track.isSolo) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackFxRack(
    track: AudioTrack,
    onUpdateEffects: (EffectConfig) -> Unit,
    onClose: () -> Unit
) {
    var eff by remember(track) { mutableStateOf(track.effects) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioAmber))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CADEIA DE EFEITOS VOCAIS: ${track.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioAmber)
                TextButton(onClick = onClose) {
                    Text("FECHAR", fontSize = 11.sp, color = TextPrimary)
                }
            }

            // Presets row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = false,
                    onClick = {
                        val p = EffectConfig(highPassEnabled = true, compressorEnabled = true, noiseGateEnabled = true, eqTreble = 3f)
                        eff = p
                        onUpdateEffects(p)
                    },
                    label = { Text("Vocal Clean", fontSize = 10.sp) }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        val p = EffectConfig(highPassEnabled = true, compressorEnabled = true, eqTreble = 5f, reverbEnabled = true)
                        eff = p
                        onUpdateEffects(p)
                    },
                    label = { Text("Rap Vocal", fontSize = 10.sp) }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        val p = EffectConfig(highPassEnabled = true, compressorEnabled = true, eqMid = 2f, reverbEnabled = false)
                        eff = p
                        onUpdateEffects(p)
                    },
                    label = { Text("Podcast", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("High-Pass (80Hz)", fontSize = 11.sp, color = TextPrimary)
                    Switch(
                        checked = eff.highPassEnabled,
                        onCheckedChange = {
                            val updated = eff.copy(highPassEnabled = it)
                            eff = updated
                            onUpdateEffects(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compressor", fontSize = 11.sp, color = TextPrimary)
                    Switch(
                        checked = eff.compressorEnabled,
                        onCheckedChange = {
                            val updated = eff.copy(compressorEnabled = it)
                            eff = updated
                            onUpdateEffects(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Noise Gate", fontSize = 11.sp, color = TextPrimary)
                    Switch(
                        checked = eff.noiseGateEnabled,
                        onCheckedChange = {
                            val updated = eff.copy(noiseGateEnabled = it)
                            eff = updated
                            onUpdateEffects(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        }
    }
}
