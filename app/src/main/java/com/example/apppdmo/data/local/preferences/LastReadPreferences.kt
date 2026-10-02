package com.example.apppdmo.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LastReadInfo(
    val bookId: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int
)

class LastReadPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lastReadFlow = MutableStateFlow<LastReadInfo?>(readLastReadFromPrefs())
    val lastReadFlow: StateFlow<LastReadInfo?> = _lastReadFlow.asStateFlow()

    fun saveLastRead(bookId: Int, bookName: String, chapter: Int, verse: Int = 1) {
        prefs.edit().apply {
            putInt(KEY_BOOK_ID, bookId)
            putString(KEY_BOOK_NAME, bookName)
            putInt(KEY_CHAPTER, chapter)
            putInt(KEY_VERSE, verse)
            apply()
        }
        _lastReadFlow.value = LastReadInfo(bookId, bookName, chapter, verse)
    }

    private fun readLastReadFromPrefs(): LastReadInfo? {
        val bookId = prefs.getInt(KEY_BOOK_ID, -1)
        val bookName = prefs.getString(KEY_BOOK_NAME, null)
        val chapter = prefs.getInt(KEY_CHAPTER, -1)
        val verse = prefs.getInt(KEY_VERSE, 1)

        return if (bookId != -1 && !bookName.isNullOrEmpty() && chapter != -1) {
            LastReadInfo(bookId, bookName, chapter, verse)
        } else {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "pdmo_bible_last_read_prefs"
        private const val KEY_BOOK_ID = "key_book_id"
        private const val KEY_BOOK_NAME = "key_book_name"
        private const val KEY_CHAPTER = "key_chapter"
        private const val KEY_VERSE = "key_verse"
    }
}
