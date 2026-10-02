package com.example.apppdmo.ui.home

import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.local.entity.DailyMessageEntity

data class HomeData(
    val communityName: String = "Comunidade Cristã",
    val dailyMessage: DailyMessageEntity? = null,
    val recentContents: List<ContentEntity> = emptyList()
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
