package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.favoriteplaces.feature_favorites.domain.model.Favorites



@Composable
fun FavoriteListView(
    favorites: Favorites,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
){
    Box(

    ){
        Column(

        ) {
            favorites.favoriteList.forEach {
                Row() {
                    Text(
                        text = it.title
                    )
                }
            }

        }
    }


}