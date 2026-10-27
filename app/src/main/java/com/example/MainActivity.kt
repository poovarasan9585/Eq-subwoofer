package com.example

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SavePresetDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubwooferScreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AuraEQTheme
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.EqViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: EqViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val accentTheme by viewModel.accentTheme.collectAsState()

            AuraEQTheme(accentTheme = accentTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

data class NavTab(
    val title: String,
    val icon: ImageVector,
    val index: Int
)

@Composable
fun MainAppContent(viewModel: EqViewModel) {
    val view = LocalView.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val showSaveDialog by viewModel.showSavePresetDialog.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val activeColor = accentTheme.primary

    // BackHandler: Return to Equalizer screen (0) if on another tab
    if (currentScreen != 0) {
        BackHandler {
            viewModel.setScreen(0)
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { viewModel.closeSavePresetDialog() },
            onSave = { name -> viewModel.saveCurrentAsPreset(name) },
            activeColor = activeColor
        )
    }

    val tabs = listOf(
        NavTab("Equalizer", Icons.Default.Tune, 0),
        NavTab("Sub DSP", Icons.Default.SpeakerGroup, 1),
        NavTab("Studio", Icons.Default.GraphicEq, 2),
        NavTab("Settings", Icons.Default.Settings, 3)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AmoledBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Persistent Mini Player (shown on screens other than the full Player screen)
                if (currentScreen != 2) {
                    MiniPlayerBar(
                        currentTrack = playbackState.currentTrack,
                        isPlaying = playbackState.isPlaying,
                        progressMs = playbackState.currentPositionMs,
                        durationMs = playbackState.durationMs,
                        isMasterEqEnabled = playbackState.isMasterEqEnabled,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.nextTrack() },
                        onBarClick = { viewModel.setScreen(2) },
                        activeColor = activeColor
                    )
                }

                // Custom Bottom Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F172A), AmoledSurface)
                            )
                        )
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = currentScreen == tab.index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        viewModel.setScreen(tab.index)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent)
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) activeColor else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) activeColor else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // App Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(activeColor, activeColor.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "AuraEQ Logo",
                            tint = AmoledSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AuraEQ",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PRO DSP",
                                color = activeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(activeColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = when (currentScreen) {
                                0 -> "10-Band Graphic Equalizer & Acoustic FX"
                                1 -> "Dedicated 20-200Hz Subwoofer Engine"
                                2 -> "High-Res Audio Player & Telemetry"
                                else -> "Audio Engine Profiles & AMOLED Customization"
                            },
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Animated Screen Switching
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition",
                modifier = Modifier.weight(1f)
            ) { target ->
                when (target) {
                    0 -> EqualizerScreen(viewModel = viewModel)
                    1 -> SubwooferScreen(viewModel = viewModel)
                    2 -> PlayerScreen(viewModel = viewModel)
                    3 -> SettingsScreen(viewModel = viewModel)
                    else -> EqualizerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
