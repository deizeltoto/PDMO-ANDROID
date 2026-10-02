package com.example.apppdmo

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apppdmo.data.local.database.AppDatabase
import com.example.apppdmo.data.local.entity.*
import com.example.apppdmo.data.remote.CatalogSyncRepository
import com.example.apppdmo.data.remote.SyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class CatalogSyncTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CatalogSyncRepository

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
        repository = CatalogSyncRepository(database)
    }

    @After fun cleanup() { database.close() }

    private fun snapshot() = JSONObject("""
        {"schemaVersion":1,"revision":1,
         "contents":[{"id":1,"title":"Publicado","description":"Descrição","body":"Texto","author":"Autor","type":"ESTUDO","imageUrl":null,"createdAt":1000}],
         "songs":[{"id":1,"number":1,"title":"Cântico","category":"Louvor","lyrics":"Letra","author":null}],
         "dailyMessages":[{"id":1,"message":"Mensagem","bibleReference":"Gn 1:1","date":"2026-10-02"}],
         "books":[{"id":1,"name":"Génesis","abbreviation":"Gn","testament":"OLD_TESTAMENT","bookOrder":1,"chapterCount":50}],
         "verses":[{"id":77,"bookId":1,"chapter":1,"verse":1,"text":"Texto bíblico"}]}
    """.trimIndent())

    private suspend fun sync(payload: String): String {
        val server = ServerSocket(0)
        server.soTimeout = 15000
        val worker = thread(isDaemon = true) {
            server.accept().use { socket ->
                val reader = socket.getInputStream().bufferedReader()
                while (true) { val line = reader.readLine(); if (line.isNullOrEmpty()) break }
                val body = payload.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    write(body); flush()
                }
            }
        }
        try { return repository.synchronize("http://127.0.0.1:${server.localPort}") }
        finally { server.close(); worker.join(1000) }
    }

    @Test fun replacesCatalogAndPreservesExistingFavorites() = runBlocking {
        database.contentDao().insertAll(listOf(
            ContentEntity(1, "Antigo", "Descrição", "Texto", "Autor", "ESTUDO"),
            ContentEntity(2, "Eliminado", "Descrição", "Texto", "Autor", "ARTIGO")
        ))
        database.contentDao().insertFavoriteContent(FavoriteContentEntity(contentId = 1))
        database.contentDao().insertFavoriteContent(FavoriteContentEntity(contentId = 2))
        database.bibleDao().insertBooks(listOf(BibleBookEntity(1, "Génesis", "Gn", "OLD_TESTAMENT", 1, 50)))
        database.bibleDao().insertVerses(listOf(BibleVerseEntity(5, 1, 1, 1, "Antigo")))
        database.bibleDao().insertFavorite(FavoriteVerseEntity(verseId = 5))
        sync(snapshot().toString())
        assertEquals("Publicado", database.contentDao().getContentById(1).first()?.title)
        assertNull(database.contentDao().getContentById(2).first())
        assertEquals(listOf(1L), database.contentDao().getFavoriteContentIds().first())
        assertEquals(listOf(77), database.bibleDao().getFavoriteVerseIds().first())
        assertNotNull(repository.lastSync())
    }

    @Test fun emptyRemoteCatalogStaysEmptyAndDoesNotReseed() = runBlocking {
        sync(snapshot().toString())
        val empty = snapshot()
        for (key in listOf("contents", "songs", "dailyMessages", "books", "verses")) empty.put(key, org.json.JSONArray())
        sync(empty.toString())
        var reseeded = false
        withContext(Dispatchers.IO) { SyncCoordinator.seed(database) { reseeded = true } }
        assertFalse(reseeded)
        assertEquals(0, database.contentDao().getCount())
        assertEquals(0, database.bibleDao().getBookCount())
        assertEquals(0, database.songDao().getSongCount())
    }

    @Test fun syncsAudioWithLyricsAndAcceptsOlderServers() = runBlocking {
        val withAudio = snapshot().apply {
            getJSONArray("songs").getJSONObject(0).put("audioUrl", "https://example.com/cantico.mp3")
        }
        sync(withAudio.toString())
        assertEquals("https://example.com/cantico.mp3", database.songDao().getSongById(1).first()?.audioUrl)
        assertEquals("Letra", database.songDao().getSongById(1).first()?.lyrics)
        val invalid = snapshot().apply {
            getJSONArray("songs").getJSONObject(0).put("audioUrl", "https://user:password@example.com/audio.mp3")
        }
        try { sync(invalid.toString()); fail("URL com credenciais foi aceite") } catch (_: IllegalArgumentException) { }
        assertEquals("https://example.com/cantico.mp3", database.songDao().getSongById(1).first()?.audioUrl)
        sync(snapshot().toString())
        assertNull(database.songDao().getSongById(1).first()?.audioUrl)
    }

    @Test fun invalidPayloadAndFailedTransactionKeepPreviousCatalog() = runBlocking {
        sync(snapshot().toString())
        val originalState = repository.lastSync()
        val invalid = snapshot().apply { getJSONArray("verses").getJSONObject(0).put("bookId", 999) }
        try { sync(invalid.toString()); fail("Uma referência inválida foi aceite") } catch (_: IllegalArgumentException) { }
        assertEquals("Publicado", database.contentDao().getContentById(1).first()?.title)
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_insert BEFORE INSERT ON contents BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        }
        try { sync(snapshot().toString()); fail("A transacção deveria falhar") } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals("Publicado", database.contentDao().getContentById(1).first()?.title)
        assertEquals(originalState, repository.lastSync())
    }
}
