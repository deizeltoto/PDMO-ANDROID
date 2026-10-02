package com.example.apppdmo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Início",
        icon = Icons.Default.Home
    )

    data object Bible : BottomNavItem(
        route = Screen.Bible.route,
        title = "Bíblia",
        icon = Icons.AutoMirrored.Filled.MenuBook
    )

    data object Songs : BottomNavItem(
        route = Screen.Songs.route,
        title = "Cânticos",
        icon = Icons.Default.MusicNote
    )

    data object Contents : BottomNavItem(
        route = Screen.Contents.route,
        title = "Conteúdos",
        icon = Icons.Default.Book
    )

    companion object {
        val items = listOf(Home, Bible, Songs, Contents)
    }
}
