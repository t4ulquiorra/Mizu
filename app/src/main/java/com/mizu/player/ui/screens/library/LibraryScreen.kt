package com.mizu.player.ui.screens.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.domain.model.FolderInfo
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.components.SelectionBar
import com.mizu.player.ui.components.VideoInfoSheet
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lavender
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun

@Composable
fun LibraryScreen(
    onNavigateToPlayer: (Uri, Boolean) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToFolder: (String, String) -> Unit = { _, _ -> },
    onNavigateToPlaylist: (Long, String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colors = MizuThemeObj.colors

    val videos by viewModel.videos.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val sortOrder by viewModel.sortOrder.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val selectedVideoIds by viewModel.selectedVideoIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val inspectedVideo by viewModel.inspectedVideo.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to Videos tab
    val tabTitles = listOf("FOLDERS", "VIDEOS", "AUDIO", "PLAYLISTS")
    val tabColors = listOf(Sun, Aqua, Lavender, Lime)

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.paper)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                        .background(Sun)
                        .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MIZU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Ink,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                // Search
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.ink
                    )
                }

                // Refresh scan
                IconButton(onClick = { viewModel.refreshMedia() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = colors.ink
                    )
                }

                // Settings
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.ink
                    )
                }

                // About
                IconButton(onClick = onNavigateToAbout) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = colors.ink
                    )
                }
            }
        },
        containerColor = colors.paper
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Neubrutalist Tabs Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(NeuTokens.ChipRadius))
                                .background(if (isSelected) tabColors[index] else colors.surface)
                                .border(
                                    NeuTokens.BorderWidth,
                                    colors.border,
                                    RoundedCornerShape(NeuTokens.ChipRadius)
                                )
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) Ink else colors.ink
                            )
                        }
                        if (index < tabTitles.size - 1) {
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }

                // Tab Content
                when (selectedTabIndex) {
                    0 -> FoldersTab(
                        folders = folders,
                        onFolderClick = { folder ->
                            onNavigateToFolder(folder.path, folder.name)
                        }
                    )
                    1 -> VideosTab(
                        videos = videos,
                        continueWatchingVideos = continueWatching,
                        selectedVideoIds = selectedVideoIds,
                        isSelectionMode = isSelectionMode,
                        viewMode = viewMode,
                        sortOrder = sortOrder,
                        onViewModeChange = { viewModel.setViewMode(it) },
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onVideoClick = { video ->
                            if (isSelectionMode) {
                                viewModel.toggleVideoSelection(video.id)
                            } else {
                                onNavigateToPlayer(video.uri, true)
                            }
                        },
                        onVideoLongClick = { video ->
                            viewModel.enterSelectionMode(video.id)
                        },
                        onVideoMoreClick = { video ->
                            viewModel.inspectVideo(video)
                        }
                    )
                    2 -> AudioTab(
                        audioList = audioTracks,
                        onAudioClick = { audio ->
                            onNavigateToPlayer(audio.uri, false)
                        },
                        onPermissionGranted = {
                            viewModel.refreshMedia()
                        }
                    )
                    3 -> PlaylistsTab(
                        playlists = playlists,
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylist(playlist.id, playlist.name)
                        },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) }
                    )
                }
            }

            // Floating Selection Bar
            if (isSelectionMode) {
                SelectionBar(
                    selectedCount = selectedVideoIds.size,
                    onClose = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAllVideos() },
                    onDelete = {
                        // Deletion handled via intent/MediaStore
                        viewModel.exitSelectionMode()
                    },
                    onShare = {
                        val selectedVideos = videos.filter { selectedVideoIds.contains(it.id) }
                        if (selectedVideos.isNotEmpty()) {
                            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "video/*"
                                putParcelableArrayListExtra(
                                    Intent.EXTRA_STREAM,
                                    ArrayList(selectedVideos.map { it.uri })
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share videos"))
                        }
                        viewModel.exitSelectionMode()
                    },
                    onAddToPlaylist = {
                        if (playlists.isNotEmpty()) {
                            viewModel.addSelectedToPlaylist(playlists.first().id)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Video Info Sheet
            VideoInfoSheet(
                video = inspectedVideo,
                onDismiss = { viewModel.inspectVideo(null) }
            )
        }
    }
}
