package com.bestfriends.beachbingo.feature.brandung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Data models ──────────────────────────────────────────────────────────────

private val BrandungTeal = Color(0xFF0D9488)

data class BrandungCard(val rank: String, val suit: String)

data class BrandungPlayerLocal(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val hand: List<BrandungCard>,
    val lives: Int = 3,
    val eliminated: Boolean = false,
    val isAI: Boolean = false,
)

data class LocalBrandungState(
    val players: List<BrandungPlayerLocal>,
    val tableCards: List<BrandungCard>,
    val deck: List<BrandungCard>,
    val currentTurnIndex: Int,
    val knockedBy: String?,
    val knockRoundRemaining: List<String>,
    val passCount: Int,
    val round: Int,
    val phase: String,
    val roundScores: Map<String, Float>,
    val roundLosers: List<String>,
    val winnerId: String?,
    val lastActionText: String,
    val aiThinking: Boolean = false,
    val showRoundEnd: Boolean = false,
)

data class BrandungOnlineSettings(val newCardsOnAllPass: Boolean, val passingForbidden: Boolean)

data class OnlineBrandungState(
    val adminId: String,
    val playerIds: List<String>,
    val players: Map<String, BrandungPlayerLocal>,
    val tableCards: List<BrandungCard>,
    val deck: List<BrandungCard>,
    val currentTurnIndex: Int,
    val knockedBy: String?,
    val knockRoundRemaining: List<String>,
    val passCount: Int,
    val round: Int,
    val phase: String,
    val lastAction: String,
    val roundScores: Map<String, Float>,
    val roundLosers: List<String>,
    val winnerId: String?,
    val settings: BrandungOnlineSettings,
)

// ── Game logic ────────────────────────────────────────────────────────────────

private val SUITS = listOf("♣", "♠", "♥", "♦")
private val RANKS = listOf("7", "8", "9", "10", "J", "Q", "K", "A")
private val RED_SUITS = setOf("♥", "♦")

private fun cardValue(rank: String): Int = when (rank) {
    "A" -> 11
    "10", "J", "Q", "K" -> 10
    "9" -> 9
    "8" -> 8
    "7" -> 7
    else -> 0
}

private fun createDeck(): List<BrandungCard> {
    val deck = SUITS.flatMap { suit -> RANKS.map { rank -> BrandungCard(rank, suit) } }.toMutableList()
    deck.shuffle()
    return deck
}

private fun calcScore(hand: List<BrandungCard>): Float {
    if (hand.size != 3) return 0f
    if (hand[0].rank == hand[1].rank && hand[1].rank == hand[2].rank) return 30.5f
    val bySuit = hand.groupBy { it.suit }
    val maxSuitScore = bySuit.values.maxOf { cards -> cards.sumOf { card -> cardValue(card.rank) }.toFloat() }
    return maxSuitScore
}

private fun isFeuerBlitz(hand: List<BrandungCard>): Boolean =
    hand.size == 3 && hand.all { it.rank == "A" }

private fun bestAiMove(
    hand: List<BrandungCard>,
    tableCards: List<BrandungCard>,
    difficulty: String,
    passCount: Int,
    knockedBy: String?,
    passForbidden: Boolean,
): String {
    if (difficulty == "ROOKIE") {
        val actions = mutableListOf("swap1", "swap3")
        if (!passForbidden && knockedBy == null) actions.add("pass")
        if (knockedBy == null) actions.add("knock")
        return actions.random()
    }
    val myScore = calcScore(hand)
    if (myScore >= 29f) {
        return if (knockedBy == null) "knock" else "pass"
    }
    var bestSingleScore = myScore
    for (hi in hand.indices) {
        for (ti in tableCards.indices) {
            val newHand = hand.toMutableList()
            newHand[hi] = tableCards[ti]
            val s = calcScore(newHand)
            if (s > bestSingleScore) bestSingleScore = s
        }
    }
    val newHandAll = tableCards.toList()
    val allSwapScore = if (newHandAll.size == 3) calcScore(newHandAll) else 0f

    return when {
        allSwapScore > myScore && allSwapScore > bestSingleScore -> "swap3"
        bestSingleScore > myScore -> "swap1"
        myScore >= 25f && knockedBy == null -> "knock"
        !passForbidden && knockedBy == null -> "pass"
        else -> "swap1"
    }
}

private fun findBestSwapIndices(hand: List<BrandungCard>, tableCards: List<BrandungCard>): Pair<Int, Int> {
    var bestHandIdx = 0
    var bestTableIdx = 0
    var bestScore = calcScore(hand)
    for (hi in hand.indices) {
        for (ti in tableCards.indices) {
            val newHand = hand.toMutableList()
            newHand[hi] = tableCards[ti]
            val s = calcScore(newHand)
            if (s > bestScore) {
                bestScore = s
                bestHandIdx = hi
                bestTableIdx = ti
            }
        }
    }
    return Pair(bestHandIdx, bestTableIdx)
}

private fun dealNewRound(
    players: List<BrandungPlayerLocal>,
): Pair<List<BrandungPlayerLocal>, Pair<List<BrandungCard>, List<BrandungCard>>> {
    var deck = createDeck().toMutableList()
    val newPlayers = players.map { p ->
        if (p.eliminated) p
        else {
            val h = listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
            p.copy(hand = h)
        }
    }
    val table = listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
    return Pair(newPlayers, Pair(table, deck.toList()))
}

private fun doSwap1(state: LocalBrandungState, playerIdx: Int, handIdx: Int, tableIdx: Int): LocalBrandungState {
    val player = state.players[playerIdx]
    val oldCard = player.hand[handIdx]
    val newCard = state.tableCards[tableIdx]
    val newHand = player.hand.toMutableList().also { it[handIdx] = newCard }
    val newTable = state.tableCards.toMutableList().also { it[tableIdx] = oldCard }
    val newPlayers = state.players.toMutableList().also {
        it[playerIdx] = player.copy(hand = newHand)
    }
    val nextTurn = nextPlayerIndex(state, playerIdx)
    return state.copy(
        players = newPlayers,
        tableCards = newTable,
        currentTurnIndex = nextTurn,
        lastActionText = "${player.displayName} tauscht 1 Karte",
    ).checkKnockRound(playerIdx)
}

