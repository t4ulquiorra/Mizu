package com.mizu.player.ui.screens.welcome

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.ui.components.MizuMascot
import com.mizu.player.ui.components.NeuButton
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lavender
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun

@Composable
fun WelcomeScreen(
    onPermissionsGranted: () -> Unit,
    onFolderPicked: (Uri) -> Unit
) {
    val colors = MizuThemeObj.colors

    // Storage permission launcher based on SDK
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        if (granted) {
            onPermissionsGranted()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onFolderPicked(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Mascot and Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            MizuMascot(size = 130.dp)

            Spacer(Modifier.height(20.dp))

            // Neubrutalist App Title badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                    .background(Sun)
                    .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "MIZU PLAYER",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Pure Offline Media. Zero Tracking.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
        }

        // Features Card
        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                FeatureRow(
                    icon = Icons.Default.VideoLibrary,
                    title = "Universal Player",
                    desc = "Plays MKV, MP4, AVI, WebM with dual engines & hardware acceleration.",
                    badgeColor = Aqua
                )
                Spacer(Modifier.height(14.dp))
                FeatureRow(
                    icon = Icons.Default.Security,
                    title = "Offline & Private",
                    desc = "No network access, no analytics, no external servers. Ever.",
                    badgeColor = Lime
                )
            }
        }

        // Two Access Paths
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose how Mizu accesses your library:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Primary Path: Grant Media Access
            NeuButton(
                text = "GRANT MEDIA ACCESS",
                onClick = { permissionLauncher.launch(permissionsToRequest) },
                modifier = Modifier.fillMaxWidth(),
                color = Sun,
                textColor = Ink,
                icon = Icons.Default.VideoLibrary
            )

            Spacer(Modifier.height(14.dp))

            // Zero-permission Path: Pick a Folder via SAF
            NeuButton(
                text = "PICK A FOLDER (NO PERMISSIONS)",
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                color = Lavender,
                textColor = Ink,
                icon = Icons.Default.Folder
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    badgeColor: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(NeuTokens.ChipRadius))
                .background(badgeColor)
                .border(2.dp, Ink, RoundedCornerShape(NeuTokens.ChipRadius)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Ink.copy(alpha = 0.8f)
            )
        }
    }
}
