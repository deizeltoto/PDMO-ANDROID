package com.example.apppdmo.data.repository

import android.content.Context
import com.example.apppdmo.data.local.dao.ContentDao
import com.example.apppdmo.data.local.dao.DailyMessageDao
import com.example.apppdmo.data.local.database.AppDatabase
import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.local.entity.DailyMessageEntity
import com.example.apppdmo.data.local.entity.FavoriteContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class CommunityRepositoryImpl(
    private val dailyMessageDao: DailyMessageDao,
    private val contentDao: ContentDao
) : CommunityRepository {

    override fun getLatestDailyMessage(): Flow<DailyMessageEntity?> {
        return dailyMessageDao.getLatestDailyMessage()
    }

    override fun getRecentContents(limit: Int): Flow<List<ContentEntity>> {
        return contentDao.getRecentContents(limit)
    }

    override fun getAllContents(): Flow<List<ContentEntity>> {
        return contentDao.getAllContents()
    }

    override fun getContentById(id: Long): Flow<ContentEntity?> {
        return contentDao.getContentById(id)
    }

    override fun getContentsByType(type: String): Flow<List<ContentEntity>> {
        return contentDao.getContentsByType(type)
    }

    override fun searchContents(query: String, typeFilter: String?): Flow<List<ContentEntity>> {
        val cleanQuery = query.trim()
        return if (typeFilter == null || typeFilter == "ALL") {
            if (cleanQuery.isEmpty()) {
                contentDao.getAllContents()
            } else {
                contentDao.searchContents(cleanQuery)
            }
        } else {
            if (cleanQuery.isEmpty()) {
                contentDao.getContentsByType(typeFilter)
            } else {
                contentDao.searchContentsByType(cleanQuery, typeFilter)
            }
        }
    }

    override fun getFavoriteContents(): Flow<List<ContentEntity>> {
        return contentDao.getFavoriteContents()
    }

    override fun getFavoriteContentIds(): Flow<Set<Long>> {
        return contentDao.getFavoriteContentIds().map { list -> list.toSet() }
    }

    override fun isFavoriteContent(contentId: Long): Flow<Boolean> {
        return contentDao.isFavoriteContent(contentId)
    }

    override suspend fun toggleFavoriteContent(contentId: Long) {
        withContext(Dispatchers.IO) {
            val isFav = contentDao.isFavoriteContent(contentId).firstOrNull() ?: false
            if (isFav) {
                contentDao.deleteFavoriteContent(contentId)
            } else {
                contentDao.insertFavoriteContent(FavoriteContentEntity(contentId = contentId))
            }
        }
    }

    override suspend fun ensureDemoDataSeeded(context: Context?) {
        withContext(Dispatchers.IO) {
            AppDatabase.populateDatabase(dailyMessageDao, contentDao)

            if (context != null && contentDao.getCount() <= 3) {
                try {
                    val jsonString = context.assets.open("contents.json").bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)

                    val contentsToInsert = mutableListOf<ContentEntity>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getLong("id")
                        val title = obj.getString("title")
                        val description = obj.getString("description")
                        val body = obj.getString("body")
                        val author = obj.getString("author")
                        val type = obj.getString("type")
                        val imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl")) obj.getString("imageUrl") else null

                        contentsToInsert.add(
                            ContentEntity(
                                id = id,
                                title = title,
                                description = description,
                                body = body,
                                author = author,
                                type = type,
                                imageUrl = imageUrl
                            )
                        )
                    }

                    if (contentsToInsert.isNotEmpty()) {
                        contentDao.insertAll(contentsToInsert)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
