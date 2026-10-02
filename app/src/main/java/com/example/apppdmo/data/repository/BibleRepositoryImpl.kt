package com.example.apppdmo.data.repository

import android.content.Context
import com.example.apppdmo.data.local.dao.BibleDao
import com.example.apppdmo.data.local.dao.SearchResultItem
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.BibleVerseEntity
import com.example.apppdmo.data.local.entity.FavoriteVerseEntity
import com.example.apppdmo.data.local.preferences.LastReadInfo
import com.example.apppdmo.data.local.preferences.LastReadPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BibleRepositoryImpl(
    private val database: com.example.apppdmo.data.local.database.AppDatabase,
    private val bibleDao: BibleDao,
    private val lastReadPreferences: LastReadPreferences
) : BibleRepository {

    override fun getAllBooks(): Flow<List<BibleBookEntity>> {
        return bibleDao.getAllBooks()
    }

    override fun getBooksByTestament(testament: String): Flow<List<BibleBookEntity>> {
        return bibleDao.getBooksByTestament(testament)
    }

    override fun getBookById(bookId: Int): Flow<BibleBookEntity?> {
        return bibleDao.getBookById(bookId)
    }

    override fun getVersesByChapter(bookId: Int, chapter: Int): Flow<List<BibleVerseEntity>> {
        return bibleDao.getVersesByChapter(bookId, chapter)
    }

    override fun searchVerses(query: String): Flow<List<SearchResultItem>> {
        if (query.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return bibleDao.searchVerses(query)
    }

    override fun getFavoriteVerses(): Flow<List<SearchResultItem>> {
        return bibleDao.getFavoriteVerses()
    }

    override fun getFavoriteVerseIds(): Flow<Set<Int>> {
        return bibleDao.getFavoriteVerseIds().map { list -> list.toSet() }
    }

    override fun isFavorite(verseId: Int): Flow<Boolean> {
        return bibleDao.isFavorite(verseId)
    }

    override suspend fun toggleFavorite(verseId: Int) {
        withContext(Dispatchers.IO) {
            val isFav = bibleDao.isFavorite(verseId).firstOrNull() ?: false
            if (isFav) {
                bibleDao.deleteFavorite(verseId)
            } else {
                bibleDao.insertFavorite(FavoriteVerseEntity(verseId = verseId))
            }
        }
    }

    override fun saveLastRead(bookId: Int, bookName: String, chapter: Int, verse: Int) {
        lastReadPreferences.saveLastRead(bookId, bookName, chapter, verse)
    }

    override fun getLastRead(): StateFlow<LastReadInfo?> {
        return lastReadPreferences.lastReadFlow
    }

    override suspend fun ensureBibleDataSeeded(context: Context) {
        withContext(Dispatchers.IO) {
          com.example.apppdmo.data.remote.SyncCoordinator.seed(database) {
            if (bibleDao.getBookCount() == 0) {
                try {
                    val jsonString = context.assets.open("bible.json").bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonString)
                    val booksArray = jsonObject.getJSONArray("books")

                    val booksToInsert = mutableListOf<BibleBookEntity>()
                    val versesToInsert = mutableListOf<BibleVerseEntity>()

                    for (i in 0 until booksArray.length()) {
                        val bookObj = booksArray.getJSONObject(i)
                        val bookId = bookObj.getInt("id")
                        val bookName = bookObj.getString("name")
                        val abbreviation = bookObj.getString("abbreviation")
                        val testament = bookObj.getString("testament")
                        val bookOrder = bookObj.getInt("bookOrder")
                        val chapterCount = bookObj.getInt("chapterCount")

                        booksToInsert.add(
                            BibleBookEntity(
                                id = bookId,
                                name = bookName,
                                abbreviation = abbreviation,
                                testament = testament,
                                bookOrder = bookOrder,
                                chapterCount = chapterCount
                            )
                        )

                        if (bookObj.has("chapters")) {
                            val chaptersArray = bookObj.getJSONArray("chapters")
                            for (j in 0 until chaptersArray.length()) {
                                val chapObj = chaptersArray.getJSONObject(j)
                                val chapterNum = chapObj.getInt("chapter")
                                val versesArray = chapObj.getJSONArray("verses")

                                for (k in 0 until versesArray.length()) {
                                    val verseObj = versesArray.getJSONObject(k)
                                    val verseNum = verseObj.getInt("verse")
                                    val text = verseObj.getString("text")

                                    versesToInsert.add(
                                        BibleVerseEntity(
                                            bookId = bookId,
                                            chapter = chapterNum,
                                            verse = verseNum,
                                            text = text
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (booksToInsert.isNotEmpty()) {
                        bibleDao.insertBooks(booksToInsert)
                    }
                    if (versesToInsert.isNotEmpty()) {
                        bibleDao.insertVerses(versesToInsert)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
          }
        }
    }
}
