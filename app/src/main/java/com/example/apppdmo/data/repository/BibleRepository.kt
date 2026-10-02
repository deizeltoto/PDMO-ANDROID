package com.example.apppdmo.data.repository

import android.content.Context
import com.example.apppdmo.data.local.dao.SearchResultItem
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.BibleVerseEntity
import com.example.apppdmo.data.local.preferences.LastReadInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BibleRepository {
    fun getAllBooks(): Flow<List<BibleBookEntity>>
    fun getBooksByTestament(testament: String): Flow<List<BibleBookEntity>>
    fun getBookById(bookId: Int): Flow<BibleBookEntity?>
    fun getVersesByChapter(bookId: Int, chapter: Int): Flow<List<BibleVerseEntity>>
    fun searchVerses(query: String): Flow<List<SearchResultItem>>
    fun getFavoriteVerses(): Flow<List<SearchResultItem>>
    fun getFavoriteVerseIds(): Flow<Set<Int>>
    fun isFavorite(verseId: Int): Flow<Boolean>
    suspend fun toggleFavorite(verseId: Int)
    fun saveLastRead(bookId: Int, bookName: String, chapter: Int, verse: Int = 1)
    fun getLastRead(): StateFlow<LastReadInfo?>
    suspend fun ensureBibleDataSeeded(context: Context)
}
