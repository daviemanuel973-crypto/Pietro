package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.LyricLine
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSyncScreen(viewModel: StudioViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
    val currentPositionMs by viewModel.audioEngine.currentPositionMs.collectAsState()

    var newLineText by remember { mutableStateOf("") }
    var selectedLineForSyncIndex by remember { mutableStateOf(0) }

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
                        Text("LETRA & SINCRONIZAÇÃO LYRIC", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text("${activeProj.lyrics.size} versos sincronizados", fontSize = 11.sp, color = ElectricViolet)
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
        bottomBar = {
            // Live Sync Button: Tap to mark timestamp for the active line
            Surface(
                color = DarkSurface,
                tonalElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) viewModel.audioEngine.pause() else viewModel.audioEngine.play()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (activeProj.lyrics.isNotEmpty() && selectedLineForSyncIndex in activeProj.lyrics.indices) {
                                val line = activeProj.lyrics[selectedLineForSyncIndex]
                                viewModel.syncCurrentLyricLine(line.id, currentPositionMs)
                                if (selectedLineForSyncIndex < activeProj.lyrics.size - 1) {
                                    selectedLineForSyncIndex++
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(horizontal = 8.dp)
                            .testTag("btn_sync_verse")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SINCRONIZAR VERSO (${formatTime(currentPositionMs)})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Add Line input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newLineText,
                    onValueChange = { newLineText = it },
                    placeholder = { Text("Digite ou cole uma linha de letra...", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (newLineText.isNotBlank()) {
                            viewModel.addLyricLine(newLineText, currentPositionMs, currentPositionMs + 3000L)
                            newLineText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            // Lyric lines list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeProj.lyrics.withIndex().toList()) { (index, line) ->
                    val isSelectedForSync = index == selectedLineForSyncIndex
                    val isCurrentlyActiveInSong = currentPositionMs in line.startMs..line.endMs

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLineForSyncIndex = index },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentlyActiveInSong) Color(0xFF2E1B4E) else if (isSelectedForSync) DarkSurfaceVariant else DarkSurface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isCurrentlyActiveInSong) NeonCyan else if (isSelectedForSync) ElectricViolet else DarkSurfaceBorder
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectedForSync) ElectricViolet else TextMuted,
                                modifier = Modifier.width(28.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.text,
                                    fontSize = 15.sp,
                                    fontWeight = if (isCurrentlyActiveInSong) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrentlyActiveInSong) NeonCyan else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatTime(line.startMs)} → ${formatTime(line.endMs)}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            IconButton(
                                onClick = {
                                    val updated = activeProj.lyrics.filter { it.id != line.id }
                                    viewModel.updateLyrics(updated)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir Linha", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
