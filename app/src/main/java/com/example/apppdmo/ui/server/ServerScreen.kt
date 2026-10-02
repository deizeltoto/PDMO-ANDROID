package com.example.apppdmo.ui.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.apppdmo.data.local.database.AppDatabase
import com.example.apppdmo.data.remote.CatalogSyncRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val preferences = remember { context.getSharedPreferences("server_settings", 0) }
    val repository = remember { CatalogSyncRepository(AppDatabase.getDatabase(context)) }
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf(preferences.getString("url", "http://10.0.2.2:3000") ?: "") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var lastSync by remember { mutableStateOf<Pair<String, Long>?>(null) }
    LaunchedEffect(Unit) { lastSync = repository.lastSync() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, enabled = !busy) { Text("Voltar") }
        Text("Servidor e sincronização", style = MaterialTheme.typography.headlineSmall)
        Text("Receba os conteúdos publicados no painel. Após sincronizar, pode continuar a ler sem internet.")
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Endereço do servidor") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
        Text("Emulador: http://10.0.2.2:3000\nTelemóvel: use o IP do computador na mesma rede. HTTP local está disponível na versão de desenvolvimento.", style = MaterialTheme.typography.bodySmall)
        Text("A sincronização substitui o catálogo local pelo catálogo deste servidor. Favoritos dos registos existentes são mantidos.")
        Button(enabled = !busy, onClick = {
            busy = true; failed = false; message = "A sincronizar…"
            preferences.edit().putString("url", address.trim()).apply()
            scope.launch {
                try {
                    message = repository.synchronize(address)
                    lastSync = repository.lastSync()
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    failed = true
                    message = "Não foi possível sincronizar: ${error.localizedMessage ?: "erro de ligação"}. Os dados locais foram mantidos."
                } finally { busy = false }
            }
        }) { Text(if (busy) "A sincronizar…" else "Sincronizar agora") }
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (message.isNotBlank()) Text(message, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        lastSync?.let { (server, time) -> Text("Última sincronização: ${DateFormat.getDateTimeInstance().format(Date(time))}\n$server", style = MaterialTheme.typography.bodySmall) }
    }
}
