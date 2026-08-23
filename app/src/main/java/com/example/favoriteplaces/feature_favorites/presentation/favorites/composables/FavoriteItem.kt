package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

@Composable
fun FavoriteItem(
    favorite: Favorite,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onMapClick: () -> Unit,
    onLovedClick: (Boolean) -> Unit,
    actionsExpanded: Boolean,
    actionsLoading: Boolean,
    onActionsToggle: () -> Unit,
    onCall: () -> Unit,
    onReserve: () -> Unit,
    onWeb: () -> Unit,
) {
    val canCall = !favorite.phoneNumber.isNullOrBlank()
    val canReserve = com.example.favoriteplaces.feature_favorites.domain.model.places
        .isReservationEligibleGoogleType(favorite.googlePrimaryType)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    drawRect(Color(favorite.color), size = Size(6.dp.toPx(), size.height))
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .testTag("favorite_text_${favorite.id}"),
                    ) {
                        Text(
                            text = favorite.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = favorite.address,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        favorite.content?.takeIf(String::isNotBlank)?.let { notes ->
                            Text(
                                modifier = Modifier.padding(top = 12.dp),
                                text = notes,
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
                                rating = favorite.rating,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("favorite_rating_${favorite.id}"),
                            )
                        }
                        IconButton(
                            modifier = Modifier
                                .testTag("favorite_heart_${favorite.id}")
                                .semantics { selected = favorite.isFavorite },
                            onClick = { onLovedClick(!favorite.isFavorite) },
                        ) {
                            Icon(
                                imageVector = if (favorite.isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                modifier = Modifier
                                    .offset(y = 6.dp)
                                    .testTag("favorite_heart_visual_${favorite.id}"),
                                contentDescription = if (favorite.isFavorite) {
                                    "Remove from Favorites"
                                } else {
                                    "Add to Favorites"
                                },
                                tint = if (favorite.isFavorite) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        IconButton(
                            modifier = Modifier
                                .testTag("favorite_map_${favorite.id}"),
                            onClick = onMapClick,
                        ) {
                            Icon(
                                Icons.Default.Map,
                                modifier = Modifier.testTag("favorite_map_visual_${favorite.id}"),
                                contentDescription = "Show ${favorite.title} on map",
                            )
                        }
                    }
                }
            }
            PlaceActionDisclosure(
                placeId = favorite.id,
                placeName = favorite.title,
                canCall = canCall,
                canReserve = canReserve,
                expanded = actionsExpanded,
                loading = actionsLoading,
                showDivider = true,
                onToggle = onActionsToggle,
                onCall = onCall,
                onReserve = onReserve,
                onWeb = onWeb,
            )
        }
    }
}

private val RATING_SLOT_SIZE = 24.dp
