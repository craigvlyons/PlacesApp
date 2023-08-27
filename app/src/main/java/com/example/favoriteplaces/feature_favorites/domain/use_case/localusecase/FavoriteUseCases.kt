package com.example.favoriteplaces.feature_favorites.domain.use_case.localusecase

data class FavoriteUseCases(
    val getFavorite: GetFavoriteUseCase,
    val getFavorites: GetFavoritesUseCase,
    val deleteFavorite: DeleteFavoriteUseCase,
    val updateIsFavorite: UpdateIsFavorite,
    val addFavorite: AddFavoriteUseCase,
    val getAllCitiesUseCase: GetAllCitiesUseCase,
    val getFavoritesByCityAndColorUseCase: GetFavoritesByCityAndColorUseCase
    )