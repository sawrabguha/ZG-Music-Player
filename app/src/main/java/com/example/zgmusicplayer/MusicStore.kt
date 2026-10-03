package com.example.zgmusicplayer

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(val id: Long, val name: String, val songIds: List<Long>)

class MusicStore(context: Context) {

    private val prefs = context.getSharedPreferences("zg_library", Context.MODE_PRIVATE)

    var favourites by mutableStateOf(loadFavourites())
        private set
    var playlists by mutableStateOf(loadPlaylists())
        private set

    // ----- Favourites -----

    fun isFavourite(songId: Long): Boolean = songId in favourites

    fun toggleFavourite(songId: Long) {
        favourites = if (songId in favourites) favourites - songId else favourites + songId
        saveFavourites()
    }

    // ----- Playlists -----

    fun createPlaylist(name: String): Long {
        val id = System.currentTimeMillis()
        playlists = playlists + Playlist(id, name.trim(), emptyList())
        savePlaylists()
        return id
    }

    fun renamePlaylist(id: Long, name: String) {
        playlists = playlists.map { if (it.id == id) it.copy(name = name.trim()) else it }
        savePlaylists()
    }

    fun deletePlaylist(id: Long) {
        playlists = playlists.filterNot { it.id == id }
        savePlaylists()
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId && songId !in it.songIds) {
                it.copy(songIds = it.songIds + songId)
            } else it
        }
        savePlaylists()
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId) it.copy(songIds = it.songIds - songId) else it
        }
        savePlaylists()
    }
    // Called after a song file is deleted from the phone
    fun forgetSong(songId: Long) {
        if (songId in favourites) {
            favourites = favourites - songId
            saveFavourites()
        }
        if (playlists.any { songId in it.songIds }) {
            playlists = playlists.map { it.copy(songIds = it.songIds - songId) }
            savePlaylists()
        }
    }
    // ----- Saving and loading -----

    private fun loadFavourites(): Set<Long> = try {
        val arr = JSONArray(prefs.getString("favourites", "[]"))
        (0 until arr.length()).map { arr.getLong(it) }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    private fun saveFavourites() {
        prefs.edit().putString("favourites", JSONArray(favourites.toList()).toString()).apply()
    }

    private fun loadPlaylists(): List<Playlist> = try {
        val arr = JSONArray(prefs.getString("playlists", "[]"))
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val ids = obj.getJSONArray("songs")
            Playlist(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                songIds = (0 until ids.length()).map { ids.getLong(it) }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun savePlaylists() {
        val arr = JSONArray()
        playlists.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("songs", JSONArray(p.songIds))
            )
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
    }
}

// Lets any screen reach the store without passing it around
val LocalMusicStore = staticCompositionLocalOf<MusicStore> {
    error("MusicStore not provided")
}