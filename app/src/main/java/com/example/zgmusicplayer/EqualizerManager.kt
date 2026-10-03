package com.example.zgmusicplayer

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object EqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var prefs: SharedPreferences? = null

    var audioSessionId: Int = 0
        private set

    // Values the screen shows (they update the screen automatically)
    var isSupported by mutableStateOf(false)
        private set
    var enabled by mutableStateOf(false)
        private set
    var bandLevels by mutableStateOf(listOf<Int>())
        private set
    var bassStrength by mutableIntStateOf(0)
        private set
    var currentPreset by mutableStateOf("Custom")
        private set

    var minLevel = -1500
        private set
    var maxLevel = 1500
        private set
    var bandLabels: List<String> = emptyList()
        private set
    var presetNames: List<String> = emptyList()
        private set

    val bassSupported: Boolean
        get() = try { bassBoost?.strengthSupported == true } catch (e: Exception) { false }

    // Called once by the app and by the playback service
    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        prefs = app.getSharedPreferences("zg_equalizer", Context.MODE_PRIVATE)
        audioSessionId = app.getSystemService(AudioManager::class.java).generateAudioSessionId()

        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val range = eq.bandLevelRange
            minLevel = range[0].toInt()
            maxLevel = range[1].toInt()
            bandLabels = (0 until eq.numberOfBands.toInt()).map {
                formatFrequency(eq.getCenterFreq(it.toShort()))
            }
            presetNames = (0 until eq.numberOfPresets.toInt()).map {
                eq.getPresetName(it.toShort())
            }
            isSupported = true
        } catch (e: Exception) {
            equalizer = null
            isSupported = false
        }

        bassBoost = try { BassBoost(0, audioSessionId) } catch (e: Exception) { null }

        restore()
    }

    fun turnOn(on: Boolean) {
        enabled = on
        try { equalizer?.enabled = on } catch (e: Exception) { }
        try { bassBoost?.enabled = on } catch (e: Exception) { }
        save()
    }

    fun setBandLevel(band: Int, level: Int) {
        try { equalizer?.setBandLevel(band.toShort(), level.toShort()) } catch (e: Exception) { }
        bandLevels = bandLevels.toMutableList().also { it[band] = level }
        currentPreset = "Custom"
        save()
    }

    fun usePreset(index: Int) {
        val eq = equalizer ?: return
        try { eq.usePreset(index.toShort()) } catch (e: Exception) { return }
        bandLevels = readLevels()
        currentPreset = presetNames.getOrElse(index) { "Custom" }
        save()
    }

    fun setBass(strength: Int) {
        bassStrength = strength
        try { bassBoost?.setStrength(strength.toShort()) } catch (e: Exception) { }
        save()
    }

    fun reset() {
        bandLevels.indices.forEach {
            try { equalizer?.setBandLevel(it.toShort(), 0) } catch (e: Exception) { }
        }
        bandLevels = readLevels()
        currentPreset = "Custom"
        setBass(0)
    }

    // ----- Helpers -----

    private fun readLevels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfBands.toInt()).map { eq.getBandLevel(it.toShort()).toInt() }
    }

    private fun formatFrequency(milliHertz: Int): String {
        val hz = milliHertz / 1000
        return if (hz >= 1000) {
            "%.1f".format(hz / 1000f).removeSuffix(".0") + " kHz"
        } else {
            "$hz Hz"
        }
    }

    private fun save() {
        prefs?.edit()
            ?.putBoolean("enabled", enabled)
            ?.putString("bands", bandLevels.joinToString(","))
            ?.putInt("bass", bassStrength)
            ?.putString("preset", currentPreset)
            ?.apply()
    }

    private fun restore() {
        val p = prefs ?: return
        val eq = equalizer

        val saved = p.getString("bands", null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
        if (eq != null && saved != null && saved.size == eq.numberOfBands.toInt()) {
            saved.forEachIndexed { i, level ->
                try { eq.setBandLevel(i.toShort(), level.toShort()) } catch (e: Exception) { }
            }
        }
        bandLevels = readLevels()
        currentPreset = p.getString("preset", "Custom") ?: "Custom"

        bassStrength = p.getInt("bass", 0)
        try { bassBoost?.setStrength(bassStrength.toShort()) } catch (e: Exception) { }

        enabled = p.getBoolean("enabled", false)
        try { eq?.enabled = enabled } catch (e: Exception) { }
        try { bassBoost?.enabled = enabled } catch (e: Exception) { }
    }
}