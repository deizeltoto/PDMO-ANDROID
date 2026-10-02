package com.example.apppdmo

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.apppdmo.ui.songs.SongAudioController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class SongAudioTest {
    @Test fun preparesPlaysPausesSeeksAndReleases() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // WAV silencioso gerado no teste: não depende de música ou de um serviço externo.
        val audio = File.createTempFile("song-audio-test", ".wav", context.cacheDir)
        val samples = 8000 * 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples * 2); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(8000); putInt(16000)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(samples * 2)
        }
        audio.outputStream().use { it.write(header.array()); it.write(ByteArray(samples * 2)) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var controller: SongAudioController
                scenario.onActivity { activity ->
                    controller = SongAudioController(activity, audio.absolutePath)
                    controller.play()
                }
                try {
                    var ready = false
                    val deadline = System.currentTimeMillis() + 10000
                    while (!ready && System.currentTimeMillis() < deadline) {
                        scenario.onActivity { ready = controller.playing || controller.error != null }
                        if (!ready) Thread.sleep(50)
                    }
                    scenario.onActivity {
                        assertNull(controller.error)
                        assertTrue("O leitor deveria estar a reproduzir", controller.playing)
                        assertTrue(controller.duration >= 7900)
                        controller.pause()
                        assertFalse(controller.playing)
                        controller.seek(1500)
                        assertEquals(1500, controller.position)
                        controller.play()
                        assertTrue(controller.playing)
                        controller.close()
                        assertFalse(controller.playing)
                        assertEquals(0, controller.duration)
                    }
                } finally { scenario.onActivity { controller.close() } }
            }
        } finally { audio.delete() }
    }
}
