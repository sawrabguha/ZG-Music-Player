package com.example.zgmusicplayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zgmusicplayer.ui.theme.AppTheme
import com.example.zgmusicplayer.ui.theme.ZGTheme

class MainActivity : ComponentActivity() {

    private lateinit var player: PlayerController
    private lateinit var store: MusicStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EqualizerManager.init(this)
        player = PlayerController(this)
        player.connect()
        store = MusicStore(this)

        val prefs = getSharedPreferences("zg_settings", Context.MODE_PRIVATE)
        val savedTheme = AppTheme.entries.find { it.name == prefs.getString("theme", null) }
            ?: AppTheme.DARK
        val savedSort = SortOrder.entries.find { it.name == prefs.getString("sort", null) }
            ?: SortOrder.TITLE

        setContent {
            var theme by remember { mutableStateOf(savedTheme) }
            var sort by remember { mutableStateOf(savedSort) }

            // Make status-bar icons visible on both dark and light themes
            LaunchedEffect(theme) {
                val style = if (theme.isDark) {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            ZGTheme(theme) {
                CompositionLocalProvider(LocalMusicStore provides store) {
                    ZGApp(
                        player = player,
                        currentTheme = theme,
                        onThemeChange = { newTheme ->
                            theme = newTheme
                            prefs.edit().putString("theme", newTheme.name).apply()
                        },
                        sortOrder = sort,
                        onSortChange = { newSort ->
                            sort = newSort
                            prefs.edit().putString("sort", newSort.name).apply()
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        player.saveState()
        super.onStop()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    SONGS("Songs", Icons.Filled.MusicNote),
    FOLDERS("Folders", Icons.Filled.Folder),
    FAVOURITES("Favourites", Icons.Filled.Favorite),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZGApp(
    player: PlayerController,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.SONGS) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {

        // ----- Main screen (always kept alive underneath) -----
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ZG Music Player") },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                NowPlayingFab(player = player, onClick = { showNowPlaying = true })
            },
            bottomBar = {
                Column {
                    MiniPlayer(player = player, onOpen = { showNowPlaying = true })
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val contentModifier = Modifier.padding(padding)

            // Loads songs once and shares them with every tab
            MusicLibrary(player = player, modifier = contentModifier) { songs ->
                val sorted = remember(songs, sortOrder) { songs.applySort(sortOrder) }

                when (selectedTab) {
                    Tab.SONGS -> SongList(
                        songs = sorted,
                        player = player,
                        modifier = contentModifier
                    )
                    Tab.FOLDERS -> FoldersScreen(
                        songs = sorted,
                        player = player,
                        modifier = contentModifier
                    )
                    Tab.FAVOURITES -> FavouritesScreen(
                        songs = sorted,
                        player = player,
                        modifier = contentModifier
                    )
                    Tab.PLAYLISTS -> PlaylistsScreen(
                        songs = sorted,
                        player = player,
                        modifier = contentModifier
                    )
                }
            }
        }

        // ----- Settings (opens on top) -----
        if (showSettings) {
            BackHandler { showSettings = false }
            Surface(Modifier.fillMaxSize()) {
                SettingsScreen(
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    sortOrder = sortOrder,
                    onSortChange = onSortChange,
                    onBack = { showSettings = false }
                )
            }
        }

        // ----- Now Playing (slides up from the bottom) -----
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            BackHandler { showNowPlaying = false }
            NowPlayingScreen(player = player, onClose = { showNowPlaying = false })
        }
    }
}