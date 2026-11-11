package com.mizu.player.ui.screens.about

import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.ui.components.MizuMascot
import com.mizu.player.ui.components.NeuButton
import com.mizu.player.ui.components.NeuCard
import com.mizu.player.ui.components.NeuTokens
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Coral
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.PlaybackLog

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = MizuThemeObj.colors
    val scrollState = rememberScrollState()

    var showLogDialog by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        // Top Bar
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
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ABOUT MIZU",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = colors.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mascot & Version
            MizuMascot(size = 100.dp)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NeuTokens.ButtonRadius))
                    .background(Sun)
                    .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.ButtonRadius))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Mizu v0.1.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "Pure Offline Media Player for Android.\nCrafted with Neubrutalism design.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Diagnostics & Playback Log Card
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "PLAYBACK DIAGNOSTICS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    DiagRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    DiagRow("Android OS", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    DiagRow("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", "))
                    DiagRow("Primary Engine", "Media3 ExoPlayer + NextLib")
                    DiagRow("Fallback Engine", "LibVLC 3.6.5")

                    Spacer(Modifier.height(14.dp))

                    // Log action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeuButton(
                            text = "View Log",
                            onClick = {
                                logText = PlaybackLog.getLogContent()
                                showLogDialog = true
                            },
                            color = Aqua,
                            icon = Icons.Default.Description,
                            modifier = Modifier.weight(1f)
                        )

                        NeuButton(
                            text = "Share",
                            onClick = {
                                val content = PlaybackLog.getLogContent()
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Mizu Playback Diagnostics")
                                    putExtra(Intent.EXTRA_TEXT, content)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostics Log"))
                            },
                            color = Lime,
                            icon = Icons.Default.Share
                        )

                        NeuButton(
                            text = "Clear",
                            onClick = {
                                PlaybackLog.clear()
                            },
                            color = Coral,
                            icon = Icons.Default.Delete
                        )
                    }
                }
            }

            // Open Source Licenses Card
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "OPEN SOURCE LICENSES",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Ink
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LicenseItem("GNU General Public License v3.0", "GPL-3.0")
                    LicenseItem("AndroidX Media3 ExoPlayer", "Apache 2.0")
                    LicenseItem("LibVLC Android (VideoLAN)", "LGPL 2.1")
                    LicenseItem("NextLib FFmpeg Renderers", "LGPL 2.1")
                    LicenseItem("Jetpack Compose & Lifecycle", "Apache 2.0")
                    LicenseItem("Google Hilt", "Apache 2.0")
                    LicenseItem("Room Database", "Apache 2.0")
                    LicenseItem("Coil Image Loader", "Apache 2.0")
                }
            }

            // Privacy pledge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NeuTokens.CardRadius))
                    .background(Lime)
                    .border(NeuTokens.BorderWidth, Ink, RoundedCornerShape(NeuTokens.CardRadius))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "100% OFFLINE PLEDGE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Mizu does not request android.permission.INTERNET. No personal media data or telemetry ever leaves your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Diagnostic Log Viewer Dialog
        if (showLogDialog) {
            AlertDialog(
                onDismissRequest = { showLogDialog = false },
                containerColor = colors.paper,
                title = { Text("PLAYBACK DIAGNOSTICS LOG", fontWeight = FontWeight.Black, color = Ink) },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .background(Ink)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = logText.ifEmpty { "Log is empty." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Green
                        )
                    }
                },
                confirmButton = {
                    NeuButton(text = "CLOSE", onClick = { showLogDialog = false })
                }
            )
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Ink.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
    }
}

@Composable
private fun LicenseItem(name: String, license: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Text(
            text = license,
            style = MaterialTheme.typography.labelSmall,
            color = Ink.copy(alpha = 0.6f)
        )
    }
}
