package com.bestfriends.beachbingo.feature.strandraeuber.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.bestfriends.beachbingo.ui.components.GameHudBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.ui.components.CardBackScene
import com.bestfriends.beachbingo.ui.components.CardFanRow
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.BgPlayerZone
import com.bestfriends.beachbingo.ui.theme.BingoCallSize
import com.bestfriends.beachbingo.ui.theme.Crimson
import com.bestfriends.beachbingo.ui.theme.DrawNumberTablet
import com.bestfriends.beachbingo.ui.theme.SandGoldLight
import com.bestfriends.beachbingo.ui.theme.CardBorderLight
import com.bestfriends.beachbingo.ui.theme.SlateBlueDark
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.Teal
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.feature.raetsel.GameSave
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Constants ──────────────────────────────────────────────────────────────────

private val AI_NAMES = listOf("🤖 Möwe", "🤖 Krabbe", "🤖 Fisch", "🤖 Hai", "🤖 Delfin")

// ── Data Models ────────────────────────────────────────────────────────────────

@Serializable
data class SpCard(
    val id: String,
    val pairId: String,
    val emoji: String,
    val name: String,
)

@Serializable
data class SpLocalPlayer(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val hand: List<SpCard>,
    val isAI: Boolean,
)

enum class SpPhase { LOBBY, DEALING, PLAYING, AI_THINKING, PAIR_REVEAL, ROUND_END, GAME_OVER }

data class SpGameState(
    val players: List<SpLocalPlayer>,
    val activePlayerIndices: List<Int>,
    val turnIndex: Int,
    val phase: SpPhase,
    val lastPairsDiscarded: List<Pair<SpCard, SpCard>>,
    val discardedPairs: List<Pair<SpCard, SpCard>>,
    val roundScores: Map<String, Int>,
    val loserUserId: String?,
    val roundNumber: Int,
    val totalRounds: Int,
    val lastActionText: String = "",
    val showRoundEnd: Boolean = false,
)

// Serializable save state (Pair is not serializable, so we use a wrapper)
@Serializable
private data class SpCardPair(val first: SpCard, val second: SpCard)

@Serializable
private data class SpSavedState(
    val players: List<SpLocalPlayer>,
    val activePlayerIndices: List<Int>,
    val turnIndex: Int,
    val phase: String,
    val lastPairsDiscarded: List<SpCardPair>,
    val discardedPairs: List<SpCardPair>,
    val roundScores: Map<String, Int>,
    val loserUserId: String?,
    val roundNumber: Int,
    val totalRounds: Int,
    val lastActionText: String,
    val showRoundEnd: Boolean,
)

private fun SpGameState.toSavedState() = SpSavedState(
    players = players,
    activePlayerIndices = activePlayerIndices,
    turnIndex = turnIndex,
    phase = phase.name,
    lastPairsDiscarded = lastPairsDiscarded.map { SpCardPair(it.first, it.second) },
    discardedPairs = discardedPairs.map { SpCardPair(it.first, it.second) },
    roundScores = roundScores,
    loserUserId = loserUserId,
    roundNumber = roundNumber,
    totalRounds = totalRounds,
    lastActionText = lastActionText,
    showRoundEnd = showRoundEnd,
)

private fun SpSavedState.toSpGameState() = SpGameState(
    players = players,
    activePlayerIndices = activePlayerIndices,
    turnIndex = turnIndex,
    phase = try { SpPhase.valueOf(phase) } catch (_: Exception) { SpPhase.PLAYING },
    lastPairsDiscarded = lastPairsDiscarded.map { Pair(it.first, it.second) },
    discardedPairs = discardedPairs.map { Pair(it.first, it.second) },
    roundScores = roundScores,
    loserUserId = loserUserId,
    roundNumber = roundNumber,
    totalRounds = totalRounds,
    lastActionText = lastActionText,
    showRoundEnd = showRoundEnd,
)

// ── Card deck ──────────────────────────────────────────────────────────────────

private val ALL_SP_CARDS: List<SpCard> = buildList {
    val pairs = listOf(
        Triple("krabbe",       "🦀", "Krabbe"),
        Triple("muschel",      "🐚", "Muschel"),
        Triple("fisch",        "🐟", "Fisch"),
        Triple("hai",          "🦈", "Hai"),
        Triple("delfin",       "🐬", "Delfin"),
        Triple("oktopus",      "🐙", "Oktopus"),
        Triple("robbe",        "🦭", "Robbe"),
        Triple("schildkroete", "🐢", "Schildkröte"),
        Triple("welle",        "🌊", "Welle"),
        Triple("surfer",       "🏄", "Surfer"),
        Triple("palme",        "🌴", "Palme"),
        Triple("sonne",        "☀️", "Sonne"),
        Triple("softeis",      "🍦", "Softeis"),
        Triple("cocktail",     "🍹", "Cocktail"),
        Triple("sonnenbrille", "🕶️", "Sonnenbrille"),
        Triple("segelboot",    "⛵", "Segelboot"),
        Triple("koralle",      "🪸", "Koralle"),
        Triple("hummer",       "🦞", "Hummer"),
    )
    for ((pairId, emoji, name) in pairs) {
        add(SpCard("${pairId}_1", pairId, emoji, name))
        add(SpCard("${pairId}_2", pairId, emoji, name))
    }
    add(SpCard("strandraeuber", "strandraeuber", "🦹", "Strandräuber"))
}

// ── Game logic ─────────────────────────────────────────────────────────────────

private fun removePairsAndTrack(hand: List<SpCard>): Pair<List<SpCard>, List<Pair<SpCard, SpCard>>> {
    val grouped = hand.groupBy { it.pairId }
    val pairedPairIds = grouped.filter { it.value.size >= 2 }.keys.toSet()
    val remaining = hand.filter { it.pairId !in pairedPairIds }
    val newPairs = pairedPairIds.mapNotNull { pairId ->
        val cards = grouped[pairId] ?: return@mapNotNull null
        if (cards.size >= 2) Pair(cards[0], cards[1]) else null
    }
    return Pair(remaining, newPairs)
}

