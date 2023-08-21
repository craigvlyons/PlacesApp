package com.example.favoriteplaces.feature_favorites.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables.AddNewFavorite
import com.example.favoriteplaces.feature_favorites.presentation.favorites.composables.FavoritesScreen
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.example.favoriteplaces.ui.theme.FavoritePlacesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the Places SDK with your API key

//        val apiKey = "AIzaSyCIqp5O_6r5xb9tphh1vEzHtAo41y5yJ_I"
//        Places.initialize(applicationContext, apiKey)

        setContent {
            FavoritePlacesTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.FavoritesScreen.route
                    ){
                        composable(route= Screen.FavoritesScreen.route){
                            FavoritesScreen(navController = navController, context = applicationContext)
                        }

                        composable(route = Screen.AddNewFavoriteScreen.route){
                            AddNewFavorite()
                        }

                    }

                }

            }
        }
    }
}
