package com.example.apppdmo.data.local.dao

import androidx.room.Embedded
import com.example.apppdmo.data.local.entity.BibleVerseEntity

data class SearchResultItem(
    @Embedded val verse: BibleVerseEntity,
    val bookName: String,
    val bookAbbreviation: String
)
