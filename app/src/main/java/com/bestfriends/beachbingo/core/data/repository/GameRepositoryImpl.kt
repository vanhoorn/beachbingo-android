package com.bestfriends.beachbingo.core.data.repository

import com.bestfriends.beachbingo.core.model.BingoCard
import com.bestfriends.beachbingo.core.model.BingoGame
import com.bestfriends.beachbingo.core.model.BingoPlayer
import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.core.model.GameResult
import com.bestfriends.beachbingo.core.model.GameStatus
import com.bestfriends.beachbingo.core.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GameRepository {

    private val games = firestore.collection("games")
    private val results = firestore.collection("results")

    override fun observeGame(gameId: String): Flow<BingoGame> = callbackFlow {
        val listener = games.document(gameId).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            snapshot?.toBingoGame()?.let { trySend(it) }
        }
        awaitClose { listener.remove() }
    }

    override fun observeUserResults(userId: String): Flow<List<GameResult>> = callbackFlow {
        val listener = results
            .whereArrayContains("playerIds", userId)
            .orderBy("finishedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fehlende Firestore-Indizes → leere Liste statt Crash
                    // Index-Link erscheint im Logcat
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(GameResult::class.java)?.copy(resultId = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override fun observeUserGames(userId: String): Flow<List<BingoGame>> = callbackFlow {
        val listener = games
            .whereArrayContains("playerIds", userId)
            .whereIn("status", listOf(GameStatus.LOBBY.name, GameStatus.RUNNING.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { it.toBingoGame() } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createGame(user: User, gameMode: GameMode, drawStyle: DrawStyle): Result<String> = runCatching {
        val gameId = games.document().id
        val player = BingoPlayer(
            userId = user.uid,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            card = BingoCardGenerator.generate()
        )
        games.document(gameId).set(
            mapOf(
                "gameId" to gameId,
                "adminId" to user.uid,
                "status" to GameStatus.LOBBY.name,
                "gameMode" to gameMode.name,
                "drawStyle" to drawStyle.name,
                "drawAnimationActive" to false,
                "totalDrawCount" to 0,
                "eliminationInterval" to user.bossLevelEliminationInterval,
                "drawnNumbers" to emptyList<Int>(),
                "currentNumber" to null,
                "createdAt" to System.currentTimeMillis(),
                "playerIds" to listOf(user.uid),
                "players" to mapOf(user.uid to player.toMap())
            )
        ).await()
        gameId
    }

    override suspend fun startDrawAnimation(gameId: String): Result<Unit> = runCatching {
        games.document(gameId).update("drawAnimationActive", true).await()
    }

    override suspend fun joinGame(gameId: String, user: User): Result<Unit> = runCatching {
        val player = BingoPlayer(
            userId = user.uid,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            card = BingoCardGenerator.generate()
        )
        games.document(gameId).update(
            mapOf(
                "players.${user.uid}" to player.toMap(),
                "playerIds" to FieldValue.arrayUnion(user.uid)
            )
        ).await()
    }

    override suspend fun startGame(gameId: String): Result<Unit> = runCatching {
        games.document(gameId).update("status", GameStatus.RUNNING.name).await()
    }

    override suspend fun drawNumber(gameId: String): Result<Int> = runCatching {
        var drawn = 0
        firestore.runTransaction { tx ->
            val snap = tx.get(games.document(gameId))
            val drawnSoFar = (snap.get("drawnNumbers") as? List<*>)
                ?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
            val remaining = (1..75).filter { it !in drawnSoFar }
            check(remaining.isNotEmpty()) { "Alle Zahlen wurden bereits gezogen" }
            drawn = remaining.random()
            val newTotalDrawCount = ((snap.get("totalDrawCount") as? Long)?.toInt() ?: 0) + 1
            val mode = (snap.get("gameMode") as? String)?.let {
                runCatching { GameMode.valueOf(it) }.getOrNull()
            }
            val updates = mutableMapOf<String, Any?>(
                "drawnNumbers" to FieldValue.arrayUnion(drawn),
                "currentNumber" to drawn,
                "drawAnimationActive" to false,
                "totalDrawCount" to newTotalDrawCount
            )
            val interval = (snap.get("eliminationInterval") as? Long)?.toInt()?.takeIf { it > 0 } ?: 5
            if (mode == GameMode.BOSS_LEVEL && newTotalDrawCount % interval == 0) {
                val playerIds = (snap.get("playerIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (playerIds.isNotEmpty()) {
                    updates["eliminationPendingPlayerId"] = playerIds.random()
                }
            }
            tx.update(games.document(gameId), updates)
        }.await()
        drawn
    }

    override suspend fun markNumber(gameId: String, userId: String, number: Int): Result<Boolean> = runCatching {
        var hasBingo = false
        firestore.runTransaction { tx ->
            val snap = tx.get(games.document(gameId))
            val game = snap.toBingoGame() ?: error("Spiel nicht gefunden")
            val player = game.players[userId] ?: error("Spieler nicht im Spiel")
            val updatedMarked = player.card.markedNumbers + number
            val updatedCard = player.card.copy(markedNumbers = updatedMarked)
            hasBingo = updatedCard.hasBingo()
            val updates = mutableMapOf<String, Any>(
                "players.$userId.card.markedNumbers" to updatedMarked.toList()
            )
            if (hasBingo) {
                updates["players.$userId.hasBingo"] = true
                updates["status"] = GameStatus.FINISHED.name
            }
            tx.update(games.document(gameId), updates)
        }.await()

        if (hasBingo) {
            saveResult(gameId, userId)
        }
        hasBingo
    }

    override suspend fun claimBingo(gameId: String, userId: String): Result<Boolean> = runCatching {
        var hasBingo = false
        firestore.runTransaction { tx ->
            val snap = tx.get(games.document(gameId))
            val game = snap.toBingoGame() ?: error("Spiel nicht gefunden")
            val player = game.players[userId] ?: error("Spieler nicht im Spiel")
            // Im AUTO_MARK-Modus: Bingo prüfen anhand der gezogenen Zahlen
            val drawnSet = game.drawnNumbers.toSet() + setOf(0)
            val cardNumbers = player.card.grid.flatten().toSet()
            val effectiveMarked = cardNumbers.intersect(drawnSet)
            val virtualCard = player.card.copy(markedNumbers = effectiveMarked)
            hasBingo = virtualCard.hasBingo()
            if (hasBingo) {
                tx.update(
                    games.document(gameId),
                    mapOf(
                        "players.$userId.hasBingo" to true,
                        "status" to GameStatus.FINISHED.name
                    )
                )
            }
        }.await()

        if (hasBingo) {
            saveResult(gameId, userId)
        }
        hasBingo
    }

    private suspend fun saveResult(gameId: String, userId: String) {
        val snap = games.document(gameId).get().await()
        val game = snap.toBingoGame() ?: return
        val winner = game.players[userId]
        val resultId = results.document().id
        val playerList = game.players.values.toList()
        results.document(resultId).set(
            mapOf(
                "resultId" to resultId,
                "gameId" to gameId,
                "winnerName" to (winner?.displayName ?: ""),
                "winnerId" to userId,
                "winnerAvatar" to (winner?.avatarUrl ?: ""),
                "playerCount" to game.players.size,
                "drawnNumbersCount" to game.drawnNumbers.size,
                "finishedAt" to System.currentTimeMillis(),
                "playerIds" to playerList.map { it.userId },
                "playerNames" to playerList.map { it.displayName },
                "playerAvatars" to playerList.map { it.avatarUrl }
            )
        ).await()
    }

    override suspend fun eliminateNumber(gameId: String, number: Int): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val snap = tx.get(games.document(gameId))
            val game = snap.toBingoGame() ?: error("Spiel nicht gefunden")
            val eliminatingPlayer = game.players[game.eliminationPendingPlayerId ?: ""]
            val updates = mutableMapOf<String, Any>(
                "drawnNumbers" to FieldValue.arrayRemove(number),
                "eliminationPendingPlayerId" to FieldValue.delete(),
                "eliminationAnimationActive" to true,
                "eliminationPlayerName" to (eliminatingPlayer?.displayName ?: ""),
                "eliminationPlayerAvatar" to (eliminatingPlayer?.avatarUrl ?: ""),
                "eliminationNumber" to number
            )
            if (game.currentNumber == number) updates["currentNumber"] = FieldValue.delete()
            game.players.forEach { (uid, player) ->
                if (number in player.card.markedNumbers) {
                    updates["players.$uid.card.markedNumbers"] = (player.card.markedNumbers - number).toList()
                }
            }
            tx.update(games.document(gameId), updates)
        }.await()
    }

    override suspend fun clearEliminationAnimation(gameId: String): Result<Unit> = runCatching {
        games.document(gameId).update(
            mapOf(
                "eliminationAnimationActive" to false,
                "eliminationPlayerName" to "",
                "eliminationPlayerAvatar" to "",
                "eliminationNumber" to 0
            )
        ).await()
    }

    override suspend fun deleteGame(gameId: String): Result<Unit> = runCatching {
        games.document(gameId).delete().await()
    }

    override suspend fun leaveGame(gameId: String, userId: String): Result<Unit> = runCatching {
        games.document(gameId).update(
            mapOf(
                "players.$userId" to FieldValue.delete(),
                "playerIds" to FieldValue.arrayRemove(userId)
            )
        ).await()
    }
}

// ── Firestore mapping helpers ──────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot?.toBingoGame(): BingoGame? {
    val data = this?.data ?: return null
    val playersRaw = data["players"] as? Map<String, Any> ?: emptyMap()
    val players = playersRaw.mapValues { (_, v) -> (v as Map<String, Any>).toBingoPlayer() }
    return BingoGame(
        gameId = id ?: "",
        adminId = data["adminId"] as? String ?: "",
        status = (data["status"] as? String)?.let {
            runCatching { GameStatus.valueOf(it) }.getOrDefault(GameStatus.LOBBY)
        } ?: GameStatus.LOBBY,
        gameMode = (data["gameMode"] as? String)?.let {
            runCatching { GameMode.valueOf(it) }.getOrDefault(GameMode.MANUAL_MARK)
        } ?: GameMode.MANUAL_MARK,
        drawStyle = (data["drawStyle"] as? String)?.let {
            runCatching { DrawStyle.valueOf(it) }.getOrDefault(DrawStyle.INSTANT)
        } ?: DrawStyle.INSTANT,
        drawAnimationActive = data["drawAnimationActive"] as? Boolean ?: false,
        players = players,
        drawnNumbers = (data["drawnNumbers"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList(),
        currentNumber = (data["currentNumber"] as? Long)?.toInt(),
        createdAt = data["createdAt"] as? Long ?: 0L,
        totalDrawCount = (data["totalDrawCount"] as? Long)?.toInt() ?: 0,
        eliminationInterval = (data["eliminationInterval"] as? Long)?.toInt() ?: 5,
        eliminationPendingPlayerId = data["eliminationPendingPlayerId"] as? String,
        eliminationAnimationActive = data["eliminationAnimationActive"] as? Boolean ?: false,
        eliminationPlayerName = data["eliminationPlayerName"] as? String ?: "",
        eliminationPlayerAvatar = data["eliminationPlayerAvatar"] as? String ?: "",
        eliminationNumber = (data["eliminationNumber"] as? Long)?.toInt() ?: 0
    )
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.toBingoPlayer(): BingoPlayer {
    val cardRaw = this["card"] as? Map<String, Any> ?: emptyMap()
    val flatGrid = (cardRaw["grid"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: List(25) { 0 }
    val markedNumbers = (cardRaw["markedNumbers"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() }?.toSet() ?: emptySet()
    return BingoPlayer(
        userId = this["userId"] as? String ?: "",
        displayName = this["displayName"] as? String ?: "",
        avatarUrl = this["avatarUrl"] as? String ?: "",
        card = BingoCard(grid = flatGrid.chunked(5), markedNumbers = markedNumbers),
        hasBingo = this["hasBingo"] as? Boolean ?: false
    )
}

private fun BingoPlayer.toMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "displayName" to displayName,
    "avatarUrl" to avatarUrl,
    "hasBingo" to hasBingo,
    "card" to mapOf(
        "grid" to card.grid.flatten(),
        "markedNumbers" to card.markedNumbers.toList()
    )
)

// ── Card generator ─────────────────────────────────────────────────────────

private object BingoCardGenerator {
    fun generate(): BingoCard {
        val cols = listOf(
            (1..15).shuffled().take(5),
            (16..30).shuffled().take(5),
            (31..45).shuffled().take(5),
            (46..60).shuffled().take(5),
            (61..75).shuffled().take(5)
        )
        val grid = (0 until 5).map { row -> cols.map { col -> col[row] } }.toMutableList()
        val mutableGrid = grid.map { it.toMutableList() }.toMutableList()
        mutableGrid[2][2] = 0
        return BingoCard(grid = mutableGrid, markedNumbers = setOf(0))
    }
}
