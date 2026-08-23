package com.example.favoriteplaces.feature_favorites.presentation.citymap

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.favoriteplaces.feature_favorites.presentation.util.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CityMapRouteTest {
    @Test
    fun groupRouteRoundTripsUnicodeAndReservedCharactersWithoutRecordData() {
        val city = "Café / Denver?&#"
        val route = Screen.CityMapScreen.groupRoute(city = city, color = -749647)

        assertEquals(city, Uri.parse(route).getQueryParameter(Screen.CityMapScreen.CITY))
        assertEquals("city_map_screen/group/-749647", route.substringBefore('?'))
        assertFalse(route.contains(city))
    }

    @Test
    fun favoriteRouteContainsOnlyTheStableRecordId() {
        assertEquals("city_map_screen/favorite/42", Screen.CityMapScreen.favoriteRoute(42))
    }

    @Test
    fun detailsRouteContainsOnlyTheStableRecordId() {
        assertEquals("place_details/42", Screen.PlaceDetailsScreen.route(42))
    }
}
