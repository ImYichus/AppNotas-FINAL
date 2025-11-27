package com.jqleapa.appnotas.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeStyle {
    PURPLE,
    EMERALD
}


private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White
)


private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    tertiary = EmeraldTertiary,
    background = DarkSurface,
    surface = DarkSurface,
    onPrimary = Color.White
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    secondary = EmeraldLightSecondary,
    tertiary = EmeraldLightTertiary,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSurface = EmeraldLightOnSurface
)


@Composable
fun AppNotasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Ponemos dynamicColor en false para que NO use los colores del fondo de pantalla de Android 12+,
    // sino que use TU tema verde.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // AQUÍ ESTÁ EL TRUCO:
    // Asignamos directamente tu esquema EmeraldLightColorScheme.
    // Si quisieras mantener modo oscuro, podrías poner un 'if (darkTheme) ... else ...'
    // pero para cumplir tu petición de que se vea verde, usaremos este por defecto.
    val colorScheme = EmeraldLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Pintamos la barra de estado del color primario (Verde Oscuro)
            window.statusBarColor = colorScheme.primary.toArgb()

            // Como la barra es oscura (verde), los íconos (batería, hora) deben ser blancos (light = false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de tener Typography.kt
        content = content
    )
}
