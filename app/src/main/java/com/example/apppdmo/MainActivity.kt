package com.example.apppdmo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.apppdmo.data.local.database.AppDatabase
import com.example.apppdmo.data.local.preferences.LastReadPreferences
import com.example.apppdmo.data.repository.BibleRepositoryImpl
import com.example.apppdmo.data.repository.CommunityRepositoryImpl
import com.example.apppdmo.data.repository.SongsRepositoryImpl
import com.example.apppdmo.navigation.AppNavigation
import com.example.apppdmo.notification.NotificationManager
import com.example.apppdmo.ui.theme.AppPDMOTheme

class MainActivity : ComponentActivity() {

    private lateinit var notificationManager: NotificationManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        notificationManager = NotificationManager(applicationContext)
        checkAndRequestNotificationPermission()

        val database = AppDatabase.getDatabase(applicationContext)
        val communityRepository = CommunityRepositoryImpl(
            database = database,
            dailyMessageDao = database.dailyMessageDao(),
            contentDao = database.contentDao()
        )

        val lastReadPreferences = LastReadPreferences(applicationContext)
        val bibleRepository = BibleRepositoryImpl(
            database = database,
            bibleDao = database.bibleDao(),
            lastReadPreferences = lastReadPreferences
        )

        val songsRepository = SongsRepositoryImpl(
            database = database,
            songDao = database.songDao()
        )

        setContent {
            AppPDMOTheme {
                AppNavigation(
                    communityRepository = communityRepository,
                    bibleRepository = bibleRepository,
                    songsRepository = songsRepository,
                    notificationManager = notificationManager
                )
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
