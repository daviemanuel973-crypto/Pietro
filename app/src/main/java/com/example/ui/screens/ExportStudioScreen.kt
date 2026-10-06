package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import com.example.ui.theme.WaveformGreen
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStudioScreen(viewModel: StudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.activeProject.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportMessage by viewModel.exportStatusMessage.collectAsState()
    val exportedAudio by viewModel.exportedAudioFile.collectAsState()
    val exportedVideo by viewModel.exportedVideoFile.collectAsState()

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
                        Text("CENTRAL DE EXPORTAÇÃO", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text(activeProj.name, fontSize = 11.sp, color = WaveformGreen)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Exporting Progress Bar
            if (isExporting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonCyan,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(exportMessage, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = NeonCyan,
                            trackColor = Color(0xFF1E293B)
                        )

                        Text(
                            text = "${(exportProgress * 100).toInt()}% concluído",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Audio WAV Export Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Mixagem de Áudio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Text(
                        text = "Gera arquivo WAV Estéreo de 16-bit 44.1kHz sem perdas com master limiter ativo.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { viewModel.renderAudioMixdown() },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_audio")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Renderizar Áudio WAV", fontWeight = FontWeight.Bold)
                    }

                    // Audio exported result
                    exportedAudio?.let { audioFile ->
                        Surface(
                            color = Color(0xFF0D251E),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WaveformGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(audioFile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${audioFile.length() / 1024} KB", fontSize = 10.sp, color = TextMuted)
                                    }
                                }

                                IconButton(
                                    onClick = { shareFile(context, audioFile, "audio/wav") }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = WaveformGreen)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Video MP4 Export Section (Static Cover or Lyric Video)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkSurfaceBorder))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Vídeo MP4 (1080p)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Text(
                        text = "Combina a capa do visual, faixa sincronizada de lyrics e a mix final em vídeo H.264 pronto para YouTube.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { viewModel.renderVideoExport("16:9") },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_export_video")
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Renderizar Lyric/Cover Vídeo MP4", fontWeight = FontWeight.Bold)
                    }

                    // Video exported result
                    exportedVideo?.let { videoFile ->
                        Surface(
                            color = Color(0xFF2A163B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(videoFile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${videoFile.length() / (1024 * 1024)} MB", fontSize = 10.sp, color = TextMuted)
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { shareFile(context, videoFile, "video/mp4") }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = NeonCyan)
                                    }

                                    IconButton(
                                        onClick = { viewModel.navigateTo(AppScreen.YOUTUBE_PUBLISH) }
                                    ) {
                                        Icon(Icons.Default.Upload, contentDescription = "Publicar no YouTube", tint = RecordingRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick YouTube Publishing CTA
            Button(
                onClick = { viewModel.navigateTo(AppScreen.YOUTUBE_PUBLISH) },
                colors = ButtonDefaults.buttonColors(containerColor = RecordingRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publicar Diretamente no YouTube", fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun shareFile(context: android.content.Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar arquivo"))
    } catch (e: Exception) {
        // Fallback or handle provider exception
    }
}
