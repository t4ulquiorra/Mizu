package com.mizu.player.ui.screens.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.domain.model.SortOrder
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.domain.model.ViewMode
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuChip
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.components.SelectionBar
import com.mizu.player.ui.components.VideoGridItem
import com.mizu.player.ui.components.VideoListItem
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FolderUtils
import com.mizu.player.util.FormatUtils
import com.mizu.player.util.NaturalOrderComparator
import com.mizu.player.util.WatchState
import com.mizu.player.util.WatchStateUtils

@Composable
fun FolderDetailScreen(
    folderPath: String,
    folderName: String,
    allVideos: List<VideoItem>,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onViewModeChange: (ViewMode) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onVideoMoreClick: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MizuThemeObj.colors
    var activeFilter by remember { mutableStateOf("ALL") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Multi-selection state
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    // Filter to this folder
    val folderVideos = remember(allVideos, folderPath) {
        allVideos.filter { it.folderPath == folderPath }
    }

    // Filter by watch state
    val filteredVideos = remember(folderVideos, activeFilter) {
        when (activeFilter) {
            "UNWATCHED" -> folderVideos.filter {
                WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.UNWATCHED
            }
            "IN_PROGRESS" -> folderVideos.filter {
                WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.IN_PROGRESS
            }
            "WATCHED" -> folderVideos.filter {
                WatchStateUtils.getWatchState(it.lastPlayedPosition, it.duration) == WatchState.WATCHED
            }
            else -> folderVideos
        }
    }

    // Sort videos
    val sortedVideos = remember(filteredVideos, sortOrder) {
        when (sortOrder) {
            SortOrder.NAME_ASC -> filteredVideos.sortedWith { a, b -> NaturalOrderComparator.compare(a.title, b.title) }
            SortOrder.NAME_DESC -> filteredVideos.sortedWith { a, b -> NaturalOrderComparator.compare(b.title, a.title) }
            SortOrder.DATE_ASC -> filteredVideos.sortedBy { it.dateModified }
            SortOrder.DATE_DESC -> filteredVideos.sortedByDescending { it.dateModified }
            SortOrder.SIZE_ASC -> filteredVideos.sortedBy { it.size }
            SortOrder.SIZE_DESC -> filteredVideos.sortedByDescending { it.size }
            SortOrder.DURATION_ASC -> filteredVideos.sortedBy { it.duration }
            SortOrder.DURATION_DESC -> filteredVideos.sortedByDescending { it.duration }
        }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // Bug E: Reset scroll on filter or sort change
    LaunchedEffect(sortOrder, activeFilter) {
        gridState.scrollToItem(0)
        listState.scrollToItem(0)
    }

    val totalDuration = remember(folderVideos) {
        folderVideos.sumOf { it.duration }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.paper)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                            .background(colors.surface)
                            .border(NeuTokens.BorderWidth, colors.border, RoundedCornerShape(NeuTokens.ButtonRadius))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Sun,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = folderName.ifEmpty { folderPath.substringAfterLast('/') },
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = colors.text,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${folderVideos.size} videos • ${FormatUtils.formatDuration(totalDuration)} • ${FolderUtils.sanitizeStoragePath(folderPath)}",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        containerColor = colors.paper,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Controls Row: Filter chips + View Mode toggle + Sort button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Filters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        NeuChip(
                            text = "All (${folderVideos.size})",
                            selected = activeFilter == "ALL",
                            onClick = { activeFilter = "ALL" }
                        )
                        NeuChip(
                            text = "Unwatched",
                            selected = activeFilter == "UNWATCHED",
                            onClick = { activeFilter = "UNWATCHED" }
                        )
                        NeuChip(
                            text = "In Progress",
                            selected = activeFilter == "IN_PROGRESS",
                            onClick = { activeFilter = "IN_PROGRESS" }
                        )
                    }

                    // View toggle & Sort
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                onViewModeChange(if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint = colors.text
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = colors.text
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
                                    text = { Text("Name (A-Z Natural)") },
                                    onClick = {
                                        onSortOrderChange(SortOrder.NAME_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z-A)") },
                                    onClick = {
                                        onSortOrderChange(SortOrder.NAME_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest)") },
                                    onClick = {
                                        onSortOrderChange(SortOrder.SIZE_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duration (Longest)") },
                                    onClick = {
                                        onSortOrderChange(SortOrder.DURATION_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Video list / grid
                if (sortedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No videos match this filter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.text
                                )
                            }
                        }
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
                        items(sortedVideos, key = { it.id }) { video ->
                            val isSelected = selectedIds.contains(video.id)
                            VideoGridItem(
                                video = video,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - video.id else selectedIds + video.id
                                    } else {
                                        onVideoClick(video)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + video.id
                                },
                                onMoreClick = { onVideoMoreClick(video) },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedVideos, key = { it.id }) { video ->
                            val isSelected = selectedIds.contains(video.id)
                            VideoListItem(
                                video = video,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - video.id else selectedIds + video.id
                                    } else {
                                        onVideoClick(video)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + video.id
                                },
                                onMoreClick = { onVideoMoreClick(video) },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode
                            )
                        }
                    }
                }
            }

            // Selection Bar
            if (isSelectionMode) {
                SelectionBar(
                    selectedCount = selectedIds.size,
                    onClose = { selectedIds = emptySet() },
                    onSelectAll = { selectedIds = sortedVideos.map { it.id }.toSet() },
                    onPlay = {
                        val firstSelected = sortedVideos.firstOrNull { selectedIds.contains(it.id) }
                        if (firstSelected != null) {
                            onVideoClick(firstSelected)
                        }
                    },
                    onAddToPlaylist = { /* Wire to bottom sheet */ },
                    onDelete = { /* Wire to delete */ },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
