package com.mizu.player.ui.screens.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.mizu.player.domain.model.AudioItem
import com.mizu.player.ui.components.NeuButton
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuChip
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lavender
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FormatUtils

@Composable
fun AudioTab(
    audioList: List<AudioItem>,
    onAudioClick: (AudioItem) -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MizuThemeObj.colors

    var hasAudioPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            onPermissionGranted()
        }
    }

    if (!hasAudioPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Permission Request Banner in Neubrutalist Style
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
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
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Aqua)
                            .border(2.dp, Ink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "AUDIO LIBRARY ACCESS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Mizu requires audio permission to discover music, albums, and podcasts on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(20.dp))

                    NeuButton(
                        text = "GRANT AUDIO ACCESS",
                        onClick = {
                            audioPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = Sun
                    )
                }
            }
        }
        return
    }

    var selectedSubTab by remember { mutableStateOf("TRACKS") }

    val albumsCount = remember(audioList) { audioList.map { it.album }.filter { it.isNotBlank() }.distinct().size }
    val artistsCount = remember(audioList) { audioList.map { it.artist }.filter { it.isNotBlank() }.distinct().size }
    val foldersCount = remember(audioList) { audioList.map { it.folderPath }.filter { it.isNotBlank() }.distinct().size }

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tabs: Tracks, Albums, Artists, Folders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeuChip(
                text = "Tracks (${audioList.size})",
                selected = selectedSubTab == "TRACKS",
                onClick = { selectedSubTab = "TRACKS" }
            )
            NeuChip(
                text = "Albums ($albumsCount)",
                selected = selectedSubTab == "ALBUMS",
                onClick = { selectedSubTab = "ALBUMS" }
            )
            NeuChip(
                text = "Artists ($artistsCount)",
                selected = selectedSubTab == "ARTISTS",
                onClick = { selectedSubTab = "ARTISTS" }
            )
            NeuChip(
                text = "Folders ($foldersCount)",
                selected = selectedSubTab == "FOLDERS",
                onClick = { selectedSubTab = "FOLDERS" }
            )
        }

        if (audioList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audio tracks found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink.copy(alpha = 0.6f)
                )
            }
        } else {
            when (selectedSubTab) {
                "TRACKS" -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(audioList, key = { it.id }) { audio ->
                            AudioTrackItem(
                                audio = audio,
                                onClick = { onAudioClick(audio) }
                            )
                        }
                    }
                }
                "ALBUMS" -> {
                    val albumGroups = remember(audioList) { audioList.groupBy { it.album } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albumGroups.keys.toList(), key = { it }) { albumName ->
                            val tracks = albumGroups[albumName] ?: emptyList()
                            AlbumCard(
                                title = albumName,
                                subtitle = "${tracks.size} tracks • ${tracks.firstOrNull()?.artist ?: ""}",
                                artUri = tracks.firstOrNull()?.albumArtUri,
                                onClick = { tracks.firstOrNull()?.let { onAudioClick(it) } }
                            )
                        }
                    }
                }
                "ARTISTS" -> {
                    val artistGroups = remember(audioList) { audioList.groupBy { it.artist } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(artistGroups.keys.toList(), key = { it }) { artistName ->
                            val tracks = artistGroups[artistName] ?: emptyList()
                            AlbumCard(
                                title = artistName,
                                subtitle = "${tracks.size} tracks",
                                icon = Icons.Default.Person,
                                onClick = { tracks.firstOrNull()?.let { onAudioClick(it) } }
                            )
                        }
                    }
                }
                "FOLDERS" -> {
                    val folderGroups = remember(audioList) { audioList.groupBy { it.folderName } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(folderGroups.keys.toList(), key = { it }) { folderName ->
                            val tracks = folderGroups[folderName] ?: emptyList()
                            AlbumCard(
                                title = folderName,
                                subtitle = "${tracks.size} tracks • ${tracks.firstOrNull()?.folderPath ?: ""}",
                                icon = Icons.Default.Folder,
                                onClick = { tracks.firstOrNull()?.let { onAudioClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackItem(
    audio: AudioItem,
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
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art or music note icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Lavender)
                    .border(1.5.dp, Ink, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (audio.albumArtUri != null) {
                    AsyncImage(
                        model = audio.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${audio.artist} • ${audio.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = FormatUtils.formatDuration(audio.duration),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Sun)
                    .border(1.5.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    title: String,
    subtitle: String,
    artUri: android.net.Uri? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Album,
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Sun)
                    .border(1.5.dp, Ink, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (artUri != null) {
                    AsyncImage(
                        model = artUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

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
