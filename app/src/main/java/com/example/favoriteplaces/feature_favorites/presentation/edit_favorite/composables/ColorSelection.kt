package com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

private val ColorBoxSize = 45.dp

@Composable
fun ColorSelection(
    colors: List<Color>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        colors.forEach { color ->
            val colorInt = color.toArgb()
            Box(
                modifier = Modifier
                    .size(ColorBoxSize)
                    .shadow(15.dp, CircleShape)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 2.dp,
                        color = if (selectedColor == colorInt) Color.DarkGray else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable {
                        onColorSelected(colorInt)
                    }
            ) {
                // You could add an icon or any content here
            }
        }
    }

}