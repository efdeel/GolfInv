package com.example.golfinv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Golf Inv Brand Colors
val GolfGreenPrimary = Color(0xFF1B4D3E)
val GolfGreenSecondary = Color(0xFF2E7D32)
val GolfAccentGold = Color(0xFFD4AF37)
val GolfBgLight = Color(0xFFF4F7F5)
val GolfSurfaceLight = Color(0xFFFFFFFF)
val GolfDarkPrimary = Color(0xFF81C784)
val GolfDarkBg = Color(0xFF121B18)
val GolfDarkSurface = Color(0xFF1E2A26)

private val DarkColorScheme = darkColorScheme(
    primary = GolfDarkPrimary,
    secondary = GolfAccentGold,
    tertiary = GolfGreenSecondary,
    background = GolfDarkBg,
    surface = GolfDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GolfGreenPrimary,
    secondary = GolfGreenSecondary,
    tertiary = GolfAccentGold,
    background = GolfBgLight,
    surface = GolfSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)

@Composable
fun GolfInvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
