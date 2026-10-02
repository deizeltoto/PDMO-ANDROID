package com.example.apppdmo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_contents")
data class FavoriteContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contentId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
