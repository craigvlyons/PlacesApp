package com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Displays place-type metadata without assigning it a semantic accent color.
 *
 * Place colors belong to the user's place classification, while a place type is
 * ordinary supporting information. Keeping this presentation centralized avoids
 * accidentally suggesting that individual types have color meanings.
 */
@Composable
fun PlaceTypeLabel(
    type: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    maxLines: Int = 1,
) {
    type?.takeIf(String::isNotBlank)?.let { value ->
        Text(
            text = value,
            modifier = modifier,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
