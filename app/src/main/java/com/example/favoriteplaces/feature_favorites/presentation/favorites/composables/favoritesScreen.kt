package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.EditFavoriteViewModel
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesEvent
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesViewModel
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedMaterialScaffoldPaddingParameter")
@Composable
fun FavoritesScreen(
    navController: NavController,
    context: Context,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()


    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditFavoriteViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(key1 = state) {
        viewModel.updateCitiesAndColors()
    }

    // Initialize location-related components
    LaunchedEffect(Unit) {
        val fusedLocationClient = { LocationServices.getFusedLocationProviderClient(context) }
        val placesClient = { Places.createClient(context) }
        val geoCoder = { Geocoder(context) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AddNewFavoriteScreen.route)
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add favorite",
                    tint = Color.White
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 10.dp)
                ,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.h4
                )
                IconButton(
                    onClick = {
                        viewModel.onEvent(FavoritesEvent.ToggleOrderSelection)
                    },
                ) {
                    androidx.compose.material.Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        )
                }
            }
            AnimatedVisibility(
                visible = state.isOrderSelectionVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                OrderSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 0.dp)
                        .wrapContentWidth(),
                    favoriteOrder = state.favoriteOrder,
                    listView = state.isListView,
                    cardView = state.isCardView,
                    onOrderChange = {
                        viewModel.onEvent(FavoritesEvent.Order(it))
                    },
                    onViewChange = {
                        viewModel.onEvent(FavoritesEvent.ToggleListOrCardView)
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                if (state.isListView) {
                    LazyColumn {
                        items(
                            items = state.cityColorList,
                            key = { cityColorItem ->
                                cityColorItem.city
                            }
                            ) { cities ->
                            cities.colorVariations.forEach { colorVariation ->
                                if (!colorVariation.favorites.isNullOrEmpty()) {
                                    FavoriteListView(
                                        city = cities.city,
                                        favoritesList = colorVariation,
                                        onEditSelect = { favorite ->
                                            navController.navigate(Screen.EditFavoriteScreen.route + "?favoriteId=${favorite.id}&favoriteColor=${favorite.color}")
                                        },
                                        onMapSelect = { city, favoriteList ->
                                            viewModel.onEvent(FavoritesEvent.CityMapSelect(city, favoriteList))
                                            navController.navigate(Screen.CityMapScreen.route)
                                        },
                                        onDelete = {
                                            favoriteToDelete -> viewModel.onEvent(FavoritesEvent.DeleteFavorite(favoriteToDelete))
                                            scope.launch {
                                                val result = scaffoldState.snackbarHostState.showSnackbar(
                                                    message = "Favorite has been deleted",
                                                    actionLabel = "Undo"
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    viewModel.onEvent(FavoritesEvent.RestoreFavorite)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }   // ListView

                // show state. favorites in FavoriteItem
                if (state.isCardView) {
                    LazyColumn() {
                        items(
                            items = state.favorites,
                            key = { favorite ->
                                favorite.id!!
                            }
                            ) { favorite ->
                            FavoriteItem(
                                favorite = favorite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate(Screen.EditFavoriteScreen.route + "?favoriteId=${favorite.id}&favoriteColor=${favorite.color}")
                                    },
                                onDeleteClick = {
                                    viewModel.onEvent(FavoritesEvent.DeleteFavorite(favorite))
                                    scope.launch {
                                        val result = scaffoldState.snackbarHostState.showSnackbar(
                                            message = "Favorite has been deleted",
                                            actionLabel = "Undo"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.onEvent(FavoritesEvent.RestoreFavorite)
                                        }
                                    }
                                },
                                onLovedClick = { isFavorite ->
                                    viewModel.onEvent(
                                        FavoritesEvent.LovedFavorite(
                                            favorite.id!!,
                                            isFavorite
                                        )
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }  // card View
        }
    }
}




