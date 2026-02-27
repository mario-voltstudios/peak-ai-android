package com.peakai.fitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Forge Mountain Palette ───────────────────────────────────────────────────
val BackgroundDark      = Color(0xFF09090B)  // zinc-950
val SurfaceDark         = Color(0xFF18181B)  // zinc-900
val SurfaceVariantDark  = Color(0xFF27272A)  // zinc-800
val AmberPrimary        = Color(0xFFF59E0B)  // amber-500
val AmberLight          = Color(0xFFFBBF24)  // amber-400
val AmberDark           = Color(0xFFD97706)  // amber-600
val OnSurface           = Color(0xFFF4F4F5)  // zinc-100
val OnSurfaceVariant    = Color(0xFFA1A1AA)  // zinc-400
val ErrorRed            = Color(0xFFEF4444)  // red-500
val SuccessGreen        = Color(0xFF22C55E)  // green-500

private val PeakColorScheme = darkColorScheme(
    primary             = AmberPrimary,
    onPrimary           = Color(0xFF1C1400),
    primaryContainer    = Color(0xFF3B2800),
    onPrimaryContainer  = AmberLight,
    secondary           = Color(0xFFA16207),
    onSecondary         = Color(0xFF1C1400),
    background          = BackgroundDark,
    onBackground        = OnSurface,
    surface             = SurfaceDark,
    onSurface           = OnSurface,
    surfaceVariant      = SurfaceVariantDark,
    onSurfaceVariant    = OnSurfaceVariant,
    outline             = Color(0xFF3F3F46),  // zinc-700
    error               = ErrorRed,
    onError             = Color.White
)

@Composable
fun PeakAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PeakColorScheme,
        typography = PeakTypography,
        content = content
    )
}
