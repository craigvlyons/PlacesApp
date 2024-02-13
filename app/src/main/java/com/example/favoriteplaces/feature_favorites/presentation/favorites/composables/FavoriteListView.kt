package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.ColorVariation
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.SwipeToDeleteContainer


@Composable
fun FavoriteListView(
    city: String,
    favoritesList: ColorVariation,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    onEditSelect: (favorite: Favorite) -> Unit,
    onMapSelect: (city: String, favoriteList: ColorVariation) -> Unit,
    onDelete: (favorite: Favorite) -> Unit
) {
    Box(
        modifier = modifier
            .shadow(6.dp, shape = RoundedCornerShape(cornerRadius))
            .border(1.dp, Color(favoritesList.color), shape = RoundedCornerShape(cornerRadius))
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
                    color = Color(favoritesList.color),
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
                text = city,
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onSurface,
            )
            Divider(thickness = 1.dp, color = Color.DarkGray)
            favoritesList.favorites.forEach {favortie ->
                SwipeToDeleteContainer(
                    item = favortie,
                    onDelete = { onDelete(it) },
                    content = {fav ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onEditSelect(fav) })
                                .padding(vertical = 8.dp)
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,

                            ) {
                            Text(
                                modifier = Modifier
                                    .weight(1f),
                                text = fav.title,
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Edit favorite"
                            )
                        }
                    }
                )
            }
        }
        IconButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp),
            onClick = {
                onMapSelect(city, favoritesList)
            }
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Show Map"
            )
        }
    }
}