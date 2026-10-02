package com.example.apppdmo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Int,
    val number: Int,
    val title: String,
    val category: String,
    val lyrics: String,
    val author: String? = null,
    val audioUrl: String? = null
)
