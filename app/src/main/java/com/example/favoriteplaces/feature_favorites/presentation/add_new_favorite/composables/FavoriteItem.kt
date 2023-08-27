package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

@Composable
fun FavoriteItem(
    favorite: Favorite,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    onDeleteClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(Color(favorite.color))
            ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                //.padding(end = 32.dp)
                //.border(1.dp, Color.Blue)
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp),
                text = favorite.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth()
                    .padding(10.dp),
                text = favorite.address,
                style = MaterialTheme.typography.bodyLarge  ,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!favorite.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .padding(10.dp),
                    text = favorite.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // add Map.

        }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete note",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
    }


}


@Preview
@Composable
fun PreviewFavorite(){
    FavoriteItem(favorite = Favorite(
        id= 0,
        title = "place to eat",
        address = "1111 main street, colorado springs, CO 80903",
        placeId = "123456789",
        content = "would i eat here again?",
        rating = 3,
        city = "",
        latitude = 364758.0,
        longitude = 3846539.0,
    )
    ) {

    }
}


