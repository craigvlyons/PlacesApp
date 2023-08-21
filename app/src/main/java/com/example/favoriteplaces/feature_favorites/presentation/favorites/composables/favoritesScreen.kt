package com.example.favoriteplaces.feature_favorites.presentation.favorites.composables

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.favoriteplaces.feature_favorites.presentation.favorites.FavoritesViewModel
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    context: Context,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

//    val locationPermissionState = rememberMultiplePermissionsState(
//        listOf(
//            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
//        )
//    )

    // Initialize location-related components
    LaunchedEffect(Unit) {
        val fusedLocationClient ={ LocationServices.getFusedLocationProviderClient(context) }
        val placesClient ={ Places.createClient(context) }
        val geoCoder = { Geocoder(context) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AddNewFavoriteScreen.route)
                },

                backgroundColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add favorite",
                    tint = Color.White
                )
            }
        },
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Places",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

        }
    }


}

