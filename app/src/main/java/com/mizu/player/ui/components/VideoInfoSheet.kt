package com.mizu.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mizu.player.domain.model.VideoItem
import com.mizu.player.ui.theme.Aqua
import com.mizu.player.ui.theme.Ink
import com.mizu.player.ui.theme.Lime
import com.mizu.player.ui.theme.MizuThemeObj
import com.mizu.player.ui.theme.Sun
import com.mizu.player.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoSheet(
    video: VideoItem?,
    onDismiss: () -> Unit
) {
    if (video == null) return

    val colors = MizuThemeObj.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.border)
                    .height(5.dp)
                    .fillMaxWidth(0.15f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Title & Resolution tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )

                val res = FormatUtils.formatResolution(video.width, video.height)
                if (res.isNotEmpty()) {
                    NeuStickerBadge(text = res, color = Sun)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Details table in a NeuCard
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Duration", FormatUtils.formatDuration(video.duration))
                    InfoRow("File Size", FormatUtils.formatFileSize(video.size))
                    InfoRow("Resolution", "${video.width} x ${video.height}")
                    InfoRow("Folder", video.folderName)
                    InfoRow("Path", video.folderPath)
                    InfoRow("MIME Type", video.mimeType)
                    InfoRow("Date", FormatUtils.formatDate(video.dateModified))
                }
            }

            Spacer(Modifier.height(20.dp))

            NeuButton(
                text = "CLOSE",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                color = Lime,
                textColor = Ink
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Ink.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            maxLines = 1
        )
    }
}
