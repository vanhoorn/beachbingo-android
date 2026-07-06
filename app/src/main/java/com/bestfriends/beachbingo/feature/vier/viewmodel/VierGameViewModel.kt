package com.bestfriends.beachbingo.feature.vier.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestfriends.beachbingo.core.model.VierStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

const val ROWS = 6
const val COLS = 7

// ─── Board helpers ────────────────────────────────────────────────────────────

fun emptyBoard(): List<Int> = List(ROWS * COLS) { 0 }

fun getAvailableRow(board: List<Int>, col: Int): Int {
    for (r in ROWS - 1 downTo 0) {
        if (board[r * COLS + col] == 0) return r
    }
    return -1
}

fun dropPieceIntoBoard(board: List<Int>, col: Int, player: Int): List<Int> {
    val row = getAvailableRow(board, col)
    if (row == -1) return board
    return board.toMutableList().also { it[row * COLS + col] = player }
}

fun checkWin(board: List<Int>, player: Int): Pair<Boolean, List<Int>> {
    val wins = mutableSetOf<Int>()
    // Horizontal
    for (r in 0 until ROWS) {
        for (c in 0..COLS - 4) {
            val idxs = (0..3).map { i -> r * COLS + c + i }
            if (idxs.all { board[it] == player }) wins.addAll(idxs)
        }
    }
    // Vertical
    for (c in 0 until COLS) {
        for (r in 0..ROWS - 4) {
            val idxs = (0..3).map { i -> (r + i) * COLS + c }
            if (idxs.all { board[it] == player }) wins.addAll(idxs)
        }
    }
    // Diagonal ↘
    for (r in 0..ROWS - 4) {
        for (c in 0..COLS - 4) {
            val idxs = (0..3).map { i -> (r + i) * COLS + (c + i) }
            if (idxs.all { board[it] == player }) wins.addAll(idxs)
        }
    }
    // Diagonal ↗
    for (r in 3 until ROWS) {
        for (c in 0..COLS - 4) {
            val idxs = (0..3).map { i -> (r - i) * COLS + (c + i) }
            if (idxs.all { board[it] == player }) wins.addAll(idxs)
        }
    }
    return Pair(wins.isNotEmpty(), wins.toList())
}

fun isBoardDraw(board: List<Int>): Boolean = board.all { it != 0 }

fun isTerminalBoard(board: List<Int>): Boolean =
    checkWin(board, 1).first || checkWin(board, 2).first || isBoardDraw(board)

// ─── AI (Minimax + Alpha-Beta) ────────────────────────────────────────────────

fun scoreWindow(window: List<Int>, player: Int): Int {
    val opp = if (player == 1) 2 else 1
    val mine = window.count { it == player }
    val empty = window.count { it == 0 }
    val oppC = window.count { it == opp }
    return when {
        mine == 4 -> 100
        mine == 3 && empty == 1 -> 5
        mine == 2 && empty == 2 -> 2
        oppC == 3 && empty == 1 -> -4
        else -> 0
    }
}

fun scoreBoard(board: List<Int>, player: Int): Int {
    var score = 0
    val centerCol = 3
    for (r in 0 until ROWS) if (board[r * COLS + centerCol] == player) score += 3
    for (r in 0 until ROWS) {
        for (c in 0..COLS - 4)
            score += scoreWindow((0..3).map { i -> board[r * COLS + c + i] }, player)
    }
    for (c in 0 until COLS) {
        for (r in 0..ROWS - 4)
            score += scoreWindow((0..3).map { i -> board[(r + i) * COLS + c] }, player)
    }
    for (r in 0..ROWS - 4) {
        for (c in 0..COLS - 4)
            score += scoreWindow((0..3).map { i -> board[(r + i) * COLS + (c + i)] }, player)
    }
    for (r in 3 until ROWS) {
        for (c in 0..COLS - 4)
            score += scoreWindow((0..3).map { i -> board[(r - i) * COLS + (c + i)] }, player)
    }
    return score
}

fun minimax(board: List<Int>, depth: Int, alpha: Int, beta: Int, maximizing: Boolean, ai: Int): Int {
    val human = if (ai == 1) 2 else 1
    if (depth == 0 || isTerminalBoard(board)) {
        return when {
            checkWin(board, ai).first -> 100000 + depth
            checkWin(board, human).first -> -100000 - depth
            else -> scoreBoard(board, ai)
        }
    }
    val cols = listOf(3, 2, 4, 1, 5, 0, 6).filter { c -> getAvailableRow(board, c) != -1 }
    var a = alpha
    var b = beta
    return if (maximizing) {
        var best = Int.MIN_VALUE
        for (c in cols) {
            val score = minimax(dropPieceIntoBoard(board, c, ai), depth - 1, a, b, false, ai)
            best = maxOf(best, score)
            a = maxOf(a, best)
            if (a >= b) break
        }
        best
    } else {
        var best = Int.MAX_VALUE
        for (c in cols) {
            val score = minimax(dropPieceIntoBoard(board, c, human), depth - 1, a, b, true, ai)
            best = minOf(best, score)
            b = minOf(b, best)
            if (a >= b) break
        }
        best
    }
}

