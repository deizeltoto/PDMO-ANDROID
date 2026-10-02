package com.example.apppdmo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apppdmo.data.local.entity.FavoriteSongEntity
import com.example.apppdmo.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY number ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    fun getSongById(id: Int): Flow<SongEntity?>

    @Query("""
        SELECT * FROM songs 
        WHERE CAST(number AS TEXT) LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%' 
           OR lyrics LIKE '%' || :query || '%'
        ORDER BY number ASC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE category = :category ORDER BY number ASC")
    fun getSongsByCategory(category: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN favorite_songs f ON s.id = f.songId
        ORDER BY s.number ASC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT songId FROM favorite_songs")
    fun getFavoriteSongIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    fun isFavorite(songId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE songId = :songId")
    suspend fun deleteFavorite(songId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
