package com.mizu.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.mizu.player.data.MediaRepository
import com.mizu.player.model.VideoItem
import com.mizu.player.ui.LibraryScreen
import com.mizu.player.ui.PermissionHelper
import com.mizu.player.ui.PermissionScreen
import com.mizu.player.ui.PlayerScreen

class MainActivity : ComponentActivity() {

    private var isVideoPlaying by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MediaRepository.initPreferences(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addOnPictureInPictureModeChangedListener { info ->
                isInPipMode = info.isInPictureInPictureMode
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0D0F14),
                    surface = Color(0xFF13161F),
                    primary = Color(0xFF3B82F6)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0F14))
                ) {
                    AppContent(
                        intent = intent,
                        isInPipMode = isInPipMode,
                        onPlaybackStateChange = { playing ->
                            isVideoPlaying = playing
                        }
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isVideoPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun AppContent(
    intent: Intent?,
    isInPipMode: Boolean = false,
    onPlaybackStateChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(PermissionHelper.hasStoragePermission(context))
    }

    var playingVideo by remember { mutableStateOf<VideoItem?>(null) }

    // Check for "Open with" Intent on first launch
    remember(intent) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            val title = uri.lastPathSegment ?: "Playing Media"
            playingVideo = VideoItem(
                id = -1,
                title = title,
                uri = uri,
                path = uri.path ?: "",
                durationMs = 0L,
                sizeBytes = 0L,
                dateModified = 0L,
                subtitleType = null
            )
        }
    }

    // Automatically re-check permission when the user returns to Mizu from system Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = PermissionHelper.hasStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!hasPermission) {
        PermissionScreen(
            onPermissionGranted = {
                hasPermission = true
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryScreen(
                onVideoClick = { video ->
                    playingVideo = video
                }
            )

            playingVideo?.let { video ->
                PlayerScreen(
                    video = video,
                    onBack = {
                        playingVideo = null
                    },
                    isInPipMode = isInPipMode,
                    onPlaybackStateChange = onPlaybackStateChange
                )
            }
        }
    }
}
