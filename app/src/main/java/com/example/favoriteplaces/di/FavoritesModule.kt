package com.example.favoriteplaces.di

import android.app.Application
import com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.data_source.db.buildFavoriteDatabase
import com.example.favoriteplaces.feature_favorites.data.repository.FavoriteRepositoryImpl
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteRepository
import com.example.favoriteplaces.feature_favorites.domain.repository.FavoriteDetailsRepository
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.AddFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.DeleteFavoriteUseCase
import com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase.FavoriteUseCases
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
        return buildFavoriteDatabase(app)
    }
    @Provides
    @Singleton
    fun provideFavoriteRepositoryImpl(db: FavoriteDatabase): FavoriteRepositoryImpl {
        return FavoriteRepositoryImpl(db.favoriteDao)
    }
    @Provides
    @Singleton
    fun provideFavoriteRepository(repository: FavoriteRepositoryImpl): FavoriteRepository = repository

    @Provides
    @Singleton
    fun provideFavoriteDetailsRepository(
        repository: FavoriteRepositoryImpl,
    ): FavoriteDetailsRepository = repository
    @Provides
    @Singleton
    fun provideFavoriteUseCase(repository: FavoriteRepository): FavoriteUseCases{
        return FavoriteUseCases(
            getFavorite = GetFavoriteUseCase(repository),
            getFavorites = GetFavoritesUseCase(repository),
            deleteFavorite = DeleteFavoriteUseCase(repository),
            updateIsFavorite = UpdateIsFavorite(repository),
            addFavorite = AddFavoriteUseCase(repository),
            getFavoritesByCityAndColor = GetFavoritesByCityAndColorUseCase(repository),
        )
    }
}
