package com.example.apppdmo.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Bible : Screen("bible")
    data object Songs : Screen("songs")
    data object Contents : Screen("contents")

    data object BibleBook : Screen("bible/book/{bookId}") {
        fun createRoute(bookId: Int) = "bible/book/$bookId"
    }

    data object BibleReader : Screen("bible/read/{bookId}/{chapter}") {
        fun createRoute(bookId: Int, chapter: Int) = "bible/read/$bookId/$chapter"
    }

    data object BibleSearch : Screen("bible/search")
    data object BibleFavorites : Screen("bible/favorites")

    data object SongDetail : Screen("songs/{songId}") {
        fun createRoute(songId: Int) = "songs/$songId"
    }

    data object ContentDetail : Screen("content/{contentId}") {
        fun createRoute(contentId: Long) = "content/$contentId"
    }

    data object About : Screen("about")
}
