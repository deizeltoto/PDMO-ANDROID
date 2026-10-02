package com.example.apppdmo.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apppdmo.data.local.entity.SongEntity
import com.example.apppdmo.data.repository.SongsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface SongDetailUiState {
    data object Loading : SongDetailUiState
    data class Success(
        val song: SongEntity,
        val isFavorite: Boolean
    ) : SongDetailUiState
    data class Error(val message: String) : SongDetailUiState
}

class SongDetailViewModel(
    private val songsRepository: SongsRepository,
    val songId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<SongDetailUiState>(SongDetailUiState.Loading)
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    init {
        loadSongDetail()
    }

    private fun loadSongDetail() {
        viewModelScope.launch {
            combine(
                songsRepository.getSongById(songId),
                songsRepository.isFavorite(songId)
            ) { song, isFav ->
                if (song == null) {
                    SongDetailUiState.Error("Cântico não encontrado.")
                } else {
                    SongDetailUiState.Success(
                        song = song,
                        isFavorite = isFav
                    )
                }
            }
                .catch { throwable ->
                    emit(SongDetailUiState.Error(throwable.localizedMessage ?: "Erro ao carregar o Cântico"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            songsRepository.toggleFavorite(songId)
        }
    }

    class Factory(
        private val songsRepository: SongsRepository,
        private val songId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SongDetailViewModel(songsRepository, songId) as T
        }
    }
}
