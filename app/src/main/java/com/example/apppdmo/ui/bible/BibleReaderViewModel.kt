package com.example.apppdmo.ui.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.BibleVerseEntity
import com.example.apppdmo.data.repository.BibleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface BibleReaderUiState {
    data object Loading : BibleReaderUiState
    data class Success(
        val book: BibleBookEntity,
        val chapter: Int,
        val verses: List<BibleVerseEntity>,
        val favoriteVerseIds: Set<Int>
    ) : BibleReaderUiState
    data class Error(val message: String) : BibleReaderUiState
}

class BibleReaderViewModel(
    private val bibleRepository: BibleRepository,
    val bookId: Int,
    initialChapter: Int
) : ViewModel() {

    private val _currentChapter = MutableStateFlow(initialChapter)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    private val _uiState = MutableStateFlow<BibleReaderUiState>(BibleReaderUiState.Loading)
    val uiState: StateFlow<BibleReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapterData()
    }

    private fun loadChapterData() {
        viewModelScope.launch {
            _uiState.value = BibleReaderUiState.Loading

            combine(
                bibleRepository.getBookById(bookId),
                _currentChapter,
                bibleRepository.getFavoriteVerseIds()
            ) { book, chapter, favIds ->
                if (book == null) {
                    BibleReaderUiState.Error("Livro não encontrado")
                } else {
                    bibleRepository.saveLastRead(book.id, book.name, chapter)
                    Triple(book, chapter, favIds)
                }
            }
                .catch { throwable ->
                    _uiState.value = BibleReaderUiState.Error(throwable.localizedMessage ?: "Erro ao carregar capítulo")
                }
                .collect { triple ->
                    if (triple is Triple<*, *, *>) {
                        val book = triple.first as BibleBookEntity
                        val chapter = triple.second as Int
                        @Suppress("UNCHECKED_CAST")
                        val favIds = triple.third as Set<Int>

                        viewModelScope.launch {
                            bibleRepository.getVersesByChapter(book.id, chapter)
                                .catch { t ->
                                    _uiState.value = BibleReaderUiState.Error(t.localizedMessage ?: "Erro ao carregar versículos")
                                }
                                .collect { verses ->
                                    _uiState.value = BibleReaderUiState.Success(
                                        book = book,
                                        chapter = chapter,
                                        verses = verses,
                                        favoriteVerseIds = favIds
                                    )
                                }
                        }
                    }
                }
        }
    }

    fun toggleFavorite(verseId: Int) {
        viewModelScope.launch {
            bibleRepository.toggleFavorite(verseId)
        }
    }

    fun goToNextChapter(chapterCount: Int) {
        if (_currentChapter.value < chapterCount) {
            _currentChapter.value += 1
        }
    }

    fun goToPreviousChapter() {
        if (_currentChapter.value > 1) {
            _currentChapter.value -= 1
        }
    }

    class Factory(
        private val bibleRepository: BibleRepository,
        private val bookId: Int,
        private val initialChapter: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BibleReaderViewModel(bibleRepository, bookId, initialChapter) as T
        }
    }
}
