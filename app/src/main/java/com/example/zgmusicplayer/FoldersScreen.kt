package com.example.zgmusicplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class MusicFolder(val path: String, val songs: List<Song>) {
    val name: String
        get() = path.substringAfterLast('/').ifEmpty { "Internal storage" }
}

@Composable
fun FoldersScreen(
    songs: List<Song>,
    player: PlayerController,
    modifier: Modifier = Modifier
) {
    // Group all songs by their folder
    val folders = remember(songs) {
        songs.groupBy { it.folder }
            .map { (path, list) -> MusicFolder(path, list) }
            .sortedBy { it.name.lowercase() }
    }

    var openFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    val openFolder = folders.find { it.path == openFolderPath }

    if (openFolder != null) {
        // ----- Inside one folder -----
        BackHandler { openFolderPath = null }

        Column(modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { openFolderPath = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        openFolder.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        openFolder.path.ifEmpty { "Internal storage" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Reuses the same list as the Songs tab, with Play all and Shuffle
            SongList(songs = openFolder.songs, player = player)
        }
    } else {
        // ----- List of all folders -----
        Column(modifier.fillMaxSize()) {
            Text(
                text = "${folders.size} folders",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(folders, key = { it.path }) { folder ->
                    FolderRow(
                        folder = folder,
                        onOpen = { openFolderPath = folder.path },
                        onPlay = { player.playQueue(folder.songs, 0, shuffle = false) }
                    )
                }
            }
        }
    }
}

@Composable
fun FolderRow(folder: MusicFolder, onOpen: () -> Unit, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                folder.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${folder.songs.size} songs · ${folder.path.ifEmpty { "Internal storage" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onPlay) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play folder",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}