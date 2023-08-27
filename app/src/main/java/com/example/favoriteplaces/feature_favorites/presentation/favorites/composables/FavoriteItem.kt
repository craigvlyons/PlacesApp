package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.LovedButton


@Composable
fun FavoriteItem(
    favorite: Favorite,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    onDeleteClick: () -> Unit,
    onLovedClick: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .shadow(6.dp, shape = RoundedCornerShape(cornerRadius))
            .border(1.dp, Color(favorite.color), shape = RoundedCornerShape(cornerRadius))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val clipPath = Path().apply {
                lineTo(size.width, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            clipPath(clipPath) {
                drawRoundRect(
                    color = Color(favorite.color),
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = favorite.title,
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingStars(rating = favorite.rating ?: 0)
                LovedButton(
                    modifier = Modifier.padding(end = 6.dp),
                    isFavorite = favorite.isFavorite,
                    onClick = { onLovedClick(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = favorite.address,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = favorite.content!!,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                //.padding(end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete favorite",
                tint = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
@Preview
fun FavoriteItemPreview() {
    val sampleFavorite = Favorite(
        id = 1,
        title = "Sample Title",
        address = "123 Sample Street",
        content = "This is a sample content for the favorite item. It can be a longer text.",
        rating = 3,
        city = "Sample City",
        latitude = 12.345,
        longitude = 67.890
    )

    FavoriteItem(favorite = sampleFavorite, onDeleteClick = {}, onLovedClick = {})
}

@Composable
fun RatingStars(
    rating: Int,
    maxRating: Int = 5,
    activeStar: ImageVector = Icons.Default.Star,
    inactiveStar: ImageVector = Icons.Default.StarBorder
) {
    Row {
        repeat(maxRating) { index ->
            Icon(
                imageVector = if (index < rating) activeStar else inactiveStar,
                contentDescription = if (index < rating) "Active Star" else "Inactive Star",
                tint = if (index < rating) Color.Yellow else Color.Gray,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}
