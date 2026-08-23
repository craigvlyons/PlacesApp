package com.example.favoriteplaces.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.favoriteplaces.feature_favorites.domain.model.settings.DEFAULT_APP_COLOR_ARGB

private fun lightColors(appColor: Color) = lightColorScheme(
    primary = appColor,
    onPrimary = readableContentColor(appColor),
    primaryContainer = appColor.copy(alpha = 0.24f).compositeOver(AppSurface),
    onPrimaryContainer = AppInk,
    secondary = Color(0xFF53665F),
    background = AppCanvas,
    onBackground = AppInk,
    surface = AppSurface,
    onSurface = AppInk,
    surfaceVariant = Color(0xFFE2E9E4),
    onSurfaceVariant = Color(0xFF424B46),
    outline = Color(0xFF727B76),
)

fun readableContentColor(background: Color): Color =
    if (background.luminance() > 0.48f) Color(0xFF202126) else Color.White

@Composable
fun FavoritePlacesTheme(
    appColor: Int = DEFAULT_APP_COLOR_ARGB,
    content: @Composable () -> Unit
) {
    val accent = Color(appColor)
    MaterialTheme(
        colorScheme = lightColors(accent),
        typography = Typography,
        content = content
    )
}
