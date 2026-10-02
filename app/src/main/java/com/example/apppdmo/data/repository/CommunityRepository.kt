package com.example.apppdmo.data.repository

import android.content.Context
import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.local.entity.DailyMessageEntity
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    fun getLatestDailyMessage(): Flow<DailyMessageEntity?>
    fun getRecentContents(limit: Int = 3): Flow<List<ContentEntity>>
    fun getAllContents(): Flow<List<ContentEntity>>
    fun getContentById(id: Long): Flow<ContentEntity?>
    fun getContentsByType(type: String): Flow<List<ContentEntity>>
    fun searchContents(query: String, typeFilter: String? = null): Flow<List<ContentEntity>>
    fun getFavoriteContents(): Flow<List<ContentEntity>>
    fun getFavoriteContentIds(): Flow<Set<Long>>
    fun isFavoriteContent(contentId: Long): Flow<Boolean>
    suspend fun toggleFavoriteContent(contentId: Long)
    suspend fun ensureDemoDataSeeded(context: Context? = null)
}
