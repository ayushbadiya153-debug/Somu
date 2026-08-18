package com.example.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF121212)
val DarkSurfaceVariant = Color(0xFF1A1A1A)
val DarkBorder = Color(0xFF262626)

val ElectricBlue = Color(0xFF007AFF)
val ElectricBlueContainer = Color(0xFF002F6C)
val ProfitGreen = Color(0xFF34C759)
val ProfitGreenContainer = Color(0xFF0A3614)
val LossRed = Color(0xFFFF3B30)
val LossRedContainer = Color(0xFF3E0A08)
val WarningYellow = Color(0xFFFFCC00)
val TextMuted = Color(0xFFA0A0A0)
val TextDarkMuted = Color(0xFF666666)

val NexusColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueContainer,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF2C2C2E),
    onSecondary = Color.White,
    tertiary = ProfitGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = LossRed,
    onError = Color.White,
    errorContainer = LossRedContainer,
    onErrorContainer = Color.White,
    outline = DarkBorder
)
