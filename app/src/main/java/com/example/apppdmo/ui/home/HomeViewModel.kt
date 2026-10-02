package com.example.apppdmo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apppdmo.data.repository.CommunityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val repository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    repository.ensureDemoDataSeeded()
                }
            } catch (_: Exception) {
                // Ignore seed error if already present
            }

            combine(
                repository.getLatestDailyMessage(),
                repository.getRecentContents(3)
            ) { dailyMessage, recentContents ->
                HomeUiState.Success(
                    HomeData(
                        communityName = "Comunidade Cristã",
                        dailyMessage = dailyMessage,
                        recentContents = recentContents
                    )
                ) as HomeUiState
            }
                .catch { throwable ->
                    emit(HomeUiState.Error(throwable.localizedMessage ?: "Erro desconhecido ao carregar dados"))
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    class Factory(private val repository: CommunityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
