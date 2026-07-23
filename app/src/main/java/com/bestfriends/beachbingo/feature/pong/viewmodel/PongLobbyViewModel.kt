package com.bestfriends.beachbingo.feature.pong.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestfriends.beachbingo.core.data.repository.AuthRepository
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.core.model.PongGame
import com.bestfriends.beachbingo.core.model.PongPlayer
import com.bestfriends.beachbingo.core.model.PongStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PongLobbyViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

    private val _uiState = MutableStateFlow(PongLobbyUiState())
    val uiState: StateFlow<PongLobbyUiState> = _uiState.asStateFlow()

    private val _activeGame = MutableStateFlow<PongGame?>(null)
    val activeGame: StateFlow<PongGame?> = _activeGame.asStateFlow()

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            _currentUid.value = user.uid
            // Load pong preferences
            _uiState.update { it.copy(
                difficulty = user.preferredPongDifficulty ?: PongDifficulty.ROOKIE,
                scoreLimit = user.preferredPongScoreLimit ?: 7,
                totalPaddles = user.preferredPongPaddles ?: 2,
            ) }
        }
    }

    private var lobbyObserveJob: kotlinx.coroutines.Job? = null

    fun observeLobbyGames(uid: String, humanCount: Int) {
        lobbyObserveJob?.cancel()
        if (humanCount < 2) {
            _activeGame.value = null
            return
        }
        lobbyObserveJob = viewModelScope.launch {
            callbackFlow {
                val q = db.collection("pongGames")
                    .whereArrayContains("playerIds", uid)
                    .whereIn("status", listOf("LOBBY", "IN_PROGRESS"))
                val reg = q.addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    val games = snap.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val rawStatus = data["status"] as? String ?: "LOBBY"
                        val status = runCatching { PongStatus.valueOf(rawStatus) }.getOrDefault(PongStatus.LOBBY)
                        PongGame(
                            gameId = doc.id,
                            adminId = data["adminId"] as? String ?: "",
                            status = status,
                            totalPaddles = (data["totalPaddles"] as? Long)?.toInt() ?: 2,
                            humanCount = (data["humanCount"] as? Long)?.toInt() ?: 1,
                            difficulty = runCatching { PongDifficulty.valueOf(data["difficulty"] as? String ?: "ROOKIE") }.getOrDefault(PongDifficulty.ROOKIE),
                            scoreLimit = (data["scoreLimit"] as? Long)?.toInt() ?: 7,
                            players = (data["players"] as? List<*>)?.mapNotNull { p ->
                                (p as? Map<*, *>)?.let { m ->
                                    PongPlayer(
                                        userId = m["userId"] as? String ?: "",
                                        displayName = m["displayName"] as? String ?: "",
                                        avatarUrl = m["avatarUrl"] as? String ?: "",
                                        side = m["side"] as? String ?: "left",
                                    )
                                }
                            } ?: emptyList(),
                            playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            createdAt = (data["createdAt"] as? Long) ?: 0L,
                        )
                    }
                    trySend(if (games.isNotEmpty()) games[0] else null)
                }
                awaitClose { reg.remove() }
            }.collect { game -> _activeGame.value = game }
        }
    }

    fun createGame(totalPaddles: Int, humanCount: Int, difficulty: PongDifficulty, scoreLimit: Int) {
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            _uiState.update { it.copy(isCreating = true, error = null) }
            try {
                val mySide = sidesForPaddles(totalPaddles)[0]
                val player = mapOf(
                    "userId" to user.uid,
                    "displayName" to user.displayName,
                    "avatarUrl" to user.avatarUrl,
                    "side" to mySide,
                )
                db.collection("pongGames").add(mapOf(
                    "adminId" to user.uid,
                    "status" to "LOBBY",
                    "totalPaddles" to totalPaddles,
                    "humanCount" to humanCount,
                    "difficulty" to difficulty.name,
                    "scoreLimit" to scoreLimit,
                    "players" to listOf(player),
                    "playerIds" to listOf(user.uid),
                    "wallSide" to null,
                    "ballX" to 200.0,
                    "ballY" to 200.0,
                    "ballVX" to 0.0,
                    "ballVY" to 0.0,
                    "speed" to 5.0,
                    "paddleLeft" to 250.0,
                    "paddleRight" to 250.0,
                    "paddleTop" to 250.0,
                    "paddleBottom" to 250.0,
                    "scoreLeft" to 0,
                    "scoreRight" to 0,
                    "scoreTop" to 0,
                    "scoreBottom" to 0,
                    "paused" to true,
                    "pauseTimer" to 90,
                    "winnerId" to null,
                    "createdAt" to System.currentTimeMillis(),
                )).await()
                _uiState.update { it.copy(isCreating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, error = e.message) }
            }
        }
    }

    fun joinGame(
        gameId: String,
        onSuccess: (gameId: String, totalPaddles: Int, humanCount: Int, difficulty: PongDifficulty, scoreLimit: Int, isHost: Boolean, mySide: String) -> Unit
    ) {
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            _uiState.update { it.copy(isJoining = true, joinError = null) }
            try {
                val snap = db.collection("pongGames").document(gameId.trim()).get().await()
                if (!snap.exists()) {
                    _uiState.update { it.copy(isJoining = false, joinError = "Spiel nicht gefunden.") }
                    return@launch
                }
                val data = snap.data!!
                val status = data["status"] as? String
                if (status != "LOBBY") {
                    _uiState.update { it.copy(isJoining = false, joinError = "Spiel läuft bereits.") }
                    return@launch
                }
                val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val players = (data["players"] as? List<*>)?.mapNotNull { p ->
                    (p as? Map<*, *>)?.let { m ->
                        PongPlayer(
                            userId = m["userId"] as? String ?: "",
                            displayName = m["displayName"] as? String ?: "",
                            avatarUrl = m["avatarUrl"] as? String ?: "",
                            side = m["side"] as? String ?: "left",
                        )
                    }
                } ?: emptyList()
                val humanCount = (data["humanCount"] as? Long)?.toInt() ?: 2
                val totalPaddles = (data["totalPaddles"] as? Long)?.toInt() ?: 2
                val difficulty = runCatching { PongDifficulty.valueOf(data["difficulty"] as? String ?: "ROOKIE") }.getOrDefault(PongDifficulty.ROOKIE)
                val scoreLimit = (data["scoreLimit"] as? Long)?.toInt() ?: 7
                val adminId = data["adminId"] as? String ?: ""

                if (playerIds.contains(user.uid)) {
                    val mySide = players.find { it.userId == user.uid }?.side ?: "left"
                    _uiState.update { it.copy(isJoining = false) }
                    onSuccess(snap.id, totalPaddles, humanCount, difficulty, scoreLimit, adminId == user.uid, mySide)
                    return@launch
                }
                if (players.size >= humanCount) {
                    _uiState.update { it.copy(isJoining = false, joinError = "Spiel ist voll.") }
                    return@launch
                }
                val takenSides = players.map { it.side }
                val freeSide = sidesForPaddles(totalPaddles).firstOrNull { it !in takenSides } ?: "right"
                val newPlayer = mapOf(
                    "userId" to user.uid,
                    "displayName" to user.displayName,
                    "avatarUrl" to user.avatarUrl,
                    "side" to freeSide,
                )
                db.collection("pongGames").document(snap.id).update(mapOf(
                    "players" to FieldValue.arrayUnion(newPlayer),
                    "playerIds" to FieldValue.arrayUnion(user.uid),
                )).await()
                _uiState.update { it.copy(isJoining = false) }
                onSuccess(snap.id, totalPaddles, humanCount, difficulty, scoreLimit, false, freeSide)
            } catch (e: Exception) {
                _uiState.update { it.copy(isJoining = false, joinError = "Fehler beim Beitreten.") }
            }
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            try {
                db.collection("pongGames").document(gameId).delete().await()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null, joinError = null) }

    companion object {
        fun sidesForPaddles(total: Int): List<String> = when (total) {
            2 -> listOf("left", "right")
            3 -> listOf("left", "right", "top")
            else -> listOf("left", "right", "top", "bottom")
        }
    }
}

data class PongLobbyUiState(
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null,
    val joinError: String? = null,
    val totalPaddles: Int = 2,
    val difficulty: PongDifficulty = PongDifficulty.ROOKIE,
    val scoreLimit: Int = 7,
)
