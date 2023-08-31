package com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.composables

import android.annotation.SuppressLint
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite
import com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.EditFavoriteEvent
import com.example.favoriteplaces.feature_favorites.presentation.edit_favorite.EditFavoriteViewModel
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun EditFavoriteScreen(
    navController: NavController,
    favoriteColor: Int,
    editFavoriteViewModel: EditFavoriteViewModel = hiltViewModel()
) {
    val titleState = editFavoriteViewModel.favoriteTitle.value
    val contentState = editFavoriteViewModel.favoriteContent.value
    val ratingState = editFavoriteViewModel.favoriteRating.value
    val addressFirst = editFavoriteViewModel.address.value.firstOrNull() ?: ""
    val addressSecond =
        editFavoriteViewModel.address.value.drop(1).mapNotNull { it.ifEmpty { null } }
            .joinToString(", ")

    val scaffoldState = rememberScaffoldState()
    val favoriteBackgroundAnimatible = remember {
        Animatable(
            Color(if (favoriteColor != -1) favoriteColor else editFavoriteViewModel.favoriteColor.value)
        )
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = true) {
        editFavoriteViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditFavoriteViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }

                is EditFavoriteViewModel.UiEvent.SaveFavorite -> {
                    navController.navigateUp()
                }

                else -> {}
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editFavoriteViewModel.onEvent(EditFavoriteEvent.SaveFavorite)
                },
                backgroundColor = MaterialTheme.colors.background
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Favorite",
                    tint = Color(MaterialTheme.colors.onSurface.toArgb())
                )
            }
        },
        scaffoldState = scaffoldState,
        floatingActionButtonPosition = FabPosition.Center
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(favoriteBackgroundAnimatible.value)
                    .padding(16.dp),
            ) {
                ColorSelection(
                    colors = Favorite.favoriteColors,
                    selectedColor = editFavoriteViewModel.favoriteColor.value,
                    onColorSelected = { color ->
                        scope.launch {
                            favoriteBackgroundAnimatible.animateTo(
                                targetValue = Color(color),
                                animationSpec = tween(durationMillis = 500)
                            )
                        }
                        editFavoriteViewModel.onEvent(EditFavoriteEvent.ChangeColor(color))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                TransparentTextField(
                    text = titleState.text,
                    hint = titleState.hint,
                    onValueChange = {
                        editFavoriteViewModel.onEvent(
                            EditFavoriteEvent.EnteredTitle(
                                it
                            )
                        )
                    },
                    onFocusChange = {
                        editFavoriteViewModel.onEvent(
                            EditFavoriteEvent.ChangeTitleFocus(
                                it
                            )
                        )
                    },
                    isHintVisible = titleState.isHintVisible,
                    singleLine = false,
                    textStyle = MaterialTheme.typography.h5,
                )

                Spacer(modifier = Modifier.height(8.dp))
                RatingStarsButtons(
                    rating = ratingState,
                    onRatingChanged = { rating ->
                        editFavoriteViewModel.onEvent(
                            EditFavoriteEvent.ChangeRating(
                                rating
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(start = 3.dp),
                    text = addressFirst,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(0.5.dp))

                Text(
                    text = addressSecond,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    )

                Spacer(modifier = Modifier.height(16.dp))

                TransparentTextField(
                    text = contentState.text,
                    hint = contentState.hint,
                    isHintVisible = contentState.isHintVisible,
                    onValueChange = {
                        editFavoriteViewModel.onEvent(
                            EditFavoriteEvent.EnteredContent(
                                it
                            )
                        )
                    },
                    onFocusChange = {
                        editFavoriteViewModel.onEvent(
                            EditFavoriteEvent.ChangeContentFocus(
                                it
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                )
            }
            IconButton(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.BottomEnd),
                onClick = {
                    editFavoriteViewModel.onEvent(EditFavoriteEvent.MapSelect)
                    navController.navigate(Screen.CityMapScreen.route)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map button"
                )
            }
        }
    }
}


