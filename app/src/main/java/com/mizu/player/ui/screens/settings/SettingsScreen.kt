package com.mizu.player.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mizu.player.data.preferences.UserPreferencesManager
import com.mizu.player.domain.model.PlayerEngine
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuChip
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val playerEngine: StateFlow<PlayerEngine> = preferencesManager.playerEngine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerEngine.AUTO)

    val resumePlayback: StateFlow<Boolean> = preferencesManager.resumePlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoPlayNext: StateFlow<Boolean> = preferencesManager.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val darkMode: StateFlow<String> = preferencesManager.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val seekDuration: StateFlow<Int> = preferencesManager.seekDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val defaultSpeed: StateFlow<Float> = preferencesManager.defaultSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    fun setPlayerEngine(engine: PlayerEngine) {
        viewModelScope.launch { preferencesManager.setPlayerEngine(engine) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setResumePlayback(enabled) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoPlayNext(enabled) }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { preferencesManager.setDarkMode(mode) }
    }

    fun setSeekDuration(seconds: Int) {
        viewModelScope.launch { preferencesManager.setSeekDuration(seconds) }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch { preferencesManager.setDefaultSpeed(speed) }
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = MizuThemeObj.colors
    val scrollState = rememberScrollState()

    val playerEngine by viewModel.playerEngine.collectAsState()
    val resumePlayback by viewModel.resumePlayback.collectAsState()
    val autoPlayNext by viewModel.autoPlayNext.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val seekDuration by viewModel.seekDuration.collectAsState()
    val defaultSpeed by viewModel.defaultSpeed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.ink
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = colors.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Section
            SettingsSection(title = "PLAYBACK ENGINE", icon = Icons.Default.Tune) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Engine Selection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Auto picks Standard (ExoPlayer + FFmpeg) and switches to Compatibility (LibVLC) for legacy/exotic codecs or complex subtitles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeuChip(
                            text = "Auto (Recommended)",
                            selected = playerEngine == PlayerEngine.AUTO,
                            onClick = { viewModel.setPlayerEngine(PlayerEngine.AUTO) },
                            selectedColor = Sun
                        )
                        NeuChip(
                            text = "Standard (Exo)",
                            selected = playerEngine == PlayerEngine.EXOPLAYER,
                            onClick = { viewModel.setPlayerEngine(PlayerEngine.EXOPLAYER) },
                            selectedColor = Aqua
                        )
                        NeuChip(
                            text = "Compatibility (VLC)",
                            selected = playerEngine == PlayerEngine.VLC,
                            onClick = { viewModel.setPlayerEngine(PlayerEngine.VLC) },
                            selectedColor = Lime
                        )
                    }
                }
            }

            // Playback Options Section
            SettingsSection(title = "PLAYBACK BEHAVIOR", icon = Icons.Default.PlayCircle) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resume playback",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Text(
                                text = "Remember position and resume videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        Switch(
                            checked = resumePlayback,
                            onCheckedChange = { viewModel.setResumePlayback(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Ink,
                                checkedTrackColor = Sun,
                                uncheckedThumbColor = Ink,
                                uncheckedTrackColor = colors.surface
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autoplay next media",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Text(
                                text = "Play next item in folder or playlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = { viewModel.setAutoPlayNext(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Ink,
                                checkedTrackColor = Lime,
                                uncheckedThumbColor = Ink,
                                uncheckedTrackColor = colors.surface
                            )
                        )
                    }
                }
            }

            // Seeking & Gestures Section
            SettingsSection(title = "SEEK DURATION", icon = Icons.Default.FastForward) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Double Tap & Button Seek Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Configures both the double-tap seek gesture and on-screen seek buttons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30, 60).forEach { sec ->
                            NeuChip(
                                text = "${sec}s",
                                selected = seekDuration == sec,
                                onClick = { viewModel.setSeekDuration(sec) }
                            )
                        }
                    }
                }
            }

            // Speed Section
            SettingsSection(title = "DEFAULT PLAYBACK SPEED", icon = Icons.Default.Speed) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            NeuChip(
                                text = "${speed}x",
                                selected = defaultSpeed == speed,
                                onClick = { viewModel.setDefaultSpeed(speed) }
                            )
                        }
                    }
                }
            }

            // Appearance Section
            SettingsSection(title = "APPEARANCE", icon = Icons.Default.DarkMode) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeuChip(
                            text = "System",
                            selected = darkMode == "system",
                            onClick = { viewModel.setDarkMode("system") }
                        )
                        NeuChip(
                            text = "Light",
                            selected = darkMode == "light",
                            onClick = { viewModel.setDarkMode("light") }
                        )
                        NeuChip(
                            text = "Dark",
                            selected = darkMode == "dark",
                            onClick = { viewModel.setDarkMode("dark") }
                        )
                    }
                }
            }
                            selected = darkMode == "dark",
                            onClick = { viewModel.setDarkMode("dark") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    val colors = MizuThemeObj.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = colors.ink
            )
        }

        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface
        ) {
            content()
        }
    }
}
