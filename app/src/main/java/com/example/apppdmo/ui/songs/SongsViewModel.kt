package com.example.apppdmo.ui.songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apppdmo.data.local.entity.SongEntity
import com.example.apppdmo.data.repository.SongsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SongsUiState {
    data object Loading : SongsUiState
    data class Success(
        val songs: List<SongEntity>,
        val favoriteSongIds: Set<Int>,
        val selectedTab: Int
    ) : SongsUiState
    data class Empty(val message: String, val selectedTab: Int) : SongsUiState
    data class Error(val message: String) : SongsUiState
}

class SongsViewModel(
    private val songsRepository: SongsRepository,
    context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 = Todos, 1 = Favoritos
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _uiState = MutableStateFlow<SongsUiState>(SongsUiState.Loading)
    val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    init {
        initializeData(context)
    }

    fun initializeData(context: Context) {
        viewModelScope.launch {
            _uiState.value = SongsUiState.Loading
            try {
                songsRepository.ensureSongsDataSeeded(context)
            } catch (_: Exception) {
            }

            val songsFlow = combine(_searchQuery.debounce(200), _selectedTab) { query, tab ->
                Pair(query, tab)
            }.flatMapLatest { (query, tab) ->
                if (tab == 1) {
                    songsRepository.getFavoriteSongs()
                } else {
                    songsRepository.searchSongs(query)
                }
            }

            combine(
                songsFlow,
                songsRepository.getFavoriteSongIds(),
                _selectedTab
            ) { songsList, favIds, tab ->
                val filteredList = if (tab == 1 && _searchQuery.value.isNotBlank()) {
                    songsList.filter {
                        it.number.toString().contains(_searchQuery.value) ||
                                it.title.contains(_searchQuery.value, ignoreCase = true) ||
                                it.lyrics.contains(_searchQuery.value, ignoreCase = true)
                    }
                } else {
                    songsList
                }

                if (filteredList.isEmpty()) {
                    val emptyMessage = if (tab == 1) {
                        "Nenhum Cântico favorito.\nToque no coração de um Cântico para guardá-lo aqui."
                    } else {
                        "Nenhum Cântico encontrado."
                    }
                    SongsUiState.Empty(emptyMessage, tab)
                } else {
                    SongsUiState.Success(
                        songs = filteredList,
                        favoriteSongIds = favIds,
                        selectedTab = tab
                    )
                }
            }
                .catch { throwable ->
                    emit(SongsUiState.Error(throwable.localizedMessage ?: "Erro ao carregar Cânticos"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onTabSelected(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun toggleFavorite(songId: Int) {
        viewModelScope.launch {
            songsRepository.toggleFavorite(songId)
        }
    }

    class Factory(
        private val songsRepository: SongsRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SongsViewModel(songsRepository, context) as T
        }
    }
}
