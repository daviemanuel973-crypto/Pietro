package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CoverSettings
import com.example.model.CoverTemplate
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualCoverScreen(viewModel: StudioViewModel) {
    val project by viewModel.activeProject.collectAsState()
    val previewBitmap by viewModel.previewCoverBitmap.collectAsState()

    BackHandler {
        viewModel.navigateTo(AppScreen.STUDIO_TIMELINE)
    }

    if (project == null) return
    val activeProj = project!!
    var settings by remember(activeProj) { mutableStateOf(activeProj.coverSettings) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CRIADOR DE CAPA & VISUAL", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text(activeProj.name, fontSize = 11.sp, color = StudioAmber)
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
            // Live Preview of Generated Cover
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioAmber))
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Prévia da Capa",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Renderizando Prévia...", color = TextMuted)
                    }
                }
            }

            // Template Selectors
            Text("Estilo & Template", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(CoverTemplate.NEON, CoverTemplate.RAP, CoverTemplate.DARK, CoverTemplate.CINEMATIC, CoverTemplate.MINIMAL).forEach { tmpl ->
                    FilterChip(
                        selected = settings.template == tmpl,
                        onClick = {
                            val updated = settings.copy(template = tmpl)
                            settings = updated
                            viewModel.updateCoverSettings(updated)
                        },
                        label = { Text(tmpl.name, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Aspect Ratio Selector
            Text("Formato de Saída", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("16:9 (YouTube)", "1:1 (Quadrado)", "9:16 (Reels/Shorts)").forEach { ratio ->
                    val key = when {
                        ratio.contains("16:9") -> "16:9"
                        ratio.contains("1:1") -> "1:1"
                        else -> "9:16"
                    }
                    FilterChip(
                        selected = settings.aspectRatio == key,
                        onClick = {
                            val updated = settings.copy(aspectRatio = key)
                            settings = updated
                            viewModel.updateCoverSettings(updated)
                        },
                        label = { Text(ratio, fontSize = 11.sp) }
                    )
                }
            }

            // Text Inputs
            OutlinedTextField(
                value = settings.title,
                onValueChange = {
                    val updated = settings.copy(title = it)
                    settings = updated
                    viewModel.updateCoverSettings(updated)
                },
                label = { Text("Título da Música na Capa", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioAmber,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.artist,
                onValueChange = {
                    val updated = settings.copy(artist = it)
                    settings = updated
                    viewModel.updateCoverSettings(updated)
                },
                label = { Text("Nome do Artista na Capa", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioAmber,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = settings.subtitle,
                onValueChange = {
                    val updated = settings.copy(subtitle = it)
                    settings = updated
                    viewModel.updateCoverSettings(updated)
                },
                label = { Text("Subtítulo / Créditos", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioAmber,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                singleLine = true
            )

            // Darkness Overlay Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Escurecimento de Fundo", fontSize = 12.sp, color = TextSecondary)
                    Text("${(settings.darkness * 100).toInt()}%", fontSize = 12.sp, color = StudioAmber)
                }
                Slider(
                    value = settings.darkness,
                    onValueChange = {
                        val updated = settings.copy(darkness = it)
                        settings = updated
                        viewModel.updateCoverSettings(updated)
                    },
                    valueRange = 0.0f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
                )
            }
        }
    }
}