private fun doSwap3(state: LocalBrandungState, playerIdx: Int): LocalBrandungState {
    val player = state.players[playerIdx]
    val newTable = player.hand
    val newPlayers = state.players.toMutableList().also {
        it[playerIdx] = player.copy(hand = state.tableCards)
    }
    val nextTurn = nextPlayerIndex(state, playerIdx)
    return state.copy(
        players = newPlayers,
        tableCards = newTable,
        currentTurnIndex = nextTurn,
        lastActionText = "${player.displayName} tauscht alle 3 Karten",
        passCount = 0,
    ).checkKnockRound(playerIdx)
}

private fun doPass(state: LocalBrandungState, playerIdx: Int, newCardsOnAllPass: Boolean): LocalBrandungState {
    val player = state.players[playerIdx]
    val nextTurn = nextPlayerIndex(state, playerIdx)
    val newPassCount = state.passCount + 1
    val activePlayers = state.players.count { !it.eliminated }
    if (newCardsOnAllPass && newPassCount >= activePlayers) {
        val newDeck = state.deck.toMutableList()
        val newTable = if (newDeck.size >= 3) {
            listOf(newDeck.removeAt(0), newDeck.removeAt(0), newDeck.removeAt(0))
        } else {
            state.tableCards
        }
        return state.copy(
            tableCards = newTable,
            deck = newDeck,
            currentTurnIndex = nextTurn,
            passCount = 0,
            lastActionText = "Alle haben geschoben – neue Tischkarten!",
        ).checkKnockRound(playerIdx)
    }
    return state.copy(
        currentTurnIndex = nextTurn,
        passCount = newPassCount,
        lastActionText = "${player.displayName} schiebt",
    ).checkKnockRound(playerIdx)
}

private fun doKnock(state: LocalBrandungState, playerIdx: Int): LocalBrandungState {
    val player = state.players[playerIdx]
    val activePlayers = state.players.filter { !it.eliminated }
    val knockRound = activePlayers.map { it.userId }.filter { it != player.userId }
    val nextTurn = nextPlayerIndex(state, playerIdx)
    return state.copy(
        knockedBy = player.userId,
        knockRoundRemaining = knockRound,
        currentTurnIndex = nextTurn,
        lastActionText = "${player.displayName} klopft! Alle haben noch 1 Zug.",
    )
}

private fun LocalBrandungState.checkKnockRound(justPlayedIdx: Int): LocalBrandungState {
    if (knockedBy == null) return this
    val player = players[justPlayedIdx]
    val remaining = knockRoundRemaining.filter { it != player.userId }
    if (remaining.isEmpty()) {
        return endRound()
    }
    return copy(knockRoundRemaining = remaining)
}

private fun LocalBrandungState.endRound(): LocalBrandungState {
    val activePlayers = players.filter { !it.eliminated }
    val feuerBlitzPlayer = activePlayers.find { isFeuerBlitz(it.hand) }
    if (feuerBlitzPlayer != null) {
        val scores = activePlayers.associate { it.userId to calcScore(it.hand) }
        val newPlayers = players.map { p ->
            if (p.eliminated || p.userId == feuerBlitzPlayer.userId) p
            else p.copy(lives = p.lives - 1, eliminated = p.lives - 1 <= 0)
        }
        val losers = newPlayers.filter { !it.eliminated && it.lives < players.find { pl -> pl.userId == it.userId }!!.lives }
            .map { it.userId }
        val survivors = newPlayers.filter { !it.eliminated }
        if (survivors.size <= 1) {
            return copy(
                players = newPlayers,
                phase = "GAME_OVER",
                winnerId = survivors.firstOrNull()?.userId,
                roundScores = scores,
                roundLosers = losers,
                showRoundEnd = true,
                lastActionText = "🔥 FEUER/BLITZ! ${feuerBlitzPlayer.displayName} hat 3 Asse!",
            )
        }
        return copy(
            players = newPlayers,
            phase = "ROUND_END",
            roundScores = scores,
            roundLosers = losers,
            showRoundEnd = true,
            lastActionText = "🔥 FEUER/BLITZ! ${feuerBlitzPlayer.displayName} hat 3 Asse!",
        )
    }

    val scores = activePlayers.associate { it.userId to calcScore(it.hand) }
    val minScore = scores.values.minOrNull() ?: 0f
    val loserIds = scores.filter { it.value == minScore }.keys.toList()
    val newPlayers = players.map { p ->
        if (p.eliminated || p.userId !in loserIds) p
        else p.copy(lives = p.lives - 1, eliminated = p.lives - 1 <= 0)
    }
    val survivors = newPlayers.filter { !it.eliminated }
    if (survivors.size <= 1) {
        return copy(
            players = newPlayers,
            phase = "GAME_OVER",
            winnerId = survivors.firstOrNull()?.userId,
            roundScores = scores,
            roundLosers = loserIds,
            showRoundEnd = true,
        )
    }
    return copy(
        players = newPlayers,
        phase = "ROUND_END",
        roundScores = scores,
        roundLosers = loserIds,
        showRoundEnd = true,
    )
}

private fun nextPlayerIndex(state: LocalBrandungState, currentIdx: Int): Int {
    val count = state.players.size
    var next = (currentIdx + 1) % count
    var safety = 0
    while (state.players[next].eliminated && safety < count) {
        next = (next + 1) % count
        safety++
    }
    return next
}

