package dev.anilbeesetti.nextplayer.settings.screens.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.AppSkin
import dev.anilbeesetti.nextplayer.core.model.ThemeConfig
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateAccentOption
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.MizuAqua
import dev.anilbeesetti.nextplayer.core.ui.theme.MizuSunYellow
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.core.ui.theme.neoShadow
import dev.anilbeesetti.nextplayer.core.ui.theme.supportsDynamicTheming
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name

@Composable
fun AppearancePreferencesScreen(viewModel: AppearancePreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppearancePreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun AppearancePreferencesScreenContent(
    state: AppearancePreferencesUiState,
    onAction: (AppearancePreferencesEvent) -> Unit,
) {
    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.appearance_name),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(AppearancePreferencesEvent.NavigateUp)
                        },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = CrateTheme.colors.bgBase,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = "CRATE ACCENT")
            CrateAccentSelector(
                selectedAccent = CrateAccentOption.ORANGE,
                onAccentSelected = { /* UI preview */ },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ListSectionTitle(text = stringResource(id = R.string.skin_and_style))
            SkinSelector(
                selectedSkin = state.preferences.appSkin,
                onSkinSelected = { skin ->
                    onAction(
                        AppearancePreferencesEvent.UpdateAppSkin(appSkin = skin),
                    )
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ListSectionTitle(text = stringResource(id = R.string.appearance_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    description = state.preferences.themeConfig.name(),
                    isChecked = state.preferences.themeConfig == ThemeConfig.ON,
                    onChecked = { onAction(AppearancePreferencesEvent.ToggleDarkTheme) },
                    icon = NextIcons.DarkMode,
                    onClick = {
                        onAction(
                            AppearancePreferencesEvent.ShowDialog(
                                value = AppearancePreferenceDialog.Theme,
                            ),
                        )
                    },
                    isFirstItem = true,
                )
                PreferenceSwitch(
                    title = stringResource(R.string.high_contrast_dark_theme),
                    description = stringResource(R.string.high_contrast_dark_theme_desc),
                    icon = NextIcons.Contrast,
                    isChecked = state.preferences.useHighContrastDarkTheme,
                    onClick = {
                        onAction(AppearancePreferencesEvent.ToggleUseHighContrastDarkTheme)
                    },
                    isLastItem = !supportsDynamicTheming(),
                )
                if (supportsDynamicTheming()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_theme),
                        description = stringResource(id = R.string.dynamic_theme_description),
                        icon = NextIcons.Appearance,
                        isChecked = state.preferences.useDynamicColors,
                        onClick = { onAction(AppearancePreferencesEvent.ToggleUseDynamicColors) },
                        isLastItem = true,
                    )
                }
            }
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                AppearancePreferenceDialog.Theme -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.dark_theme),
                        onDismissClick = {
                            onAction(AppearancePreferencesEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(ThemeConfig.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = (it == state.preferences.themeConfig),
                                onClick = {
                                    onAction(
                                        AppearancePreferencesEvent.UpdateThemeConfig(
                                            themeConfig = it,
                                        ),
                                    )
                                    onAction(AppearancePreferencesEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkinSelector(
    selectedSkin: AppSkin,
    onSkinSelected: (AppSkin) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkinPreviewCard(
            skin = AppSkin.NEUBRUTALISM,
            title = stringResource(id = R.string.skin_neubrutalism),
            subtitle = stringResource(id = R.string.skin_neubrutalism_desc),
            isSelected = selectedSkin == AppSkin.NEUBRUTALISM,
            onClick = { onSkinSelected(AppSkin.NEUBRUTALISM) },
            modifier = Modifier.weight(1f),
        )
        SkinPreviewCard(
            skin = AppSkin.MATERIAL_YOU,
            title = stringResource(id = R.string.skin_material_you),
            subtitle = stringResource(id = R.string.skin_material_you_desc),
            isSelected = selectedSkin == AppSkin.MATERIAL_YOU,
            onClick = { onSkinSelected(AppSkin.MATERIAL_YOU) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SkinPreviewCard(
    skin: AppSkin,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNeo = skin == AppSkin.NEUBRUTALISM
    val shape = if (isNeo) RoundedCornerShape(10.dp) else RoundedCornerShape(16.dp)
    val borderWidth = if (isNeo || isSelected) 2.dp else 1.dp
    val borderColor = when {
        isNeo -> Color(0xFF111111)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val cardBackground = if (isNeo) {
        Color(0xFFFFF8EE)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val titleColor = if (isNeo) {
        Color(0xFF111111)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (isNeo) {
        Color(0xFF444444)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .then(
                other = if (isNeo) {
                    Modifier.neoShadow(
                        offset = 3.dp,
                        shadowColor = Color(0xFF111111),
                        cornerRadius = 10.dp,
                        enabled = true,
                    )
                } else {
                    Modifier
                },
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape,
            )
            .clip(shape = shape)
            .background(color = cardBackground)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(
                        shape = if (isNeo) {
                            RoundedCornerShape(6.dp)
                        } else {
                            RoundedCornerShape(10.dp)
                        },
                    )
                    .background(
                        color = if (isNeo) {
                            MizuSunYellow
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    )
                    .then(
                        other = if (isNeo) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = Color(0xFF111111),
                                shape = RoundedCornerShape(6.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(
                                shape = if (isNeo) {
                                    RoundedCornerShape(4.dp)
                                } else {
                                    CircleShape
                                },
                            )
                            .background(
                                color = if (isNeo) {
                                    MizuAqua
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                            .then(
                                other = if (isNeo) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = Color(0xFF111111),
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    Text(
                        text = if (isNeo) "MIZU" else "Next",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isNeo) {
                            Color(0xFF111111)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 2,
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(4.dp))
                        .background(
                            color = if (isNeo) {
                                Color(0xFF111111)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 2.dp,
                        ),
                ) {
                    Text(
                        text = stringResource(id = R.string.active),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun CrateAccentSelector(
    selectedAccent: CrateAccentOption,
    onAccentSelected: (CrateAccentOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CrateAccentOption.entries.forEach { option ->
            val isSelected = selectedAccent == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CrateTheme.shapes.card)
                    .background(
                        color = if (isSelected) {
                            CrateTheme.colors.bgSurfaceKey
                        } else {
                            CrateTheme.colors.bgSurface
                        },
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) option.accent else CrateTheme.colors.borderStrong,
                        shape = CrateTheme.shapes.card,
                    )
                    .clickable { onAccentSelected(option) }
                    .padding(
                        vertical = 12.dp,
                        horizontal = 6.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(option.accent),
                    )
                    Text(
                        text = option.title.uppercase(),
                        style = CrateTheme.typography.monoSmallCaps.copy(fontSize = 9.sp),
                        color = if (isSelected) option.accent else CrateTheme.colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AppearancePreferencesScreenPreview() {
    NextPlayerTheme {
        AppearancePreferencesScreenContent(
            state = AppearancePreferencesUiState(),
            onAction = {},
        )
    }
}
