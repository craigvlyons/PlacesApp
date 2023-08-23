package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

data class FavoriteUseCases(
    val getFavorite: GetFavoriteUseCase,
    val getFavorites: GetFavoritesUseCase,
    val deleteFavorite: DeleteFavoriteUseCase,
    val addFavorite: AddFavoriteUseCase
    )