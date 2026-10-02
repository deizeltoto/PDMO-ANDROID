package com.example.apppdmo.data.remote

import com.example.apppdmo.data.local.database.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializa a carga de exemplos e a sincronização para não misturar os catálogos. */
object SyncCoordinator {
    val mutex = Mutex()

    suspend fun seed(database: AppDatabase, action: suspend () -> Unit) {
        mutex.withLock {
            val synced = database.query("SELECT COUNT(*) FROM sync_state", null).use {
                it.moveToFirst() && it.getInt(0) > 0
            }
            if (!synced) action()
        }
    }
}
