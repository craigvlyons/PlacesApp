package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables


import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LovedButton(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Boolean) -> Unit) {
    var isSelected by remember { mutableStateOf(isFavorite) }

    IconButton(
        onClick = {
            isSelected = !isSelected
            onClick(isSelected)
        }
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isSelected) "Solid Heart" else "Outlined Heart",
            tint = if (isSelected) Color.Red else Color.DarkGray
        )
    }
}


@Composable
@Preview
fun HeartButtonPreview() {
    LovedButton(
        isFavorite = true,
        onClick = {     }
    )
}