fun getBestMove(board: List<Int>, ai: Int, difficulty: String = "SNIPER"): Int {
    val cols = listOf(3, 2, 4, 1, 5, 0, 6).filter { c -> getAvailableRow(board, c) != -1 }
    // Rookie: 40% random, Sniper: 15% random, BossLevel: always optimal
    val randomChance = when (difficulty) { "ROOKIE" -> 0.40; "SNIPER" -> 0.15; else -> 0.0 }
    if (Math.random() < randomChance) return cols.random()
    var bestCol = cols[0]
    var bestScore = Int.MIN_VALUE
    for (c in cols) {
        val score = minimax(dropPieceIntoBoard(board, c, ai), 5, Int.MIN_VALUE, Int.MAX_VALUE, false, ai)
        if (score > bestScore) {
            bestScore = score
            bestCol = c
        }
    }
    return bestCol
}

// ─── UI State ─────────────────────────────────────────────────────────────────

data class VierGameUiState(
    val board: List<Int> = emptyBoard(),
    val currentPlayer: Int = 1,       // 1 or 2 (AI mode local)
    val winner: Int? = null,           // 1, 2, or null
    val draw: Boolean = false,
    val winCells: List<Int> = emptyList(),
    val aiThinking: Boolean = false,
    // Online
    val onlineCurrentTurn: String = "",
    val onlinePlayers: List<Map<String, String>> = emptyList(),
    val onlineStatus: String = "",
    val onlineWinnerId: String? = null,
    val onlineIsDraw: Boolean = false,
    // Drop animation
    val lastDroppedCell: Int = -1,
    val lastDroppedRow: Int = -1,
    val dropAnimKey: Int = 0,
)

