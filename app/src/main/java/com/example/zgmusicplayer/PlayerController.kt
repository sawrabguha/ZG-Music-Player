package com.example.zgmusicplayer

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

class PlayerController(private val context: Context) {

    private var controller: MediaController? = null
    private var library: Map<Long, Song> = emptyMap()
    private val prefs = context.getSharedPreferences("zg_player", Context.MODE_PRIVATE)
    private var restoreDone = false

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var shuffleOn by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
            // Save whenever the queue, song, play/pause, shuffle or repeat changes
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED
                )
            ) {
                saveState()
            }
        }
    }

    fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener) }
            refresh()
            tryRestore()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        saveState()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    fun setLibrary(songs: List<Song>) {
        library = songs.associateBy { it.id }
        refresh()
        tryRestore()
    }

    private fun refresh() {
        val c = controller ?: return
        currentSong = c.currentMediaItem?.mediaId?.toLongOrNull()?.let { library[it] }
        isPlaying = c.playWhenReady
        shuffleOn = c.shuffleModeEnabled
        repeatMode = c.repeatMode
    }

    // ----- Remember the last song -----

    fun saveState() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        val ids = (0 until c.mediaItemCount).joinToString(",") { c.getMediaItemAt(it).mediaId }
        prefs.edit()
            .putString("queue", ids)
            .putString("current", c.currentMediaItem?.mediaId)
            .putLong("position", c.currentPosition)
            .putBoolean("shuffle", c.shuffleModeEnabled)
            .putInt("repeat", c.repeatMode)
            .apply()
    }

    // Loads the last queue (paused) once both the player and the song list are ready
    private fun tryRestore() {
        if (restoreDone) return
        val c = controller ?: return
        if (library.isEmpty()) return
        restoreDone = true

        // Music is still playing from before (app was only closed, not stopped)
        if (c.mediaItemCount > 0) return

        val ids = prefs.getString("queue", null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?: return
        val songs = ids.mapNotNull { library[it] }   // skips songs deleted since
        if (songs.isEmpty()) return

        val currentId = prefs.getString("current", null)?.toLongOrNull()
        val index = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val position = if (songs[index].id == currentId) prefs.getLong("position", 0L) else 0L

        c.shuffleModeEnabled = prefs.getBoolean("shuffle", false)
        c.repeatMode = prefs.getInt("repeat", Player.REPEAT_MODE_OFF)
        c.setMediaItems(songs.map { it.toMediaItem() }, index, position)
        c.prepare()   // ready but paused — press play to continue
    }

    // ----- Playback -----

    // Play a list of songs starting from one of them
    fun playQueue(songs: List<Song>, startIndex: Int, shuffle: Boolean? = null) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        restoreDone = true
        if (shuffle != null) c.shuffleModeEnabled = shuffle
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playWhenReady) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    // Buttons: "previous" restarts the song if more than 3 seconds have played
    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }

    // Swipes: always go to the actual next / previous song
    fun nextSong() { controller?.seekToNextMediaItem() }
    fun previousSong() { controller?.seekToPreviousMediaItem() }
    fun hasNext(): Boolean = controller?.hasNextMediaItem() == true
    fun hasPrevious(): Boolean = controller?.hasPreviousMediaItem() == true

    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun position(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long =
        controller?.duration?.takeIf { it > 0 } ?: (currentSong?.duration ?: 0L)

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    // Removes a deleted song from the play queue
    fun removeSong(songId: Long) {
        val c = controller ?: return
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(i).mediaId == songId.toString()) c.removeMediaItem(i)
        }
    }
}

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    )
    .build()