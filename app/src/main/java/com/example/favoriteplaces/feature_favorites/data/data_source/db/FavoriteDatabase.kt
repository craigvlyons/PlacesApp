package com.example.favoriteplaces.feature_favorites.data.data_source.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 4)
abstract class FavoriteDatabase: RoomDatabase() {
    abstract val favoriteDao: FavoriteDao

    companion object{
        const val DATABASE_NAME = "favorites_db"
    }
}
