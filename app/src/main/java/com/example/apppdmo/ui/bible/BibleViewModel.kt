package com.example.apppdmo.ui.bible

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apppdmo.data.local.dao.SearchResultItem
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.Testament
import com.example.apppdmo.data.local.preferences.LastReadInfo
import com.example.apppdmo.data.repository.BibleRepository
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

sealed interface BibleUiState {
    data object Loading : BibleUiState
    data class Success(
        val oldTestamentBooks: List<BibleBookEntity>,
        val newTestamentBooks: List<BibleBookEntity>,
        val lastRead: LastReadInfo?
    ) : BibleUiState
    data class Error(val message: String) : BibleUiState
}

class BibleViewModel(
    private val bibleRepository: BibleRepository,
    context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<BibleUiState>(BibleUiState.Loading)
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    val lastRead: StateFlow<LastReadInfo?> = bibleRepository.getLastRead()

    val favoriteVerses: StateFlow<List<SearchResultItem>> = bibleRepository.getFavoriteVerses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchResults: StateFlow<List<SearchResultItem>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            bibleRepository.searchVerses(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        initializeData(context)
    }

    fun initializeData(context: Context) {
        viewModelScope.launch {
            _uiState.value = BibleUiState.Loading
            try {
                bibleRepository.ensureBibleDataSeeded(context)
            } catch (_: Exception) {
            }

            combine(
                bibleRepository.getBooksByTestament(Testament.OLD_TESTAMENT.name),
                bibleRepository.getBooksByTestament(Testament.NEW_TESTAMENT.name),
                bibleRepository.getLastRead()
            ) { otBooks, ntBooks, lastReadInfo ->
                if (otBooks.isEmpty() && ntBooks.isEmpty()) {
                    BibleUiState.Error("Nenhum livro bíblico encontrado.")
                } else {
                    BibleUiState.Success(
                        oldTestamentBooks = otBooks,
                        newTestamentBooks = ntBooks,
                        lastRead = lastReadInfo
                    )
                }
            }
                .catch { throwable ->
                    emit(BibleUiState.Error(throwable.localizedMessage ?: "Erro ao carregar livros da Bíblia"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    class Factory(
        private val bibleRepository: BibleRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BibleViewModel(bibleRepository, context) as T
        }
    }
}
