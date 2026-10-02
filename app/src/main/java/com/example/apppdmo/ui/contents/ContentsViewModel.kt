package com.example.apppdmo.ui.contents

import android.content.Context
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

sealed interface ContentsUiState {
    data object Loading : ContentsUiState
    data class Success(
        val contents: List<ContentEntity>,
        val favoriteContentIds: Set<Long>,
        val activeFilter: String
    ) : ContentsUiState
    data class Empty(val message: String, val activeFilter: String) : ContentsUiState
    data class Error(val message: String) : ContentsUiState
}

class ContentsViewModel(
    private val repository: CommunityRepository,
    context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL") // ALL, ESTUDO, Pregação, ARTICLE, FAVORITES
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _uiState = MutableStateFlow<ContentsUiState>(ContentsUiState.Loading)
    val uiState: StateFlow<ContentsUiState> = _uiState.asStateFlow()

    init {
        initializeData(context)
    }

    fun initializeData(context: Context) {
        viewModelScope.launch {
            _uiState.value = ContentsUiState.Loading
            try {
                repository.ensureDemoDataSeeded(context)
            } catch (_: Exception) {
            }

            val contentsFlow = combine(_searchQuery.debounce(200), _selectedFilter) { query, filter ->
                Pair(query, filter)
            }.flatMapLatest { (query, filter) ->
                if (filter == "FAVORITES") {
                    repository.getFavoriteContents()
                } else {
                    val filterType = if (filter == "ALL") null else filter
                    repository.searchContents(query, filterType)
                }
            }

            combine(
                contentsFlow,
                repository.getFavoriteContentIds(),
                _selectedFilter
            ) { contentsList, favIds, filter ->
                val filteredList = if (filter == "FAVORITES" && _searchQuery.value.isNotBlank()) {
                    contentsList.filter {
                        it.title.contains(_searchQuery.value, ignoreCase = true) ||
                                it.description.contains(_searchQuery.value, ignoreCase = true) ||
                                it.author.contains(_searchQuery.value, ignoreCase = true) ||
                                it.body.contains(_searchQuery.value, ignoreCase = true)
                    }
                } else {
                    contentsList
                }

                if (filteredList.isEmpty()) {
                    val emptyMsg = if (filter == "FAVORITES") {
                        "Ainda não guardou nenhum conteúdo como favorito."
                    } else {
                        "Nenhum conteúdo encontrado para os filtros selecionados."
                    }
                    ContentsUiState.Empty(emptyMsg, filter)
                } else {
                    ContentsUiState.Success(
                        contents = filteredList,
                        favoriteContentIds = favIds,
                        activeFilter = filter
                    )
                }
            }
                .catch { throwable ->
                    emit(ContentsUiState.Error(throwable.localizedMessage ?: "Erro ao carregar conteúdos"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filterKey: String) {
        _selectedFilter.value = filterKey
    }

    fun toggleFavorite(contentId: Long) {
        viewModelScope.launch {
            repository.toggleFavoriteContent(contentId)
        }
    }

    class Factory(
        private val repository: CommunityRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContentsViewModel(repository, context) as T
        }
    }
}
