package com.example.favoriteplaces.feature_favorites.presentation

import android.graphics.Color
import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.example.favoriteplaces.feature_favorites.presentation.add_new_favorite.composables.AddNewFavorite
import com.example.favoriteplaces.feature_favorites.presentation.backup.DataBackupScreen
import com.example.favoriteplaces.feature_favorites.presentation.citymap.composables.CityMapScreen
import com.example.favoriteplaces.feature_favorites.presentation.favorites.composables.FavoritesScreen
import com.example.favoriteplaces.feature_favorites.presentation.nearby.NearbyScreen
import com.example.favoriteplaces.feature_favorites.presentation.place_details.PlaceDetailsScreen
import com.example.favoriteplaces.feature_favorites.presentation.settings.SettingsScreen
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import com.example.favoriteplaces.ui.theme.FavoritePlacesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { FavoritePlacesRoot() }
    }
}

@Composable
private fun FavoritePlacesRoot(
    viewModel: AppAppearanceViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    FavoritePlacesTheme(appColor = preferences.appColor) {
        StatusBarIconContrast()
        PlacesNavigation()
    }
}

@Composable
private fun StatusBarIconContrast() {
    val view = LocalView.current
    val useDarkIcons = MaterialTheme.colorScheme.onPrimary != ComposeColor.White

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
    }
}

@Composable
private fun PlacesNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val destinations = listOf(
        PrimaryDestination(Screen.FavoritesScreen.route, "Saved", Icons.Default.Bookmarks),
        PrimaryDestination(Screen.AddNewFavoriteScreen.route, "Find", Icons.Default.Search),
        PrimaryDestination(Screen.NearbyScreen.route, "Nearby", Icons.Default.NearMe),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.primary),
        )

        Scaffold(
            containerColor = ComposeColor.Transparent,
            bottomBar = {
                if (destinations.any { it.route == currentRoute }) {
                    NavigationBar(
                        modifier = Modifier.testTag("primary_navigation"),
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = destination.route == currentRoute,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { appPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.FavoritesScreen.route,
                modifier = Modifier.padding(appPadding),
            ) {
                composable(Screen.FavoritesScreen.route) { FavoritesScreen(navController) }
                composable(Screen.AddNewFavoriteScreen.route) { AddNewFavorite(navController) }
                composable(Screen.NearbyScreen.route) { NearbyScreen(navController) }
                composable(
                    route = Screen.CityMapScreen.favoritePattern,
                    arguments = listOf(navArgument(Screen.CityMapScreen.FAVORITE_ID) {
                        type = NavType.IntType
                    }),
                ) { CityMapScreen(navController) }
                composable(
                    route = Screen.CityMapScreen.groupPattern,
                    arguments = listOf(
                        navArgument(Screen.CityMapScreen.COLOR) { type = NavType.IntType },
                        navArgument(Screen.CityMapScreen.CITY) { type = NavType.StringType },
                    ),
                ) { CityMapScreen(navController) }
                composable(Screen.DataBackupScreen.route) { DataBackupScreen(navController) }
                composable(Screen.SettingsScreen.route) { SettingsScreen(navController) }
                composable(
                    route = Screen.PlaceDetailsScreen.pattern,
                    arguments = listOf(navArgument(Screen.PlaceDetailsScreen.FAVORITE_ID) {
                        type = NavType.IntType
                    }),
                ) { PlaceDetailsScreen(navController) }
            }
        }
    }
}

private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)
