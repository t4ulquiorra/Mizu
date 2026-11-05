package com.mizu.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.mizu.player.ui.PlayerScreen

class MainActivity : ComponentActivity() {

    private var currentVideoUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val activeUri = currentVideoUri
                if (activeUri != null) {
                    PlayerScreen(
                        videoUri = activeUri,
                        onClose = { currentVideoUri = null }
                    )
                } else {
                    HomeScreen(
                        onSelectVideo = { uri ->
                            try {
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (_: Exception) {}
                            currentVideoUri = uri
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            currentVideoUri = intent.data
        }
    }
}

@Composable
private fun HomeScreen(
    onSelectVideo: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let(onSelectVideo)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { launcher.launch(arrayOf("video/*")) }
        ) {
            Text(text = stringResource(R.string.open_video))
        }
    }
}
