package dev.anilbeesetti.nextplayer.feature.more.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.media.services.MediaOperationsService
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.components.BindTopLevelFab
import dev.anilbeesetti.nextplayer.core.ui.components.LocalNavigationBottomPadding
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabKey
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateButtonVariant
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateMonoBreadcrumb
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateMonoLabel
import dev.anilbeesetti.nextplayer.core.ui.components.crate.CrateTactileButton
import dev.anilbeesetti.nextplayer.core.ui.components.rememberRestorableFocusState
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusGroup
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusItem
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.theme.CrateTheme
import dev.anilbeesetti.nextplayer.feature.more.components.CrateHandOfCards
import dev.anilbeesetti.nextplayer.feature.more.components.HistoryEmptyState
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.VideoGridItem

@Composable
fun MoreScreen(viewModel: MoreViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
internal fun MoreScreenContent(state: MoreUiState, onAction: (MoreAction) -> Unit) {
    BindTopLevelFab(
        TopLevelFabKey.MORE,
        NextIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.more),
                            style = CrateTheme.typography.titleLarge,
                            color = CrateTheme.colors.inkPrimary,
                        )
                        CrateMonoBreadcrumb(
                            section = "UTILITIES",
                            count = "SYSTEM & TOOLS",
                        )
                    }
                },
            )
        },
        containerColor = CrateTheme.colors.bgBase,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .background(CrateTheme.colors.bgBase),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .tvListFocus()
                    .padding(horizontal = 16.dp)
                    .padding(
                        top = 8.dp,
                        bottom =
                            scaffoldPadding.calculateBottomPadding() +
                                LocalNavigationBottomPadding.current + 96.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CrateMonoLabel(text = "KEYCAP UTILITIES")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    CrateTactileButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        variant = CrateButtonVariant.SECONDARY,
                        onClick = { onAction(MoreAction.OpenVault) },
                    ) {
                        Icon(
                            imageVector = NextIcons.Lock,
                            contentDescription = stringResource(R.string.vault),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.vault),
                            style = CrateTheme.typography.titleSmall,
                        )
                    }
                    CrateTactileButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        variant = CrateButtonVariant.SECONDARY,
                        onClick = { filePicker.launch(input = "video/*") },
                    ) {
                        Icon(
                            imageVector = NextIcons.FileOpen,
                            contentDescription = stringResource(R.string.pick_file),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.pick_file),
                            style = CrateTheme.typography.titleSmall,
                        )
                    }
                    if (MediaOperationsService.supportsTrash()) {
                        CrateTactileButton(
                            modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                            variant = CrateButtonVariant.SECONDARY,
                            onClick = { onAction(MoreAction.OpenTrash) },
                        ) {
                            Icon(
                                imageVector = NextIcons.Delete,
                                contentDescription = stringResource(R.string.trash),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.trash),
                                style = CrateTheme.typography.titleSmall,
                            )
                        }
                    }
                    CrateTactileButton(
                        modifier = Modifier.weight(weight = 1f).tvFocusRing(),
                        variant = CrateButtonVariant.SECONDARY,
                        onClick = { onAction(MoreAction.OpenSettings) },
                    ) {
                        Icon(
                            imageVector = NextIcons.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.settings),
                            style = CrateTheme.typography.titleSmall,
                        )
                    }
                }
                HistorySection(
                    history = state.history,
                    preferences = state.preferences,
                    onMoreClick = { onAction(MoreAction.OpenHistory) },
                    onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
                )
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: DataState<List<Video>>,
    preferences: ApplicationPreferences,
    onMoreClick: () -> Unit,
    onVideoClick: (Video) -> Unit,
) {
    val videos = history.result.orEmpty().take(n = 10)
    val isTv = LocalContext.current.isTelevision
    val historyButtonFocusRequester = remember { FocusRequester() }
    val historyFocusState = rememberRestorableFocusState()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            // Keep the full header in the vertical focus path, including its empty title area.
            modifier = Modifier.fillMaxWidth().thenIf(condition = isTv) { focusGroup() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp).weight(weight = 1f),
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .focusRequester(focusRequester = historyButtonFocusRequester)
                    .focusProperties {
                        if (isTv && !preferences.isHistoryPaused && videos.isNotEmpty()) {
                            down = historyFocusState.requester
                        }
                    }
                    .tvFocusRing(),
            ) {
                Icon(
                    imageVector = NextIcons.ArrowForward,
                    contentDescription = stringResource(R.string.history),
                )
            }
        }
        when {
            preferences.isHistoryPaused -> HistoryEmptyState(
                isHistoryOff = true,
                modifier = Modifier.fillMaxWidth(),
            )

            history is DataState.Success && videos.isEmpty() -> HistoryEmptyState(
                isHistoryOff = false,
                modifier = Modifier.fillMaxWidth(),
            )

            else -> {
                if (isTv) {
                    LazyRow(
                        modifier = Modifier
                            .focusProperties {
                                onExit = {
                                    if (requestedFocusDirection == FocusDirection.Up) {
                                        historyButtonFocusRequester.requestFocus()
                                    }
                                }
                            }
                            .restorableFocusGroup(
                                state = historyFocusState,
                                ready = false,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(
                            items = videos,
                            key = { it.uriString },
                        ) { video ->
                            VideoGridItem(
                                video = video,
                                isRecentlyPlayedVideo = false,
                                textStyle = MaterialTheme.typography.bodySmall,
                                preferences = preferences,
                                modifier = Modifier
                                    .width(140.dp)
                                    .restorableFocusItem(
                                        state = historyFocusState,
                                        video.uriString,
                                    ),
                                onClick = { onVideoClick(video) },
                            )
                        }
                    }
                } else {
                    CrateHandOfCards(
                        videos = videos,
                        onVideoClick = onVideoClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CrateTactileButton(
                        onClick = onMoreClick,
                        variant = CrateButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "VIEW FULL HISTORY →",
                            style = CrateTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MoreScreenContentPreview() {
    MoreScreenContent(
        state = MoreUiState(
            history = DataState.Success(value = emptyList()),
        ),
        onAction = {},
    )
}
