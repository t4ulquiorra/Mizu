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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizu.player.data.UserPreferences
import java.util.Collections
import java.util.Locale

data class LanguageItem(val code: String, val name: String)

val AVAILABLE_LANGUAGES = listOf(
    LanguageItem("ja", "Japanese"),
    LanguageItem("en", "English"),
    LanguageItem("es", "Spanish"),
    LanguageItem("fr", "French"),
    LanguageItem("de", "German"),
    LanguageItem("it", "Italian"),
    LanguageItem("pt", "Portuguese"),
    LanguageItem("ru", "Russian"),
    LanguageItem("zh", "Chinese"),
    LanguageItem("ko", "Korean"),
    LanguageItem("hi", "Hindi"),
    LanguageItem("ar", "Arabic"),
    LanguageItem("id", "Indonesian"),
    LanguageItem("vi", "Vietnamese"),
    LanguageItem("th", "Thai"),
    LanguageItem("pl", "Polish"),
    LanguageItem("tr", "Turkish"),
    LanguageItem("uk", "Ukrainian"),
    LanguageItem("nl", "Dutch"),
    LanguageItem("sv", "Swedish")
)

fun getLanguageDisplayName(code: String): String {
    val known = AVAILABLE_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
    if (known != null) return known.name
    return try {
        val loc = Locale(code)
        loc.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        code.uppercase()
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var audioLangs by remember {
        mutableStateOf(UserPreferences.getPreferredAudioLanguages(context))
    }
    var subLangs by remember {
        mutableStateOf(UserPreferences.getPreferredSubtitleLanguages(context))
    }
    var subsOffByDefault by remember {
        mutableStateOf(UserPreferences.isSubtitlesOffByDefault(context))
    }

    var showAddAudioDialog by remember { mutableStateOf(false) }
    var showAddSubDialog by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF13161F))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Audio Language Priority Section
            item {
                Column {
                    Text(
                        text = "Audio Language Priority",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When videos have multiple audio tracks, the first matching language in this list is chosen by default.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF13161F))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (audioLangs.isEmpty()) {
                            Text(
                                text = "No priorities set (file default track will be used)",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            audioLangs.forEachIndexed { index, code ->
                                LanguageRow(
                                    index = index,
                                    code = code,
                                    name = getLanguageDisplayName(code),
                                    canMoveUp = index > 0,
                                    canMoveDown = index < audioLangs.size - 1,
                                    onMoveUp = {
                                        val mutable = audioLangs.toMutableList()
                                        Collections.swap(mutable, index, index - 1)
                                        audioLangs = mutable
                                        UserPreferences.setPreferredAudioLanguages(context, mutable)
                                    },
                                    onMoveDown = {
                                        val mutable = audioLangs.toMutableList()
                                        Collections.swap(mutable, index, index + 1)
                                        audioLangs = mutable
                                        UserPreferences.setPreferredAudioLanguages(context, mutable)
                                    },
                                    onDelete = {
                                        val mutable = audioLangs.toMutableList()
                                        mutable.removeAt(index)
                                        audioLangs = mutable
                                        UserPreferences.setPreferredAudioLanguages(context, mutable)
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = { showAddAudioDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E222D),
                                contentColor = Color(0xFF3B82F6)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Audio Language",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Subtitle Language Priority Section
            item {
                Column {
                    Text(
                        text = "Subtitle Language Priority",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Controls default subtitle track selection when opening new videos.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF13161F))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Toggle: No subtitles by default
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No subtitles by default",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Keep subtitles turned off unless manually enabled or saved for that video.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = subsOffByDefault,
                                onCheckedChange = { checked ->
                                    subsOffByDefault = checked
                                    UserPreferences.setSubtitlesOffByDefault(context, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF1E222D)
                                )
                            )
                        }

                        if (!subsOffByDefault) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Preferred Languages (in priority order)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFCBD5E1)
                            )

                            if (subLangs.isEmpty()) {
                                Text(
                                    text = "No priorities set (file default subtitle will be used)",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                subLangs.forEachIndexed { index, code ->
                                    LanguageRow(
                                        index = index,
                                        code = code,
                                        name = getLanguageDisplayName(code),
                                        canMoveUp = index > 0,
                                        canMoveDown = index < subLangs.size - 1,
                                        onMoveUp = {
                                            val mutable = subLangs.toMutableList()
                                            Collections.swap(mutable, index, index - 1)
                                            subLangs = mutable
                                            UserPreferences.setPreferredSubtitleLanguages(context, mutable)
                                        },
                                        onMoveDown = {
                                            val mutable = subLangs.toMutableList()
                                            Collections.swap(mutable, index, index + 1)
                                            subLangs = mutable
                                            UserPreferences.setPreferredSubtitleLanguages(context, mutable)
                                        },
                                        onDelete = {
                                            val mutable = subLangs.toMutableList()
                                            mutable.removeAt(index)
                                            subLangs = mutable
                                            UserPreferences.setPreferredSubtitleLanguages(context, mutable)
                                        }
                                    )
                                }
                            }

                            Button(
                                onClick = { showAddSubDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E222D),
                                    contentColor = Color(0xFF3B82F6)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Subtitle Language",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Audio Language Dialog
    if (showAddAudioDialog) {
        val availableToAdd = AVAILABLE_LANGUAGES.filter { it.code !in audioLangs }
        LanguagePickerModal(
            title = "Add Audio Language",
            languages = availableToAdd,
            onDismiss = { showAddAudioDialog = false },
            onSelect = { item ->
                val mutable = audioLangs.toMutableList()
                mutable.add(item.code)
                audioLangs = mutable
                UserPreferences.setPreferredAudioLanguages(context, mutable)
                showAddAudioDialog = false
            }
        )
    }

    // Add Subtitle Language Dialog
    if (showAddSubDialog) {
        val availableToAdd = AVAILABLE_LANGUAGES.filter { it.code !in subLangs }
        LanguagePickerModal(
            title = "Add Subtitle Language",
            languages = availableToAdd,
            onDismiss = { showAddSubDialog = false },
            onSelect = { item ->
                val mutable = subLangs.toMutableList()
                mutable.add(item.code)
                subLangs = mutable
                UserPreferences.setPreferredSubtitleLanguages(context, mutable)
                showAddSubDialog = false
            }
        )
    }
}

@Composable
fun LanguageRow(
    index: Int,
    code: String,
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E222D))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority Badge
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF2B3245), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = code.uppercase(),
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // Reorder buttons
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Move Up",
                tint = if (canMoveUp) Color.White else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Move Down",
                tint = if (canMoveDown) Color.White else Color(0xFF475569),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LanguagePickerModal(
    title: String,
    languages: List<LanguageItem>,
    onDismiss: () -> Unit,
    onSelect: (LanguageItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        text = {
            if (languages.isEmpty()) {
                Text(
                    text = "All supported languages have already been added.",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(languages) { _, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 14.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = item.code.uppercase(),
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E222D))
            ) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF13161F)
    )
}