private fun startNewRound(state: LocalBrandungState): LocalBrandungState {
    val (newPlayers, tableAndDeck) = dealNewRound(state.players)
    val (newTable, newDeck) = tableAndDeck
    val firstIdx = newPlayers.indexOfFirst { !it.eliminated }.coerceAtLeast(0)
    return LocalBrandungState(
        players = newPlayers,
        tableCards = newTable,
        deck = newDeck,
        currentTurnIndex = firstIdx,
        knockedBy = null,
        knockRoundRemaining = emptyList(),
        passCount = 0,
        round = state.round + 1,
        phase = "TURN",
        roundScores = emptyMap(),
        roundLosers = emptyList(),
        winnerId = null,
        lastActionText = "Runde ${state.round + 1} beginnt!",
        aiThinking = false,
        showRoundEnd = false,
    )
}

// ── Online helpers ────────────────────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
private fun parseOnlineState(data: Map<String, Any>): OnlineBrandungState {
    val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    val playersRaw = data["players"]
    val playersMap: Map<String, BrandungPlayerLocal> = when (playersRaw) {
        is Map<*, *> -> (playersRaw as Map<String, Map<String, Any>>).mapValues { (_, p) ->
            val handRaw = (p["hand"] as? List<*>) ?: emptyList<Any?>()
            BrandungPlayerLocal(
                userId = p["userId"] as? String ?: "",
                displayName = p["displayName"] as? String ?: "",
                avatarUrl = p["avatarUrl"] as? String ?: "🏄",
                hand = handRaw.mapNotNull { c ->
                    val cm = c as? Map<*, *> ?: return@mapNotNull null
                    BrandungCard(cm["rank"] as? String ?: return@mapNotNull null, cm["suit"] as? String ?: return@mapNotNull null)
                },
                lives = ((p["lives"] as? Long) ?: 3L).toInt(),
                eliminated = p["eliminated"] as? Boolean ?: false,
            )
        }
        is List<*> -> (playersRaw as List<Map<String, Any>>).associate { p ->
            val pid = p["userId"] as? String ?: ""
            val handRaw = (p["hand"] as? List<*>) ?: emptyList<Any?>()
            pid to BrandungPlayerLocal(
                userId = pid,
                displayName = p["displayName"] as? String ?: "",
                avatarUrl = p["avatarUrl"] as? String ?: "🏄",
                hand = handRaw.mapNotNull { c ->
                    val cm = c as? Map<*, *> ?: return@mapNotNull null
                    BrandungCard(cm["rank"] as? String ?: return@mapNotNull null, cm["suit"] as? String ?: return@mapNotNull null)
                },
                lives = ((p["lives"] as? Long) ?: 3L).toInt(),
                eliminated = p["eliminated"] as? Boolean ?: false,
            )
        }
        else -> emptyMap()
    }

    val tableCards = ((data["tableCards"] as? List<*>) ?: emptyList<Any?>()).mapNotNull { c ->
        val cm = c as? Map<*, *> ?: return@mapNotNull null
        BrandungCard(cm["rank"] as? String ?: return@mapNotNull null, cm["suit"] as? String ?: return@mapNotNull null)
    }
    val deck = ((data["deck"] as? List<*>) ?: emptyList<Any?>()).mapNotNull { c ->
        val cm = c as? Map<*, *> ?: return@mapNotNull null
        BrandungCard(cm["rank"] as? String ?: return@mapNotNull null, cm["suit"] as? String ?: return@mapNotNull null)
    }
    val settingsRaw = (data["settings"] as? Map<*, *>) ?: emptyMap<String, Any>()
    val settings = BrandungOnlineSettings(
        newCardsOnAllPass = settingsRaw["newCardsOnAllPass"] as? Boolean ?: false,
        passingForbidden = settingsRaw["passingForbidden"] as? Boolean ?: false,
    )
    val roundScoresRaw = (data["roundScores"] as? Map<*, *>) ?: emptyMap<String, Any>()
    val roundScores = roundScoresRaw.entries.associate { (k, v) ->
        (k as? String ?: "") to ((v as? Double)?.toFloat() ?: (v as? Long)?.toFloat() ?: 0f)
    }
    return OnlineBrandungState(
        adminId = data["adminId"] as? String ?: "",
        playerIds = playerIds,
        players = playersMap,
        tableCards = tableCards,
        deck = deck,
        currentTurnIndex = ((data["currentTurnIndex"] as? Long) ?: 0L).toInt(),
        knockedBy = data["knockedByUserId"] as? String,
        knockRoundRemaining = ((data["knockRoundRemaining"] as? List<*>) ?: emptyList<Any?>()).filterIsInstance<String>(),
        passCount = ((data["passCount"] as? Long) ?: 0L).toInt(),
        round = ((data["round"] as? Long) ?: 1L).toInt(),
        phase = data["phase"] as? String ?: "",
        lastAction = data["lastAction"] as? String ?: "",
        roundScores = roundScores,
        roundLosers = ((data["roundLosers"] as? List<*>) ?: emptyList<Any?>()).filterIsInstance<String>(),
        winnerId = data["winnerId"] as? String,
        settings = settings,
    )
}

private fun nextOnlinePlayerIndex(os: OnlineBrandungState, currentIdx: Int): Int {
    val count = os.playerIds.size
    if (count == 0) return 0
    var next = (currentIdx + 1) % count
    var safety = 0
    while (safety < count) {
        val pid = os.playerIds.getOrNull(next) ?: break
        if (!(os.players[pid]?.eliminated ?: false)) break
        next = (next + 1) % count
        safety++
    }
    return next
}

