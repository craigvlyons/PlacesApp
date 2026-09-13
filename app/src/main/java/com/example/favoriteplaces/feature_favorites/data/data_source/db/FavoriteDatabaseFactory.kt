package com.example.favoriteplaces.feature_favorites.data.data_source.db

import android.content.Context
import androidx.room.Room

fun buildFavoriteDatabase(
    context: Context,
    name: String = FavoriteDatabase.DATABASE_NAME,
): FavoriteDatabase = Room.databaseBuilder(
    context.applicationContext,
    FavoriteDatabase::class.java,
    name,
).build()
