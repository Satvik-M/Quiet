package com.satvikm.quiet.data.favorites

import com.satvikm.quiet.domain.model.LaunchableApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
) {
    /** Ordered by the user's chosen position, not alphabetically. */
    val favorites: Flow<List<FavoriteEntity>> = favoriteDao.observeAll()

    suspend fun toggleFavorite(app: LaunchableApp) {
        if (favoriteDao.isFavorite(app.id)) {
            favoriteDao.delete(app.id)
        } else if (favoriteDao.count() < MAX_FAVORITES) {
            favoriteDao.insert(FavoriteEntity(appId = app.id, position = favoriteDao.count()))
        }
    }

    companion object {
        /** Tier 0 spec: 1–8 favorites shown as plain text lines on home. */
        const val MAX_FAVORITES = 8
    }
}