@Suppress("UNCHECKED_CAST")
private suspend fun initOnlineGame(db: FirebaseFirestore, gameId: String) {
    try {
        val snap = db.collection("brandungGames").document(gameId).get().await()
        val data = snap.data ?: return
        if ((data["phase"] as? String).isNullOrBlank().not()) return

        val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: return
        val playersRaw = data["players"]
        val playerDataByPid: Map<String, Map<*, *>> = when (playersRaw) {
            is Map<*, *> -> (playersRaw as Map<String, Map<String, Any>>)
            is List<*> -> (playersRaw as List<Map<String, Any>>).associateBy { it["userId"] as? String ?: "" }
            else -> emptyMap()
        }

        var deck = createDeck().toMutableList()
        val newPlayersMap = HashMap<String, Any>()
        for (pid in playerIds) {
            val pd = playerDataByPid[pid] ?: emptyMap<String, Any>()
            val hand = if (deck.size >= 3) {
                listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
            } else emptyList()
            newPlayersMap[pid] = mapOf(
                "userId" to pid,
                "displayName" to (pd["displayName"] as? String ?: "Spieler"),
                "avatarUrl" to (pd["avatarUrl"] as? String ?: "🏄"),
                "hand" to hand.map { mapOf("rank" to it.rank, "suit" to it.suit) },
                "lives" to 3L,
                "eliminated" to false,
                "knocked" to false,
            )
        }
        val tableCards = if (deck.size >= 3) {
            listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
        } else emptyList()

        val updates = HashMap<String, Any?>()
        updates["players"] = newPlayersMap
        updates["tableCards"] = tableCards.map { mapOf("rank" to it.rank, "suit" to it.suit) }
        updates["deck"] = deck.map { mapOf("rank" to it.rank, "suit" to it.suit) }
        updates["currentTurnIndex"] = 0L
        updates["passCount"] = 0L
        updates["round"] = 1L
        updates["phase"] = "TURN"
        updates["lastAction"] = "Runde 1 beginnt!"
        updates["settings"] = mapOf("newCardsOnAllPass" to false, "passingForbidden" to false)
        updates["roundScores"] = emptyMap<String, Double>()
        updates["roundLosers"] = emptyList<String>()
        updates["knockRoundRemaining"] = emptyList<String>()
        updates["knockedByUserId"] = null

        db.collection("brandungGames").document(gameId)
            .update(updates as Map<String, Any>)
            .await()
    } catch (_: Exception) {}
}

@Suppress("UNCHECKED_CAST")
private suspend fun resolveOnlineRound(db: FirebaseFirestore, gameId: String, uid: String) {
    delay(2500L)
    try {
        val snap = db.collection("brandungGames").document(gameId).get().await()
        val data = snap.data ?: return
        val os = parseOnlineState(data)
        if (os.phase != "ROUND_END") return

        val activePlayers = os.players.values.filter { !it.eliminated }
        val feuerPlayer = activePlayers.find { isFeuerBlitz(it.hand) }

        val scores: Map<String, Float>
        val loserIds: List<String>
        if (feuerPlayer != null) {
            scores = activePlayers.associate { it.userId to calcScore(it.hand) }
            loserIds = activePlayers.filter { it.userId != feuerPlayer.userId }.map { it.userId }
        } else {
            scores = activePlayers.associate { it.userId to calcScore(it.hand) }
            val minScore = scores.values.minOrNull() ?: 0f
            loserIds = scores.filter { it.value == minScore }.keys.toList()
        }

        val updates = HashMap<String, Any?>()
        updates["roundScores"] = scores.mapValues { it.value.toDouble() }
        updates["roundLosers"] = loserIds

        val eliminatedAfter = mutableSetOf<String>()
        for ((pid, player) in os.players) {
            if (player.eliminated) {
                eliminatedAfter.add(pid)
            } else {
                val newLife = if (pid in loserIds) player.lives - 1 else player.lives
                updates["players.$pid.lives"] = newLife.toLong()
                if (newLife <= 0) {
                    updates["players.$pid.eliminated"] = true
                    eliminatedAfter.add(pid)
                }
            }
        }

        val survivorIds = os.playerIds.filter { it !in eliminatedAfter }

        if (survivorIds.size <= 1) {
            updates["phase"] = "GAME_OVER"
            updates["winnerId"] = survivorIds.firstOrNull()
            updates["status"] = "FINISHED"

            db.collection("brandungGames").document(gameId)
                .update(updates as Map<String, Any>)
                .await()

            db.collection("brandungResults").add(
                mapOf(
                    "playerIds" to os.playerIds,
                    "winnerId" to (survivorIds.firstOrNull() ?: ""),
                    "rounds" to os.round.toLong(),
                    "mode" to "online",
                    "createdAt" to System.currentTimeMillis(),
                )
            ).await()
        } else {
            var deck = createDeck().toMutableList()
            val tableCards = if (deck.size >= 3) {
                listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
            } else emptyList()

            for (pid in os.playerIds) {
                if (pid !in eliminatedAfter && deck.size >= 3) {
                    val hand = listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
                    updates["players.$pid.hand"] = hand.map { mapOf("rank" to it.rank, "suit" to it.suit) }
                    updates["players.$pid.knocked"] = false
                }
            }

            updates["tableCards"] = tableCards.map { mapOf("rank" to it.rank, "suit" to it.suit) }
            updates["deck"] = deck.map { mapOf("rank" to it.rank, "suit" to it.suit) }
            updates["phase"] = "TURN"
            updates["knockedByUserId"] = null
            updates["knockRoundRemaining"] = emptyList<String>()
            updates["passCount"] = 0L
            updates["currentTurnIndex"] = 0L
            updates["round"] = (os.round + 1).toLong()
            updates["lastAction"] = "Runde ${os.round + 1} beginnt!"

            db.collection("brandungGames").document(gameId)
                .update(updates as Map<String, Any>)
                .await()
        }
    } catch (_: Exception) {}
}