@HiltViewModel
class VierGameViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VierGameUiState())
    val uiState: StateFlow<VierGameUiState> = _uiState.asStateFlow()

    private var onlineListener: ListenerRegistration? = null
    private var dropAnimKey = 0

    private var cachedUid: String = ""
    private var cachedDisplayName: String = ""
    private var cachedAvatarUrl: String = ""
    private var currentAiDrinkId: String = "lager"
    private var aiResultWritten = false

    fun initAi(myDrinkId: String) {
        currentAiDrinkId = myDrinkId
        aiResultWritten = false
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snap = firestore.collection("users").document(uid).get().await()
                cachedUid = uid
                cachedDisplayName = snap.getString("displayName") ?: ""
                cachedAvatarUrl = snap.getString("avatarUrl") ?: ""
            } catch (_: Exception) {}
        }
    }

    private fun saveAiResult(humanWon: Boolean, draw: Boolean) {
        if (aiResultWritten || cachedUid.isEmpty()) return
        aiResultWritten = true
        viewModelScope.launch {
            try {
                firestore.collection("vierGames").add(
                    mapOf(
                        "adminId" to cachedUid,
                        "status" to VierStatus.FINISHED.name,
                        "humanCount" to 1,
                        "players" to listOf(
                            mapOf(
                                "userId" to cachedUid,
                                "displayName" to cachedDisplayName,
                                "avatarUrl" to cachedAvatarUrl,
                                "drinkId" to currentAiDrinkId,
                            )
                        ),
                        "playerIds" to listOf(cachedUid),
                        "winnerId" to if (humanWon) cachedUid else null,
                        "isDraw" to draw,
                        "board" to _uiState.value.board,
                        "currentTurn" to cachedUid,
                        "createdAt" to System.currentTimeMillis(),
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    fun initOnline(gameId: String) {
        onlineListener?.remove()
        onlineListener = firestore.collection("vierGames").document(gameId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val board = (snap.get("board") as? List<Long>)?.map { it.toInt() } ?: emptyBoard()
                val currentTurn = snap.getString("currentTurn") ?: ""
                @Suppress("UNCHECKED_CAST")
                val rawPlayers = snap.get("players") as? List<Map<String, Any>> ?: emptyList()
                val players = rawPlayers.map { p ->
                    mapOf(
                        "userId" to (p["userId"] as? String ?: ""),
                        "displayName" to (p["displayName"] as? String ?: ""),
                        "drinkId" to (p["drinkId"] as? String ?: "lager"),
                        "avatarUrl" to (p["avatarUrl"] as? String ?: ""),
                    )
                }
                val status = snap.getString("status") ?: ""
                val winnerId = snap.getString("winnerId")
                val isDraw = snap.getBoolean("isDraw") ?: false

                // Detect new drop for animation
                val prevBoard = _uiState.value.board
                var droppedCell = -1
                var droppedRow = -1
                for (i in board.indices) {
                    if (prevBoard.getOrElse(i) { 0 } == 0 && board[i] != 0) {
                        droppedCell = i
                        droppedRow = i / COLS
                        break
                    }
                }
                if (droppedCell != -1) dropAnimKey++

                _uiState.update {
                    it.copy(
                        board = board,
                        onlineCurrentTurn = currentTurn,
                        onlinePlayers = players,
                        onlineStatus = status,
                        onlineWinnerId = winnerId,
                        onlineIsDraw = isDraw,
                        lastDroppedCell = droppedCell,
                        lastDroppedRow = droppedRow,
                        dropAnimKey = dropAnimKey,
                    )
                }
            }
    }

    fun dropPieceAi(col: Int, difficulty: String = "SNIPER") {
        val state = _uiState.value
        if (state.currentPlayer != 1 || state.winner != null || state.draw || state.aiThinking) return
        val row = getAvailableRow(state.board, col)
        if (row == -1) return

        val newBoard = dropPieceIntoBoard(state.board, col, 1)
        val (won, winCells) = checkWin(newBoard, 1)
        val draw = !won && isBoardDraw(newBoard)

        dropAnimKey++
        _uiState.update {
            it.copy(
                board = newBoard,
                currentPlayer = 2,
                winner = if (won) 1 else null,
                draw = draw,
                winCells = winCells,
                aiThinking = !won && !draw,
                lastDroppedCell = row * COLS + col,
                lastDroppedRow = row,
                dropAnimKey = dropAnimKey,
            )
        }

        if (won) { saveAiResult(humanWon = true, draw = false); return }
        if (draw) { saveAiResult(humanWon = false, draw = true); return }

        viewModelScope.launch {
            delay(500)
            val current = _uiState.value
            if (current.currentPlayer != 2 || current.winner != null || current.draw) return@launch
            val aiCol = getBestMove(current.board, 2, difficulty)
            val aiRow = getAvailableRow(current.board, aiCol)
            val aiBoard = dropPieceIntoBoard(current.board, aiCol, 2)
            val (aiWon, aiWinCells) = checkWin(aiBoard, 2)
            val aiDraw = !aiWon && isBoardDraw(aiBoard)
            dropAnimKey++
            _uiState.update {
                it.copy(
                    board = aiBoard,
                    currentPlayer = 1,
                    winner = if (aiWon) 2 else null,
                    draw = aiDraw,
                    winCells = aiWinCells,
                    aiThinking = false,
                    lastDroppedCell = aiRow * COLS + aiCol,
                    lastDroppedRow = aiRow,
                    dropAnimKey = dropAnimKey,
                )
            }
            if (aiWon) saveAiResult(humanWon = false, draw = false)
            else if (aiDraw) saveAiResult(humanWon = false, draw = true)
        }
    }

    fun dropPieceOnline(col: Int, gameId: String) {
        val uid = auth.currentUser?.uid ?: return
        val state = _uiState.value
        if (state.onlineCurrentTurn != uid) return
        if (state.onlineStatus != VierStatus.RUNNING.name) return
        if (getAvailableRow(state.board, col) == -1) return

        viewModelScope.launch {
            val myPlayerIndex = state.onlinePlayers.indexOfFirst { it["userId"] == uid }
            if (myPlayerIndex == -1) return@launch
            val myPiece = myPlayerIndex + 1

            val newBoard = dropPieceIntoBoard(state.board, col, myPiece)
            val (won, _) = checkWin(newBoard, myPiece)
            val draw = !won && isBoardDraw(newBoard)
            val opponentId = state.onlinePlayers.firstOrNull { it["userId"] != uid }?.get("userId") ?: uid

            try {
                firestore.collection("vierGames").document(gameId).update(
                    mapOf(
                        "board" to newBoard,
                        "currentTurn" to opponentId,
                        "winnerId" to if (won) uid else null,
                        "isDraw" to draw,
                        "status" to if (won || draw) VierStatus.FINISHED.name else VierStatus.RUNNING.name,
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    fun restartAi() {
        aiResultWritten = false
        _uiState.update { VierGameUiState() }
    }

    fun clearDropAnim() {
        _uiState.update { it.copy(lastDroppedCell = -1) }
    }

    override fun onCleared() {
        super.onCleared()
        onlineListener?.remove()
    }
}
