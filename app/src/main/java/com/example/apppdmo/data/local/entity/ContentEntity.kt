package com.example.apppdmo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contents")
data class ContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val body: String,
    val author: String,
    val type: String, // ESTUDO, Pregação, ARTIGO
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
