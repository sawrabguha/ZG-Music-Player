package com.example.zgmusicplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.zgmusicplayer.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onBack: () -> Unit
) {
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    var showThemes by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    if (showEqualizer) {
        BackHandler { showEqualizer = false }
        EqualizerScreen(onBack = { showEqualizer = false })
        return
    }
    if (showThemes) {
        BackHandler { showThemes = false }
        ThemesScreen(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onBack = { showThemes = false }
        )
        return
    }
    if (showAbout) {
        BackHandler { showAbout = false }
        AboutScreen(onBack = { showAbout = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            // ----- Appearance -----
            Box(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Appearance")
            }
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Themes",
                subtitle = "${currentTheme.label} · ${currentTheme.subtitle}",
                onClick = { showThemes = true }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ----- Audio -----
            Box(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Audio")
            }
            SettingsRow(
                icon = Icons.Filled.GraphicEq,
                title = "Equalizer",
                subtitle = if (EqualizerManager.enabled) {
                    "On · ${EqualizerManager.currentPreset}"
                } else "Off",
                onClick = { showEqualizer = true }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ----- Song order -----
            Box(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Song order")
            }
            SortOrder.entries.forEach { option ->
                RadioRow(
                    text = option.label,
                    selected = option == sortOrder,
                    onClick = { onSortChange(option) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // ----- About -----
            Box(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("About")
            }
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "About ZG Music Player",
                subtitle = "Version ${appVersion(context)} · Sawrab Guha",
                onClick = { showAbout = true }
            )
        }
    }
}

@Composable
fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}