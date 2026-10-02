package com.example.apppdmo.ui.songs

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Leitor do detalhe do cântico. Todos os métodos são chamados na thread principal. */
@Suppress("DEPRECATION")
class SongAudioController(context: Context, private val url: String) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var prepared = false
    var loading by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var duration by mutableStateOf(0)
        private set
    var position by mutableStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change != AudioManager.AUDIOFOCUS_GAIN) pause()
    }
    private val timeout = Runnable { fail("O áudio demorou demasiado a responder. Verifique a ligação e tente novamente.") }

    fun play() {
        if (loading || playing) return
        error = null
        if (prepared) { startPrepared(); return }
        loading = true
        val media = MediaPlayer()
        player = media
        try {
            media.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            media.setOnPreparedListener {
                if (player === it) {
                    handler.removeCallbacks(timeout)
                    prepared = true; loading = false
                    duration = it.duration.coerceAtLeast(0)
                    startPrepared()
                }
            }
            media.setOnCompletionListener {
                if (player === it) {
                    playing = false; position = duration
                    audioManager.abandonAudioFocus(focusListener)
                }
            }
            media.setOnErrorListener { source, _, _ ->
                if (player === source) fail("Não foi possível reproduzir o áudio. Verifique a internet, o link e o formato do ficheiro.")
                true
            }
            media.setDataSource(url)
            media.prepareAsync()
            handler.postDelayed(timeout, 30000)
        } catch (_: Exception) { fail("Não foi possível abrir o áudio. Verifique o link e a ligação.") }
    }

    private fun startPrepared() {
        val media = player ?: return
        if (audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            error = "O áudio está ocupado por outra aplicação. Tente novamente."
            return
        }
        try {
            if (duration > 0 && position >= duration) { media.seekTo(0); position = 0 }
            media.start(); playing = true
        } catch (_: Exception) { fail("Falha ao iniciar o áudio. Tente novamente.") }
    }

    fun pause() {
        if (loading) { releasePlayer(); return }
        if (prepared && playing) {
            try { player?.pause(); position = player?.currentPosition ?: position }
            catch (_: IllegalStateException) { releasePlayer() }
        }
        playing = false
        audioManager.abandonAudioFocus(focusListener)
    }

    fun seek(milliseconds: Int) {
        if (!prepared) return
        try {
            position = milliseconds.coerceIn(0, duration)
            player?.seekTo(position)
        } catch (_: IllegalStateException) { fail("Não foi possível avançar no áudio.") }
    }

    fun refreshPosition() {
        if (prepared && playing) {
            try { position = (player?.currentPosition ?: 0).coerceIn(0, duration) }
            catch (_: IllegalStateException) { fail("A reprodução foi interrompida. Tente novamente.") }
        }
    }

    private fun fail(message: String) { releasePlayer(); error = message }

    private fun releasePlayer() {
        handler.removeCallbacks(timeout)
        val previous = player
        player = null
        previous?.release()
        prepared = false; loading = false; playing = false
        position = 0; duration = 0
        audioManager.abandonAudioFocus(focusListener)
    }

    fun close() { releasePlayer() }
}