private fun initGame(
    aiCount: Int,
    uid: String,
    displayName: String,
    avatarUrl: String,
    totalRounds: Int,
): SpGameState {
    val humanPlayer = SpLocalPlayer(uid, displayName, avatarUrl, emptyList(), false)
    val aiPlayers = (0 until aiCount).map { i ->
        SpLocalPlayer("ai_$i", AI_NAMES[i % AI_NAMES.size], AI_NAMES[i % AI_NAMES.size], emptyList(), true)
    }
    val allPlayers = listOf(humanPlayer) + aiPlayers

    val deck = ALL_SP_CARDS.toMutableList().also { it.shuffle() }
    val rawHands: Array<MutableList<SpCard>> = Array(allPlayers.size) { mutableListOf() }
    deck.forEachIndexed { i, card -> rawHands[i % allPlayers.size].add(card) }

    val discardedPairs = mutableListOf<Pair<SpCard, SpCard>>()
    val playersWithHands = allPlayers.mapIndexed { i, player ->
        val (cleanHand, pairs) = removePairsAndTrack(rawHands[i].toList())
        discardedPairs.addAll(pairs)
        player.copy(hand = cleanHand)
    }

    // Remove players who already have 0 cards after initial pair removal
    val initialActive = playersWithHands.indices.filter { playersWithHands[it].hand.isNotEmpty() }
    // Draw order: AIs first, human last (player to right of admin starts)
    val activeIndices = (initialActive.filter { it != 0 } + initialActive.filter { it == 0 })

    return SpGameState(
        players = playersWithHands,
        activePlayerIndices = activeIndices,
        turnIndex = 0,
        phase = SpPhase.PLAYING,
        lastPairsDiscarded = emptyList(),
        discardedPairs = discardedPairs,
        roundScores = allPlayers.associate { it.userId to 0 },
        loserUserId = null,
        roundNumber = 1,
        totalRounds = totalRounds,
        lastActionText = "Runde 1 beginnt! Paare wurden abgelegt.",
    )
}

private fun executeDrawCard(
    state: SpGameState,
    cardIndexInTarget: Int,
): SpGameState {
    val activeCount = state.activePlayerIndices.size
    if (activeCount < 2) return state
    val drawerActiveIdx = state.turnIndex
    val targetActiveIdx = (state.turnIndex - 1 + activeCount) % activeCount

    val drawerPlayerIdx = state.activePlayerIndices[drawerActiveIdx]
    val targetPlayerIdx = state.activePlayerIndices[targetActiveIdx]

    val drawer = state.players[drawerPlayerIdx]
    val target = state.players[targetPlayerIdx]
    if (target.hand.isEmpty()) return state

    val safeIdx = cardIndexInTarget.coerceIn(0, target.hand.size - 1)
    val drawnCard = target.hand[safeIdx]
    val newTargetHand = target.hand.toMutableList().also { it.removeAt(safeIdx) }
    val handAfterDraw = drawer.hand + drawnCard
    val (newDrawerHand, newPairs) = removePairsAndTrack(handAfterDraw)

    val newPlayers = state.players.toMutableList()
    newPlayers[drawerPlayerIdx] = drawer.copy(hand = newDrawerHand)
    newPlayers[targetPlayerIdx] = target.copy(hand = newTargetHand)

    val newDiscarded = state.discardedPairs + newPairs
    val newLastPairs = newPairs

    // Compute new active indices (remove players with 0 cards)
    val newActiveIndices = state.activePlayerIndices.filter { idx ->
        newPlayers[idx].hand.isNotEmpty()
    }

    val actionText = when {
        newPairs.isNotEmpty() -> "${drawer.displayName} zieht ${drawnCard.emoji} — Paar abgelegt! ${newPairs.joinToString { "${it.first.emoji}" }}"
        else -> "${drawer.displayName} zieht ${drawnCard.emoji} von ${target.displayName}"
    }

    if (newActiveIndices.size <= 1) {
        val loserId = newActiveIndices.firstOrNull()?.let { newPlayers[it].userId }
        val newScores = state.roundScores.toMutableMap()
        if (loserId != null) {
            newScores[loserId] = (newScores[loserId] ?: 0) + 1
        }

        // Check if more rounds remain
        val isLastRound = state.roundNumber >= state.totalRounds
        return state.copy(
            players = newPlayers,
            activePlayerIndices = newActiveIndices,
            phase = if (isLastRound) SpPhase.GAME_OVER else SpPhase.ROUND_END,
            lastPairsDiscarded = newLastPairs,
            discardedPairs = newDiscarded,
            roundScores = newScores,
            loserUserId = loserId,
            lastActionText = actionText,
            showRoundEnd = true,
        )
    }

    // Advance turn: next player after current drawer in new active list
    val nextTurnIndex = computeNextTurnIndex(state.activePlayerIndices, newActiveIndices, drawerActiveIdx)

    return state.copy(
        players = newPlayers,
        activePlayerIndices = newActiveIndices,
        turnIndex = nextTurnIndex,
        phase = SpPhase.PLAYING,
        lastPairsDiscarded = newLastPairs,
        discardedPairs = newDiscarded,
        lastActionText = actionText,
    )
}

private fun computeNextTurnIndex(
    oldIndices: List<Int>,
    newIndices: List<Int>,
    oldDrawerIdx: Int,
): Int {
    if (newIndices.isEmpty()) return 0
    val drawerPlayerIdx = oldIndices.getOrNull(oldDrawerIdx) ?: return 0
    val drawerNewPos = newIndices.indexOf(drawerPlayerIdx)
    if (drawerNewPos >= 0) {
        return (drawerNewPos + 1) % newIndices.size
    }
    // Drawer was eliminated: find next player after drawer in old order
    for (offset in 1..oldIndices.size) {
        val nextOldIdx = (oldDrawerIdx + offset) % oldIndices.size
        val nextPlayerIdx = oldIndices.getOrNull(nextOldIdx) ?: continue
        val nextNewPos = newIndices.indexOf(nextPlayerIdx)
        if (nextNewPos >= 0) return nextNewPos
    }
    return 0
}

