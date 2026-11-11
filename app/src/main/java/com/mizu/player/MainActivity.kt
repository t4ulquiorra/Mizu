package com.mizu.player

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mizu.player.data.preferences.UserPreferencesManager
import com.mizu.player.data.repository.MediaRepositoryImpl
import com.mizu.player.domain.repository.MediaRepository
import androidx.hilt.navigation.compose.hiltViewModel
import com.mizu.player.ui.screens.about.AboutScreen
import com.mizu.player.ui.screens.folder.FolderDetailScreen
import com.mizu.player.ui.screens.library.LibraryScreen
import com.mizu.player.ui.screens.library.LibraryViewModel
import com.mizu.player.ui.screens.playlist.PlaylistDetailScreen
import com.mizu.player.ui.screens.player.PlayerScreen
import com.mizu.player.ui.screens.search.SearchScreen
import com.mizu.player.ui.screens.settings.SettingsScreen
import com.mizu.player.ui.screens.welcome.WelcomeScreen
import com.mizu.player.ui.theme.MizuTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: UserPreferencesManager

    @Inject
    lateinit var mediaRepository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intentDataUri = intent?.data

        lifecycleScope.launch {
            val isOnboarded = preferencesManager.onboardingComplete.first()

            setContent {
                val darkModePref by preferencesManager.darkMode.collectAsState(initial = "system")
                val isDark = when (darkModePref) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }

                MizuTheme(darkTheme = isDark) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    val isVideoMedia = intent?.type?.startsWith("audio") != true
                    val startDestination = if (intentDataUri != null) {
                        "player?uri=${URLEncoder.encode(intentDataUri.toString(), StandardCharsets.UTF_8.name())}&isVideo=$isVideoMedia"
                    } else if (isOnboarded) {
                        "library"
                    } else {
                        "welcome"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onPermissionsGranted = {
                                    scope.launch {
                                        preferencesManager.setOnboardingComplete(true)
                                        navController.navigate("library") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                },
                                onFolderPicked = { treeUri ->
                                    scope.launch {
                                        preferencesManager.setOnboardingComplete(true)
                                        (mediaRepository as? MediaRepositoryImpl)?.addFolderTree(treeUri)
                                        navController.navigate("library") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("library") {
                            LibraryScreen(
                                onNavigateToPlayer = { uri, isVideo ->
                                    val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name())
                                    navController.navigate("player?uri=$encodedUri&isVideo=$isVideo")
                                },
                                onNavigateToSearch = { navController.navigate("search") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToAbout = { navController.navigate("about") },
                                onNavigateToFolder = { path, name ->
                                    val encPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                                    val encName = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
                                    navController.navigate("folder?path=$encPath&name=$encName")
                                },
                                onNavigateToPlaylist = { id, name ->
                                    val encName = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
                                    navController.navigate("playlist?id=$id&name=$encName")
                                }
                            )
                        }

                        composable(
                            route = "playlist?id={id}&name={name}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.LongType },
                                navArgument("name") { type = NavType.StringType; defaultValue = "Playlist" }
                            )
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getLong("id") ?: 0L
                            val rawName = backStackEntry.arguments?.getString("name") ?: "Playlist"
                            val decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8.name())

                            val libraryViewModel: LibraryViewModel = hiltViewModel()
                            val items by libraryViewModel.getPlaylistItems(playlistId).collectAsState(initial = emptyList())

                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistName = decodedName,
                                items = items,
                                onItemClick = { item ->
                                    val encodedUri = URLEncoder.encode(item.mediaUri, StandardCharsets.UTF_8.name())
                                    navController.navigate("player?uri=$encodedUri&isVideo=${item.isVideo}")
                                },
                                onDeleteItem = { itemId ->
                                    libraryViewModel.removeFromPlaylist(itemId)
                                },
                                onDeletePlaylist = {
                                    libraryViewModel.deletePlaylist(playlistId)
                                    navController.popBackStack()
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "folder?path={path}&name={name}",
                            arguments = listOf(
                                navArgument("path") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType; defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val rawPath = backStackEntry.arguments?.getString("path") ?: ""
                            val decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name())
                            val rawName = backStackEntry.arguments?.getString("name") ?: ""
                            val decodedName = URLDecoder.decode(rawName, StandardCharsets.UTF_8.name())

                            val libraryViewModel: LibraryViewModel = hiltViewModel()
                            val videos by libraryViewModel.videos.collectAsState()
                            val viewMode by libraryViewModel.viewMode.collectAsState()
                            val sortOrder by libraryViewModel.sortOrder.collectAsState()

                            FolderDetailScreen(
                                folderPath = decodedPath,
                                folderName = decodedName,
                                allVideos = videos,
                                viewMode = viewMode,
                                sortOrder = sortOrder,
                                onViewModeChange = { libraryViewModel.setViewMode(it) },
                                onSortOrderChange = { libraryViewModel.setSortOrder(it) },
                                onVideoClick = { video ->
                                    val encodedUri = URLEncoder.encode(video.uri.toString(), StandardCharsets.UTF_8.name())
                                    navController.navigate("player?uri=$encodedUri&isVideo=true")
                                },
                                onVideoMoreClick = { video ->
                                    libraryViewModel.inspectVideo(video)
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "player?uri={uri}&isVideo={isVideo}",
                            arguments = listOf(
                                navArgument("uri") { type = NavType.StringType },
                                navArgument("isVideo") { type = NavType.BoolType; defaultValue = true }
                            )
                        ) { backStackEntry ->
                            val rawUri = backStackEntry.arguments?.getString("uri") ?: ""
                            val decodedUri = URLDecoder.decode(rawUri, StandardCharsets.UTF_8.name())
                            val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: true

                            PlayerScreen(
                                mediaUri = Uri.parse(decodedUri),
                                isVideo = isVideo,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onMediaClick = { uri, isVideo ->
                                    val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name())
                                    navController.navigate("player?uri=$encodedUri&isVideo=$isVideo")
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("about") {
                            AboutScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
