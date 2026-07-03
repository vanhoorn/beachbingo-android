package com.bestfriends.beachbingo.feature.bingo.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bestfriends.beachbingo.core.data.repository.AuthRepository
import com.bestfriends.beachbingo.core.data.repository.GameRepository
import com.bestfriends.beachbingo.core.model.BingoGame
import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameStatus
import kotlinx.coroutines.delay
import com.bestfriends.beachbingo.core.model.User
import com.bestfriends.beachbingo.core.tablet.SecondaryFirebase
import com.bestfriends.beachbingo.navigation.Screen
import com.google.firebase.firestore.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class BingoViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val gameId: String = savedStateHandle.toRoute<Screen.Game>().gameId

    val currentUser = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val game: StateFlow<BingoGame> = gameRepository.observeGame(gameId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BingoGame()
    )

    private val _uiState = MutableStateFlow(BingoUiState())
    val uiState: StateFlow<BingoUiState> = _uiState.asStateFlow()

    init {
        // Feuerwerk-Animation auslösen sobald ein Spieler hasBingo=true bekommt
        viewModelScope.launch {
            var prevHasBingo = false
            game.collect { g ->
                val currentHasBingo = g.players.values.any { it.hasBingo }
                if (!prevHasBingo && currentHasBingo) {
                    _uiState.update { it.copy(showBingoAnimation = true) }
                }
                prevHasBingo = currentHasBingo
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            gameRepository.startGame(gameId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun drawNumber() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDrawing = true) }
            if (game.value.drawStyle == DrawStyle.DRUM) {
                gameRepository.startDrawAnimation(gameId)
                delay(3000)
            }
            gameRepository.drawNumber(gameId)
                .onSuccess { _uiState.update { it.copy(isDrawing = false) } }
                .onFailure { e -> _uiState.update { it.copy(isDrawing = false, error = e.message) } }
        }
    }

    fun markNumber(number: Int) {
        viewModelScope.launch {
            val userId = currentUser.value?.uid ?: return@launch
            val drawnNumbers = game.value.drawnNumbers
            if (number !in drawnNumbers) return@launch
            gameRepository.markNumber(gameId, userId, number)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun claimBingo() {
        viewModelScope.launch {
            val userId = currentUser.value?.uid ?: return@launch
            _uiState.update { it.copy(isClaimingBingo = true) }
            gameRepository.claimBingo(gameId, userId)
                .onSuccess { hasBingo ->
                    _uiState.update { it.copy(isClaimingBingo = false) }
                    if (!hasBingo) {
                        _uiState.update { it.copy(error = "Noch kein Bingo! 😅") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isClaimingBingo = false, error = e.message) }
                }
        }
    }

    fun leaveGame() {
        viewModelScope.launch {
            val userId = currentUser.value?.uid ?: return@launch
            gameRepository.leaveGame(gameId, userId)
            // Player 2 verlässt das Spiel ebenfalls
            _player2User.value?.uid?.let { gameRepository.leaveGame(gameId, it) }
            _player2User.value = null
        }
    }

    // ── Tablet: Player 2 ──────────────────────────────────────────────────────

    private val _player2User = MutableStateFlow<User?>(null)
    val player2User: StateFlow<User?> = _player2User.asStateFlow()

    private val _tabletUiState = MutableStateFlow(TabletUiState())
    val tabletUiState: StateFlow<TabletUiState> = _tabletUiState.asStateFlow()

    fun loginPlayer2(email: String, password: String) {
        viewModelScope.launch {
            _tabletUiState.update { it.copy(isLoggingIn = true, loginError = null) }
            runCatching {
                val auth = SecondaryFirebase.getAuth()
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid
                val doc = SecondaryFirebase.getFirestore()
                    .collection("users").document(uid).get().await()
                doc.toObject<User>()?.copy(uid = uid, email = email)
                    ?: User(uid = uid, email = email)
            }.onSuccess { user ->
                _player2User.value = user
                // Nur beitreten wenn noch nicht im Spiel
                if (!game.value.players.containsKey(user.uid)) {
                    gameRepository.joinGame(gameId, user)
                }
                _tabletUiState.update { it.copy(isLoggingIn = false) }
            }.onFailure { e ->
                _tabletUiState.update { it.copy(isLoggingIn = false, loginError = e.message) }
            }
        }
    }

    fun markNumberPlayer2(number: Int) {
        viewModelScope.launch {
            val userId = _player2User.value?.uid ?: return@launch
            if (number !in game.value.drawnNumbers) return@launch
            gameRepository.markNumber(gameId, userId, number)
                .onSuccess { hasBingo ->
                    if (hasBingo) _uiState.update { it.copy(showBingoAnimation = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun claimBingoPlayer2() {
        viewModelScope.launch {
            val userId = _player2User.value?.uid ?: return@launch
            _tabletUiState.update { it.copy(isClaimingBingo2 = true) }
            gameRepository.claimBingo(gameId, userId)
                .onSuccess { hasBingo ->
                    _tabletUiState.update { it.copy(isClaimingBingo2 = false) }
                    if (hasBingo) {
                        _uiState.update { it.copy(showBingoAnimation = true) }
                    } else {
                        _uiState.update { it.copy(error = "Noch kein Bingo! 😅") }
                    }
                }
                .onFailure { e ->
                    _tabletUiState.update { it.copy(isClaimingBingo2 = false) }
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun clearTabletLoginError() = _tabletUiState.update { it.copy(loginError = null) }

    fun deleteGame() {
        viewModelScope.launch {
            gameRepository.deleteGame(gameId)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun eliminateNumber(number: Int) {
        viewModelScope.launch {
            gameRepository.eliminateNumber(gameId, number)
                .onSuccess {
                    delay(3000)
                    gameRepository.clearEliminationAnimation(gameId)
                }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun dismissBingoAnimation() = _uiState.update { it.copy(showBingoAnimation = false) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class BingoUiState(
    val isDrawing: Boolean = false,
    val isClaimingBingo: Boolean = false,
    val showBingoAnimation: Boolean = false,
    val error: String? = null
)

data class TabletUiState(
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val isClaimingBingo2: Boolean = false
)
