package com.example.zgmusicplayer

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val HeartRed = Color(0xFFE53935)

// ---------------- Heart button (used on Now Playing) ----------------

@Composable
fun FavouriteButton(song: Song) {
    val store = LocalMusicStore.current
    val fav = store.isFavourite(song.id)
    IconButton(onClick = { store.toggleFavourite(song.id) }) {
        Icon(
            if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (fav) "Remove from favourites" else "Add to favourites",
            tint = if (fav) HeartRed else LocalContentColor.current
        )
    }
}

// ---------------- Favourites tab ----------------

@Composable
fun FavouritesScreen(
    songs: List<Song>,
    player: PlayerController,
    modifier: Modifier = Modifier
) {
    val store = LocalMusicStore.current
    val favourites = store.favourites
    val favSongs = remember(songs, favourites) { songs.filter { it.id in favourites } }

    if (favSongs.isEmpty()) {
        EmptyMessage(
            modifier = modifier,
            icon = Icons.Filled.FavoriteBorder,
            title = "No favourites yet",
            subtitle = "Tap ⋮ on any song → Add to favourites, or the ♡ on the Now Playing screen."
        )
    } else {
        SongList(songs = favSongs, player = player, modifier = modifier)
    }
}

// ---------------- Playlists tab ----------------

@Composable
fun PlaylistsScreen(
    songs: List<Song>,
    player: PlayerController,
    modifier: Modifier = Modifier
) {
    val store = LocalMusicStore.current
    val songsById = remember(songs) { songs.associateBy { it.id } }

    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    val open = store.playlists.find { it.id == openId }

    if (open != null) {
        // ----- Inside one playlist -----
        BackHandler { openId = null }
        val list = open.songIds.mapNotNull { songsById[it] }

        Column(modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { openId = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        open.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${list.size} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (list.isEmpty()) {
                EmptyMessage(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "This playlist is empty",
                    subtitle = "Tap ⋮ on any song → Add to playlist."
                )
            } else {
                SongList(
                    songs = list,
                    player = player,
                    onRemoveFromList = { song -> store.removeFromPlaylist(open.id, song.id) }
                )
            }
        }
    } else {
        // ----- List of playlists -----
        Column(modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${store.playlists.size} playlists",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create playlist")
                }
            }

            if (store.playlists.isEmpty()) {
                EmptyMessage(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "No playlists yet",
                    subtitle = "Tap Create playlist to make your first one."
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(store.playlists, key = { it.id }) { p ->
                        PlaylistRow(
                            playlist = p,
                            count = p.songIds.count { it in songsById },
                            onOpen = { openId = p.id },
                            onPlay = {
                                player.playQueue(p.songIds.mapNotNull { songsById[it] }, 0, shuffle = false)
                            },
                            onRename = { renameTarget = p },
                            onDelete = { deleteTarget = p }
                        )
                    }
                }
            }
        }
    }

    // ----- Dialogs -----
    if (showCreate) {
        NameDialog(
            title = "New playlist",
            initial = "",
            confirmText = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                store.createPlaylist(name)
                showCreate = false
            }
        )
    }

    renameTarget?.let { p ->
        NameDialog(
            title = "Rename playlist",
            initial = p.name,
            confirmText = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                store.renamePlaylist(p.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete playlist?") },
            text = { Text("\"${p.name}\" will be deleted. Your songs will not be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    store.deletePlaylist(p.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PlaylistRow(
    playlist: Playlist,
    count: Int,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$count songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onPlay) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play playlist",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}

// ---------------- Dialogs ----------------

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Playlist name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddToPlaylistDialog(song: Song, onDismiss: () -> Unit) {
    val store = LocalMusicStore.current
    val context = LocalContext.current
    var creatingNew by remember { mutableStateOf(false) }

    if (creatingNew) {
        NameDialog(
            title = "New playlist",
            initial = "",
            confirmText = "Create",
            onDismiss = onDismiss,
            onConfirm = { name ->
                val id = store.createPlaylist(name)
                store.addToPlaylist(id, song.id)
                Toast.makeText(context, "Added to ${name.trim()}", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // "New playlist" option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { creatingNew = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("New playlist", color = MaterialTheme.colorScheme.primary)
                }
                // Existing playlists
                store.playlists.forEach { p ->
                    val already = song.id in p.songIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                store.addToPlaylist(p.id, song.id)
                                Toast.makeText(context, "Added to ${p.name}", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            p.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (already) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Already added",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------------- Shared empty-state message ----------------

@Composable
fun EmptyMessage(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}