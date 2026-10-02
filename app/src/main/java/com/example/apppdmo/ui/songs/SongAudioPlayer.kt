package com.example.apppdmo.ui.songs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SongAudioPlayer(audioUrl: String?, songId: Int, modifier: Modifier = Modifier) {
    if (audioUrl.isNullOrBlank()) {
        return
    }
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(audioUrl, songId) { SongAudioController(context, audioUrl) }
    var seekPosition by remember(audioUrl, songId) { mutableStateOf<Float?>(null) }
    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) controller.pause()
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) controller.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
            controller.close()
        }
    }
    LaunchedEffect(controller, controller.playing) {
        while (controller.playing) { controller.refreshPosition(); delay(400) }
    }
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledIconButton(onClick = {
                    if (controller.playing || controller.loading) controller.pause() else controller.play()
                }) {
                    Icon(if (controller.playing || controller.loading) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (controller.loading) "Cancelar carregamento" else if (controller.playing) "Pausar áudio" else "Reproduzir áudio")
                }
                Column(Modifier.weight(1f)) {
                    Text(if (controller.loading) "A carregar áudio…" else "Ouvir cântico", style = MaterialTheme.typography.titleSmall)
                    Text("A letra está disponível abaixo", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (controller.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (controller.duration > 0) {
                Slider(value = seekPosition ?: controller.position.toFloat(),
                    onValueChange = { seekPosition = it },
                    onValueChangeFinished = { seekPosition?.let { controller.seek(it.toInt()) }; seekPosition = null },
                    valueRange = 0f..controller.duration.toFloat())
                Text("${audioTime((seekPosition ?: controller.position.toFloat()).toInt())} / ${audioTime(controller.duration)}", style = MaterialTheme.typography.bodySmall)
            }
            controller.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun audioTime(milliseconds: Int): String = String.format(Locale.getDefault(), "%d:%02d", milliseconds / 60000, (milliseconds / 1000) % 60)
