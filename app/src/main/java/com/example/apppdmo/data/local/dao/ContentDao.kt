package com.example.apppdmo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.local.entity.FavoriteContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM contents ORDER BY createdAt DESC")
    fun getAllContents(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM contents WHERE id = :id LIMIT 1")
    fun getContentById(id: Long): Flow<ContentEntity?>

    @Query("SELECT * FROM contents WHERE type = :type ORDER BY createdAt DESC")
    fun getContentsByType(type: String): Flow<List<ContentEntity>>

    @Query("SELECT * FROM contents ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentContents(limit: Int = 3): Flow<List<ContentEntity>>

    @Query("""
        SELECT * FROM contents 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
           OR author LIKE '%' || :query || '%' 
           OR body LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchContents(query: String): Flow<List<ContentEntity>>

    @Query("""
        SELECT * FROM contents 
        WHERE type = :type 
          AND (title LIKE '%' || :query || '%' 
               OR description LIKE '%' || :query || '%' 
               OR author LIKE '%' || :query || '%' 
               OR body LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchContentsByType(query: String, type: String): Flow<List<ContentEntity>>

    @Query("""
        SELECT c.* FROM contents c
        INNER JOIN favorite_contents f ON c.id = f.contentId
        ORDER BY f.createdAt DESC
    """)
    fun getFavoriteContents(): Flow<List<ContentEntity>>

    @Query("SELECT contentId FROM favorite_contents")
    fun getFavoriteContentIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_contents WHERE contentId = :contentId)")
    fun isFavoriteContent(contentId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteContent(favorite: FavoriteContentEntity)

    @Query("DELETE FROM favorite_contents WHERE contentId = :contentId")
    suspend fun deleteFavoriteContent(contentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<ContentEntity>)

    @Query("SELECT COUNT(*) FROM contents")
    suspend fun getCount(): Int
}
