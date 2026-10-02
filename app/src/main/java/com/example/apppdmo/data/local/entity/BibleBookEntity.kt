package com.example.apppdmo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_books")
data class BibleBookEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val abbreviation: String,
    val testament: String, // Testament.OLD_TESTAMENT.name or Testament.NEW_TESTAMENT.name
    val bookOrder: Int,
    val chapterCount: Int
)
