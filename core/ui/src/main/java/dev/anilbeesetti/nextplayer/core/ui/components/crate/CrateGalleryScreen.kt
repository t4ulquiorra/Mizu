package dev.anilbeesetti.nextplayer.core.ui.components.crate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateAccentOption
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme

@Composable
fun CrateGalleryScreen(onNavigateBack: (() -> Unit)? = null) {
    var selectedAccent by remember { mutableStateOf(CrateAccentOption.ORANGE) }
    var selectedSegment by remember { mutableStateOf("CRATE") }
    var toggleState by remember { mutableStateOf(true) }
    var clickCount by remember { mutableIntStateOf(0) }

    CrateTheme(accentOption = selectedAccent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CrateTheme.colors.bgBase)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (onNavigateBack != null) {
                        CrateCircleButton(
                            onClick = onNavigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                    Column {
                        Text(
                            text = "The Crate",
                            style = CrateTheme.typography.titleLarge,
                            color = CrateTheme.colors.inkPrimary,
                        )
                        CrateMonoBreadcrumb(
                            section = "COMPONENT GALLERY",
                            count = "PHASE 2",
                        )
                    }
                }
                CrateStampBadge(text = "SPEC READY")
            }

            // Accent Switcher
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CrateMonoLabel(text = "ACCENT COLOR THEME")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrateAccentOption.entries.forEach { option ->
                        CrateTactileButton(
                            onClick = { selectedAccent = option },
                            variant = if (selectedAccent == option) {
                                CrateButtonVariant.ACCENT
                            } else {
                                CrateButtonVariant.SECONDARY
                            },
                        ) {
                            Text(
                                text = option.title,
                                style = CrateTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            // Segmented Bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CrateMonoLabel(text = "TACTILE SEGMENTED BAR")
                CrateSegmentedBar(
                    items = listOf(
                        "CRATE",
                        "WALL",
                        "LIST",
                    ),
                    selectedItem = selectedSegment,
                    onItemSelected = { selectedSegment = it },
                )
            }

            // Tactile Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CrateMonoLabel(text = "TACTILE BUTTONS (COLLAPSING SHADOW)")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrateTactileButton(
                        onClick = { clickCount++ },
                        variant = CrateButtonVariant.ACCENT,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play ($clickCount)",
                            style = CrateTheme.typography.titleSmall,
                        )
                    }

                    CrateTactileButton(
                        onClick = { clickCount = 0 },
                        variant = CrateButtonVariant.SECONDARY,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset",
                            style = CrateTheme.typography.titleSmall,
                        )
                    }

                    CrateTactileButton(
                        onClick = {},
                        variant = CrateButtonVariant.PAPER,
                    ) {
                        Text(
                            text = "Paper Key",
                            style = CrateTheme.typography.titleSmall,
                        )
                    }
                }
            }

            // Tactile Circle Buttons & Toggles
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CrateMonoLabel(text = "TACTILE CIRCLE BUTTONS & TOGGLE")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrateCircleButton(
                        onClick = {},
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                    )
                    CrateCircleButton(
                        onClick = {},
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        backgroundColor = CrateTheme.colors.accent,
                        borderColor = CrateTheme.colors.accent,
                        iconColor = CrateTheme.colors.bgBase,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CrateToggle(
                            checked = toggleState,
                            onCheckedChange = { toggleState = it },
                        )
                        CrateMonoLabel(
                            text = if (toggleState) "ACTIVE" else "MUTED",
                            color = if (toggleState) {
                                CrateTheme.colors.accent
                            } else {
                                CrateTheme.colors.inkMuted
                            },
                        )
                    }
                }
            }

            // Manila Paper Cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CrateMonoLabel(text = "4-SHADE MANILA PAPER CARDS")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    (0..3).forEach { index ->
                        CratePaperCard(
                            index = index,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CrateMonoLabel(
                                    text = "FOLDER 0$index",
                                    color = CrateTheme.colors.inkPaper.copy(alpha = 0.6f),
                                    style = CrateTheme.typography.labelMonoSmall,
                                )
                                Text(
                                    text = "Shade #$index",
                                    style = CrateTheme.typography.titleSmall,
                                    color = CrateTheme.colors.inkPaper,
                                )
                                Text(
                                    text = "24 items",
                                    style = CrateTheme.typography.bodySmall,
                                    color = CrateTheme.colors.inkPaper.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }

            // Typography Scale
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CrateMonoLabel(text = "TYPOGRAPHY SCALE")
                Text(
                    text = "Display 32sp — The Crate",
                    style = CrateTheme.typography.titleDisplay,
                    color = CrateTheme.colors.inkPrimary,
                )
                Text(
                    text = "Title Large 24sp — Bricolage Grotesque",
                    style = CrateTheme.typography.titleLarge,
                    color = CrateTheme.colors.inkPrimary,
                )
                Text(
                    text = "Title Medium 18sp — Editorial Depth",
                    style = CrateTheme.typography.titleMedium,
                    color = CrateTheme.colors.inkSecondary,
                )
                Text(
                    text = "Body 14sp — JetBrains Mono small caps labels and counters",
                    style = CrateTheme.typography.bodyMedium,
                    color = CrateTheme.colors.inkMuted,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
