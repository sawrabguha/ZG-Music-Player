package com.example.zgmusicplayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// To rename a theme, change only the first text in quotes (the label).
enum class AppTheme(val label: String, val subtitle: String, val isDark: Boolean) {
    DARK("Mayabini", "Silver night", true),
    LIGHT("Mann Bawra", "Golden day", false),
    LIGHT_GREEN("Tree", "Fresh green", false),
    SAFFRON("Ya Ali", "Saffron", false),
    MAYABINI("Maya", "Violet magic", true),
    ANAMIKA("Anamika", "Rose pink", false),
    DIL("Dil", "Ocean blue", true)
}

fun AppTheme.colorScheme(): ColorScheme = when (this) {
    AppTheme.DARK -> zgDark(
        primary = Color(0xFFC5CAE9), onPrimary = Color(0xFF1A1A2E),
        container = Color(0xFF2E3250), onContainer = Color(0xFFE3E6FF),
        background = Color(0xFF0F1115), onBackground = Color(0xFFE8E8EC),
        variant = Color(0xFF2A2D34), onVariant = Color(0xFFB8BBC4),
        panel = Color(0xFF1A1C22)
    )
    AppTheme.LIGHT -> zgLight(
        primary = Color(0xFFB26A00),
        container = Color(0xFFFFE7B3), onContainer = Color(0xFF3A2500),
        background = Color(0xFFFFFDF7), onBackground = Color(0xFF1F1B16),
        variant = Color(0xFFF3EDE0), onVariant = Color(0xFF5E5646),
        panel = Color(0xFFFBF5E8)
    )
    AppTheme.LIGHT_GREEN -> zgLight(
        primary = Color(0xFF2E7D32),
        container = Color(0xFFC8E6C9), onContainer = Color(0xFF0B2E0D),
        background = Color(0xFFE8F5E9), onBackground = Color(0xFF102A12),
        variant = Color(0xFFD0E8D2), onVariant = Color(0xFF3B5A3D),
        panel = Color(0xFFDCEFDD)
    )
    AppTheme.SAFFRON -> zgLight(
        primary = Color(0xFFE67E00),
        container = Color(0xFFFFD8A8), onContainer = Color(0xFF3D2000),
        background = Color(0xFFFFF4E5), onBackground = Color(0xFF2B1A0A),
        variant = Color(0xFFFFE3C2), onVariant = Color(0xFF6B4A2A),
        panel = Color(0xFFFFEBD1)
    )
    AppTheme.MAYABINI -> zgDark(
        primary = Color(0xFFCFA8FF), onPrimary = Color(0xFF2A0A4A),
        container = Color(0xFF4A2A70), onContainer = Color(0xFFEBDCFF),
        background = Color(0xFF140D1E), onBackground = Color(0xFFEDE6F5),
        variant = Color(0xFF2E2440), onVariant = Color(0xFFC3B6D6),
        panel = Color(0xFF1E1530)
    )
    AppTheme.ANAMIKA -> zgLight(
        primary = Color(0xFFC2185B),
        container = Color(0xFFFFD6E4), onContainer = Color(0xFF3E0019),
        background = Color(0xFFFFF5F8), onBackground = Color(0xFF2A1A20),
        variant = Color(0xFFF8E1E9), onVariant = Color(0xFF6B4A55),
        panel = Color(0xFFFDEBF1)
    )
    AppTheme.DIL -> zgDark(
        primary = Color(0xFF64B5F6), onPrimary = Color(0xFF002C4F),
        container = Color(0xFF0D3B66), onContainer = Color(0xFFD4E8FF),
        background = Color(0xFF0A1622), onBackground = Color(0xFFE3EDF7),
        variant = Color(0xFF1C2D3E), onVariant = Color(0xFFAFC3D6),
        panel = Color(0xFF102030)
    )
}

// Builds a complete light colour set from a few base colours
private fun zgLight(
    primary: Color,
    onPrimary: Color = Color.White,
    container: Color,
    onContainer: Color,
    background: Color,
    onBackground: Color,
    variant: Color,
    onVariant: Color,
    panel: Color
) = lightColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = container, onPrimaryContainer = onContainer,
    secondary = primary, onSecondary = onPrimary,
    secondaryContainer = container, onSecondaryContainer = onContainer,
    tertiary = primary, onTertiary = onPrimary,
    tertiaryContainer = container, onTertiaryContainer = onContainer,
    background = background, onBackground = onBackground,
    surface = background, onSurface = onBackground,
    surfaceVariant = variant, onSurfaceVariant = onVariant,
    surfaceContainerLowest = background, surfaceContainerLow = panel,
    surfaceContainer = panel, surfaceContainerHigh = variant,
    surfaceContainerHighest = variant,
    outline = onVariant, outlineVariant = variant
)

// Builds a complete dark colour set from a few base colours
private fun zgDark(
    primary: Color,
    onPrimary: Color,
    container: Color,
    onContainer: Color,
    background: Color,
    onBackground: Color,
    variant: Color,
    onVariant: Color,
    panel: Color
) = darkColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = container, onPrimaryContainer = onContainer,
    secondary = primary, onSecondary = onPrimary,
    secondaryContainer = container, onSecondaryContainer = onContainer,
    tertiary = primary, onTertiary = onPrimary,
    tertiaryContainer = container, onTertiaryContainer = onContainer,
    background = background, onBackground = onBackground,
    surface = background, onSurface = onBackground,
    surfaceVariant = variant, onSurfaceVariant = onVariant,
    surfaceContainerLowest = background, surfaceContainerLow = panel,
    surfaceContainer = panel, surfaceContainerHigh = variant,
    surfaceContainerHighest = variant,
    outline = onVariant, outlineVariant = variant
)

@Composable
fun ZGTheme(theme: AppTheme, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = theme.colorScheme(),
        typography = Typography,
        content = content
    )
}