private fun shuffleTargetHand(state: SpGameState, targetPlayerIdx: Int): SpGameState {
    val newPlayers = state.players.toMutableList()
    val target = newPlayers[targetPlayerIdx]
    newPlayers[targetPlayerIdx] = target.copy(hand = target.hand.shuffled())
    return state.copy(players = newPlayers)
}

private fun startNewRound(state: SpGameState): SpGameState {
    val deck = ALL_SP_CARDS.toMutableList().also { it.shuffle() }
    val playerCount = state.players.size
    val rawHands: Array<MutableList<SpCard>> = Array(playerCount) { mutableListOf() }
    deck.forEachIndexed { i, card -> rawHands[i % playerCount].add(card) }

    val discardedPairs = mutableListOf<Pair<SpCard, SpCard>>()
    val newPlayers = state.players.mapIndexed { i, player ->
        val (cleanHand, pairs) = removePairsAndTrack(rawHands[i].toList())
        discardedPairs.addAll(pairs)
        player.copy(hand = cleanHand)
    }

    val newActiveIndices = newPlayers.indices.filter { newPlayers[it].hand.isNotEmpty() }
    val activeOrdered = (newActiveIndices.filter { it != 0 } + newActiveIndices.filter { it == 0 })

    return state.copy(
        players = newPlayers,
        activePlayerIndices = activeOrdered,
        turnIndex = 0,
        phase = SpPhase.PLAYING,
        lastPairsDiscarded = emptyList(),
        discardedPairs = discardedPairs,
        loserUserId = null,
        roundNumber = state.roundNumber + 1,
        lastActionText = "Runde ${state.roundNumber + 1} beginnt!",
        showRoundEnd = false,
    )
}

// ── Online helpers ────────────────────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
private fun parseOnlineSpCard(raw: Any?): SpCard? {
    val m = raw as? Map<*, *> ?: return null
    return SpCard(
        id = m["id"] as? String ?: return null,
        pairId = m["pairId"] as? String ?: return null,
        emoji = m["emoji"] as? String ?: return null,
        name = m["name"] as? String ?: return null,
    )
}

