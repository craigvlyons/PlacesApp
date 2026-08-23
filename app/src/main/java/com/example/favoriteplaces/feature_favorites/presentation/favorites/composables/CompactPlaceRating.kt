package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun CompactPlaceRating(
    rating: Int?,
    modifier: Modifier = Modifier,
) {
    val displayRating = rating?.coerceIn(MIN_RATING, MAX_RATING) ?: MIN_RATING
    Row(
        modifier = modifier.semantics {
            contentDescription = "Rating $displayRating out of $MAX_RATING"
        },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayRating.toString(),
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (displayRating > MIN_RATING) {
                RATED_STAR_COLOR
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
    }
}

private val RATED_STAR_COLOR = Color(0xFFF5B400)
private const val MIN_RATING = 0
private const val MAX_RATING = 5
