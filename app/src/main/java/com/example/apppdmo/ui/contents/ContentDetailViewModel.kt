package com.example.apppdmo.ui.contents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface ContentDetailUiState {
    data object Loading : ContentDetailUiState
    data class Success(
        val content: ContentEntity,
        val isFavorite: Boolean
    ) : ContentDetailUiState
    data class Error(val message: String) : ContentDetailUiState
}

class ContentDetailViewModel(
    private val repository: CommunityRepository,
    val contentId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContentDetailUiState>(ContentDetailUiState.Loading)
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            combine(
                repository.getContentById(contentId),
                repository.isFavoriteContent(contentId)
            ) { item, isFav ->
                if (item == null) {
                    ContentDetailUiState.Error("Publicação não encontrada.")
                } else {
                    ContentDetailUiState.Success(
                        content = item,
                        isFavorite = isFav
                    )
                }
            }
                .catch { throwable ->
                    emit(ContentDetailUiState.Error(throwable.localizedMessage ?: "Erro ao carregar publicação"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavoriteContent(contentId)
        }
    }

    class Factory(
        private val repository: CommunityRepository,
        private val contentId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContentDetailViewModel(repository, contentId) as T
        }
    }
}
