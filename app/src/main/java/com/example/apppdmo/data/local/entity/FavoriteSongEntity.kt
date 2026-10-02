package com.example.apppdmo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int,
    val createdAt: Long = System.currentTimeMillis()
)
