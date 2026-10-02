package com.example.apppdmo.data.remote

import androidx.room.withTransaction
import com.example.apppdmo.data.local.database.AppDatabase
import com.example.apppdmo.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

class CatalogSyncRepository(private val database: AppDatabase) {
    suspend fun synchronize(address: String): String = withContext(Dispatchers.IO) {
        val base = address.trim().trimEnd('/')
        val url = URL(base)
        require(url.protocol in listOf("http", "https") && url.host.isNotBlank() && url.userInfo == null && url.query == null && url.ref == null && url.path.isEmpty()) {
            "Informe o endereço do servidor, por exemplo http://10.0.2.2:3000"
        }
        val connection = URL("$base/api/sync").openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        val payload = try {
            check(connection.responseCode == 200) { "O servidor respondeu com HTTP ${connection.responseCode}." }
            // Limita o tamanho antes de alterar a base local.
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    check(output.size() + count <= 32 * 1024 * 1024) { "Catálogo demasiado grande (máximo 32 MB)." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            JSONObject(String(bytes, Charsets.UTF_8))
        } finally { connection.disconnect() }
        require(payload.getInt("schemaVersion") == 1) { "Versão de API incompatível." }
        val contents = payload.getJSONArray("contents").objects().map {
            ContentEntity(it.positiveId().toLong(), it.getString("title"), it.getString("description"), it.getString("body"), it.getString("author"), it.getString("type"), it.nullableString("imageUrl"), it.getLong("createdAt"))
        }
        val songs = payload.getJSONArray("songs").objects().map {
            SongEntity(it.positiveId(), it.getInt("number"), it.getString("title"), it.getString("category"), it.getString("lyrics"), it.nullableString("author"))
        }
        val messages = payload.getJSONArray("dailyMessages").objects().map {
            DailyMessageEntity(it.positiveId().toLong(), it.getString("message"), it.getString("bibleReference"), it.getString("date"))
        }
        val books = payload.getJSONArray("books").objects().map {
            BibleBookEntity(it.positiveId(), it.getString("name"), it.getString("abbreviation"), it.getString("testament"), it.getInt("bookOrder"), it.getInt("chapterCount"))
        }
        val verses = payload.getJSONArray("verses").objects().map {
            BibleVerseEntity(it.positiveId(), it.getInt("bookId"), it.getInt("chapter"), it.getInt("verse"), it.getString("text"))
        }
        for (ids in listOf(contents.map { it.id }, songs.map { it.id }, messages.map { it.id }, books.map { it.id }, verses.map { it.id })) {
            require(ids.distinct().size == ids.size) { "O catálogo contém IDs duplicados." }
        }
        val bookById = books.associateBy { it.id }
        require(books.all { it.chapterCount > 0 && it.bookOrder > 0 && it.testament in listOf("OLD_TESTAMENT", "NEW_TESTAMENT") }) { "Livros inválidos no catálogo." }
        require(verses.all { it.verse > 0 && it.chapter in 1..(bookById[it.bookId]?.chapterCount ?: 0) }) { "Referências bíblicas inválidas." }
        require(verses.map { Triple(it.bookId, it.chapter, it.verse) }.distinct().size == verses.size) { "Referências bíblicas duplicadas." }
        require(contents.all { it.type in listOf("ESTUDO", "Pregação", "ARTIGO") && it.createdAt > 0 }) { "Conteúdos inválidos." }
        require(songs.all { it.number > 0 } && songs.map { it.number }.distinct().size == songs.size) { "Números de cânticos inválidos." }
        SyncCoordinator.mutex.withLock {
            database.withTransaction {
                val sql = database.openHelper.writableDatabase
                // Remapeia favoritos bíblicos pela referência, inclusive na primeira sincronização.
                val verseFavorites = mutableListOf<Triple<Int, Int, Int>>()
                sql.query("SELECT v.bookId, v.chapter, v.verse FROM favorite_verses f JOIN bible_verses v ON v.id=f.verseId").use { cursor ->
                    while (cursor.moveToNext()) verseFavorites.add(Triple(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2)))
                }
                for (table in listOf("contents", "songs", "daily_messages", "bible_verses", "bible_books", "favorite_verses")) sql.execSQL("DELETE FROM $table")
                database.contentDao().insertAll(contents)
                database.songDao().insertSongs(songs)
                for (message in messages) database.dailyMessageDao().insert(message)
                database.bibleDao().insertBooks(books)
                database.bibleDao().insertVerses(verses)
                val favoriteReferences = verseFavorites.toSet()
                for (verse in verses) if (Triple(verse.bookId, verse.chapter, verse.verse) in favoriteReferences) database.bibleDao().insertFavorite(FavoriteVerseEntity(verseId = verse.id))
                sql.execSQL("DELETE FROM favorite_contents WHERE contentId NOT IN (SELECT id FROM contents)")
                sql.execSQL("DELETE FROM favorite_songs WHERE songId NOT IN (SELECT id FROM songs)")
                sql.execSQL("INSERT OR REPLACE INTO sync_state(id, serverUrl, syncedAt) VALUES (1, ?, ?)", arrayOf(base, System.currentTimeMillis()))
            }
        }
        "Sincronizado: ${contents.size} conteúdos, ${songs.size} cânticos, ${messages.size} mensagens, ${books.size} livros e ${verses.size} versículos."
    }

    suspend fun lastSync(): Pair<String, Long>? = withContext(Dispatchers.IO) {
        database.query("SELECT serverUrl, syncedAt FROM sync_state WHERE id = 1", null).use {
            if (it.moveToFirst()) it.getString(0) to it.getLong(1) else null
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.positiveId(): Int {
        val id = getLong("id")
        require(id in 1..Int.MAX_VALUE.toLong()) { "ID fora do intervalo suportado." }
        return id.toInt()
    }
}
