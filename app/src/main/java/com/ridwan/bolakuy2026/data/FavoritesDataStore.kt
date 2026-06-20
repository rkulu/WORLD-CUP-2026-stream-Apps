package com.ridwan.bolakuy2026.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.favoritesStore: DataStore<Preferences> by preferencesDataStore(name = "bolakuy_favorites")

class FavoritesDataStore(private val context: Context) {
    companion object {
        private val FAVORITE_IDS = stringSetPreferencesKey("favorite_channel_ids")
    }

    val favoriteChannelIds: Flow<Set<String>> = context.favoritesStore.data
        .map { preferences ->
            preferences[FAVORITE_IDS] ?: emptySet()
        }

    suspend fun toggleFavorite(channelId: String) {
        context.favoritesStore.edit { preferences ->
            val current = preferences[FAVORITE_IDS] ?: emptySet()
            val newSet = current.toMutableSet()
            if (newSet.contains(channelId)) {
                newSet.remove(channelId)
            } else {
                newSet.add(channelId)
            }
            preferences[FAVORITE_IDS] = newSet
        }
    }
}
