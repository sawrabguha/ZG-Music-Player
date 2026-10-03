package com.example.zgmusicplayer

import android.util.Size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

// ---------------- Mini player (bar above the tabs) ----------------

@Composable
fun MiniPlayer(player: PlayerController, onOpen: () -> Unit) {
    val song = player.currentSong ?: return

    val progress by produceState(0f, song.id) {
        while (true) {
            val total = player.duration()
            value = if (total > 0) {
                (player.position().toFloat() / total).coerceIn(0f, 1f)
            } else 0f
            delay(500)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(song = song, modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { player.playPause() }) {
                    Icon(
                        if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (player.isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}

// ---------------- Full Now Playing screen ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(player: PlayerController, onClose: () -> Unit) {
    val song = player.currentSong
    val density = LocalDensity.current
    val closeDistance = with(density) { 150.dp.toPx() }
    val swipeDistance = with(density) { 80.dp.toPx() }

    var dragY by remember { mutableFloatStateOf(0f) }          // swipe down to minimize
    var dragX by remember { mutableFloatStateOf(0f) }          // swipe cover left / right
    var direction by remember { mutableIntStateOf(1) }         // 1 = next, -1 = previous
    var showAddDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragY }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragY = (dragY + delta).coerceAtLeast(0f)
                },
                onDragStopped = { velocity ->
                    if (dragY > closeDistance || velocity > 2500f) {
                        onClose()
                    } else {
                        animate(dragY, 0f) { value, _ -> dragY = value }
                    }
                }
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Now Playing") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize")
                        }
                    },
                    actions = {
                        if (song != null) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = "Add to playlist"
                                )
                            }
                            FavouriteButton(song = song)
                        }
                    }
                )
            }
        ) { padding ->
            if (song == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nothing is playing")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ----- Album art: drag left for next, right for previous -----
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = dragX
                                alpha = 1f - (abs(dragX) / size.width.coerceAtLeast(1f))
                                    .coerceIn(0f, 0.5f)
                            }
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta -> dragX += delta },
                                onDragStopped = {
                                    if (dragX < -swipeDistance && player.hasNext()) {
                                        direction = 1
                                        dragX = 0f
                                        player.nextSong()
                                    } else if (dragX > swipeDistance && player.hasPrevious()) {
                                        direction = -1
                                        dragX = 0f
                                        player.previousSong()
                                    } else {
                                        animate(dragX, 0f) { value, _ -> dragX = value }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = song,
                            contentKey = { it.id },
                            transitionSpec = {
                                (slideInHorizontally { width -> width * direction } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> -width * direction } + fadeOut())
                            },
                            label = "albumArt"
                        ) { s ->
                            LargeAlbumArt(
                                song = s,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ----- Title + artist (fade when the song changes) -----
                    AnimatedContent(
                        targetState = song,
                        contentKey = { it.id },
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "songTitle"
                    ) { s ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                s.title,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SeekBar(player = player, songId = song.id)
                    Spacer(Modifier.height(8.dp))
                    PlayerControls(
                        player = player,
                        onPrevious = {
                            direction = -1
                            player.previous()
                        },
                        onNext = {
                            direction = 1
                            player.next()
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Swipe the cover to change song · Swipe down to minimize",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }

        if (showAddDialog && song != null) {
            AddToPlaylistDialog(song = song, onDismiss = { showAddDialog = false })
        }
    }
}

@Composable
fun SeekBar(player: PlayerController, songId: Long) {
    var position by remember(songId) { mutableLongStateOf(player.position()) }
    var duration by remember(songId) { mutableLongStateOf(player.duration()) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    // Update the position twice per second
    LaunchedEffect(songId) {
        while (true) {
            if (!dragging) position = player.position()
            duration = player.duration()
            delay(500)
        }
    }

    val maxValue = duration.coerceAtLeast(1L).toFloat()
    val shown = if (dragging) dragValue.toLong() else position

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = shown.toFloat().coerceIn(0f, maxValue),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                player.seekTo(dragValue.toLong())
                position = dragValue.toLong()
                dragging = false
            },
            valueRange = 0f..maxValue,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                formatDuration(shown),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatDuration(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlayerControls(
    player: PlayerController,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { player.toggleShuffle() }) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (player.shuffleOn) activeColor else inactiveColor
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(36.dp)
            )
        }
        FilledIconButton(onClick = { player.playPause() }, modifier = Modifier.size(72.dp)) {
            Icon(
                if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (player.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp)
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(36.dp)
            )
        }
        IconButton(onClick = { player.cycleRepeat() }) {
            Icon(
                if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne
                else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (player.repeatMode == Player.REPEAT_MODE_OFF) inactiveColor
                else activeColor
            )
        }
    }
}

// High-quality cover picture for the Now Playing screen
@Composable
fun LargeAlbumArt(song: Song, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val art by produceState<ImageBitmap?>(ArtCache.get(song.id), song.id) {
        val big = withContext(Dispatchers.IO) {
            try {
                context.contentResolver
                    .loadThumbnail(song.uri, Size(800, 800), null)
                    .asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        if (big != null) value = big
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
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
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ---------------- Round "Now Playing" button (bottom right) ----------------

@Composable
fun NowPlayingFab(player: PlayerController, onClick: () -> Unit) {
    val song = player.currentSong ?: return

    // The cover spins slowly like a record while music plays
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(player.isPlaying) {
        if (player.isPlaying) {
            while (true) {
                rotation.snapTo(rotation.value % 360f)
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = 8000, easing = LinearEasing)
                )
            }
        }
    }

    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp)
    ) {
        AlbumArt(
            song = song,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
        )
    }
}