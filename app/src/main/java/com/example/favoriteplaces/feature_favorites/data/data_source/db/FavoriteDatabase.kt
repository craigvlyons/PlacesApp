package com.example.favoriteplaces.feature_favorites.data.data_source.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.favoriteplaces.feature_favorites.domain.model.Favorite

@Database(entities = [Favorite::class], version = 1)
abstract class FavoriteDatabase: RoomDatabase() {
    abstract val favoriteDao: FavoriteDao

    companion object{
        const val DATABASE_NAME = "favorites_db"
    }
}