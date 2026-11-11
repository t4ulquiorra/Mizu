package com.mizu.player.ui.screens.search

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.domain.repository.MediaRepository
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuChip
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val matchedVideos: StateFlow<List<VideoItem>> = _query.flatMapLatest { q ->
        repository.searchVideos(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchedAudio: StateFlow<List<AudioItem>> = _query.flatMapLatest { q ->
        repository.searchAudio(q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(newQuery: String) {
        _query.value = newQuery
    }
}

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onMediaClick: (Uri, Boolean) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val colors = MizuThemeObj.colors
    val query by viewModel.query.collectAsState()
    val videos by viewModel.matchedVideos.collectAsState()
    val audioList by viewModel.matchedAudio.collectAsState()

    var filterType by remember { mutableStateOf("ALL") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // Search Input Top Bar
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

            Spacer(Modifier.width(4.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text("Search videos and music…", color = colors.ink.copy(alpha = 0.5f)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.ink
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = colors.ink
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                    .background(colors.surface)
                    .border(
                        NeuTokens.BorderWidth,
                        colors.border,
                        RoundedCornerShape(NeuTokens.ButtonRadius)
                    ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeuChip(
                text = "All (${videos.size + audioList.size})",
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" }
            )
            NeuChip(
                text = "Videos (${videos.size})",
                selected = filterType == "VIDEOS",
                onClick = { filterType = "VIDEOS" }
            )
            NeuChip(
                text = "Audio (${audioList.size})",
                selected = filterType == "AUDIO",
                onClick = { filterType = "AUDIO" }
            )
        }

        // Results
        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type to search your media files",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink.copy(alpha = 0.5f)
                )
            }
        } else if (videos.isEmpty() && audioList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results found for \"$query\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filterType == "ALL" || filterType == "VIDEOS") {
                    items(videos, key = { "v_${it.id}" }) { video ->
                        SearchResultItem(
                            title = video.title,
                            subtitle = "${FormatUtils.formatDuration(video.duration)} • ${video.folderName}",
                            isVideo = true,
                            onClick = { onMediaClick(video.uri, true) }
                        )
                    }
                }

                if (filterType == "ALL" || filterType == "AUDIO") {
                    items(audioList, key = { "a_${it.id}" }) { audio ->
                        SearchResultItem(
                            title = audio.title,
                            subtitle = "${audio.artist} • ${FormatUtils.formatDuration(audio.duration)}",
                            isVideo = false,
                            onClick = { onMediaClick(audio.uri, false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    title: String,
    subtitle: String,
    isVideo: Boolean,
    onClick: () -> Unit
) {
    val colors = MizuThemeObj.colors

    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isVideo) Sun else Aqua)
                    .border(1.5.dp, Ink, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
