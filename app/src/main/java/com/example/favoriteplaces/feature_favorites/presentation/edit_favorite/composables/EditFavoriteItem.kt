package com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite


@Composable
fun EditFavoriteItem(
    favorite: Favorite,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    onRatingChanged: (Int) -> Unit
) {

    Box(
        modifier = modifier
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
            TransparentTextField(
                text = favorite.title,
                hint = "",
                onValueChange = {},
                textStyle = MaterialTheme.typography.h4,
                onFocusChange = {}
            )

            Spacer(modifier = Modifier.height(8.dp))
            RatingStarsButtons(
                rating = favorite.rating ?: 0, // currentRating
                onRatingChanged = onRatingChanged
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = favorite.address!!,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn() {
                item {
                    TransparentTextField(
                        text = favorite.content!!,
                        hint = "",
                        onValueChange = {},
                        textStyle = MaterialTheme.typography.body1,
                        onFocusChange = {}
                    )
                }
            }

        }

    }
}

@Composable
@Preview
fun EditFavoriteItemPreview() {
    val sampleFavorite = Favorite(
        id = 1,
        title = "Sample Title",
        address = "123 Sample Street, colorado springs, CO 80917",
        content = "This is a sample content for the favorite item. It can be a longer text.",
        rating = 3,
        city = "Sample City",
        latitude = 12.345,
        longitude = 67.890
    )

    EditFavoriteItem(favorite = sampleFavorite, onRatingChanged = {})
}
//
//@Composable
//fun RatingStarsButtons(
//    rating: Int,
//    maxRating: Int = 5,
//    onRatingChanged: (Int) -> Unit,
//    activeStar: ImageVector = Icons.Default.Star,
//    inactiveStar: ImageVector = Icons.Default.StarBorder
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        repeat(maxRating) { index ->
//            ImageButton(
//                isSelected = index < rating,
//                onClick = { onRatingChanged(index + 1) },
//                contentDescription = if (index < rating) "Active Star" else "Inactive Star",
//                modifier = Modifier//.padding(2.dp)
//
//            ) {
//                Icon(
//                    imageVector = if (index < rating) activeStar else inactiveStar,
//                    contentDescription = null, // Content description already set at ImageButton level
//                    tint = if (index < rating) Color.Yellow else Color.Gray
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun ImageButton(
//    isSelected: Boolean,
//    onClick: () -> Unit,
//    contentDescription: String,
//    modifier: Modifier = Modifier,
//    content: @Composable () -> Unit
//) {
//    Button(
//        onClick = onClick,
//        shape = CircleShape,
//        colors = ButtonDefaults.buttonColors(
//            backgroundColor = Color.Transparent,
//            contentColor = if (isSelected) Color.Yellow else Color.Gray
//        ),
//        contentPadding = PaddingValues(0.dp),
//        modifier = modifier
//    ) {
//        content()
//    }
//}