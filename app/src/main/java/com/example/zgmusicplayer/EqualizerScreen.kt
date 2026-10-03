package com.example.zgmusicplayer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val eq = EqualizerManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { eq.reset() }, enabled = eq.isSupported) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { padding ->
        if (!eq.isSupported) {
            EmptyMessage(
                modifier = Modifier.padding(padding),
                icon = Icons.Filled.GraphicEq,
                title = "Equalizer not available",
                subtitle = "This phone does not allow apps to use an equalizer."
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ----- On / Off -----
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Enable equalizer",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = eq.enabled, onCheckedChange = { eq.turnOn(it) })
                    }
                }

                // ----- Presets -----
                if (eq.presetNames.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Presets")
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        eq.presetNames.forEachIndexed { index, name ->
                            FilterChip(
                                selected = eq.currentPreset == name,
                                onClick = { eq.usePreset(index) },
                                label = { Text(name) },
                                enabled = eq.enabled
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }

                // ----- Bands -----
                Spacer(Modifier.height(24.dp))
                SectionTitle("Bands")
                eq.bandLevels.forEachIndexed { band, level ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            eq.bandLabels.getOrElse(band) { "" },
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(64.dp)
                        )
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { eq.setBandLevel(band, it.roundToInt()) },
                            valueRange = eq.minLevel.toFloat()..eq.maxLevel.toFloat(),
                            enabled = eq.enabled,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "%+.1f dB".format(level / 100f),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }

                // ----- Bass boost -----
                if (eq.bassSupported) {
                    Spacer(Modifier.height(24.dp))
                    SectionTitle("Bass boost")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = eq.bassStrength.toFloat(),
                            onValueChange = { eq.setBass(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eq.enabled,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${eq.bassStrength / 10}%",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Changes apply instantly while music is playing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}