@Suppress("UNCHECKED_CAST")
private suspend fun executeOnlineSwap1(
    db: FirebaseFirestore,
    gameId: String,
    uid: String,
    os: OnlineBrandungState,
    handIdx: Int,
    tableIdx: Int,
) {
    val player = os.players[uid] ?: return
    if (handIdx >= player.hand.size || tableIdx >= os.tableCards.size) return
    val oldCard = player.hand[handIdx]
    val newCard = os.tableCards[tableIdx]
    val newHand = player.hand.toMutableList().also { it[handIdx] = newCard }
    val newTable = os.tableCards.toMutableList().also { it[tableIdx] = oldCard }
    val nextIdx = nextOnlinePlayerIndex(os, os.currentTurnIndex)

    val updates = HashMap<String, Any?>()
    updates["players.$uid.hand"] = newHand.map { mapOf("rank" to it.rank, "suit" to it.suit) }
    updates["tableCards"] = newTable.map { mapOf("rank" to it.rank, "suit" to it.suit) }
    updates["lastAction"] = "${player.displayName} tauscht 1 Karte"
    updates["currentTurnIndex"] = nextIdx.toLong()

    if (os.knockedBy != null) {
        val remaining = os.knockRoundRemaining.filter { it != uid }
        if (remaining.isEmpty()) {
            updates["phase"] = "ROUND_END"
            updates["knockRoundRemaining"] = emptyList<String>()
        } else {
            updates["knockRoundRemaining"] = remaining
        }
    }

    db.collection("brandungGames").document(gameId)
        .update(updates as Map<String, Any>)
        .await()
}

@Suppress("UNCHECKED_CAST")
private suspend fun executeOnlineSwap3(
    db: FirebaseFirestore,
    gameId: String,
    uid: String,
    os: OnlineBrandungState,
) {
    val player = os.players[uid] ?: return
    val newHand = os.tableCards
    val newTable = player.hand
    val nextIdx = nextOnlinePlayerIndex(os, os.currentTurnIndex)

    val updates = HashMap<String, Any?>()
    updates["players.$uid.hand"] = newHand.map { mapOf("rank" to it.rank, "suit" to it.suit) }
    updates["tableCards"] = newTable.map { mapOf("rank" to it.rank, "suit" to it.suit) }
    updates["lastAction"] = "${player.displayName} tauscht alle 3 Karten"
    updates["currentTurnIndex"] = nextIdx.toLong()
    updates["passCount"] = 0L

    if (os.knockedBy != null) {
        val remaining = os.knockRoundRemaining.filter { it != uid }
        if (remaining.isEmpty()) {
            updates["phase"] = "ROUND_END"
            updates["knockRoundRemaining"] = emptyList<String>()
        } else {
            updates["knockRoundRemaining"] = remaining
        }
    }

    db.collection("brandungGames").document(gameId)
        .update(updates as Map<String, Any>)
        .await()
}

@Suppress("UNCHECKED_CAST")
private suspend fun executeOnlinePass(
    db: FirebaseFirestore,
    gameId: String,
    uid: String,
    os: OnlineBrandungState,
) {
    if (os.knockedBy != null) return
    val player = os.players[uid] ?: return
    val nextIdx = nextOnlinePlayerIndex(os, os.currentTurnIndex)
    val activePlayers = os.players.values.filter { !it.eliminated }
    val newPassCount = os.passCount + 1

    val updates = HashMap<String, Any?>()
    updates["currentTurnIndex"] = nextIdx.toLong()

    if (os.settings.newCardsOnAllPass && newPassCount >= activePlayers.size) {
        val deck = os.deck.toMutableList()
        if (deck.size >= 3) {
            val newTable = listOf(deck.removeAt(0), deck.removeAt(0), deck.removeAt(0))
            updates["tableCards"] = newTable.map { mapOf("rank" to it.rank, "suit" to it.suit) }
            updates["deck"] = deck.map { mapOf("rank" to it.rank, "suit" to it.suit) }
            updates["passCount"] = 0L
            updates["lastAction"] = "Alle haben geschoben – neue Tischkarten!"
        } else {
            updates["passCount"] = newPassCount.toLong()
            updates["lastAction"] = "${player.displayName} schiebt"
        }
    } else {
        updates["passCount"] = newPassCount.toLong()
        updates["lastAction"] = "${player.displayName} schiebt"
    }

    db.collection("brandungGames").document(gameId)
        .update(updates as Map<String, Any>)
        .await()
}

