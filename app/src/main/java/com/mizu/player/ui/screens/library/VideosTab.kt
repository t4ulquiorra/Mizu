package com.mizu.player.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.mizu.player.util.WatchState
import com.mizu.player.util.WatchStateUtils
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizu.player.domain.model.SortOrder
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.domain.model.ViewMode
import com.mizu.player.ui.components.ContinueWatchingRow
import com.mizu.player.ui.components.NeuChip
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.components.VideoGridItem
import com.mizu.player.ui.components.VideoListItem
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun

@Composable
fun VideosTab(
    videos: List<VideoItem>,
    continueWatchingVideos: List<VideoItem>,
    selectedVideoIds: Set<Long>,
    isSelectionMode: Boolean,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onViewModeChange: (ViewMode) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onVideoMoreClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MizuThemeObj.colors
    var activeFilter by remember { mutableStateOf("ALL") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // Bug E: Reset scroll on filter or sort change
    LaunchedEffect(sortOrder, activeFilter) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }

    // Dynamic counts
    val unwatchedCount = remember(videos) {
        videos.count { WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.UNWATCHED }
    }
    val inProgressCount = remember(videos) {
        videos.count { WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.IN_PROGRESS }
    }
    val watchedCount = remember(videos) {
        videos.count { WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.WATCHED }
    }
    val subtitledCount = remember(videos) {
        videos.count { it.title.contains("sub", ignoreCase = true) || it.displayName.contains("sub", ignoreCase = true) }
    }
    val fourKCount = remember(videos) {
        videos.count { it.width >= 3840 || it.height >= 2160 }
    }
    val hdrCount = remember(videos) {
        videos.count { it.title.contains("hdr", ignoreCase = true) || it.displayName.contains("hdr", ignoreCase = true) }
    }

    val filteredVideos = remember(videos, activeFilter) {
        when (activeFilter) {
            "UNWATCHED" -> videos.filter {
                WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.UNWATCHED
            }
            "IN_PROGRESS" -> videos.filter {
                WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.IN_PROGRESS
            }
            "WATCHED" -> videos.filter {
                WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.WATCHED
            }
            "SUBTITLED" -> videos.filter {
                it.title.contains("sub", ignoreCase = true) || it.displayName.contains("sub", ignoreCase = true)
            }
            "4K" -> videos.filter { it.width >= 3840 || it.height >= 2160 }
            "HDR" -> videos.filter {
                it.title.contains("hdr", ignoreCase = true) || it.displayName.contains("hdr", ignoreCase = true)
            }
            else -> videos
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Controls Row: Filter chips + View Mode toggle + Sort button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Horizontal scrollable filter chips
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuChip(
                    text = "All (${videos.size})",
                    selected = activeFilter == "ALL",
                    onClick = { activeFilter = "ALL" }
                )
                NeuChip(
                    text = "Unwatched ($unwatchedCount)",
                    selected = activeFilter == "UNWATCHED",
                    onClick = { activeFilter = "UNWATCHED" }
                )
                NeuChip(
                    text = "In Progress ($inProgressCount)",
                    selected = activeFilter == "IN_PROGRESS",
                    onClick = { activeFilter = "IN_PROGRESS" }
                )
                NeuChip(
                    text = "Watched ($watchedCount)",
                    selected = activeFilter == "WATCHED",
                    onClick = { activeFilter = "WATCHED" }
                )
                if (subtitledCount > 0) {
                    NeuChip(
                        text = "Subtitled ($subtitledCount)",
                        selected = activeFilter == "SUBTITLED",
                        onClick = { activeFilter = "SUBTITLED" }
                    )
                }
                if (fourKCount > 0) {
                    NeuChip(
                        text = "4K ($fourKCount)",
                        selected = activeFilter == "4K",
                        onClick = { activeFilter = "4K" }
                    )
                }
                if (hdrCount > 0) {
                    NeuChip(
                        text = "HDR ($hdrCount)",
                        selected = activeFilter == "HDR",
                        onClick = { activeFilter = "HDR" }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // View toggle & Sort
            Row(verticalAlignment = Alignment.CenterVertically) {
                // View Mode
                IconButton(
                    onClick = {
                        onViewModeChange(
                            if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                        )
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle view",
                        tint = colors.ink
                    )
                }

                // Sort Dropdown
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = colors.ink
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date (Newest first)") },
                            onClick = {
                                onSortOrderChange(SortOrder.DATE_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date (Oldest first)") },
                            onClick = {
                                onSortOrderChange(SortOrder.DATE_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (A to Z)") },
                            onClick = {
                                onSortOrderChange(SortOrder.NAME_ASC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (Z to A)") },
                            onClick = {
                                onSortOrderChange(SortOrder.NAME_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Size (Largest first)") },
                            onClick = {
                                onSortOrderChange(SortOrder.SIZE_DESC)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duration (Longest first)") },
                            onClick = {
                                onSortOrderChange(SortOrder.DURATION_DESC)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (filteredVideos.isEmpty() && continueWatchingVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No videos found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink.copy(alpha = 0.6f)
                )
            }
        } else if (viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (continueWatchingVideos.isNotEmpty() && activeFilter == "ALL") {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ContinueWatchingRow(
                            videos = continueWatchingVideos,
                            onVideoClick = onVideoClick,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                items(filteredVideos, key = { it.id }) { video ->
                    VideoGridItem(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onLongClick = { onVideoLongClick(video) },
                        onMoreClick = { onVideoMoreClick(video) },
                        isSelected = selectedVideoIds.contains(video.id),
                        isSelectionMode = isSelectionMode
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (continueWatchingVideos.isNotEmpty() && activeFilter == "ALL") {
                    item {
                        ContinueWatchingRow(
                            videos = continueWatchingVideos,
                            onVideoClick = onVideoClick,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                items(filteredVideos, key = { it.id }) { video ->
                    VideoListItem(
                        video = video,
                        onClick = { onVideoClick(video) },
                        onLongClick = { onVideoLongClick(video) },
                        onMoreClick = { onVideoMoreClick(video) },
                        isSelected = selectedVideoIds.contains(video.id),
                        isSelectionMode = isSelectionMode
                    )
                }
            }
        }
    }
}
