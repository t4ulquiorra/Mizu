package com.mizu.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.FolderItem
import com.mizu.player.model.VideoItem
import com.mizu.player.util.AndroidThumbnailFetcher
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC
}

@Composable
fun LibraryScreen(
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    // Minimum 2 columns on phones/portrait, Maximum 5 columns on wide tablets
    val gridColumns = (configuration.screenWidthDp / 180).coerceIn(2, 5)

    val tabs = listOf("Folders", "Videos", "Playlists")
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }

    var isGridView by remember { mutableStateOf(true) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Handle System Back Navigation
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    BackHandler(enabled = !isSearchActive && MediaRepository.selectedFolder != null) {
        MediaRepository.selectedFolder = null
    }

    // High performance image loader with hardware thumbnail cache + in-memory cache
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(AndroidThumbnailFetcher.Factory(context))
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.30)
                    .build()
            }
            .crossfade(false) // Disable crossfade during fast scrolling for buttery 120fps
            .build()
    }

    LaunchedEffect(Unit) {
        MediaRepository.loadIfNeeded(context)
    }

    fun sortVideos(list: List<VideoItem>): List<VideoItem> {
        return when (sortOption) {
            SortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.DATE_DESC -> list.sortedByDescending { it.dateModified }
            SortOption.DATE_ASC -> list.sortedBy { it.dateModified }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> list.sortedBy { it.sizeBytes }
        }
    }

    fun sortFolders(list: List<FolderItem>): List<FolderItem> {
        return when (sortOption) {
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_DESC -> list.sortedByDescending { it.videos.maxOfOrNull { v -> v.dateModified } ?: 0L }
            SortOption.DATE_ASC -> list.sortedBy { it.videos.maxOfOrNull { v -> v.dateModified } ?: 0L }
            SortOption.SIZE_DESC -> list.sortedByDescending { it.videoCount }
            SortOption.SIZE_ASC -> list.sortedBy { it.videoCount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
    ) {
        // TOP HEADER BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF13161F))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isSearchActive) {
                // Right-capped Search Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFF1E222D), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search in media files",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFF3B82F6)),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    searchQuery = ""
                                } else {
                                    isSearchActive = false
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // Standard Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeFolder = MediaRepository.selectedFolder
                    if (activeFolder != null) {
                        // Folder Detail Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { MediaRepository.selectedFolder = null }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeFolder.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        // Home Header
                        Text(
                            text = "Mizu",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFCBD5E1)
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = Color(0xFFCBD5E1)
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(Color(0xFF1E222D))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name (A to Z)", color = Color.White) },
                                    onClick = { sortOption = SortOption.NAME_ASC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z to A)", color = Color.White) },
                                    onClick = { sortOption = SortOption.NAME_DESC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Newest first)", color = Color.White) },
                                    onClick = { sortOption = SortOption.DATE_DESC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Oldest first)", color = Color.White) },
                                    onClick = { sortOption = SortOption.DATE_ASC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest first)", color = Color.White) },
                                    onClick = { sortOption = SortOption.SIZE_DESC; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Smallest first)", color = Color.White) },
                                    onClick = { sortOption = SortOption.SIZE_ASC; showSortMenu = false }
                                )
                            }
                        }

                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List",
                                tint = Color(0xFFCBD5E1)
                            )
                        }

                        if (activeFolder == null) {
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = Color(0xFFCBD5E1)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(Color(0xFF1E222D))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh library", color = Color.White) },
                                        onClick = {
                                            showMoreMenu = false
                                            scope.launch {
                                                MediaRepository.refresh(context)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val activeFolder = MediaRepository.selectedFolder

        // TABS WITH SMOOTH TOUCH-INTERPOLATED UNDERLINE INDICATOR
        if (activeFolder == null) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color(0xFF13161F),
                contentColor = Color(0xFF3B82F6),
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        val currentTab = tabPositions[pagerState.currentPage]
                        val targetPage = pagerState.targetPage
                        val targetTab = tabPositions.getOrNull(targetPage) ?: currentTab
                        val fraction = abs(pagerState.currentPageOffsetFraction)

                        val left = lerp(currentTab.left, targetTab.left, fraction)
                        val right = lerp(currentTab.right, targetTab.right, fraction)
                        val width = right - left

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = left)
                                .width(width)
                                .height(3.dp)
                                .background(Color(0xFF3B82F6), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }
        }

        // MAIN CONTENT BODY (SWIPEABLE HORIZONTAL PAGER)
        Box(modifier = Modifier.weight(1f)) {
            if (MediaRepository.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else if (activeFolder != null) {
                // Folder Video View (Active Folder)
                val folderVideos = sortVideos(activeFolder.videos).filter {
                    searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true)
                }
                VideosContentView(
                    videos = folderVideos,
                    isGridView = isGridView,
                    columns = gridColumns,
                    imageLoader = imageLoader,
                    onVideoClick = onVideoClick
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            // Folders Tab (Supports Grid & List + Sorting)
                            val filteredFolders = sortFolders(MediaRepository.folders).filter {
                                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
                            }
                            FoldersContentView(
                                folders = filteredFolders,
                                isGridView = isGridView,
                                columns = gridColumns,
                                onFolderClick = { folder -> MediaRepository.selectedFolder = folder }
                            )
                        }
                        1 -> {
                            // Master Videos Tab
                            val filteredVideos = sortVideos(MediaRepository.allVideos).filter {
                                searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true)
                            }
                            VideosContentView(
                                videos = filteredVideos,
                                isGridView = isGridView,
                                columns = gridColumns,
                                imageLoader = imageLoader,
                                onVideoClick = onVideoClick
                            )
                        }
                        2 -> {
                            // Playlists Tab
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No playlists yet",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersContentView(
    folders: List<FolderItem>,
    isGridView: Boolean,
    columns: Int,
    onFolderClick: (FolderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No folders found", color = Color(0xFF64748B), fontSize = 15.sp)
        }
        return
    }

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(folders, key = { it.path }) { folder ->
                FolderGridCard(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders, key = { it.path }) { folder ->
                FolderListRow(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }
}

@Composable
fun FolderGridCard(
    folder: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        // Clean folder card: icon centered with soft background, NO number on thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF161922)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = folder.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Video count shown ONLY below title
        Text(
            text = "${folder.videoCount} video${if (folder.videoCount > 1) "s" else ""}",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
fun FolderListRow(
    folder: FolderItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161922))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222736)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${folder.videoCount} video${if (folder.videoCount > 1) "s" else ""}",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun VideosContentView(
    videos: List<VideoItem>,
    isGridView: Boolean,
    columns: Int,
    imageLoader: ImageLoader,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No videos found", color = Color(0xFF64748B), fontSize = 15.sp)
        }
        return
    }

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoGridCard(
                    video = video,
                    imageLoader = imageLoader,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoListRow(
                    video = video,
                    imageLoader = imageLoader,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E222D)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.uri,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtitle Badge (Bottom-Left)
            if (video.subtitleType != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color(0xE610141D), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.subtitleType,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Duration Badge (Bottom-Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.formattedDuration(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = video.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun VideoListRow(
    video: VideoItem,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail (16:9 aspect)
        Box(
            modifier = Modifier
                .width(128.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E222D)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.uri,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = video.formattedDuration(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (video.subtitleType != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.subtitleType,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val size = video.formattedSize()
                if (size.isNotEmpty()) {
                    Text(
                        text = size,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        IconButton(onClick = { /* More actions sheet */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