@Suppress("UNCHECKED_CAST")
private suspend fun executeOnlineKnock(
    db: FirebaseFirestore,
    gameId: String,
    uid: String,
    os: OnlineBrandungState,
) {
    val player = os.players[uid] ?: return
    val activePlayers = os.players.values.filter { !it.eliminated }
    val knockRound = activePlayers.map { it.userId }.filter { it != uid }
    val nextIdx = nextOnlinePlayerIndex(os, os.currentTurnIndex)

    val updates = HashMap<String, Any?>()
    updates["knockedByUserId"] = uid
    updates["players.$uid.knocked"] = true
    updates["knockRoundRemaining"] = knockRound
    updates["currentTurnIndex"] = nextIdx.toLong()
    updates["lastAction"] = "${player.displayName} klopft! Alle haben noch 1 Zug."

    db.collection("brandungGames").document(gameId)
        .update(updates as Map<String, Any>)
        .await()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandungGameScreen(
    mode: String,
    gameId: String?,
    aiCount: Int,
    difficulty: String,
    onNavigateBack: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    val audio = remember { BrandungAudioManager() }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    var soundEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var newCardsOnAllPass by remember { mutableStateOf(false) }
    var passForbidden by remember { mutableStateOf(false) }

    var localState by remember { mutableStateOf<LocalBrandungState?>(null) }
    var onlineState by remember { mutableStateOf<OnlineBrandungState?>(null) }

    var selectedHandIdx by remember { mutableStateOf<Int?>(null) }
    var selectedTableIdx by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        var snd = true
        var mus = true
        try {
            val snap = db.collection("users").document(uid).get().await()
            snd = snap.getBoolean("soundEnabled") ?: true
            mus = snap.getBoolean("musicEnabled") ?: true
            soundEnabled = snd
            musicEnabled = mus
            newCardsOnAllPass = snap.getBoolean("brandungNewCardsOnAllPass") ?: false
            passForbidden = snap.getBoolean("brandungPassingForbidden") ?: false
        } catch (_: Exception) {}
        audio.startMusic(snd, mus)

        if (mode == "ai") {
            val snap = try { db.collection("users").document(uid).get().await() } catch (_: Exception) { null }
            val displayName = snap?.getString("displayName") ?: "Du"
            val avatarUrl = snap?.getString("avatarUrl") ?: "🏄"
            val humanPlayer = BrandungPlayerLocal(uid, displayName, avatarUrl, emptyList(), 3, false, false)
            val aiNames = listOf("Meereswind", "Tiefseekrabbe", "Wellenreiter", "Strandläufer", "Neptun")
            val aiEmojis = listOf("🤖", "🦀", "🏄", "🐠", "🔱")
            val aiPlayers = (0 until aiCount).map { i ->
                BrandungPlayerLocal(
                    userId = "ai_$i",
                    displayName = aiNames[i % aiNames.size],
                    avatarUrl = aiEmojis[i % aiEmojis.size],
                    hand = emptyList(),
                    lives = 3,
                    eliminated = false,
                    isAI = true,
                )
            }
            val allPlayers = listOf(humanPlayer) + aiPlayers
            val (dealtPlayers, tableAndDeck) = dealNewRound(allPlayers)
            val (table, deck) = tableAndDeck
            localState = LocalBrandungState(
                players = dealtPlayers,
                tableCards = table,
                deck = deck,
                currentTurnIndex = 0,
                knockedBy = null,
                knockRoundRemaining = emptyList(),
                passCount = 0,
                round = 1,
                phase = "TURN",
                roundScores = emptyMap(),
                roundLosers = emptyList(),
                winnerId = null,
                lastActionText = "Runde 1 beginnt!",
            )
            audio.playSound("card_deal")
        }
    }

    // Online: snapshot listener
    LaunchedEffect(gameId) {
        if (mode != "online" || gameId == null) return@LaunchedEffect
        val listener = db.collection("brandungGames").document(gameId)
            .addSnapshotListener { snap, _ ->
                val data = snap?.data ?: return@addSnapshotListener
                onlineState = parseOnlineState(data)
            }
        try { awaitCancellation() } finally { listener.remove() }
    }

    // Online: admin initializes game if not yet initialized
    val shouldInitOnline = mode == "online" && onlineState?.phase.isNullOrBlank() && onlineState?.adminId == uid
    LaunchedEffect(shouldInitOnline) {
        if (!shouldInitOnline || gameId == null) return@LaunchedEffect
        initOnlineGame(db, gameId)
    }

    // Online: admin resolves round end (key = round number, fires once per round end)
    val adminRoundEndKey = if (mode == "online" && onlineState?.phase == "ROUND_END" && onlineState?.adminId == uid)
        onlineState?.round else null
    LaunchedEffect(adminRoundEndKey) {
        if (adminRoundEndKey == null || gameId == null) return@LaunchedEffect
        resolveOnlineRound(db, gameId, uid)
    }

    // AI move logic
    val state = localState
    LaunchedEffect(state?.currentTurnIndex, state?.phase) {
        val s = localState ?: return@LaunchedEffect
        if (s.phase != "TURN") return@LaunchedEffect
        val currentPlayer = s.players.getOrNull(s.currentTurnIndex) ?: return@LaunchedEffect
        if (!currentPlayer.isAI) return@LaunchedEffect

        localState = s.copy(aiThinking = true)
        delay(1200L)
        val current = localState ?: return@LaunchedEffect
        if (current.phase != "TURN") return@LaunchedEffect
        val aiPlayer = current.players.getOrNull(current.currentTurnIndex) ?: return@LaunchedEffect
        if (!aiPlayer.isAI) return@LaunchedEffect

        val action = bestAiMove(aiPlayer.hand, current.tableCards, difficulty, current.passCount, current.knockedBy, passForbidden)
        val newState = when (action) {
            "swap3" -> doSwap3(current, current.currentTurnIndex)
            "knock" -> doKnock(current, current.currentTurnIndex)
            "pass" -> if (!passForbidden && current.knockedBy == null) doPass(current, current.currentTurnIndex, newCardsOnAllPass) else {
                val (hi, ti) = findBestSwapIndices(aiPlayer.hand, current.tableCards)
                doSwap1(current, current.currentTurnIndex, hi, ti)
            }
            else -> {
                val (hi, ti) = findBestSwapIndices(aiPlayer.hand, current.tableCards)
                doSwap1(current, current.currentTurnIndex, hi, ti)
            }
        }
        localState = newState.copy(aiThinking = false)
        selectedHandIdx = null
        selectedTableIdx = null
    }

    // Save result when AI game ends
    LaunchedEffect(state?.phase, state?.winnerId) {
        val s = localState ?: return@LaunchedEffect
        if (s.phase != "GAME_OVER" || s.winnerId == null) return@LaunchedEffect
        if (mode == "ai" && uid.isNotBlank()) {
            try {
                val resultData = mapOf(
                    "playerIds" to s.players.map { it.userId },
                    "players" to s.players.map { p ->
                        mapOf(
                            "userId" to p.userId,
                            "displayName" to p.displayName,
                            "avatarUrl" to p.avatarUrl,
                            "isAI" to p.isAI,
                        )
                    },
                    "winnerId" to s.winnerId,
                    "rounds" to s.round,
                    "mode" to "ai",
                    "difficulty" to difficulty,
                    "createdAt" to System.currentTimeMillis(),
                )
                db.collection("brandungResults").add(resultData).await()
            } catch (_: Exception) {}
        }
    }

    // Project online state to display state
    val effectivePaseForbidden = if (mode == "online") onlineState?.settings?.passingForbidden ?: false else passForbidden
    val effectiveNewCardsOnAllPass = if (mode == "online") onlineState?.settings?.newCardsOnAllPass ?: false else newCardsOnAllPass

    val currentState: LocalBrandungState? = when (mode) {
        "online" -> onlineState?.let { os ->
            LocalBrandungState(
                players = os.playerIds.mapNotNull { pid -> os.players[pid] },
                tableCards = os.tableCards,
                deck = os.deck,
                currentTurnIndex = os.currentTurnIndex,
                knockedBy = os.knockedBy,
                knockRoundRemaining = os.knockRoundRemaining,
                passCount = os.passCount,
                round = os.round,
                phase = os.phase,
                roundScores = os.roundScores,
                roundLosers = os.roundLosers,
                winnerId = os.winnerId,
                lastActionText = os.lastAction,
                aiThinking = false,
                showRoundEnd = os.phase == "ROUND_END" || os.phase == "GAME_OVER",
            )
        }
        else -> localState
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BRANDUNG", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        val s = currentState
                        if (s != null) {
                            Text("Runde ${s.round}", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        if (currentState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lade…", color = TextMuted)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Opponents ──
            val opponents = currentState.players.filter { it.userId != uid }
            if (opponents.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Gegner", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        opponents.forEach { opponent ->
                            val isCurrentTurn = currentState.players.getOrNull(currentState.currentTurnIndex)?.userId == opponent.userId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrentTurn) BrandungTeal.copy(alpha = 0.2f) else Surface2Dark,
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(opponent.avatarUrl, fontSize = 20.sp)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        opponent.displayName + if (isCurrentTurn) " ← Zug" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCurrentTurn) BrandungTeal else TextPrimary,
                                        fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    Text(
                                        "🌊".repeat(opponent.lives) + if (opponent.eliminated) " ❌" else "",
                                        fontSize = 14.sp,
                                    )
                                }
                                if (opponent.isAI && currentState.aiThinking && isCurrentTurn) {
                                    Text("…", color = BrandungTeal, fontSize = 18.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { HiddenCard() }
                                }
                            }
                        }
                    }
                }
            }

            // ── Table (middle) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface2Dark),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Tischmitte", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Stapel: ${currentState.deck.size}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentState.tableCards.forEachIndexed { idx, card ->
                            val isSelected = selectedTableIdx == idx
                            BrandungPlayingCard(
                                rank = card.rank,
                                suit = card.suit,
                                faceUp = true,
                                selected = isSelected,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable {
                                        val isMyTurn = currentState.players.getOrNull(currentState.currentTurnIndex)?.userId == uid
                                        if (isMyTurn && currentState.phase == "TURN") {
                                            if (!isSelected) audio.playSound("card_select")
                                            selectedTableIdx = if (isSelected) null else idx
                                        }
                                    },
                            )
                            if (idx < currentState.tableCards.size - 1) Spacer(Modifier.width(4.dp))
                        }
                    }
                }
            }

            // ── Human hand ──
            val humanPlayer = currentState.players.find { it.userId == uid }
            if (humanPlayer != null) {
                val isMyTurn = currentState.players.getOrNull(currentState.currentTurnIndex)?.userId == uid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMyTurn && currentState.phase == "TURN")
                            BrandungTeal.copy(alpha = 0.1f)
                        else SurfaceDark
                    ),
                    border = if (isMyTurn && currentState.phase == "TURN")
                        androidx.compose.foundation.BorderStroke(1.5.dp, BrandungTeal.copy(alpha = 0.5f))
                    else null,
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Deine Hand",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMyTurn && currentState.phase == "TURN") BrandungTeal else TextMuted,
                            )
                            val score = calcScore(humanPlayer.hand)
                            Text(
                                "${if (score == 30.5f) "30.5" else score.toInt().toString()} Pkt",
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    score >= 29 -> Success
                                    score >= 20 -> SandGold
                                    else -> TextMuted
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            humanPlayer.hand.forEachIndexed { idx, card ->
                                val isSelected = selectedHandIdx == idx
                                BrandungPlayingCard(
                                    rank = card.rank,
                                    suit = card.suit,
                                    faceUp = true,
                                    selected = isSelected,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            if (isMyTurn && currentState.phase == "TURN") {
                                                if (!isSelected) audio.playSound("card_select")
                                                selectedHandIdx = if (isSelected) null else idx
                                            }
                                        },
                                )
                                if (idx < humanPlayer.hand.size - 1) Spacer(Modifier.width(4.dp))
                            }
                        }
                        Text(
                            "🌊".repeat(humanPlayer.lives),
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }

                // ── Action buttons ──
                if (isMyTurn && currentState.phase == "TURN") {
                    val canSwap1 = selectedHandIdx != null && selectedTableIdx != null
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val hi = selectedHandIdx ?: return@Button
                                val ti = selectedTableIdx ?: return@Button
                                audio.playSound("card_place")
                                if (mode == "online") {
                                    val os = onlineState ?: return@Button
                                    scope.launch { executeOnlineSwap1(db, gameId ?: return@launch, uid, os, hi, ti) }
                                } else {
                                    localState = doSwap1(currentState, currentState.currentTurnIndex, hi, ti)
                                }
                                selectedHandIdx = null
                                selectedTableIdx = null
                            },
                            enabled = canSwap1,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandungTeal),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("1 Karte tauschen", fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    audio.playSound("card_place")
                                    if (mode == "online") {
                                        val os = onlineState ?: return@Button
                                        scope.launch { executeOnlineSwap3(db, gameId ?: return@launch, uid, os) }
                                    } else {
                                        localState = doSwap3(currentState, currentState.currentTurnIndex)
                                    }
                                    selectedHandIdx = null
                                    selectedTableIdx = null
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Alle 3 tauschen", fontWeight = FontWeight.Bold)
                            }

                            if (!effectivePaseForbidden && currentState.knockedBy == null) {
                                OutlinedButton(
                                    onClick = {
                                        if (mode == "online") {
                                            val os = onlineState ?: return@OutlinedButton
                                            scope.launch { executeOnlinePass(db, gameId ?: return@launch, uid, os) }
                                        } else {
                                            localState = doPass(currentState, currentState.currentTurnIndex, effectiveNewCardsOnAllPass)
                                        }
                                        selectedHandIdx = null
                                        selectedTableIdx = null
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                                ) {
                                    Text("Schieben")
                                }
                            }
                        }

                        if (currentState.knockedBy == null) {
                            OutlinedButton(
                                onClick = {
                                    audio.playSound("card_knock")
                                    if (mode == "online") {
                                        val os = onlineState ?: return@OutlinedButton
                                        scope.launch { executeOnlineKnock(db, gameId ?: return@launch, uid, os) }
                                    } else {
                                        localState = doKnock(currentState, currentState.currentTurnIndex)
                                    }
                                    selectedHandIdx = null
                                    selectedTableIdx = null
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, SandGold.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SandGold),
                            ) {
                                Text("Klopfen ✊", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = SandGold.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    "✊ ${currentState.players.find { it.userId == currentState.knockedBy }?.displayName} hat geklopft!",
                                    modifier = Modifier.padding(12.dp),
                                    color = SandGold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else if (currentState.phase == "TURN") {
                    val turnPlayer = currentState.players.getOrNull(currentState.currentTurnIndex)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Surface2Dark,
                    ) {
                        Text(
                            text = if (currentState.aiThinking) "🤖 ${turnPlayer?.displayName} denkt nach…"
                                   else "${turnPlayer?.displayName ?: "?"} ist dran",
                            modifier = Modifier.padding(12.dp),
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // ── Status text ──
            if (currentState.lastActionText.isNotBlank()) {
                Text(
                    currentState.lastActionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Round End Dialog ──
    val roundEndState = currentState
    if (roundEndState != null && roundEndState.showRoundEnd) {
        val humanInLosers = uid in roundEndState.roundLosers
        val feuerHuman = humanPlayer?.let { isFeuerBlitz(it.hand) } == true
        LaunchedEffect(roundEndState.round, roundEndState.phase) {
            if (feuerHuman) audio.playSound("card_feuer")
            if (humanInLosers) audio.playSound("life_lost")
            if (!humanInLosers && !feuerHuman) {
                val score = humanPlayer?.hand?.let { calcScore(it) } ?: 0f
                if (score == 31f) audio.playSound("level_complete")
            }
        }
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val isGameOver = roundEndState.phase == "GAME_OVER"
                    Text(
                        if (isGameOver) "🏆 Spiel beendet!" else "Runde ${roundEndState.round} beendet",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isGameOver) SandGold else TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )

                    HorizontalDivider(color = Surface2Dark)

                    roundEndState.players.filter { !it.eliminated || it.userId in roundEndState.roundScores.keys }.forEach { player ->
                        val score = roundEndState.roundScores[player.userId]
                        val isLoser = player.userId in roundEndState.roundLosers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${player.avatarUrl} ${player.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (score != null) {
                                    Text(
                                        "${if (score == 30.5f) "30.5" else score.toInt().toString()} Pkt",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isLoser -> Danger
                                            score >= 29 -> Success
                                            else -> TextSub
                                        },
                                        fontWeight = if (isLoser) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                Text("🌊".repeat(roundEndState.players.find { it.userId == player.userId }?.lives?.coerceAtLeast(0) ?: 0))
                                if (isLoser) Text("−1 Leben", style = MaterialTheme.typography.labelSmall, color = Danger)
                            }
                        }
                    }

                    if (isGameOver) {
                        val winner = roundEndState.players.find { it.userId == roundEndState.winnerId }
                        if (winner != null) {
                            HorizontalDivider(color = Surface2Dark)
                            Text(
                                "🏆 ${winner.avatarUrl} ${winner.displayName} gewinnt!",
                                style = MaterialTheme.typography.titleMedium,
                                color = SandGold,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandungTeal),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Zur Lobby", fontWeight = FontWeight.Bold)
                        }
                    } else if (mode == "online") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Surface2Dark,
                        ) {
                            Text(
                                "⏳ Warte auf nächste Runde…",
                                modifier = Modifier.padding(12.dp),
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                localState = startNewRound(roundEndState)
                                audio.playSound("card_deal")
                                selectedHandIdx = null
                                selectedTableIdx = null
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandungTeal),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Nächste Runde 🌊", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Card Composable ────────────────────────────────────────────────────────────

@Composable
fun BrandungPlayingCard(
    rank: String,
    suit: String,
    faceUp: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val isRed = suit in RED_SUITS
    val cardColor = if (isRed) Danger else Color(0xFF1A1A2E)
    val borderColor = if (selected) BrandungTeal else Color(0xFFDDE0E4)

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .background(if (faceUp) Color(0xFFFFFBF0) else BrandungTeal),
    ) {
        if (faceUp) {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
            ) {
                Text(
                    text = rank,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardColor,
                    lineHeight = 12.sp,
                )
                Text(
                    text = suit,
                    fontSize = 10.sp,
                    color = cardColor,
                    lineHeight = 11.sp,
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = suit,
                        fontSize = 22.sp,
                        color = cardColor,
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🌊", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun HiddenCard() {
    Box(
        modifier = Modifier
            .size(width = 28.dp, height = 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BrandungTeal.copy(alpha = 0.7f))
            .border(1.dp, BrandungTeal, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("🌊", fontSize = 10.sp)
    }
}
