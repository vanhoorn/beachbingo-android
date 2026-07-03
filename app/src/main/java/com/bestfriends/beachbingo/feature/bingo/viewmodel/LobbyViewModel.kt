package com.bestfriends.beachbingo.feature.bingo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestfriends.beachbingo.core.data.repository.AuthRepository
import com.bestfriends.beachbingo.core.data.repository.GameRepository
import com.bestfriends.beachbingo.core.model.BingoGame
import com.bestfriends.beachbingo.core.model.GameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val userGames: StateFlow<List<BingoGame>> = currentUser
        .filterNotNull()
        .flatMapLatest { user -> gameRepository.observeUserGames(user.uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val userResults: StateFlow<List<GameResult>> = currentUser
        .filterNotNull()
        .flatMapLatest { user -> gameRepository.observeUserResults(user.uid) }
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    fun createGame() {
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            gameRepository.createGame(user, user.preferredGameMode, user.preferredDrawStyle)
                .onSuccess { gameId -> _uiState.update { it.copy(isLoading = false, navigateToGameId = gameId) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun joinGame(gameId: String) {
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            gameRepository.joinGame(gameId.trim(), user)
                .onSuccess { _uiState.update { it.copy(isLoading = false, navigateToGameId = gameId.trim()) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            gameRepository.deleteGame(gameId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearNavigate() = _uiState.update { it.copy(navigateToGameId = null, error = null) }
}

data class LobbyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToGameId: String? = null
)
