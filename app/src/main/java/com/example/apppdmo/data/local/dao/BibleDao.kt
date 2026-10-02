package com.example.apppdmo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.BibleVerseEntity
import com.example.apppdmo.data.local.entity.FavoriteVerseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {

    @Query("SELECT * FROM bible_books ORDER BY bookOrder ASC")
    fun getAllBooks(): Flow<List<BibleBookEntity>>

    @Query("SELECT * FROM bible_books WHERE id = :bookId LIMIT 1")
    fun getBookById(bookId: Int): Flow<BibleBookEntity?>

    @Query("SELECT * FROM bible_books WHERE testament = :testament ORDER BY bookOrder ASC")
    fun getBooksByTestament(testament: String): Flow<List<BibleBookEntity>>

    @Query("SELECT * FROM bible_verses WHERE bookId = :bookId AND chapter = :chapter ORDER BY verse ASC")
    fun getVersesByChapter(bookId: Int, chapter: Int): Flow<List<BibleVerseEntity>>

    @Query("""
        SELECT v.id AS id, v.bookId AS bookId, v.chapter AS chapter, v.verse AS verse, v.text AS text,
               b.name AS bookName, b.abbreviation AS bookAbbreviation
        FROM bible_verses v
        INNER JOIN bible_books b ON v.bookId = b.id
        WHERE v.text LIKE '%' || :query || '%'
        ORDER BY b.bookOrder ASC, v.chapter ASC, v.verse ASC
    """)
    fun searchVerses(query: String): Flow<List<SearchResultItem>>

    @Query("""
        SELECT v.id AS id, v.bookId AS bookId, v.chapter AS chapter, v.verse AS verse, v.text AS text,
               b.name AS bookName, b.abbreviation AS bookAbbreviation
        FROM favorite_verses f
        INNER JOIN bible_verses v ON f.verseId = v.id
        INNER JOIN bible_books b ON v.bookId = b.id
        ORDER BY f.createdAt DESC
    """)
    fun getFavoriteVerses(): Flow<List<SearchResultItem>>

    @Query("SELECT verseId FROM favorite_verses")
    fun getFavoriteVerseIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_verses WHERE verseId = :verseId)")
    fun isFavorite(verseId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteVerseEntity)

    @Query("DELETE FROM favorite_verses WHERE verseId = :verseId")
    suspend fun deleteFavorite(verseId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BibleBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerseEntity>)

    @Query("SELECT COUNT(*) FROM bible_books")
    suspend fun getBookCount(): Int
}
