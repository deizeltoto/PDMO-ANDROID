package com.example.apppdmo.data.repository

import android.content.Context
import com.example.apppdmo.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

interface SongsRepository {
    fun getAllSongs(): Flow<List<SongEntity>>
    fun getSongById(id: Int): Flow<SongEntity?>
    fun searchSongs(query: String): Flow<List<SongEntity>>
    fun getFavoriteSongs(): Flow<List<SongEntity>>
    fun getFavoriteSongIds(): Flow<Set<Int>>
    fun isFavorite(songId: Int): Flow<Boolean>
    suspend fun toggleFavorite(songId: Int)
    suspend fun ensureSongsDataSeeded(context: Context)
}
