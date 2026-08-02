package com.example.attendancetracker.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.attendancetracker.data.local.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary              = Indigo60,
    onPrimary            = DarkBg,
    primaryContainer     = DarkSurface2,
    onPrimaryContainer   = Indigo80,
    secondary            = Teal80,
    onSecondary          = DarkBg,
    secondaryContainer   = DarkCard,
    onSecondaryContainer = Teal80,
    background           = DarkBg,
    onBackground         = Color(0xFFE8E8FF),
    surface              = DarkSurface,
    onSurface            = Color(0xFFE8E8FF),
    surfaceVariant       = DarkCard,
    onSurfaceVariant     = Color(0xFFBBBBDD),
    outline              = DarkCardBorder,
    error                = Coral,
    onError              = DarkBg,
)

private val AmoledColorScheme = darkColorScheme(
    primary              = Indigo60,
    onPrimary            = Color.Black,
    primaryContainer     = Color(0xFF141414),
    onPrimaryContainer   = Indigo80,
    secondary            = Teal80,
    onSecondary          = Color.Black,
    secondaryContainer   = Color(0xFF1E1E1E),
    onSecondaryContainer = Teal80,
    background           = Color.Black,
    onBackground         = Color(0xFFFFFFFF),
    surface              = Color(0xFF0A0A0A),
    onSurface            = Color(0xFFFFFFFF),
    surfaceVariant       = Color(0xFF121212),
    onSurfaceVariant     = Color(0xFFCCCCCC),
    outline              = Color(0xFF262626),
    error                = Coral,
    onError              = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary              = Indigo40,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFEADDFF),
    onPrimaryContainer   = Color(0xFF21005D),
    secondary            = Teal40,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF004D40),
    background           = Color(0xFFF8F9FA),
    onBackground         = Color(0xFF1C1B1F),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1C1B1F),
    surfaceVariant       = Color(0xFFF0F0F6),
    onSurfaceVariant     = Color(0xFF666680),
    outline              = Color(0xFFE0E0E8),
    error                = Coral,
    onError              = Color.White,
)

@Composable
fun AttendanceTrackerTheme(
    mode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val targetScheme = when (mode) {
        AppThemeMode.LIGHT  -> LightColorScheme
        AppThemeMode.DARK   -> DarkColorScheme
        AppThemeMode.AMOLED -> AmoledColorScheme
    }

    // Animated colors for smooth theme transitions
    val animatedBg by animateColorAsState(targetScheme.background, animationSpec = tween(300), label = "bg")
    val animatedSurface by animateColorAsState(targetScheme.surface, animationSpec = tween(300), label = "surface")
    val animatedSurfaceVariant by animateColorAsState(targetScheme.surfaceVariant, animationSpec = tween(300), label = "surfaceVariant")
    val animatedOnBg by animateColorAsState(targetScheme.onBackground, animationSpec = tween(300), label = "onBg")
    val animatedOutline by animateColorAsState(targetScheme.outline, animationSpec = tween(300), label = "outline")

    val animatedColorScheme = targetScheme.copy(
        background     = animatedBg,
        surface        = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
        onBackground   = animatedOnBg,
        outline        = animatedOutline
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography  = Typography,
        content     = content
    )
}
