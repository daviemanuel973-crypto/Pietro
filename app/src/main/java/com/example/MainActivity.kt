package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ExportStudioScreen
import com.example.ui.screens.LyricsSyncScreen
import com.example.ui.screens.MixerPanelScreen
import com.example.ui.screens.ProjectsListScreen
import com.example.ui.screens.StudioTimelineScreen
import com.example.ui.screens.VisualCoverScreen
import com.example.ui.screens.YouTubePublishScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val studioViewModel: StudioViewModel = viewModel()
                val currentScreen by studioViewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    when (currentScreen) {
                        AppScreen.PROJECTS_LIST -> ProjectsListScreen(studioViewModel)
                        AppScreen.STUDIO_TIMELINE -> StudioTimelineScreen(studioViewModel)
                        AppScreen.RECORDING_STUDIO -> StudioTimelineScreen(studioViewModel)
                        AppScreen.MIXER_PANEL -> MixerPanelScreen(studioViewModel)
                        AppScreen.LYRICS_SYNC -> LyricsSyncScreen(studioViewModel)
                        AppScreen.VISUAL_COVER -> VisualCoverScreen(studioViewModel)
                        AppScreen.EXPORT_STUDIO -> ExportStudioScreen(studioViewModel)
                        AppScreen.YOUTUBE_PUBLISH -> YouTubePublishScreen(studioViewModel)
                    }
                }
            }
        }
    }
}
