package com.ridwan.bolakuy2026.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ridwan.bolakuy2026.data.Channel
import com.ridwan.bolakuy2026.data.EmbeddedPlaylist
import com.ridwan.bolakuy2026.data.FavoritesDataStore
import com.ridwan.bolakuy2026.data.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    object Loading : UiState
    data class Success(
        val channels: List<Channel>,
        val categories: List<String>,
        val selectedCategory: String,
        val searchQuery: String
    ) : UiState
    data class Error(val message: String) : UiState
}

class ChannelViewModel(application: Application) : AndroidViewModel(application) {
    private val favoritesDataStore = FavoritesDataStore(application)

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedChannel = MutableStateFlow<Channel?>(null)
    val selectedChannel = _selectedChannel.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoritesDataStore.favoriteChannelIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredChannels: StateFlow<List<Channel>> = combine(
        _allChannels,
        _selectedCategory,
        _searchQuery,
        favoriteIds
    ) { channels, category, query, favs ->
        var list = channels
        
        if (category == "Favorites") {
            list = list.filter { favs.contains(it.id) }
        } else if (category != "All") {
            list = list.filter { it.groupTitle == category }
        }

        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.tvgName.contains(query, ignoreCase = true) ||
                it.groupTitle.contains(query, ignoreCase = true)
            }
        }
        
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        _loading,
        _error,
        filteredChannels,
        _categories,
        _selectedCategory,
        _searchQuery
    ) { array ->
        val loading = array[0] as Boolean
        val error = array[1] as String?
        val channels = array[2] as List<Channel>
        val cats = array[3] as List<String>
        val selectedCat = array[4] as String
        val query = array[5] as String
        when {
            loading -> UiState.Loading
            error != null -> UiState.Error(error)
            else -> UiState.Success(
                channels = channels,
                categories = cats,
                selectedCategory = selectedCat,
                searchQuery = query
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val parsed = withContext(Dispatchers.IO) {
                    M3uParser.parse(EmbeddedPlaylist.playlist)
                }
                _allChannels.value = parsed
                
                val cats = listOf("All", "Favorites") + parsed.map { it.groupTitle }.distinct().sorted()
                _categories.value = cats
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed parsing playlist"
                _loading.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playChannel(channel: Channel?) {
        _selectedChannel.value = channel
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            favoritesDataStore.toggleFavorite(channelId)
        }
    }
}
