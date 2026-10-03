package com.example.zgmusicplayer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.LruCache
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Actions any song row can use (like deleting a song)
class SongActions(val delete: (Song) -> Unit)

val LocalSongActions = staticCompositionLocalOf { SongActions(delete = {}) }

// ---------------- Loads songs once (asks permission first) ----------------

@Composable
fun MusicLibrary(
    player: PlayerController,
    modifier: Modifier = Modifier,
    content: @Composable (List<Song>) -> Unit
) {
    val context = LocalContext.current
    val store = LocalMusicStore.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    var songs by remember { mutableStateOf<List<Song>?>(null) }

    // ----- Deleting a song -----
    var pendingDelete by remember { mutableStateOf<Song?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val song = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && song != null) {
            val updated = songs?.filterNot { it.id == song.id } ?: emptyList()
            songs = updated
            player.removeSong(song.id)
            player.setLibrary(updated)
            store.forgetSong(song.id)
            Toast.makeText(context, "Deleted \"${song.title}\"", Toast.LENGTH_SHORT).show()
        }
    }
    val actions = remember {
        SongActions(delete = { song ->
            pendingDelete = song
            // Android shows its own "Allow deleting this file?" popup
            val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(song.uri))
            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        })
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val loaded = withContext(Dispatchers.IO) { loadSongs(context) }
            songs = loaded
            player.setLibrary(loaded)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        }
    }

    val list = songs
    when {
        !hasPermission -> PermissionMessage(
            modifier = modifier,
            onAllow = { permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO) },
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
        )
        list == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        list.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No songs found on this phone")
        }
        else -> CompositionLocalProvider(LocalSongActions provides actions) {
            content(list)
        }
    }
}

@Composable
fun PermissionMessage(
    modifier: Modifier = Modifier,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "ZG Music Player needs permission to find the music on your phone.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAllow) { Text("Allow access") }
        TextButton(onClick = onOpenSettings) { Text("Open app settings") }
    }
}

// ---------------- Song list (used by Songs, Folders, Favourites, Playlists) ----------------

@Composable
fun SongList(
    songs: List<Song>,
    player: PlayerController,
    modifier: Modifier = Modifier,
    onRemoveFromList: ((Song) -> Unit)? = null
) {
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${songs.size} songs",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = { player.playQueue(songs, 0, shuffle = false) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play all")
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = { player.playQueue(songs, songs.indices.random(), shuffle = true) }
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Shuffle")
            }
        }

        val currentId = player.currentSong?.id
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentId,
                    onClick = { player.playQueue(songs, index) },
                    onRemoveFromList = onRemoveFromList?.let { remove -> { remove(song) } }
                )
            }
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemoveFromList: (() -> Unit)? = null
) {
    val store = LocalMusicStore.current
    val actions = LocalSongActions.current
    val isFav = store.isFavourite(song.id)
    var menuOpen by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(song = song, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isFav) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favourite",
                tint = HeartRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            formatDuration(song.duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ⋮ menu
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (isFav) "Remove from favourites" else "Add to favourites") },
                    leadingIcon = {
                        Icon(
                            if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) HeartRed else LocalContentColor.current
                        )
                    },
                    onClick = {
                        store.toggleFavourite(song.id)
                        menuOpen = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                    },
                    onClick = {
                        menuOpen = false
                        showAddDialog = true
                    }
                )
                if (onRemoveFromList != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from playlist") },
                        leadingIcon = {
                            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null)
                        },
                        onClick = {
                            menuOpen = false
                            onRemoveFromList()
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete from phone", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menuOpen = false
                        actions.delete(song)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddToPlaylistDialog(song = song, onDismiss = { showAddDialog = false })
    }
}

// ---------------- Album art (small, cached) ----------------

object ArtCache {
    private val cache = LruCache<Long, ImageBitmap>(300)
    private val noArt = mutableSetOf<Long>()

    fun get(id: Long): ImageBitmap? = cache.get(id)
    fun put(id: Long, art: ImageBitmap) { cache.put(id, art) }
    fun markNoArt(id: Long) { synchronized(noArt) { noArt.add(id) } }
    fun hasNoArt(id: Long): Boolean = synchronized(noArt) { id in noArt }
}

@Composable
fun AlbumArt(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val art by produceState(initialValue = ArtCache.get(song.id), song.id) {
        if (value == null && !ArtCache.hasNoArt(song.id)) {
            value = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver
                        .loadThumbnail(song.uri, Size(160, 160), null)
                        .asImageBitmap()
                        .also { ArtCache.put(song.id, it) }
                } catch (e: Exception) {
                    ArtCache.markNoArt(song.id)
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val picture = art
        if (picture != null) {
            Image(
                bitmap = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}