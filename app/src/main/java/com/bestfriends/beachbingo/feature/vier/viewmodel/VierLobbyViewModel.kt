package com.bestfriends.beachbingo.feature.vier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestfriends.beachbingo.core.model.VierGame
import com.bestfriends.beachbingo.core.model.VierPlayer
import com.bestfriends.beachbingo.core.model.VierStatus
import com.bestfriends.beachbingo.feature.vier.ui.DRINKS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class VierLobbyUiState(
    val preferredDrinkId: String = "lager",
    val preferredDifficulty: String = "SNIPER",
    val onlineStep: String = "choose", // "choose" | "waiting"
    val gameCode: String = "",
    val waitingPlayerCount: Int = 1,
    val error: String? = null,
    val navigateToGame: NavigateToVierGame? = null,
)

data class NavigateToVierGame(
    val mode: String,
    val gameId: String?,
    val myDrinkId: String,
    val aiDrinkId: String?,
    val aiDifficulty: String = "SNIPER",
)

@HiltViewModel
class VierLobbyViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VierLobbyUiState())
    val uiState: StateFlow<VierLobbyUiState> = _uiState.asStateFlow()

    private var waitingListener: ListenerRegistration? = null

    init {
        loadPreferredDrink()
    }

    private fun loadPreferredDrink() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val snap = firestore.collection("users").document(uid).get().await()
            val drinkId = snap.getString("preferredVierDrinkId") ?: "lager"
            val difficulty = snap.getString("preferredVierDifficulty") ?: "SNIPER"
            _uiState.update { it.copy(preferredDrinkId = drinkId, preferredDifficulty = difficulty) }
        }
    }

    fun createOnlineGame(myDrinkId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val userSnap = firestore.collection("users").document(uid).get().await()
                val displayName = userSnap.getString("displayName") ?: ""
                val avatarUrl = userSnap.getString("avatarUrl") ?: ""

                val code = generateCode()
                val game = mapOf(
                    "adminId" to uid,
                    "status" to VierStatus.LOBBY.name,
                    "humanCount" to 2,
                    "players" to listOf(
                        mapOf("userId" to uid, "displayName" to displayName, "avatarUrl" to avatarUrl, "drinkId" to myDrinkId)
                    ),
                    "playerIds" to listOf(uid),
                    "board" to List(42) { 0 },
                    "currentTurn" to uid,
                    "winnerId" to null,
                    "isDraw" to false,
                    "createdAt" to System.currentTimeMillis(),
                )
                firestore.collection("vierGames").document(code).set(game).await()
                _uiState.update { it.copy(gameCode = code, onlineStep = "waiting") }

                waitingListener = firestore.collection("vierGames").document(code)
                    .addSnapshotListener { snap, _ ->
                        if (snap == null || !snap.exists()) return@addSnapshotListener
                        @Suppress("UNCHECKED_CAST")
                        val players = snap.get("players") as? List<Map<String, Any>> ?: emptyList()
                        _uiState.update { it.copy(waitingPlayerCount = players.size) }
                        if (players.size >= 2) {
                            waitingListener?.remove()
                            waitingListener = null
                            _uiState.update {
                                it.copy(
                                    navigateToGame = NavigateToVierGame(
                                        mode = "online",
                                        gameId = code,
                                        myDrinkId = myDrinkId,
                                        aiDrinkId = null,
                                    )
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erstellen fehlgeschlagen. Bitte nochmal versuchen.") }
            }
        }
    }

    fun joinOnlineGame(code: String, myDrinkId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val userSnap = firestore.collection("users").document(uid).get().await()
                val displayName = userSnap.getString("displayName") ?: ""
                val avatarUrl = userSnap.getString("avatarUrl") ?: ""

                val trimmed = code.trim().uppercase()
                val gameRef = firestore.collection("vierGames").document(trimmed)
                val snap = gameRef.get().await()

                if (!snap.exists()) {
                    _uiState.update { it.copy(error = "Spiel nicht gefunden!") }
                    return@launch
                }

                val status = snap.getString("status") ?: ""
                if (status != VierStatus.LOBBY.name) {
                    _uiState.update { it.copy(error = "Dieses Spiel läuft bereits.") }
                    return@launch
                }

                @Suppress("UNCHECKED_CAST")
                val playerIds = snap.get("playerIds") as? List<String> ?: emptyList()

                if (playerIds.contains(uid)) {
                    // Already in the game
                    _uiState.update {
                        it.copy(
                            navigateToGame = NavigateToVierGame(
                                mode = "online",
                                gameId = trimmed,
                                myDrinkId = myDrinkId,
                                aiDrinkId = null,
                            )
                        )
                    }
                    return@launch
                }

                @Suppress("UNCHECKED_CAST")
                val players = snap.get("players") as? List<Map<String, Any>> ?: emptyList()
                if (players.size >= 2) {
                    _uiState.update { it.copy(error = "Das Spiel ist bereits voll.") }
                    return@launch
                }

                val takenDrinks = players.map { it["drinkId"] as? String ?: "" }
                val finalDrinkId = if (takenDrinks.contains(myDrinkId)) {
                    DRINKS.firstOrNull { !takenDrinks.contains(it.id) }?.id ?: myDrinkId
                } else {
                    myDrinkId
                }

                val newPlayer = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "avatarUrl" to avatarUrl,
                    "drinkId" to finalDrinkId,
                )
                gameRef.update(
                    mapOf(
                        "playerIds" to FieldValue.arrayUnion(uid),
                        "players" to (players + newPlayer),
                        "status" to VierStatus.RUNNING.name,
                    )
                ).await()

                _uiState.update {
                    it.copy(
                        navigateToGame = NavigateToVierGame(
                            mode = "online",
                            gameId = trimmed,
                            myDrinkId = finalDrinkId,
                            aiDrinkId = null,
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Beitreten fehlgeschlagen.") }
            }
        }
    }

    fun cancelWaiting() {
        waitingListener?.remove()
        waitingListener = null
        val code = _uiState.value.gameCode
        if (code.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    firestore.collection("vierGames").document(code).delete().await()
                } catch (_: Exception) {}
            }
        }
        _uiState.update { it.copy(onlineStep = "choose", gameCode = "", waitingPlayerCount = 1) }
    }

    fun clearNavigate() = _uiState.update { it.copy(navigateToGame = null, error = null) }

    override fun onCleared() {
        super.onCleared()
        waitingListener?.remove()
    }

    private fun generateCode(): String {
        val chars = ('A'..'Z').toList()
        return (1..6).map { chars.random() }.joinToString("")
    }
}
