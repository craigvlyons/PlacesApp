package com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun RatingStarsButtons(
    rating: Int,
    maxRating: Int = 5,
    onRatingChanged: (Int) -> Unit,
    activeStar: ImageVector = Icons.Default.Star,
    inactiveStar: ImageVector = Icons.Default.StarBorder
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(maxRating) { index ->
            ImageButton(
                isSelected = index < rating,
                onClick = { onRatingChanged(index + 1) },
                contentDescription = if (index < rating) "Active Star" else "Inactive Star",
                modifier = Modifier//.padding(2.dp)

            ) {
                Icon(
                    imageVector = if (index < rating) activeStar else inactiveStar,
                    contentDescription = null, // Content description already set at ImageButton level
                    tint = if (index < rating) Color.Yellow else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ImageButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = if (isSelected) Color.Yellow else Color.Gray
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.elevation(0.dp),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
@Preview
fun RatingStarsButtonsPreview() {
    val maxRating = 5
    val currentRating = 3

    RatingStarsButtons(
        rating = currentRating,
        maxRating = maxRating,
        onRatingChanged = { /* Handle rating change */ }
    )
}

@Composable
@Preview
fun ImageButtonPreview() {
    ImageButton(
        isSelected = true,
        onClick = { /* Handle button click */ },
        contentDescription = "Button Description"
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.Yellow
        )
    }
}