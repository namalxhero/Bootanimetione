package com.nipuna.bootanimator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OneUIDarkScheme = darkColorScheme(
    primary = OneUIBlue,
    onPrimary = Color.White,
    secondary = OneUIBlueDark,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = CardDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDarkAlt,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed
)

@Composable
fun BootAnimatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OneUIDarkScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
