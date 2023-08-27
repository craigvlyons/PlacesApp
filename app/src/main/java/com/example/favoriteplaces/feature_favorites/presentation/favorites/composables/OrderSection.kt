package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.favoriteplaces.feature_favorites.presentation.util.FavoriteOrder
import com.example.favoriteplaces.feature_favorites.presentation.util.OrderType


@Composable
fun OrderSection(
    modifier: Modifier = Modifier,
    favoriteOrder: FavoriteOrder = FavoriteOrder.City(OrderType.Descending),
    listView: Boolean = true,
    cardView: Boolean = false,
    onOrderChange: (FavoriteOrder) -> Unit,
    onViewChange: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            DefaultRadioButton(
                text = "City",
                selected = favoriteOrder is FavoriteOrder.City,
                onSelect = { onOrderChange(FavoriteOrder.City(favoriteOrder.orderType)) }
            )
            DefaultRadioButton(
                text = "Loved",
                selected = favoriteOrder is FavoriteOrder.IsFavorite,
                onSelect = { onOrderChange(FavoriteOrder.IsFavorite(favoriteOrder.orderType)) }
            )
            DefaultRadioButton(
                text = "Color",
                selected = favoriteOrder is FavoriteOrder.Color,
                onSelect = { onOrderChange(FavoriteOrder.Color(favoriteOrder.orderType)) }
            )
            DefaultRadioButton(
                text = "Rating",
                selected = favoriteOrder is FavoriteOrder.Rating,
                onSelect = { onOrderChange(FavoriteOrder.Rating(favoriteOrder.orderType)) }
            )
        }

        //Spacer(modifier = modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            DefaultRadioButton(
                text = "Ascending",
                selected = favoriteOrder.orderType is OrderType.Ascending,
                onSelect = { onOrderChange(favoriteOrder.copy(OrderType.Ascending)) }
            )

            DefaultRadioButton(
                text = "Descending",
                selected = favoriteOrder.orderType is OrderType.Descending,
                onSelect = { onOrderChange(favoriteOrder.copy(OrderType.Descending)) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            DefaultRadioButton(
                text = "List View",
                selected = listView,
                onSelect = { onViewChange() }
            )

            DefaultRadioButton(
                text = "Card View",
                selected = cardView,
                onSelect = { onViewChange() }
            )
        }

    }
}



@Preview(showBackground = true)
@Composable
fun OrderSectionPreview() {
    OrderSection(
        favoriteOrder = FavoriteOrder.City(OrderType.Descending),
        onOrderChange = {},
        onViewChange = {}
    )
}