@Suppress("UNCHECKED_CAST")
private fun parseOnlineGameState(data: Map<String, Any>): SpGameState? {
    val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: return null
    val activePlayerIds = (data["activePlayerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val turnIndex = ((data["turnIndex"] as? Long) ?: 0L).toInt()
    val phase = try { SpPhase.valueOf(data["phase"] as? String ?: "PLAYING") } catch (_: Exception) { SpPhase.PLAYING }
    val roundNumber = ((data["roundNumber"] as? Long) ?: 1L).toInt()
    val totalRounds = ((data["totalRounds"] as? Long) ?: 3L).toInt()
    val loserId = data["loserId"] as? String
    val scoresRaw = (data["scores"] as? Map<*, *>) ?: emptyMap<String, Any>()
    val scores = scoresRaw.entries.associate { (k, v) ->
        (k as? String ?: "") to ((v as? Long)?.toInt() ?: 0)
    }

    val playersRaw = data["players"] as? Map<*, *> ?: return null
    val playersList = playerIds.mapNotNull { pid ->
        val p = playersRaw[pid] as? Map<*, *> ?: return@mapNotNull null
        val handRaw = (p["hand"] as? List<*>) ?: emptyList<Any>()
        val parsedHand = handRaw.mapNotNull { parseOnlineSpCard(it) }
        SpLocalPlayer(
            userId = pid,
            displayName = p["displayName"] as? String ?: "",
            avatarUrl = p["avatarUrl"] as? String ?: "🦹",
            hand = parsedHand,
            isAI = false,
        )
    }

    val activeIndices = activePlayerIds.mapNotNull { aid -> playersList.indexOfFirst { it.userId == aid }.takeIf { it >= 0 } }
    val activeOrdered = activeIndices.ifEmpty { playersList.indices.toList() }

    return SpGameState(
        players = playersList,
        activePlayerIndices = activeOrdered,
        turnIndex = turnIndex % activeOrdered.size.coerceAtLeast(1),
        phase = phase,
        lastPairsDiscarded = emptyList(),
        discardedPairs = emptyList(),
        roundScores = scores,
        loserUserId = loserId,
        roundNumber = roundNumber,
        totalRounds = totalRounds,
        lastActionText = data["lastAction"] as? String ?: "",
        showRoundEnd = phase == SpPhase.ROUND_END || phase == SpPhase.GAME_OVER,
    )
}

private fun spCardToMap(card: SpCard): Map<String, String> = mapOf(
    "id" to card.id,
    "pairId" to card.pairId,
    "emoji" to card.emoji,
    "name" to card.name,
)

@Suppress("UNCHECKED_CAST")
private suspend fun initOnlineSpGame(db: FirebaseFirestore, gameId: String) {
    try {
        val snap = db.collection("strandraeuberGames").document(gameId).get().await()
        val data = snap.data ?: return
        val phase = data["phase"] as? String ?: ""
        if (phase == "LOBBY" || phase == "PLAYING" || phase == "ROUND_END" || phase == "GAME_OVER") return

        val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: return
        val playersRaw = data["players"] as? Map<*, *> ?: return
        val totalRounds = ((data["totalRounds"] as? Long) ?: 3L).toInt()

        val deck = ALL_SP_CARDS.toMutableList().also { it.shuffle() }
        val rawHands: Array<MutableList<SpCard>> = Array(playerIds.size) { mutableListOf() }
        deck.forEachIndexed { i, card -> rawHands[i % playerIds.size].add(card) }

        val updates = HashMap<String, Any?>()
        for ((i, pid) in playerIds.withIndex()) {
            val (cleanHand, _) = removePairsAndTrack(rawHands[i].toList())
            updates["players.$pid.hand"] = cleanHand.map { spCardToMap(it) }
            updates["players.$pid.cardCount"] = cleanHand.size.toLong()
        }
        // Active player IDs: AIs first (none in online), humans in order, skip 0-card players
        val activeIds = playerIds.filter { pid ->
            val p = playersRaw[pid] as? Map<*, *>
            // We don't know final hand yet, keep all for now (will be pruned after deal)
            true
        }
        updates["activePlayerIds"] = activeIds
        updates["turnIndex"] = 0L
        updates["phase"] = "PLAYING"
        updates["roundNumber"] = 1L
        updates["lastAction"] = "Runde 1 beginnt!"
        updates["status"] = "RUNNING"

        db.collection("strandraeuberGames").document(gameId)
            .update(updates as Map<String, Any>).await()
    } catch (_: Exception) {}
}

@Suppress("UNCHECKED_CAST")
private suspend fun executeOnlineDrawCard(
    db: FirebaseFirestore,
    gameId: String,
    uid: String,
    state: SpGameState,
    cardIndexInTarget: Int,
) {
    val activeCount = state.activePlayerIndices.size
    if (activeCount < 2) return
    val drawerActiveIdx = state.turnIndex
    val targetActiveIdx = (state.turnIndex - 1 + activeCount) % activeCount
    val drawerPlayerIdx = state.activePlayerIndices[drawerActiveIdx]
    val targetPlayerIdx = state.activePlayerIndices[targetActiveIdx]

    val drawer = state.players[drawerPlayerIdx]
    val target = state.players[targetPlayerIdx]
    if (target.hand.isEmpty()) return
    val safeIdx = cardIndexInTarget.coerceIn(0, target.hand.size - 1)
    val drawnCard = target.hand[safeIdx]
    val newTargetHand = target.hand.toMutableList().also { it.removeAt(safeIdx) }
    val handAfterDraw = drawer.hand + drawnCard
    val (newDrawerHand, _) = removePairsAndTrack(handAfterDraw)

    val newActiveIds = state.activePlayerIndices.mapNotNull { idx ->
        val p = state.players[idx]
        val newHand = when (idx) {
            drawerPlayerIdx -> newDrawerHand
            targetPlayerIdx -> newTargetHand
            else -> p.hand
        }
        if (newHand.isEmpty()) null else p.userId
    }

    val isLastRound = state.roundNumber >= state.totalRounds
    val newPhase = if (newActiveIds.size <= 1) {
        if (isLastRound) "GAME_OVER" else "ROUND_END"
    } else "PLAYING"

    val newActiveIndices = newActiveIds.mapNotNull { aid ->
        val idx = state.players.indexOfFirst { it.userId == aid }.takeIf { it >= 0 }
        idx
    }
    val nextTurnIndex = if (newActiveIndices.size > 1) {
        computeNextTurnIndex(state.activePlayerIndices, newActiveIndices, drawerActiveIdx)
    } else 0

    val newScores = state.roundScores.toMutableMap()
    val loserId = if (newActiveIds.size <= 1) newActiveIds.firstOrNull() else null
    if (loserId != null) {
        newScores[loserId] = (newScores[loserId] ?: 0) + 1
    }

    val updates = HashMap<String, Any?>()
    updates["players.${drawer.userId}.hand"] = newDrawerHand.map { spCardToMap(it) }
    updates["players.${drawer.userId}.cardCount"] = newDrawerHand.size.toLong()
    updates["players.${target.userId}.hand"] = newTargetHand.map { spCardToMap(it) }
    updates["players.${target.userId}.cardCount"] = newTargetHand.size.toLong()
    updates["activePlayerIds"] = newActiveIds
    updates["turnIndex"] = nextTurnIndex.toLong()
    updates["phase"] = newPhase
    updates["scores"] = newScores.mapValues { it.value.toLong() }
    updates["lastAction"] = "${drawer.displayName} zieht von ${target.displayName}"
    if (loserId != null) {
        updates["loserId"] = loserId
        updates["loserName"] = state.players.find { it.userId == loserId }?.displayName ?: ""
    }
    if (newPhase == "GAME_OVER" || newPhase == "ROUND_END") {
        updates["status"] = if (newPhase == "GAME_OVER") "FINISHED" else "RUNNING"
    }

    db.collection("strandraeuberGames").document(gameId)
        .update(updates as Map<String, Any>).await()
}

@Suppress("UNCHECKED_CAST")
private suspend fun executeOnlineNextRound(db: FirebaseFirestore, gameId: String, state: SpGameState) {
    delay(3000L)
    try {
        val deck = ALL_SP_CARDS.toMutableList().also { it.shuffle() }
        val playerCount = state.players.size
        val rawHands: Array<MutableList<SpCard>> = Array(playerCount) { mutableListOf() }
        deck.forEachIndexed { i, card -> rawHands[i % playerCount].add(card) }

        val updates = HashMap<String, Any?>()
        val newActiveIds = mutableListOf<String>()
        for ((i, player) in state.players.withIndex()) {
            val (cleanHand, _) = removePairsAndTrack(rawHands[i].toList())
            updates["players.${player.userId}.hand"] = cleanHand.map { spCardToMap(it) }
            updates["players.${player.userId}.cardCount"] = cleanHand.size.toLong()
            if (cleanHand.isNotEmpty()) newActiveIds.add(player.userId)
        }
        updates["activePlayerIds"] = newActiveIds
        updates["turnIndex"] = 0L
        updates["phase"] = "PLAYING"
        updates["roundNumber"] = (state.roundNumber + 1).toLong()
        updates["loserId"] = null
        updates["lastAction"] = "Runde ${state.roundNumber + 1} beginnt!"
        updates["status"] = "RUNNING"

        db.collection("strandraeuberGames").document(gameId)
            .update(updates as Map<String, Any>).await()
    } catch (_: Exception) {}
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun StrandraeuberGameScreen(
    mode: String,
    gameId: String?,
    aiCount: Int,
    difficulty: String,
    totalRounds: Int,
    saveId: String? = null,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val audio = remember { StrandraeuberAudioManager(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }

    var localState by remember { mutableStateOf<SpGameState?>(null) }
    var onlineRawState by remember { mutableStateOf<SpGameState?>(null) }

    var selectedCardIndex by remember { mutableIntStateOf(-1) }
    var showLoser by remember { mutableStateOf(false) }

    // ── Restore from save ──────────────────────────────────────────────────────
    LaunchedEffect(saveId) {
        if (saveId == null) return@LaunchedEffect
        val save = SoloGameSaveManager.getGameSave(context, "strandraeuber")
        if (save == null || save.id != saveId) return@LaunchedEffect
        try {
            val restored = Json.decodeFromString<SpSavedState>(save.gameState)
            localState = restored.toSpGameState().copy(
                phase = SpPhase.PLAYING,
                showRoundEnd = false,
            )
        } catch (_: Exception) {}
    }

    // Load settings and init game
    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        audio.startMusic(soundEnabled, musicEnabled)

        if (mode == "AI" && saveId != null) return@LaunchedEffect
        if (mode == "AI") {
            try {
                val snap = db.collection("users").document(uid).get().await()
                val displayName = snap.getString("displayName") ?: "Du"
                val avatarUrl = snap.getString("avatarUrl") ?: "🏄"
                localState = initGame(aiCount, uid, displayName, avatarUrl, totalRounds)
                audio.playSound("card_shuffle")
            } catch (_: Exception) {
                localState = initGame(aiCount, uid, "Du", "🏄", totalRounds)
            }
        }
    }

    // Online: snapshot listener
    LaunchedEffect(gameId) {
        if (mode != "ONLINE" || gameId == null) return@LaunchedEffect
        val listener = db.collection("strandraeuberGames").document(gameId)
            .addSnapshotListener { snap, _ ->
                val data = snap?.data ?: return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                onlineRawState = parseOnlineGameState(data as Map<String, Any>)
            }
        try { awaitCancellation() } finally { listener.remove() }
    }

    // Online: admin initializes
    val onlinePhase = onlineRawState?.phase
    val isOnlineAdmin = mode == "ONLINE" && onlineRawState?.players?.firstOrNull()?.userId == uid
    LaunchedEffect(onlinePhase) {
        if (mode != "ONLINE" || gameId == null) return@LaunchedEffect
        if (onlinePhase == SpPhase.DEALING) {
            if (isOnlineAdmin) initOnlineSpGame(db, gameId)
        }
    }

    // Online: admin handles round transition
    val roundEndKey = if (mode == "ONLINE" && onlineRawState?.phase == SpPhase.ROUND_END) onlineRawState?.roundNumber else null
    LaunchedEffect(roundEndKey) {
        if (roundEndKey == null || gameId == null || !isOnlineAdmin) return@LaunchedEffect
        val s = onlineRawState ?: return@LaunchedEffect
        executeOnlineNextRound(db, gameId, s)
    }

    var isPaused by remember { mutableStateOf(false) }

    val currentState: SpGameState? = when (mode) {
        "ONLINE" -> onlineRawState
        else -> localState
    }

    // AI move logic
    val stateForAi = localState
    LaunchedEffect(stateForAi?.turnIndex, stateForAi?.activePlayerIndices, isPaused) {
        val s = localState ?: return@LaunchedEffect
        if (s.phase != SpPhase.PLAYING) return@LaunchedEffect
        val activeCount = s.activePlayerIndices.size
        if (activeCount < 2) return@LaunchedEffect
        val drawerIdx = s.activePlayerIndices[s.turnIndex]
        val drawer = s.players.getOrNull(drawerIdx) ?: return@LaunchedEffect
        if (!drawer.isAI) return@LaunchedEffect
        if (isPaused) return@LaunchedEffect

        localState = s.copy(phase = SpPhase.AI_THINKING)

        val delayMs = when (difficulty) {
            "ROOKIE" -> 1500L
            "BOSS_LEVEL" -> 1000L
            else -> 1200L
        }
        delay(delayMs)

        val current = localState ?: return@LaunchedEffect
        if (current.phase != SpPhase.AI_THINKING) return@LaunchedEffect

        val activeCountNow = current.activePlayerIndices.size
        val targetActiveIdx = (current.turnIndex - 1 + activeCountNow) % activeCountNow
        val targetPlayerIdx = current.activePlayerIndices[targetActiveIdx]

        // Possibly shuffle target's hand
        val afterShuffle = when (difficulty) {
            "BOSS_LEVEL" -> {
                audio.playSound("card_shuffle")
                shuffleTargetHand(current, targetPlayerIdx)
            }
            "SNIPER" -> {
                if (Math.random() < 0.30) {
                    audio.playSound("card_shuffle")
                    shuffleTargetHand(current, targetPlayerIdx)
                } else current
            }
            else -> current
        }

        val target = afterShuffle.players.getOrNull(targetPlayerIdx) ?: return@LaunchedEffect
        val cardIndex = when (difficulty) {
            "ROOKIE" -> 0
            else -> if (target.hand.isNotEmpty()) (0 until target.hand.size).random() else 0
        }

        val newState = executeDrawCard(afterShuffle.copy(phase = SpPhase.PLAYING), cardIndex)
        if (newState.lastPairsDiscarded.isNotEmpty()) audio.playSound("pair_discard")
        audio.playSound("card_draw")
        localState = newState
        selectedCardIndex = -1

        if (newState.phase == SpPhase.GAME_OVER || newState.phase == SpPhase.ROUND_END) {
            audio.playSound("game_over")
            showLoser = true
        }
    }

    // Signal human's turn
    LaunchedEffect(currentState?.turnIndex, currentState?.phase) {
        val s = currentState ?: return@LaunchedEffect
        if (s.phase != SpPhase.PLAYING) return@LaunchedEffect
        val activeCount = s.activePlayerIndices.size
        if (activeCount < 2) return@LaunchedEffect
        val drawerIdx = s.activePlayerIndices[s.turnIndex]
        val drawer = s.players.getOrNull(drawerIdx) ?: return@LaunchedEffect
        if (drawer.userId == uid && !drawer.isAI) {
            audio.playSound("turn_ping")
        }
    }

    // Save AI result
    LaunchedEffect(stateForAi?.phase, stateForAi?.loserUserId) {
        val s = localState ?: return@LaunchedEffect
        if (s.phase != SpPhase.GAME_OVER || s.loserUserId == null) return@LaunchedEffect
        if (mode == "AI" && uid.isNotBlank()) {
            try {
                db.collection("strandraeuberResults").add(
                    mapOf(
                        "playerIds" to s.players.map { it.userId },
                        "players" to s.players.map { p ->
                            mapOf("userId" to p.userId, "displayName" to p.displayName, "avatarUrl" to p.avatarUrl, "isAI" to p.isAI)
                        },
                        "loserId" to s.loserUserId,
                        "loserName" to (s.players.find { it.userId == s.loserUserId }?.displayName ?: ""),
                        "rounds" to s.roundNumber,
                        "mode" to "ai",
                        "difficulty" to difficulty,
                        "scores" to s.roundScores.mapValues { it.value.toLong() },
                        "createdAt" to System.currentTimeMillis(),
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    var startingGame by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    BackHandler { showQuitDialog = true }

    if (showRules) {
        ALL_GAME_RULES["strandraeuber"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }

    val st = localState
    if (showQuitDialog) {
        if (mode == "AI" && st != null) {
            GameSaveQuitDialog(
                emoji = "🦹",
                message = "Runde ${st.roundNumber} · ${st.players.size} Spieler",
                onContinue = { showQuitDialog = false },
                onSaveAndQuit = {
                    val saveData = GameSave(
                        id = java.util.UUID.randomUUID().toString(),
                        gameType = "strandraeuber",
                        difficulty = difficulty,
                        gameState = Json.encodeToString(st.copy(phase = SpPhase.PLAYING, showRoundEnd = false).toSavedState()),
                        displayLabel = "Runde ${st.roundNumber} · ${st.players.size} Spieler",
                        savedAt = System.currentTimeMillis(),
                    )
                    SoloGameSaveManager.saveGame(context, saveData)
                    showQuitDialog = false
                    onNavigateBack()
                },
                onQuitWithoutSave = {
                    SoloGameSaveManager.deleteGameSave(context, "strandraeuber")
                    showQuitDialog = false
                    onNavigateBack()
                },
            )
        } else {
            Dialog(onDismissRequest = { showQuitDialog = false }) {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🏳️", style = MaterialTheme.typography.headlineLarge)
                        Text("Spiel verlassen?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "Du kannst über den Code wieder beitreten.",
                            style = MaterialTheme.typography.labelMedium, color = TextMuted, textAlign = TextAlign.Center,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showQuitDialog = false },
                                modifier = Modifier.weight(1f).height(44.dp),
                            ) { Text("Bleiben", color = TextPrimary) }
                            Button(
                                onClick = { showQuitDialog = false; onNavigateBack() },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Verlassen", color = Color.White) }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            GameHudBar(
                paused = isPaused,
                onPauseToggle = { isPaused = !isPaused },
                onQuit = { showQuitDialog = true },
                onShowRules = { showRules = true },
            ) {
                val s = currentState
                if (s != null) {
                    Text("🦹", style = MaterialTheme.typography.titleSmall)
                    Text("Runde ${s.roundNumber}/${s.totalRounds}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) { padding ->
        if (isPaused) {
            Box(Modifier.fillMaxSize().padding(padding).background(BgDark), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Pause, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(52.dp))
                    Text("Pausiert", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Tippe zum Weiterspielen", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                }
            }
            return@Scaffold
        }
        if (currentState == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Lade…", color = TextMuted)
            }
            return@Scaffold
        }

        if (mode == "ONLINE" && (currentState.phase == SpPhase.LOBBY || currentState.phase == SpPhase.DEALING)) {
            if (isOnlineAdmin && currentState.phase == SpPhase.LOBBY) {
                val players = onlineRawState?.players ?: emptyList()
                val canStart = players.size >= 2
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            "Spieler einladen",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.padding(4.dp),
                        ) {
                            QrCodeImage(
                                content = "https://thebeachbingo.netlify.app/strandraeuber/lobby?join=$gameId",
                                size = 160.dp,
                            )
                        }
                        Text(
                            gameId ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Crimson,
                            letterSpacing = 6.sp,
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "${players.size} / 6 Spieler",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                players.forEachIndexed { idx, player ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text("🦹", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (idx == 0) "${player.displayName} (Host)" else player.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (!startingGame && canStart) {
                                    startingGame = true
                                    scope.launch {
                                        try {
                                            db.collection("strandraeuberGames").document(gameId!!)
                                                .update(mapOf("status" to "RUNNING", "phase" to "DEALING")).await()
                                        } catch (_: Exception) {
                                            startingGame = false
                                        }
                                    }
                                }
                            },
                            enabled = canStart && !startingGame,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (startingGame) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (canStart) {
                                Text("Spiel starten (${players.size} Spieler) 🦹", fontWeight = FontWeight.Bold)
                            } else {
                                Text("⏳ Warte auf Spieler (${players.size}/2)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        CircularProgressIndicator(color = Crimson)
                        Text(
                            if (currentState.phase == SpPhase.DEALING) "Karten werden ausgeteilt…"
                            else "Warte auf Spielstart…",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (currentState.phase == SpPhase.LOBBY) {
                            Text(
                                "Der Host startet das Spiel gleich.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
            return@Scaffold
        }

        val activeCount = currentState.activePlayerIndices.size
        val drawerPlayerIdx = if (activeCount > 0) currentState.activePlayerIndices.getOrNull(currentState.turnIndex) else null
        val targetActiveIdx = if (activeCount >= 2) (currentState.turnIndex - 1 + activeCount) % activeCount else -1
        val targetPlayerIdx = if (targetActiveIdx >= 0) currentState.activePlayerIndices.getOrNull(targetActiveIdx) else null
        val drawerPlayer = drawerPlayerIdx?.let { currentState.players.getOrNull(it) }
        val targetPlayer = targetPlayerIdx?.let { currentState.players.getOrNull(it) }
        val humanPlayer = currentState.players.find { it.userId == uid }
        val isMyTurn = drawerPlayer?.userId == uid

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Opponents ──
            val opponents = currentState.players.filter { it.userId != uid }
            if (opponents.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useRowLayout = maxWidth > 600.dp
                    if (useRowLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            opponents.forEach { opp ->
                                val isTarget = opp.userId == targetPlayer?.userId
                                val isDrawer = opp.userId == drawerPlayer?.userId
                                val isAiThinking = currentState.phase == SpPhase.AI_THINKING && isDrawer
                                OpponentFan(
                                    player = opp,
                                    isTarget = isTarget && isMyTurn && currentState.phase == SpPhase.PLAYING,
                                    isDrawer = isDrawer,
                                    isAiThinking = isAiThinking,
                                    selectedCardIndex = if (isTarget && isMyTurn) selectedCardIndex else -1,
                                    onCardSelected = { idx -> selectedCardIndex = idx },
                                    compact = true,
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            opponents.forEach { opp ->
                                val isTarget = opp.userId == targetPlayer?.userId
                                val isDrawer = opp.userId == drawerPlayer?.userId
                                val isAiThinking = currentState.phase == SpPhase.AI_THINKING && isDrawer
                                OpponentFan(
                                    player = opp,
                                    isTarget = isTarget && isMyTurn && currentState.phase == SpPhase.PLAYING,
                                    isDrawer = isDrawer,
                                    isAiThinking = isAiThinking,
                                    selectedCardIndex = if (isTarget && isMyTurn) selectedCardIndex else -1,
                                    onCardSelected = { idx -> selectedCardIndex = idx },
                                    compact = false,
                                )
                            }
                        }
                    }
                }
            }

            // ── Status card ──
            SpStatusCard(
                state = currentState,
                isMyTurn = isMyTurn,
                drawerName = drawerPlayer?.displayName ?: "?",
                targetName = targetPlayer?.displayName ?: "?",
            )

            // ── Confirm draw button (only when human is drawer and selected a card) ──
            if (isMyTurn && currentState.phase == SpPhase.PLAYING && selectedCardIndex >= 0) {
                Button(
                    onClick = {
                        val idx = selectedCardIndex
                        selectedCardIndex = -1
                        val newState = if (mode == "AI") {
                            executeDrawCard(currentState, idx).also { ns ->
                                if (ns.lastPairsDiscarded.isNotEmpty()) audio.playSound("pair_discard")
                                audio.playSound("card_draw")
                                if (ns.phase == SpPhase.GAME_OVER || ns.phase == SpPhase.ROUND_END) {
                                    audio.playSound("game_over")
                                    showLoser = true
                                }
                            }
                        } else null
                        if (newState != null) localState = newState
                        if (mode == "ONLINE") {
                            scope.launch {
                                executeOnlineDrawCard(db, gameId ?: return@launch, uid, currentState, idx)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Karte ziehen (${selectedCardIndex + 1}. Karte) 🦹", fontWeight = FontWeight.Bold)
                }
            }

            // ── Own cards ──
            if (humanPlayer != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMyTurn && currentState.phase == SpPhase.PLAYING)
                            Crimson.copy(alpha = 0.08f)
                        else SurfaceDark
                    ),
                    border = if (!isMyTurn && targetPlayer?.userId == uid && currentState.phase == SpPhase.PLAYING)
                        androidx.compose.foundation.BorderStroke(1.5.dp, SandGoldLight.copy(alpha = 0.5f))
                    else null,
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Deine Hand (${humanPlayer.hand.size} Karten)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                            if (targetPlayer?.userId == uid && currentState.phase == SpPhase.PLAYING) {
                                Text(
                                    "← Du wirst gezogen!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SandGoldLight,
                                )
                            }
                        }
                        // Shuffle / Sort buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (mode == "AI") {
                                        val s = localState ?: return@OutlinedButton
                                        val humanIdx = s.players.indexOfFirst { it.userId == uid }
                                        if (humanIdx >= 0) {
                                            audio.playSound("card_shuffle")
                                            localState = shuffleTargetHand(s, humanIdx)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                            ) { Text("🔀 Mischen") }
                            OutlinedButton(
                                onClick = {
                                    if (mode == "AI") {
                                        val s = localState ?: return@OutlinedButton
                                        val humanIdx = s.players.indexOfFirst { it.userId == uid }
                                        if (humanIdx >= 0) {
                                            val sorted = s.players[humanIdx].hand.sortedBy { it.name }
                                            val newPlayers = s.players.toMutableList()
                                            newPlayers[humanIdx] = s.players[humanIdx].copy(hand = sorted)
                                            localState = s.copy(players = newPlayers)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                            ) { Text("🔤 Sortieren") }
                        }
                        // Own cards face-up
                        if (humanPlayer.hand.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✓ Alle Paare abgelegt!", color = Crimson, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val isTablet = maxWidth > 600.dp
                                val cardW = if (isTablet) 72.dp else 54.dp
                                val cardH = if (isTablet) 100.dp else 76.dp
                                val gap  = if (isTablet) 6.dp else 4.dp
                                if (isTablet) {
                                    val rows = humanPlayer.hand.chunked(9)
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(gap),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        rows.forEach { rowCards ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                                rowCards.forEach { card ->
                                                    SpFaceUpCard(card, card.pairId == "strandraeuber", cardW, cardH)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        CardFanRow(cards = humanPlayer.hand) { card, _ ->
                                            SpFaceUpCard(card, card.pairId == "strandraeuber", cardW, cardH)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Score overview ──
            if (currentState.roundScores.values.any { it > 0 }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Strandräuber-Punkte", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        currentState.players.forEach { p ->
                            val score = currentState.roundScores[p.userId] ?: 0
                            if (score > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${p.avatarUrl} ${p.displayName}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    Text("🦹 × $score", style = MaterialTheme.typography.bodySmall, color = Crimson, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ── Abgelegte Paare ──
            if (currentState.discardedPairs.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "✅ Abgelegte Paare (${currentState.discardedPairs.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            currentState.discardedPairs.forEach { (a, _) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Success.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, Success.copy(alpha = 0.3f)
                                    ),
                                ) {
                                    Text(
                                        a.emoji,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Round End / Game Over Dialog ──
    val dialogState = currentState
    if (dialogState != null && dialogState.showRoundEnd) {
        val isGameOver = dialogState.phase == SpPhase.GAME_OVER
        val loser = dialogState.players.find { it.userId == dialogState.loserUserId }
        val isMyLoss = dialogState.loserUserId == uid

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
                    Text(
                        if (isGameOver) "🏁 Spiel beendet!" else "Runde ${dialogState.roundNumber} beendet",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isGameOver) Crimson else TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )

                    if (loser != null) {
                        Text("🦹", fontSize = DrawNumberTablet)
                        Text(
                            "${loser.displayName} hält den Strandräuber!",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isMyLoss) Crimson else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }

                    HorizontalDivider(color = Surface2Dark)

                    // Scores
                    Text("Punkte", style = MaterialTheme.typography.labelLarge, color = TextSub)
                    dialogState.players.forEach { p ->
                        val score = dialogState.roundScores[p.userId] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${p.avatarUrl} ${p.displayName}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                if (score == 0) "✓" else "🦹 × $score",
                                color = if (score == 0) TextMuted else Crimson,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (isGameOver) {
                        val totalScores = dialogState.roundScores
                        val finalLoser = totalScores.maxByOrNull { it.value }?.key?.let { lid ->
                            dialogState.players.find { it.userId == lid }
                        }
                        if (finalLoser != null) {
                            Text(
                                "🦹 ${finalLoser.displayName} hat das Gesamtspiel verloren!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Crimson,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Zur Lobby", fontWeight = FontWeight.Bold)
                        }
                    } else if (mode == "AI") {
                        Button(
                            onClick = {
                                localState = startNewRound(dialogState)
                                audio.playSound("card_shuffle")
                                selectedCardIndex = -1
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Nächste Runde 🦹", fontWeight = FontWeight.Bold)
                        }
                    } else {
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
                    }
                }
            }
        }
    }
}

// ── Composables ────────────────────────────────────────────────────────────────

@Composable
private fun SpStatusCard(
    state: SpGameState,
    isMyTurn: Boolean,
    drawerName: String,
    targetName: String,
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isMyTurn && state.phase == SpPhase.PLAYING -> Crimson.copy(alpha = 0.15f)
            state.phase == SpPhase.AI_THINKING -> Surface2Dark
            else -> SurfaceDark
        },
        animationSpec = tween(300),
        label = "statusBg",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val statusText = when {
                state.phase == SpPhase.AI_THINKING ->
                    "🤖 $drawerName überlegt…"
                isMyTurn && state.phase == SpPhase.PLAYING ->
                    "Dein Zug! Wähle eine Karte von $targetName"
                state.phase == SpPhase.PLAYING ->
                    "$drawerName ist am Zug (zieht von $targetName)"
                else -> state.lastActionText
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMyTurn && state.phase == SpPhase.PLAYING) Crimson else TextPrimary,
                fontWeight = if (isMyTurn) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
            if (state.lastActionText.isNotBlank() && state.phase == SpPhase.PLAYING) {
                Text(
                    state.lastActionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OpponentFan(
    player: SpLocalPlayer,
    isTarget: Boolean,
    isDrawer: Boolean,
    isAiThinking: Boolean,
    selectedCardIndex: Int,
    onCardSelected: (Int) -> Unit,
    compact: Boolean = false,
) {
    val cardCount = player.hand.size
    Card(
        modifier = if (compact) Modifier.widthIn(min = 160.dp, max = 260.dp) else Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isTarget -> Crimson.copy(alpha = 0.08f)
                isDrawer -> Teal.copy(alpha = 0.08f)
                else -> SurfaceDark
            }
        ),
        border = if (isTarget)
            androidx.compose.foundation.BorderStroke(1.5.dp, Crimson.copy(alpha = 0.6f))
        else if (isDrawer)
            androidx.compose.foundation.BorderStroke(1.dp, Teal.copy(alpha = 0.4f))
        else null,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(player.avatarUrl, fontSize = BingoCallSize)
                    Column {
                        Text(
                            player.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isTarget) Crimson else if (isDrawer) Teal else TextPrimary,
                            fontWeight = if (isTarget || isDrawer) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            when {
                                cardCount == 0 -> "✓ Raus"
                                isAiThinking -> "🤔 überlegt…"
                                isTarget -> "← ${cardCount} Karten — Wähle eine!"
                                isDrawer -> "↗ Zieht gerade"
                                else -> "$cardCount Karten"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTarget) Crimson else TextMuted,
                        )
                    }
                }
                if (cardCount == 0) {
                    Text("✓", color = Success, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (cardCount > 0) {
                // Non-overlapping row of hidden cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(cardCount) { i ->
                        val isSelected = i == selectedCardIndex
                        Box(
                            modifier = Modifier
                                .offset(y = if (isSelected) (-8).dp else 0.dp)
                                .size(width = 40.dp, height = 58.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .then(
                                    if (isSelected)
                                        Modifier.border(2.5.dp, SandGoldLight, RoundedCornerShape(5.dp))
                                    else if (isTarget)
                                        Modifier.border(1.5.dp, Crimson.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                                    else
                                        Modifier.border(1.dp, SlateBlueDark, RoundedCornerShape(5.dp))
                                )
                                .clickable(enabled = isTarget) { onCardSelected(i) }
                        ) {
                            CardBackScene(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpFaceUpCard(card: SpCard, highlight: Boolean = false, cardWidth: Dp = 54.dp, cardHeight: Dp = 76.dp) {
    val bgColor by animateColorAsState(
        targetValue = if (highlight) Crimson.copy(alpha = 0.3f) else BgPlayerZone,
        animationSpec = tween(600),
        label = "cardBg",
    )
    val emojiSp = (cardWidth.value * 0.37f).sp
    val nameSp  = (cardWidth.value * 0.14f).sp
    Box(
        modifier = Modifier
            .size(width = cardWidth, height = cardHeight)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (highlight) 2.5.dp else 1.dp,
                color = if (highlight) Crimson else CardBorderLight,
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Text(card.emoji, fontSize = emojiSp)
            Text(
                card.name,
                fontSize = nameSp,
                color = if (highlight) Crimson else TextSub,
                textAlign = TextAlign.Center,
                lineHeight = (nameSp.value * 1.25f).sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
