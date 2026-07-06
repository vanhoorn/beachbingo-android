package com.bestfriends.beachbingo.feature.join.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestfriends.beachbingo.core.data.repository.AuthRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class JoinDestination {
    data class Bingo(val gameId: String) : JoinDestination()
    data class Pong(
        val gameId: String,
        val totalPaddles: Int,
        val humanCount: Int,
        val difficulty: String,
        val scoreLimit: Int,
        val isHost: Boolean,
        val mySide: String,
    ) : JoinDestination()
    data class Vier(val gameId: String, val myDrinkId: String) : JoinDestination()
}

data class JoinUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val destination: JoinDestination? = null,
)

@HiltViewModel
class JoinViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinUiState())
    val uiState: StateFlow<JoinUiState> = _uiState.asStateFlow()

    fun joinGame(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val bingoDeferred = async { firestore.collection("games").document(code).get().await() }
                val pongDeferred  = async { firestore.collection("pongGames").document(code).get().await() }
                val vierDeferred  = async { firestore.collection("vierGames").document(code).get().await() }

                val bingoSnap = bingoDeferred.await()
                val pongSnap  = pongDeferred.await()
                val vierSnap  = vierDeferred.await()

                val destination: JoinDestination? = when {
                    bingoSnap.exists() -> joinBingo(code, bingoSnap.data!!, user.uid, user.displayName, user.avatarUrl)
                    pongSnap.exists()  -> joinPong(code, pongSnap.data!!, user.uid, user.displayName, user.avatarUrl)
                    vierSnap.exists()  -> joinVier(code, vierSnap.data!!, user.uid, user.displayName, user.avatarUrl, user.preferredVierDrinkId ?: "lager")
                    else               -> { _uiState.update { it.copy(isLoading = false, error = "Kein Spiel mit diesem Code gefunden.") }; null }
                }
                if (destination != null) {
                    _uiState.update { it.copy(isLoading = false, destination = destination) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Beitreten fehlgeschlagen.") }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun joinBingo(
        code: String, data: Map<String, Any>, uid: String, displayName: String, avatarUrl: String
    ): JoinDestination? {
        val status = data["status"] as? String ?: ""
        if (status == "FINISHED") {
            _uiState.update { it.copy(isLoading = false, error = "Dieses Spiel ist bereits beendet.") }
            return null
        }
        val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        if (!playerIds.contains(uid)) {
            val card = generateBingoCard()
            val newPlayer = mapOf(
                "userId" to uid,
                "displayName" to displayName,
                "avatarUrl" to avatarUrl,
                "card" to mapOf("grid" to card, "markedNumbers" to emptyList<Int>()),
                "hasBingo" to false,
            )
            val rawPlayers = data["players"]
            val updates: Map<String, Any> = if (rawPlayers is List<*>) {
                // Web-created game: players is array → convert to Map keyed by userId
                val playersMap = mutableMapOf<String, Any>()
                @Suppress("UNCHECKED_CAST")
                rawPlayers.filterIsInstance<Map<*, *>>().forEach { p ->
                    val pUid = p["userId"] as? String ?: return@forEach
                    playersMap[pUid] = p as Map<String, Any>
                }
                playersMap[uid] = newPlayer
                mapOf("playerIds" to FieldValue.arrayUnion(uid), "players" to playersMap)
            } else {
                // Android-created game: players is already a Map
                mapOf("playerIds" to FieldValue.arrayUnion(uid), "players.$uid" to newPlayer)
            }
            firestore.collection("games").document(code).update(updates).await()
        }
        return JoinDestination.Bingo(code)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun joinPong(
        code: String, data: Map<String, Any>, uid: String, displayName: String, avatarUrl: String
    ): JoinDestination? {
        val status = data["status"] as? String ?: ""
        if (status != "LOBBY") {
            _uiState.update { it.copy(isLoading = false, error = "Dieses Spiel läuft bereits.") }
            return null
        }
        val totalPaddles = (data["totalPaddles"] as? Long)?.toInt() ?: 2
        val humanCount   = (data["humanCount"]   as? Long)?.toInt() ?: 2
        val difficulty   = data["difficulty"] as? String ?: "ROOKIE"
        val scoreLimit   = (data["scoreLimit"] as? Long)?.toInt() ?: 7
        val adminId      = data["adminId"] as? String ?: ""
        val playerIds    = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val players = (data["players"] as? List<*>)?.mapNotNull { p ->
            (p as? Map<*, *>)?.let { mapOf("userId" to it["userId"], "side" to it["side"]) }
        } ?: emptyList()

        if (playerIds.contains(uid)) {
            val mySide = players.find { it["userId"] == uid }?.get("side") as? String ?: "left"
            return JoinDestination.Pong(code, totalPaddles, humanCount, difficulty, scoreLimit, adminId == uid, mySide)
        }
        if (players.size >= humanCount) {
            _uiState.update { it.copy(isLoading = false, error = "Das Spiel ist voll.") }
            return null
        }
        val takenSides = players.mapNotNull { it["side"] as? String }
        val freeSide   = sidesForPaddles(totalPaddles).firstOrNull { it !in takenSides } ?: "right"
        val newPlayer  = mapOf("userId" to uid, "displayName" to displayName, "avatarUrl" to avatarUrl, "side" to freeSide)
        firestore.collection("pongGames").document(code).update(
            mapOf(
                "players"   to FieldValue.arrayUnion(newPlayer),
                "playerIds" to FieldValue.arrayUnion(uid),
            )
        ).await()
        return JoinDestination.Pong(code, totalPaddles, humanCount, difficulty, scoreLimit, false, freeSide)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun joinVier(
        code: String, data: Map<String, Any>, uid: String, displayName: String, avatarUrl: String, preferredDrinkId: String
    ): JoinDestination? {
        val status = data["status"] as? String ?: ""
        if (status != "LOBBY") {
            _uiState.update { it.copy(isLoading = false, error = "Dieses Spiel läuft bereits.") }
            return null
        }
        val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val players   = (data["players"] as? List<*>)?.mapNotNull { p ->
            (p as? Map<*, *>)?.let { mapOf("userId" to it["userId"], "drinkId" to it["drinkId"]) }
        } ?: emptyList()

        if (playerIds.contains(uid)) {
            val myDrinkId = players.find { it["userId"] == uid }?.get("drinkId") as? String ?: preferredDrinkId
            return JoinDestination.Vier(code, myDrinkId)
        }
        if (players.size >= 2) {
            _uiState.update { it.copy(isLoading = false, error = "Das Spiel ist voll.") }
            return null
        }
        val takenDrinks = players.mapNotNull { it["drinkId"] as? String }
        val myDrinkId   = if (takenDrinks.contains(preferredDrinkId)) {
            listOf("lager","weizen","hefeweizen","radler","pils","cocktail").firstOrNull { it !in takenDrinks } ?: preferredDrinkId
        } else preferredDrinkId
        val newPlayer = mapOf("userId" to uid, "displayName" to displayName, "avatarUrl" to avatarUrl, "drinkId" to myDrinkId)
        firestore.collection("vierGames").document(code).update(
            mapOf(
                "playerIds" to FieldValue.arrayUnion(uid),
                "players"   to (players.map { p -> mapOf("userId" to p["userId"], "drinkId" to p["drinkId"]) } + newPlayer),
                "status"    to "RUNNING",
            )
        ).await()
        return JoinDestination.Vier(code, myDrinkId)
    }

    fun clearNavigate() = _uiState.update { it.copy(destination = null, error = null) }

    private fun sidesForPaddles(total: Int) = when (total) {
        2    -> listOf("left", "right")
        3    -> listOf("left", "right", "top")
        else -> listOf("left", "right", "top", "bottom")
    }

    private fun generateBingoCard(): List<Int> {
        val cols = listOf(
            (1..15).shuffled().take(5),
            (16..30).shuffled().take(5),
            (31..45).shuffled().take(5),
            (46..60).shuffled().take(5),
            (61..75).shuffled().take(5),
        )
        val flat = (0 until 5).flatMap { row -> cols.map { col -> col[row] } }.toMutableList()
        flat[12] = 0
        return flat
    }
}
