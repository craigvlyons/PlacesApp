package com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteEvent
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.AddNewFavoriteViewModel


@Composable
fun AddNewFavorite(
    viewModel: AddNewFavoriteViewModel = hiltViewModel()
) {
    val searchString = viewModel.favoriteTitle.value
    val uiState by viewModel.uiState
    var selectedId = ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            //verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TransparentHintField(
                    text = searchString.text,
                    hint = searchString.hint,
                    onValueChange = { newValue ->
                        viewModel.onEvent(AddNewFavoriteEvent.EnteredSearch(newValue))
                    },
                    onFocusChange = { viewModel.onEvent(AddNewFavoriteEvent.ChangeSearchFocus(it)) },
                    isHintVisible = searchString.isHintVisible,
                    textStyle = androidx.compose.material.MaterialTheme.typography.body1,
                    modifier = Modifier
                        .padding(10.dp),
                )
            }
            // Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {

                Button(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary)
                        .border(1.dp, Color.DarkGray, shape = RoundedCornerShape(size = 20.dp)),
                    onClick = { viewModel.onEvent(AddNewFavoriteEvent.Search) },

                ) {
                    Text(
                        text = "Search")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(uiState.predictions) { prediction ->
                    PredictionCard(
                        prediction = prediction,
                        location = viewModel.uiState.value.currentLocation,
                        selectedId = selectedId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedId = prediction.placeId
                                viewModel.onEvent(AddNewFavoriteEvent.SelectedResult(prediction))
                            },

                        )
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }

        }


    }


}


