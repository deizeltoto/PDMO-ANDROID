package com.example.apppdmo.data.repository

import android.content.Context
import com.example.apppdmo.data.local.dao.SongDao
import com.example.apppdmo.data.local.entity.FavoriteSongEntity
import com.example.apppdmo.data.local.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class SongsRepositoryImpl(
    private val database: com.example.apppdmo.data.local.database.AppDatabase,
    private val songDao: SongDao
) : SongsRepository {

    override fun getAllSongs(): Flow<List<SongEntity>> {
        return songDao.getAllSongs()
    }

    override fun getSongById(id: Int): Flow<SongEntity?> {
        return songDao.getSongById(id)
    }

    override fun searchSongs(query: String): Flow<List<SongEntity>> {
        if (query.isBlank()) return songDao.getAllSongs()
        return songDao.searchSongs(query)
    }

    override fun getFavoriteSongs(): Flow<List<SongEntity>> {
        return songDao.getFavoriteSongs()
    }

    override fun getFavoriteSongIds(): Flow<Set<Int>> {
        return songDao.getFavoriteSongIds().map { list -> list.toSet() }
    }

    override fun isFavorite(songId: Int): Flow<Boolean> {
        return songDao.isFavorite(songId)
    }

    override suspend fun toggleFavorite(songId: Int) {
        withContext(Dispatchers.IO) {
            val isFav = songDao.isFavorite(songId).firstOrNull() ?: false
            if (isFav) {
                songDao.deleteFavorite(songId)
            } else {
                songDao.insertFavorite(FavoriteSongEntity(songId = songId))
            }
        }
    }

    override suspend fun ensureSongsDataSeeded(context: Context) {
        withContext(Dispatchers.IO) {
          com.example.apppdmo.data.remote.SyncCoordinator.seed(database) {
            if (songDao.getSongCount() == 0) {
                try {
                    val jsonString = context.assets.open("songs.json").bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)

                    val songsToInsert = mutableListOf<SongEntity>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getInt("id")
                        val number = obj.getInt("number")
                        val title = obj.getString("title")
                        val category = obj.getString("category")
                        val lyrics = obj.getString("lyrics")
                        val author = if (obj.has("author")) obj.getString("author") else null

                        songsToInsert.add(
                            SongEntity(
                                id = id,
                                number = number,
                                title = title,
                                category = category,
                                lyrics = lyrics,
                                author = author
                            )
                        )
                    }

                    if (songsToInsert.isNotEmpty()) {
                        songDao.insertSongs(songsToInsert)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
          }
        }
    }
}
