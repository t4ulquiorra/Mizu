package com.mizu.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.FolderItem
import com.mizu.player.model.VideoItem
import com.mizu.player.util.AndroidThumbnailFetcher
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import com.mizu.player.model.SortOption

@Composable
fun LibraryScreen(
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    // Adaptive Grid Columns: 2 minimum on phones, up to 5 on wide tablets
    val videoGridColumns = (configuration.screenWidthDp / 180).coerceIn(2, 5)
    val folderGridColumns = (configuration.screenWidthDp / 130).coerceIn(2, 5)

    val tabs = listOf("Folders", "Videos", "Playlists")
    val pagerState = rememberPagerState(initialPage = MediaRepository.selectedTab.coerceIn(0, tabs.size - 1)) { tabs.size }

    // Persist active tab across navigation and app restarts
    LaunchedEffect(pagerState.currentPage) {
        MediaRepository.updateSelectedTab(context, pagerState.currentPage)
    }

    var isGridView by remember { mutableStateOf(MediaRepository.isGridView) }
    var sortOption by remember { mutableStateOf(MediaRepository.sortOption) }
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
                                    fontSize = 14.5.sp
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.5.sp
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
                                fontSize = 17.sp,
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
                                    onClick = { 
                                        sortOption = SortOption.NAME_ASC
                                        MediaRepository.updateSortOption(context, SortOption.NAME_ASC)
                                        showSortMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z to A)", color = Color.White) },
                                    onClick = { 
                                        sortOption = SortOption.NAME_DESC
                                        MediaRepository.updateSortOption(context, SortOption.NAME_DESC)
                                        showSortMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Newest first)", color = Color.White) },
                                    onClick = { 
                                        sortOption = SortOption.DATE_DESC
                                        MediaRepository.updateSortOption(context, SortOption.DATE_DESC)
                                        showSortMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Oldest first)", color = Color.White) },
                                    onClick = { 
                                        sortOption = SortOption.DATE_ASC
                                        MediaRepository.updateSortOption(context, SortOption.DATE_ASC)
                                        showSortMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest first)", color = Color.White) },
                                    onClick = { 
                                        sortOption = SortOption.SIZE_DESC
                                        MediaRepository.updateSortOption(context, SortOption.SIZE_DESC)
                                        showSortMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Smallest first)", color = Color.White) },
                                    onClick = { 
                                        sortOption = SortOption.SIZE_ASC
                                        MediaRepository.updateSortOption(context, SortOption.SIZE_ASC)
                                        showSortMenu = false 
                                    }
                                )
                            }
                        }

                        IconButton(onClick = { 
                            isGridView = !isGridView 
                            MediaRepository.updateGridView(context, isGridView)
                        }) {
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

        // FLUID TAB BAR (CONTINUOUS SUB-PIXEL TRACKING + ELASTIC SPRING UNDERLINE)
        if (activeFolder == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0xFF13161F))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        // Continuous interactive progress from pager: exactly 0.0 -> 1.0 -> 2.0
                        val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabs.size - 1).toFloat())
                        val dist = abs(progress - index)
                        val activeFraction = (1f - dist).coerceIn(0f, 1f)
                        val tabTextColor = lerpColor(
                            Color(0xFF8E9BAE),
                            Color.White,
                            activeFraction
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.5.sp,
                                fontWeight = if (activeFraction > 0.5f) FontWeight.SemiBold else FontWeight.Medium,
                                color = tabTextColor
                            )
                        }
                    }
                }

                // Elastic pill indicator with physical spring stretch that tracks continuous finger gestures
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .layout { measurable, constraints ->
                            val tabWidth = constraints.maxWidth.toFloat() / tabs.size
                            val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                .coerceIn(0f, (tabs.size - 1).toFloat())
                            val centerPx = (progress + 0.5f) * tabWidth

                            // Elastic stretch on finger drag (expands toward midpoint, snaps back on settle)
                            val segmentFraction = (progress - progress.toInt()).let { if (it < 0f) it + 1f else it }
                            val stretchPx = sin(segmentFraction * Math.PI.toFloat()) * 12.dp.toPx()
                            val indicatorWidth = (28.dp.toPx() + stretchPx).roundToInt().coerceAtLeast(1)

                            val leftPx = (centerPx - indicatorWidth / 2f).roundToInt()
                            val placeable = measurable.measure(
                                constraints.copy(minWidth = indicatorWidth, maxWidth = indicatorWidth)
                            )
                            layout(constraints.maxWidth, placeable.height) {
                                placeable.placeRelative(leftPx, 0)
                            }
                        }
                        .background(
                            color = Color(0xFF3B82F6),
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.dp, bottomEnd = 1.dp)
                        )
                )
            }
        }

        // Memoize sorted and filtered lists so scrolling never triggers heavy list sorting/allocations
        val folderVideos = remember(activeFolder?.videos, sortOption, searchQuery) {
            if (activeFolder == null) emptyList()
            else sortVideos(activeFolder.videos).filter {
                searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true)
            }
        }
        val filteredFolders = remember(MediaRepository.folders, sortOption, searchQuery) {
            sortFolders(MediaRepository.folders).filter {
                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
            }
        }
        val filteredVideos = remember(MediaRepository.allVideos, sortOption, searchQuery) {
            sortVideos(MediaRepository.allVideos).filter {
                searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true)
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
                VideosContentView(
                    videos = folderVideos,
                    isGridView = isGridView,
                    columns = videoGridColumns,
                    imageLoader = imageLoader,
                    onVideoClick = onVideoClick
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            // Folders Tab (Supports Grid & List + Sorting)
                            FoldersContentView(
                                folders = filteredFolders,
                                isGridView = isGridView,
                                columns = folderGridColumns,
                                onFolderClick = { folder ->
                                    MediaRepository.selectedFolder = folder
                                    MediaRepository.enrichFolderSubtitles(folder)
                                }
                            )
                        }
                        1 -> {
                            // Master Videos Tab
                            VideosContentView(
                                videos = filteredVideos,
                                isGridView = isGridView,
                                columns = videoGridColumns,
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
                                    fontSize = 14.sp
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
            Text(text = "No folders found", color = Color(0xFF64748B), fontSize = 14.sp)
        }
        return
    }

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp), // Extra horizontal breathing room
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = folders,
                key = { it.path },
                contentType = { "folder_card" }
            ) { folder ->
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
            items(
                items = folders,
                key = { it.path },
                contentType = { "folder_row" }
            ) { folder ->
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
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = folder.name,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            maxLines = 2,
            minLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Video count shown ONLY below title
        Text(
            text = "${folder.videoCount} video${if (folder.videoCount > 1) "s" else ""}",
            fontSize = 11.sp,
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222736)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Folder",
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${folder.videoCount} video${if (folder.videoCount > 1) "s" else ""}",
                fontSize = 12.sp,
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
            Text(text = "No videos found", color = Color(0xFF64748B), fontSize = 14.sp)
        }
        return
    }

    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp), // Extra horizontal breathing room
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = videos,
                key = { it.id },
                contentType = { "video_card" }
            ) { video ->
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
            items(
                items = videos,
                key = { it.id },
                contentType = { "video_row" }
            ) { video ->
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
    val context = LocalContext.current
    val imageRequest = remember(video.uri) {
        ImageRequest.Builder(context)
            .data(video.uri)
            .memoryCacheKey(video.id.toString())
            .diskCacheKey(video.id.toString())
            .build()
    }

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
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtitle Badge (Bottom-Left) with snug height hugging text
            if (video.subtitleType != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 5.dp, bottom = 6.dp)
                        .background(Color(0xE60F172A), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = video.subtitleType,
                        color = Color(0xFF38BDF8),
                        fontSize = 9.5.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
            }

            // Duration Badge (Bottom-Right) with snug height hugging text/number
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 6.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 0.5.dp)
            ) {
                Text(
                    text = video.formattedDuration(),
                    color = Color.White,
                    fontSize = 9.5.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }

            // Sleek Watch Progress Bar (Bottom edge)
            val progress = MediaRepository.getProgressFraction(video)
            if (progress > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x80000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Uniform 2-line title height prevents jagged card bottoms in grid
        Text(
            text = video.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
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
    val context = LocalContext.current
    val imageRequest = remember(video.uri) {
        ImageRequest.Builder(context)
            .data(video.uri)
            .memoryCacheKey(video.id.toString())
            .diskCacheKey(video.id.toString())
            .build()
    }

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
                model = imageRequest,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Badge (tight, compact height)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 5.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = video.formattedDuration(),
                    color = Color.White,
                    fontSize = 9.5.sp,
                    lineHeight = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Sleek Watch Progress Bar (Bottom edge)
            val progress = MediaRepository.getProgressFraction(video)
            if (progress > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x80000000))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (video.subtitleType != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.subtitleType,
                            color = Color(0xFF38BDF8),
                            fontSize = 9.5.sp,
                            lineHeight = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                val size = video.formattedSize()
                if (size.isNotEmpty()) {
                    Text(
                        text = size,
                        fontSize = 11.5.sp,
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
