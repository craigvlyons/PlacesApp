package com.example.favoriteplaces.di

import android.app.Application
import androidx.room.Room
import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.repository.FavoriteRepositoryImpl
import com.example.favoriteplaces.feature_favorites.data.repository.MapItemsRepositoryImpl
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.MapItemsRepository
import com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase.GetMapItemsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase.MapItemsCacheUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.cacheusecase.SaveMapItemsUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.AddFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.DeleteFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteExistsByPlaceIdUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetAllCitiesUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesByCityAndColorUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.GetFavoritesUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.UpdateIsFavorite
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FavoritesModule {
    @Provides
    @Singleton
    fun provideFavoriteDatabase(app: Application): FavoriteDatabase{
        return Room.databaseBuilder(
            app,
            FavoriteDatabase::class.java,
            FavoriteDatabase.DATABASE_NAME
        ).build()
    }
    @Provides
    @Singleton
    fun provideFavoriteRepository(db: FavoriteDatabase) : FavoriteRepository{
        return FavoriteRepositoryImpl(db.favoriteDao)
    }
    @Provides
    @Singleton
    fun provideFavoriteUseCase(repository: FavoriteRepository): FavoriteUseCases{
        return FavoriteUseCases(
            getFavorite = GetFavoriteUseCase(repository),
            getFavorites = GetFavoritesUseCase(repository),
            deleteFavorite = DeleteFavoriteUseCase(repository),
            updateIsFavorite = UpdateIsFavorite(repository),
            addFavorite = AddFavoriteUseCase(repository),
            getAllCities = GetAllCitiesUseCase(repository),
            getFavoritesByCityAndColor = GetFavoritesByCityAndColorUseCase(repository),
            favoriteExistsByPlaceIdUseCase = FavoriteExistsByPlaceIdUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideMapItemsRepository() : MapItemsRepository {
        return MapItemsRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideMapItemsUseCase( repository: MapItemsRepository) : MapItemsCacheUseCases {
        return MapItemsCacheUseCases(
            getMapItems = GetMapItemsUseCase(repository),
            saveMapItems = SaveMapItemsUseCase(repository)
        )
    }

}