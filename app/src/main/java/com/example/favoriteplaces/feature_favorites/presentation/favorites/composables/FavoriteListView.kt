package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.favorites.ColorGroupUiModel
import com.example.favoriteplaces.feature_favorites.presentation.sharedcomposables.SwipeToDeleteContainer

@Composable
fun FavoriteListView(
    favoritesList: ColorGroupUiModel,
    modifier: Modifier = Modifier,
    onOpen: (Favorite) -> Unit,
    onLovedClick: (Favorite, Boolean) -> Unit,
    onMapSelect: (Favorite) -> Unit,
    onDelete: (Favorite) -> Unit,
    expandedActionId: Int?,
    loadingActionIds: Set<Int>,
    onActionsToggle: (Favorite) -> Unit,
    onCall: (Favorite) -> Unit,
    onReserve: (Favorite) -> Unit,
    onWeb: (Favorite) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
            Column(
                Modifier.fillMaxWidth()
                    .drawBehind {
                        drawRect(Color(favoritesList.color), size = Size(6.dp.toPx(), size.height))
                    }
                    .padding(start = 20.dp, top = 10.dp, end = 16.dp, bottom = 2.dp)
            ) {
                favoritesList.favorites.forEach { favorite ->
                    SwipeToDeleteContainer(item = favorite, onDelete = onDelete) { item ->
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpen(item) }
                                        .padding(top = 2.dp, end = 8.dp)
                                        .testTag("grouped_text_${item.id}"),
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = item.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    item.content?.takeIf(String::isNotBlank)?.let { notes ->
                                        Text(
                                            text = notes,
                                            modifier = Modifier.padding(top = 6.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier.height(RATING_SLOT_SIZE),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CompactPlaceRating(
                                            rating = item.rating,
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .testTag("grouped_rating_${item.id}"),
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier
                                            .testTag("grouped_favorite_heart_${item.id}")
                                            .semantics { selected = item.isFavorite },
                                        onClick = { onLovedClick(item, !item.isFavorite) },
                                    ) {
                                        Icon(
                                            if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            modifier = Modifier
                                                .offset(y = 6.dp)
                                                .testTag("grouped_heart_visual_${item.id}"),
                                            contentDescription = if (item.isFavorite) {
                                                "Remove from Favorites"
                                            } else {
                                                "Add to Favorites"
                                            },
                                            tint = if (item.isFavorite) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.testTag("grouped_map_${item.id}"),
                                        onClick = { onMapSelect(item) },
                                    ) {
                                        Icon(
                                            Icons.Default.Map,
                                            modifier = Modifier.testTag("grouped_map_visual_${item.id}"),
                                            contentDescription = "Show ${item.title} on map",
                                        )
                                    }
                                }
                            }
                            val canCall = !item.phoneNumber.isNullOrBlank()
                            val canReserve = com.example.favoriteplaces.feature_favorites.domain.model.places
                                .isReservationEligibleGoogleType(item.googlePrimaryType)
                            PlaceActionDisclosure(
                                placeId = item.id,
                                placeName = item.title,
                                canCall = canCall,
                                canReserve = canReserve,
                                expanded = expandedActionId == item.id,
                                loading = item.id in loadingActionIds,
                                showDivider = true,
                                onToggle = { onActionsToggle(item) },
                                onCall = { onCall(item) },
                                onReserve = { onReserve(item) },
                                onWeb = { onWeb(item) },
                            )
                        }
                    }
                }
            }
    }
}

private val RATING_SLOT_SIZE = 24.dp
