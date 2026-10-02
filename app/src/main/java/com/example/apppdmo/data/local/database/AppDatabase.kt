package com.example.apppdmo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.apppdmo.data.local.entity.SyncStateEntity
import com.example.apppdmo.data.local.dao.BibleDao
import com.example.apppdmo.data.local.dao.ContentDao
import com.example.apppdmo.data.local.dao.DailyMessageDao
import com.example.apppdmo.data.local.dao.SongDao
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.BibleVerseEntity
import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.local.entity.DailyMessageEntity
import com.example.apppdmo.data.local.entity.FavoriteContentEntity
import com.example.apppdmo.data.local.entity.FavoriteSongEntity
import com.example.apppdmo.data.local.entity.FavoriteVerseEntity
import com.example.apppdmo.data.local.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [
        ContentEntity::class,
        DailyMessageEntity::class,
        BibleBookEntity::class,
        BibleVerseEntity::class,
        FavoriteVerseEntity::class,
        SongEntity::class,
        FavoriteSongEntity::class,
        FavoriteContentEntity::class,
        SyncStateEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao
    abstract fun dailyMessageDao(): DailyMessageDao
    abstract fun bibleDao(): BibleDao
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "app_pdmo_database"
                )
                    .addMigrations(object : Migration(4, 5) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS sync_state (id INTEGER NOT NULL PRIMARY KEY, serverUrl TEXT NOT NULL, syncedAt INTEGER NOT NULL)")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(dailyMessageDao: DailyMessageDao, contentDao: ContentDao) {
            withContext(Dispatchers.IO) {
                if (dailyMessageDao.getCount() == 0) {
                    dailyMessageDao.insert(
                        DailyMessageEntity(
                            message = "Entrega o teu caminho ao Senhor, confia nele, e ele tudo fará.",
                            bibleReference = "Salmos 37:5",
                            date = "Mensagem do dia"
                        )
                    )
                }

                if (contentDao.getCount() == 0) {
                    val demoContents = listOf(
                        ContentEntity(
                            title = "Estudo sobre a Fé e Perseverança",
                            description = "Um estudo aprofundado sobre como manter a fé fundamentada nas Escrituras em momentos de provação.",
                            body = "A fé é a certeza das coisas que se esperam e a convicção de fatos que não se veem. Neste estudo analisamos Hebreus 11...",
                            author = "Pr. João Silva",
                            type = "ESTUDO"
                        ),
                        ContentEntity(
                            title = "A Importância da Oração em Comunidade",
                            description = "Pregação edificante sobre o impacto espiritual do clamor e união dos irmãos.",
                            body = "Quando a igreja se reúne em oração, corações são transformados e correntes quebradas...",
                            author = "Pr. Mateus Costa",
                            type = "Pregação"
                        ),
                        ContentEntity(
                            title = "Viver em União e Amor Fraterno",
                            description = "Artigo reflexivo sobre a essência do mandamento do amor e da comunhão cristã.",
                            body = "Oh! Quão bom e quão suave é que os irmãos vivam em união! Este artigo aborda a prática diária do amor...",
                            author = "Diác. Ana Maria",
                            type = "ARTIGO"
                        )
                    )
                    contentDao.insertAll(demoContents)
                }
            }
        }
    }
}
