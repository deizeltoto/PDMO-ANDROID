package com.example.apppdmo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apppdmo.data.local.entity.DailyMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyMessageDao {
    @Query("SELECT * FROM daily_messages ORDER BY id DESC LIMIT 1")
    fun getLatestDailyMessage(): Flow<DailyMessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyMessage: DailyMessageEntity)

    @Query("SELECT COUNT(*) FROM daily_messages")
    suspend fun getCount(): Int
}
