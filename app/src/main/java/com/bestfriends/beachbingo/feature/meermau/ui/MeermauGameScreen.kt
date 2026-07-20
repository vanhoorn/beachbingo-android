package com.bestfriends.beachbingo.feature.meermau.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.brandung.ui.CardBackScene
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.SandGold
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
import kotlin.random.Random

// ── Constants & Models ────────────────────────────────────────────────────────

private val MeermauViolet = Color(0xFF7C3AED)

private val MM_SUITS = listOf("♣", "♠", "♥", "♦")
private val MM_RANKS = listOf("7", "8", "9", "10", "J", "Q", "K", "A")
private val MM_RED_SUITS = setOf("♥", "♦")
private val MM_CARD_POINTS = mapOf("7" to 7, "8" to 8, "9" to 9, "10" to 10, "J" to 20, "Q" to 10, "K" to 10, "A" to 11)

data class MMCard(val suit: String, val rank: String, val id: String)

data class MMPlayer(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val hand: List<MMCard>,
    val isAI: Boolean,
    val totalScore: Int,
    val eliminated: Boolean,
)

data class MMSettings(val reverseOn9: Boolean, val stopperOn8: Boolean, val wildOn10: Boolean)

data class MMState(
    val players: List<MMPlayer>,
    val drawPile: List<MMCard>,
    val discardPile: List<MMCard>,
    val currentPlayerIndex: Int,
    val direction: Int,
    val drawPending: Int,
    val wishSuit: String?,
    val phase: String,
    val mauPlayerId: String?,
    val drawnCard: MMCard?,
    val roundWinnerId: String?,
    val gameWinnerId: String?,
    val roundScores: Map<String, Int>,
    val round: Int,
    val lastActionText: String,
    val aiThinking: Boolean,
    val difficulty: String,
    val settings: MMSettings,
    val lastSkippedId: String?,
)

// ── Pure game logic ───────────────────────────────────────────────────────────

private fun createMMDeck(): List<MMCard> = MM_SUITS.flatMap { s -> MM_RANKS.map { r -> MMCard(s, r, "$s$r") } }

private fun shuffleMMDeck(deck: List<MMCard>): List<MMCard> = deck.shuffled()

private fun canPlayMM(card: MMCard, top: MMCard, wishSuit: String?, drawPending: Int, settings: MMSettings): Boolean {
    if (drawPending > 0) {
        if (card.rank == "7") return true
        if (settings.stopperOn8 && card.rank == "8") return true
        return false
    }
    if (wishSuit != null) {
        if (card.rank == "J") return true
        if (settings.wildOn10 && card.rank == "10") return true
        return card.suit == wishSuit
    }
    return card.suit == top.suit || card.rank == top.rank
}

private fun mmHandPoints(hand: List<MMCard>): Int = hand.sumOf { MM_CARD_POINTS[it.rank] ?: 0 }

private fun bestWishSuitMM(hand: List<MMCard>): String {
    val counts = MM_SUITS.associateWith { s -> hand.count { it.suit == s } }
    return counts.maxByOrNull { it.value }?.key ?: "♠"
}

private fun mmAIMove(hand: List<MMCard>, top: MMCard, wishSuit: String?, drawPending: Int, difficulty: String, settings: MMSettings): Pair<String?, String?> {
    val playable = hand.filter { canPlayMM(it, top, wishSuit, drawPending, settings) }
    if (difficulty == "ROOKIE") {
        if (playable.isEmpty() || Random.nextFloat() < 0.15f) return null to null
        val chosen = playable.random()
        val ws = if (chosen.rank == "J" || (settings.wildOn10 && chosen.rank == "10")) bestWishSuitMM(hand.filter { it.id != chosen.id }) else null
        return chosen.id to ws
    }
    if (playable.isEmpty()) return null to null
    fun weight(r: String) = when (r) { "7" -> 40; "8" -> 30; "J" -> 20; "9" -> 10; else -> 0 }
    val chosen = playable.maxByOrNull { weight(it.rank) + (MM_CARD_POINTS[it.rank] ?: 0) }!!
    val ws = if (chosen.rank == "J" || (settings.wildOn10 && chosen.rank == "10")) bestWishSuitMM(hand.filter { it.id != chosen.id }) else null
    return chosen.id to ws
}

private fun nextMMIdx(from: Int, dir: Int, players: List<MMPlayer>, extraSkip: Int = 0): Int {
    val n = players.size
    var idx = from
    var skips = 1 + extraSkip
    var safety = 0
    while (skips > 0 && safety < n * 2) {
        idx = ((idx + dir) % n + n) % n
        if (!players[idx].eliminated) skips--
        safety++
    }
    return idx
}

private fun mmReshuffleIfNeeded(draw: List<MMCard>, discard: List<MMCard>): Pair<List<MMCard>, List<MMCard>> {
    if (draw.isNotEmpty() || discard.size <= 1) return draw to discard
    val top = discard.last()
    val newDraw = shuffleMMDeck(discard.dropLast(1))
    return newDraw to listOf(top)
}

private fun mmDealCards(playerCount: Int): Triple<List<List<MMCard>>, List<MMCard>, MMCard> {
    var deck = shuffleMMDeck(createMMDeck()).toMutableList()
    val hands = Array(playerCount) { mutableListOf<MMCard>() }
    repeat(5) { repeat(playerCount) { p -> hands[p].add(deck.removeAt(0)) } }
    var topCard: MMCard? = null
    val remaining = mutableListOf<MMCard>()
    for (c in deck) {
        if (topCard == null && c.rank != "J") topCard = c else remaining.add(c)
    }
    if (topCard == null) return mmDealCards(playerCount)
    return Triple(hands.map { it.toList() }, remaining, topCard)
}

private fun doMMPlay(state: MMState, playerIdx: Int, cardId: String, chosenWishSuit: String? = null): MMState {
    val player = state.players[playerIdx]
    val card = player.hand.find { it.id == cardId } ?: return state
    val newHand = player.hand.filter { it.id != cardId }
    val newDiscard = state.discardPile + card

    val newPlayers = state.players.toMutableList().also {
        it[playerIdx] = player.copy(hand = newHand)
    }

    // Determine next state based on card rank
    var nextDrawPending = state.drawPending
    var nextWishSuit: String? = if (card.rank == "7" || card.rank == "8") state.wishSuit else null
    var nextDir = state.direction
    var extraSkip = 0
    var nextPhase = "PLAYING"
    var action = "${player.displayName} spielt ${card.rank}${card.suit}"

    when (card.rank) {
        "7" -> { nextDrawPending = state.drawPending + 2; action = "${player.displayName} spielt 7 → ${state.drawPending + 2} Karten ziehen!" }
        "8" -> {
            if (state.drawPending > 0 && state.settings.stopperOn8) {
                nextDrawPending = 0
                action = "${player.displayName} stoppt den Stapel mit 8!"
            } else {
                extraSkip = 1
                action = "${player.displayName} spielt 8 → ${state.players[nextMMIdx(playerIdx, nextDir, state.players)].displayName} ausgesetzt!"
            }
        }
        "9" -> {
            if (state.settings.reverseOn9) {
                nextDir = -state.direction
                action = "${player.displayName} spielt 9 → Richtung umgekehrt!"
            }
        }
        "J" -> {
            if (player.isAI || chosenWishSuit != null) {
                nextWishSuit = chosenWishSuit ?: bestWishSuitMM(newHand)
                action = "${player.displayName} spielt Bube → wünscht ${nextWishSuit}!"
            } else {
                val st = state.copy(players = newPlayers, discardPile = newDiscard, drawnCard = null, phase = "WISH", lastActionText = "${player.displayName} spielt Bube – Farbe wählen!")
                return st
            }
        }
        "10" -> {
            if (state.settings.wildOn10) {
                if (player.isAI || chosenWishSuit != null) {
                    nextWishSuit = chosenWishSuit ?: bestWishSuitMM(newHand)
                    action = "${player.displayName} spielt 10 → wünscht ${nextWishSuit}!"
                } else {
                    val st = state.copy(players = newPlayers, discardPile = newDiscard, drawnCard = null, phase = "WISH", lastActionText = "${player.displayName} spielt 10 – Farbe wählen!")
                    return st
                }
            }
        }
    }

    val skippedId = if (extraSkip > 0) state.players[nextMMIdx(playerIdx, nextDir, state.players)].userId else null
    val nextIdx = nextMMIdx(playerIdx, nextDir, newPlayers.map { if (it.userId == skippedId) it.copy(eliminated = true) else it }, extraSkip = 0)
    val finalPlayers = newPlayers

    val (rDraw, rDiscard) = mmReshuffleIfNeeded(state.drawPile, newDiscard)

    val nextState = state.copy(
        players = finalPlayers,
        drawPile = rDraw,
        discardPile = rDiscard,
        currentPlayerIndex = nextIdx,
        direction = nextDir,
        drawPending = nextDrawPending,
        wishSuit = nextWishSuit,
        drawnCard = null,
        lastActionText = action,
        phase = nextPhase,
        lastSkippedId = skippedId,
    )
    return mmCheckWin(nextState, playerIdx, nextPhase)
}

private fun mmCheckWin(state: MMState, playedIdx: Int, nextPhase: String): MMState {
    if (nextPhase == "WISH") return state
    val player = state.players[playedIdx]
    if (player.hand.isEmpty()) return mmResolveRound(state, playedIdx)
    if (player.hand.size == 1) {
        return state.copy(phase = "MAU_CHECK", mauPlayerId = player.userId)
    }
    return state
}

private fun mmResolveRound(state: MMState, winnerIdx: Int): MMState {
    val winner = state.players[winnerIdx]
    val newScores = state.roundScores.toMutableMap()
    state.players.forEachIndexed { idx, p ->
        if (!p.eliminated && idx != winnerIdx) {
            val pts = mmHandPoints(p.hand)
            newScores[p.userId] = (newScores[p.userId] ?: 0) + pts
        }
    }
    val newPlayers = state.players.map { p ->
        val total = newScores[p.userId] ?: p.totalScore
        p.copy(totalScore = total, eliminated = total >= 100 || (p.eliminated && p.userId != winner.userId))
    }
    val alive = newPlayers.filter { !it.eliminated }
    if (alive.size <= 1) {
        return state.copy(
            players = newPlayers, roundScores = newScores, roundWinnerId = winner.userId,
            gameWinnerId = (alive.firstOrNull() ?: winner).userId,
            phase = "GAME_OVER", lastActionText = "${winner.displayName} hat die Karten los!",
        )
    }
    return state.copy(
        players = newPlayers, roundScores = newScores, roundWinnerId = winner.userId,
        phase = "ROUND_END", lastActionText = "${winner.displayName} ist raus!",
    )
}

private fun mmStartNewRound(state: MMState): MMState {
    val alive = state.players.filter { !it.eliminated }
    val (hands, drawPile, topCard) = mmDealCards(alive.size)
    val newPlayers = state.players.mapIndexed { _, p ->
        val aliveIdx = alive.indexOfFirst { it.userId == p.userId }
        if (!p.eliminated && aliveIdx >= 0) p.copy(hand = hands[aliveIdx]) else p
    }
    val firstIdx = newPlayers.indexOfFirst { !it.eliminated }.coerceAtLeast(0)
    return state.copy(
        players = newPlayers,
        drawPile = drawPile,
        discardPile = listOf(topCard),
        currentPlayerIndex = firstIdx,
        direction = 1,
        drawPending = 0,
        wishSuit = null,
        phase = "PLAYING",
        mauPlayerId = null,
        drawnCard = null,
        roundWinnerId = null,
        roundScores = state.players.associate { it.userId to it.totalScore },
        round = state.round + 1,
        lastActionText = "Runde ${state.round + 1} beginnt!",
        aiThinking = false,
        lastSkippedId = null,
    )
}

private fun doMMDraw(state: MMState, playerIdx: Int): MMState {
    val player = state.players[playerIdx]
    var (draw, discard) = mmReshuffleIfNeeded(state.drawPile, state.discardPile)
    if (draw.isEmpty()) return state.copy(lastActionText = "Stapel leer – kein Ziehen möglich")

    if (state.drawPending > 0) {
        // Draw penalty cards
        val count = minOf(state.drawPending, draw.size)
        val drawnCards = draw.take(count)
        draw = draw.drop(count)
        val newHand = player.hand + drawnCards
        val nextIdx = nextMMIdx(playerIdx, state.direction, state.players)
        val newPlayers = state.players.toMutableList().also { it[playerIdx] = player.copy(hand = newHand) }
        return state.copy(
            players = newPlayers, drawPile = draw, discardPile = discard,
            currentPlayerIndex = nextIdx, drawPending = 0, drawnCard = null,
            lastActionText = "${player.displayName} zieht ${count} Karten!",
        )
    }

    // Draw one card, offer to play
    val drawnCard = draw.first()
    draw = draw.drop(1)
    return state.copy(drawPile = draw, discardPile = discard, drawnCard = drawnCard, lastActionText = "${player.displayName} zieht eine Karte")
}

// ── Online serialization ──────────────────────────────────────────────────────

private fun serializeMMCard(c: MMCard): Map<String, Any> =
    mapOf("suit" to c.suit, "rank" to c.rank, "id" to c.id)

@Suppress("UNCHECKED_CAST")
private fun deserializeMMCard(m: Any?): MMCard? {
    val map = m as? Map<*, *> ?: return null
    return MMCard(
        suit = map["suit"] as? String ?: return null,
        rank = map["rank"] as? String ?: return null,
        id   = map["id"]   as? String ?: return null,
    )
}

private fun serializeMMPlayerOnline(p: MMPlayer): Map<String, Any> = mapOf(
    "userId" to p.userId, "displayName" to p.displayName, "avatarUrl" to p.avatarUrl,
    "isAI" to p.isAI, "hand" to p.hand.map { serializeMMCard(it) },
    "totalScore" to p.totalScore, "eliminated" to p.eliminated,
)

@Suppress("UNCHECKED_CAST")
private fun deserializeMMPlayerOnline(userId: String, m: Any?): MMPlayer? {
    val map = m as? Map<*, *> ?: return null
    return MMPlayer(
        userId      = userId,
        displayName = map["displayName"] as? String ?: "?",
        avatarUrl   = map["avatarUrl"]   as? String ?: "🃏",
        hand        = ((map["hand"] as? List<*>) ?: emptyList<Any>()).mapNotNull { deserializeMMCard(it) },
        isAI        = map["isAI"]        as? Boolean ?: false,
        totalScore  = (map["totalScore"] as? Long)?.toInt() ?: (map["totalScore"] as? Int) ?: 0,
        eliminated  = map["eliminated"]  as? Boolean ?: false,
    )
}

@Suppress("UNCHECKED_CAST")
private fun parseOnlineMMState(data: Map<String, Any>, difficulty: String): Pair<String, MMState> {
    val adminId   = data["adminId"] as? String ?: ""
    val playerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val playersRaw = data["players"] as? Map<*, *> ?: emptyMap<String, Any>()
    val players   = playerIds.mapNotNull { id -> deserializeMMPlayerOnline(id, playersRaw[id]) }
    val drawPile  = ((data["drawPile"]   as? List<*>) ?: emptyList<Any>()).mapNotNull { deserializeMMCard(it) }
    val discardPile = ((data["discardPile"] as? List<*>) ?: emptyList<Any>()).mapNotNull { deserializeMMCard(it) }
    val roundScoresRaw = data["roundScores"] as? Map<*, *> ?: emptyMap<String, Any>()
    val roundScores = roundScoresRaw.entries.associate { (k, v) ->
        (k as? String ?: "") to ((v as? Long)?.toInt() ?: (v as? Int) ?: 0)
    }
    val settingsRaw = data["settings"] as? Map<*, *>
    val settings = MMSettings(
        reverseOn9  = settingsRaw?.get("reverseOn9")  as? Boolean ?: false,
        stopperOn8  = settingsRaw?.get("stopperOn8")  as? Boolean ?: false,
        wildOn10    = settingsRaw?.get("wildOn10")    as? Boolean ?: false,
    )
    return adminId to MMState(
        players = players, drawPile = drawPile, discardPile = discardPile,
        currentPlayerIndex = (data["currentPlayerIndex"] as? Long)?.toInt() ?: 0,
        direction   = (data["direction"]   as? Long)?.toInt() ?: 1,
        drawPending = (data["drawPending"] as? Long)?.toInt() ?: 0,
        wishSuit    = data["wishSuit"]     as? String,
        phase       = data["phase"]        as? String ?: "PLAYING",
        mauPlayerId = data["mauPlayerId"]  as? String,
        drawnCard   = null,
        roundWinnerId = data["roundWinnerId"] as? String,
        gameWinnerId  = data["gameWinnerId"]  as? String,
        roundScores = roundScores,
        round       = (data["round"] as? Long)?.toInt() ?: 1,
        lastActionText = data["lastActionText"] as? String ?: "",
        aiThinking  = false, difficulty = difficulty, settings = settings,
        lastSkippedId = data["lastSkippedId"] as? String,
    )
}

private fun serializeMMState(state: MMState, adminId: String, playerIds: List<String>): Map<String, Any?> = mapOf(
    "adminId" to adminId, "status" to "RUNNING", "playerIds" to playerIds,
    "players"      to state.players.associate { p -> p.userId to serializeMMPlayerOnline(p) },
    "drawPile"     to state.drawPile.map  { serializeMMCard(it) },
    "discardPile"  to state.discardPile.map { serializeMMCard(it) },
    "currentPlayerIndex" to state.currentPlayerIndex,
    "direction"    to state.direction,    "drawPending" to state.drawPending,
    "wishSuit"     to state.wishSuit,     "phase"       to state.phase,
    "mauPlayerId"  to state.mauPlayerId,  "roundWinnerId" to state.roundWinnerId,
    "gameWinnerId" to state.gameWinnerId, "roundScores" to state.roundScores,
    "round"        to state.round,        "lastActionText" to state.lastActionText,
    "lastSkippedId" to state.lastSkippedId,
    "settings" to mapOf(
        "reverseOn9" to state.settings.reverseOn9,
        "stopperOn8" to state.settings.stopperOn8,
        "wildOn10"   to state.settings.wildOn10,
    ),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeermauGameScreen(
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var localState by remember { mutableStateOf<MMState?>(null) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var adminId by remember { mutableStateOf("") }
    var onlinePlayerIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val writeOnline: (MMState) -> Unit = { newState ->
        if (mode == "online" && gameId != null) {
            scope.launch {
                try {
                    db.collection("meermauGames").document(gameId)
                        .set(serializeMMState(newState, adminId, onlinePlayerIds))
                        .await()
                } catch (_: Exception) {}
            }
        }
    }

    // Init
    LaunchedEffect(Unit) {
        val userSnap = db.collection("users").document(uid).get().await()
        val displayName = userSnap.getString("displayName") ?: "Du"
        val avatarUrl = userSnap.getString("avatarUrl") ?: "🃏"
        val settings = MMSettings(
            reverseOn9 = userSnap.getBoolean("meermauReverseOn9") ?: false,
            stopperOn8 = userSnap.getBoolean("meermauStopperOn8") ?: false,
            wildOn10 = userSnap.getBoolean("meermauWildOn10") ?: false,
        )

        if (mode == "online" && gameId != null) {
            val gameSnap = db.collection("meermauGames").document(gameId).get().await()
            val gameData = gameSnap.data ?: return@LaunchedEffect
            adminId = gameData["adminId"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val ids = (gameData["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            onlinePlayerIds = ids
            if (uid == adminId) {
                @Suppress("UNCHECKED_CAST")
                val playersRaw = gameData["players"] as? Map<String, Any> ?: emptyMap()
                val allPlayers = ids.mapNotNull { playerId ->
                    val pData = playersRaw[playerId] as? Map<*, *> ?: return@mapNotNull null
                    MMPlayer(
                        userId = playerId,
                        displayName = pData["displayName"] as? String ?: "Spieler",
                        avatarUrl = pData["avatarUrl"] as? String ?: "🃏",
                        hand = emptyList(), isAI = false, totalScore = 0, eliminated = false,
                    )
                }
                val (hands, drawPile, topCard) = mmDealCards(allPlayers.size)
                val initialPlayers = allPlayers.mapIndexed { idx, p -> p.copy(hand = hands[idx]) }
                val initState = MMState(
                    players = initialPlayers, drawPile = drawPile, discardPile = listOf(topCard),
                    currentPlayerIndex = 0, direction = 1, drawPending = 0, wishSuit = null,
                    phase = "PLAYING", mauPlayerId = null, drawnCard = null, roundWinnerId = null,
                    gameWinnerId = null, roundScores = initialPlayers.associate { it.userId to 0 },
                    round = 1, lastActionText = "Runde 1 beginnt!", aiThinking = false,
                    difficulty = difficulty, settings = settings, lastSkippedId = null,
                )
                db.collection("meermauGames").document(gameId)
                    .set(serializeMMState(initState, adminId, ids)).await()
            }
            return@LaunchedEffect
        }

        val aiNames = listOf("Mia", "Leo", "Sam")
        val aiAvatars = listOf("🤖", "🦾", "🎯")
        val humanPlayer = MMPlayer(uid, displayName, avatarUrl, emptyList(), false, 0, false)
        val aiPlayers = (0 until aiCount).map { i ->
            MMPlayer("ai_$i", aiNames[i], aiAvatars[i], emptyList(), true, 0, false)
        }
        val allPlayers = listOf(humanPlayer) + aiPlayers
        val (hands, drawPile, topCard) = mmDealCards(allPlayers.size)
        val initialPlayers = allPlayers.mapIndexed { idx, p -> p.copy(hand = hands[idx]) }
        localState = MMState(
            players = initialPlayers, drawPile = drawPile, discardPile = listOf(topCard),
            currentPlayerIndex = 0, direction = 1, drawPending = 0, wishSuit = null,
            phase = "PLAYING", mauPlayerId = null, drawnCard = null, roundWinnerId = null,
            gameWinnerId = null, roundScores = initialPlayers.associate { it.userId to 0 },
            round = 1, lastActionText = "Runde 1 beginnt!", aiThinking = false,
            difficulty = difficulty, settings = settings, lastSkippedId = null,
        )
    }

    // Online: Firestore snapshot listener
    LaunchedEffect(gameId) {
        if (mode != "online" || gameId == null) return@LaunchedEffect
        val listener = db.collection("meermauGames").document(gameId)
            .addSnapshotListener { snap, _ ->
                val data = snap?.data ?: return@addSnapshotListener
                if ((data["phase"] as? String).isNullOrBlank()) return@addSnapshotListener
                val (parsedAdmin, parsedState) = parseOnlineMMState(data, difficulty)
                adminId = parsedAdmin
                onlinePlayerIds = (data["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                localState = parsedState
            }
        try { awaitCancellation() } finally { listener.remove() }
    }

    // Online: admin starts new round after ROUND_END
    LaunchedEffect(localState?.phase, localState?.round) {
        if (mode != "online" || gameId == null || uid != adminId || adminId.isEmpty()) return@LaunchedEffect
        val st = localState ?: return@LaunchedEffect
        if (st.phase != "ROUND_END") return@LaunchedEffect
        delay(3000L)
        val newRound = mmStartNewRound(st)
        try {
            db.collection("meermauGames").document(gameId)
                .set(serializeMMState(newRound, adminId, onlinePlayerIds)).await()
        } catch (_: Exception) {}
    }

    // AI turn
    LaunchedEffect(localState?.currentPlayerIndex, localState?.phase, localState?.drawnCard) {
        if (mode == "online") return@LaunchedEffect
        val st = localState ?: return@LaunchedEffect
        if (st.phase != "PLAYING" || st.drawnCard != null) return@LaunchedEffect
        val current = st.players.getOrNull(st.currentPlayerIndex) ?: return@LaunchedEffect
        if (!current.isAI) return@LaunchedEffect
        localState = st.copy(aiThinking = true)
        delay(1200)
        val top = st.discardPile.lastOrNull() ?: return@LaunchedEffect
        val (cardId, wishSuit) = mmAIMove(current.hand, top, st.wishSuit, st.drawPending, st.difficulty, st.settings)
        localState = if (cardId != null) {
            doMMPlay(st.copy(aiThinking = false), st.currentPlayerIndex, cardId, wishSuit)
        } else {
            doMMDraw(st.copy(aiThinking = false), st.currentPlayerIndex)
        }
    }

    // AI drawn-card decision
    LaunchedEffect(localState?.drawnCard, localState?.currentPlayerIndex) {
        if (mode == "online") return@LaunchedEffect
        val st = localState ?: return@LaunchedEffect
        val drawnCard = st.drawnCard ?: return@LaunchedEffect
        val current = st.players.getOrNull(st.currentPlayerIndex) ?: return@LaunchedEffect
        if (!current.isAI) return@LaunchedEffect
        delay(600)
        val top = st.discardPile.lastOrNull() ?: return@LaunchedEffect
        val canPlay = canPlayMM(drawnCard, top, st.wishSuit, st.drawPending, st.settings)
        localState = if (canPlay && (st.difficulty != "ROOKIE" || Random.nextFloat() > 0.3f)) {
            val ws = if (drawnCard.rank == "J" || (st.settings.wildOn10 && drawnCard.rank == "10")) bestWishSuitMM(current.hand) else null
            doMMPlay(st, st.currentPlayerIndex, drawnCard.id, ws)
        } else {
            // Keep drawn card
            val newHand = current.hand + drawnCard
            val nextIdx = nextMMIdx(st.currentPlayerIndex, st.direction, st.players)
            val newPlayers = st.players.toMutableList().also { it[st.currentPlayerIndex] = current.copy(hand = newHand) }
            st.copy(players = newPlayers, drawnCard = null, currentPlayerIndex = nextIdx, lastActionText = "${current.displayName} behält die Karte")
        }
    }

    // AI MAU check
    LaunchedEffect(localState?.phase, localState?.mauPlayerId) {
        if (mode == "online") return@LaunchedEffect
        val st = localState ?: return@LaunchedEffect
        if (st.phase != "MAU_CHECK") return@LaunchedEffect
        val mauPlayer = st.players.find { it.userId == st.mauPlayerId } ?: return@LaunchedEffect
        if (!mauPlayer.isAI) return@LaunchedEffect
        delay(700)
        localState = st.copy(phase = "PLAYING", mauPlayerId = null, lastActionText = "${mauPlayer.displayName}: MAU!")
    }

    // Save result on game over
    LaunchedEffect(localState?.phase) {
        val st = localState ?: return@LaunchedEffect
        if (st.phase != "GAME_OVER") return@LaunchedEffect
        val winnerId = st.gameWinnerId ?: return@LaunchedEffect
        val winner = st.players.find { it.userId == winnerId } ?: return@LaunchedEffect
        try {
            db.collection("meermauResults").add(
                mapOf(
                    "winnerId" to winnerId,
                    "playerIds" to st.players.map { it.userId },
                    "players" to st.players.map { mapOf("userId" to it.userId, "displayName" to it.displayName, "avatarUrl" to it.avatarUrl) },
                    "rounds" to st.round,
                    "mode" to mode,
                    "difficulty" to difficulty,
                    "createdAt" to System.currentTimeMillis(),
                )
            ).await()
        } catch (_: Exception) {}
    }

    val st = localState
    if (st == null) {
        Box(Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
            Text("Lade Spiel…", color = TextMuted)
        }
        return
    }

    val myPlayer = st.players.find { it.userId == uid }
    val myHand = myPlayer?.hand ?: emptyList()
    val topCard = st.discardPile.lastOrNull()
    val currentPlayer = st.players.getOrNull(st.currentPlayerIndex)
    val isMyTurn = currentPlayer?.userId == uid
    val opponents = st.players.filter { it.userId != uid }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MeerMau", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Runde ${st.round}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Opponents ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                opponents.forEach { opp ->
                    val isOppTurn = currentPlayer?.userId == opp.userId
                    Surface(
                        color = if (isOppTurn) MeermauViolet.copy(alpha = 0.18f) else SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.5.dp, if (isOppTurn) MeermauViolet else Color(0xFF1E3050), RoundedCornerShape(12.dp)),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(opp.avatarUrl, fontSize = 22.sp)
                            Text(opp.displayName, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${opp.hand.size} Karten · ${opp.totalScore}P", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (opp.eliminated) Text("❌ Aus", style = MaterialTheme.typography.labelSmall, color = Danger)
                            // Face-down cards
                            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp), modifier = Modifier.padding(top = 6.dp)) {
                                opp.hand.take(5).forEach { _ ->
                                    Box(
                                        modifier = Modifier.size(width = 28.dp, height = 40.dp).clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, MeermauViolet.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                                    ) { CardBackScene(modifier = Modifier.fillMaxSize()) }
                                }
                                if (opp.hand.size > 5) Text("+${opp.hand.size - 5}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            }
                        }
                    }
                }
            }

            // ── Table ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        // Draw pile
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(width = 56.dp, height = 80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MeermauViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))) {
                                CardBackScene(modifier = Modifier.fillMaxSize())
                            }
                            Text("${st.drawPile.size} 🂠", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (st.drawPending > 0) Text("+${st.drawPending}", style = MaterialTheme.typography.labelMedium, color = Danger, fontWeight = FontWeight.Bold)
                        }

                        // Direction indicator
                        Text(if (st.direction == 1) "→" else "←", fontSize = 24.sp, color = MeermauViolet)

                        // Discard pile
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (topCard != null) {
                                MMPlayingCard(rank = topCard.rank, suit = topCard.suit, faceUp = true, selected = false)
                            }
                            Text("Ablage", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }

                    // Wish suit / skip indicators
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (st.wishSuit != null) {
                            Surface(color = MeermauViolet.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("Farbe: ${st.wishSuit}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = MeermauViolet, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (st.lastSkippedId != null) {
                            Spacer(Modifier.width(8.dp))
                            val skippedName = st.players.find { it.userId == st.lastSkippedId }?.displayName ?: ""
                            Surface(color = Danger.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                Text("$skippedName ausgesetzt", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Danger, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // ── Status ──
            Text(
                text = when {
                    st.aiThinking -> "${currentPlayer?.displayName ?: ""} denkt…"
                    isMyTurn && st.phase == "PLAYING" -> "Du bist dran"
                    isMyTurn && st.phase == "MAU_CHECK" -> "Sag MAU!"
                    st.phase == "PLAYING" -> "${currentPlayer?.displayName ?: ""} ist dran"
                    else -> ""
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = if (isMyTurn) MeermauViolet else TextSub,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = st.lastActionText,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            // ── Player hand ──
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text(
                    "Du · ${myHand.size + if (st.drawnCard != null && isMyTurn) 1 else 0} Karten · ${myPlayer?.totalScore ?: 0} Punkte",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    myHand.forEach { card ->
                        val canPlay = topCard != null && isMyTurn && st.phase == "PLAYING" &&
                            canPlayMM(card, topCard, st.wishSuit, st.drawPending, st.settings)
                        MMPlayingCard(
                            rank = card.rank, suit = card.suit, faceUp = true,
                            selected = selectedCardId == card.id,
                            modifier = Modifier
                                .clickable(enabled = canPlay || (isMyTurn && selectedCardId == card.id)) {
                                    selectedCardId = if (selectedCardId == card.id) null else if (canPlay) card.id else null
                                }
                                .alpha(if (canPlay || selectedCardId == card.id || !isMyTurn) 1f else 0.45f)
                                .offset(y = if (selectedCardId == card.id) (-8).dp else 0.dp),
                        )
                    }
                    val drawnCard = st.drawnCard
                    if (drawnCard != null && isMyTurn) {
                        val canPlay = topCard != null && canPlayMM(drawnCard, topCard, st.wishSuit, st.drawPending, st.settings)
                        Box {
                            MMPlayingCard(
                                rank = drawnCard.rank, suit = drawnCard.suit, faceUp = true,
                                selected = selectedCardId == drawnCard.id,
                                modifier = Modifier.clickable(enabled = isMyTurn && canPlay) {
                                    selectedCardId = if (selectedCardId == drawnCard.id) null else drawnCard.id
                                }.alpha(if (isMyTurn && canPlay) 1f else 0.5f).offset(y = if (selectedCardId == drawnCard.id) (-8).dp else 0.dp),
                            )
                            Surface(color = MeermauViolet, shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)) {
                                Text("NEU", fontSize = 7.sp, color = Color.White, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
            }

            // ── Action buttons ──
            val drawnCard = st.drawnCard
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (drawnCard != null && isMyTurn) {
                    // Keep or play drawn card
                    OutlinedButton(
                        onClick = {
                            val newHand = myHand + drawnCard
                            val nextIdx = nextMMIdx(st.currentPlayerIndex, st.direction, st.players)
                            val newPlayers = st.players.toMutableList().also {
                                it[st.currentPlayerIndex] = it[st.currentPlayerIndex].copy(hand = newHand)
                            }
                            val ns = st.copy(players = newPlayers, drawnCard = null, currentPlayerIndex = nextIdx, lastActionText = "Du behältst die Karte")
                            localState = ns; selectedCardId = null
                            writeOnline(ns)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                    ) { Text("Behalten") }

                    val canPlayDrawn = topCard != null && canPlayMM(drawnCard, topCard, st.wishSuit, st.drawPending, st.settings)
                    Button(
                        onClick = {
                            val s = doMMPlay(st, st.currentPlayerIndex, drawnCard.id)
                            localState = s; selectedCardId = null
                            writeOnline(s)
                        },
                        enabled = canPlayDrawn,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MeermauViolet, disabledContainerColor = MeermauViolet.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Spielen", fontWeight = FontWeight.Bold) }
                } else {
                    // Draw or play
                    OutlinedButton(
                        onClick = {
                            if (!isMyTurn || st.phase != "PLAYING") return@OutlinedButton
                            localState = doMMDraw(st, st.currentPlayerIndex)
                            selectedCardId = null
                        },
                        enabled = isMyTurn && st.phase == "PLAYING" && drawnCard == null,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                    ) { Text(if (st.drawPending > 0) "Ziehen (+${st.drawPending})" else "Karte ziehen") }

                    Button(
                        onClick = {
                            val cid = selectedCardId ?: return@Button
                            val s = doMMPlay(st, st.currentPlayerIndex, cid)
                            localState = s; selectedCardId = null
                            writeOnline(s)
                        },
                        enabled = isMyTurn && st.phase == "PLAYING" && selectedCardId != null,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MeermauViolet, disabledContainerColor = MeermauViolet.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Spielen", fontWeight = FontWeight.Bold) }
                }
            }

            // MAU button
            if (st.phase == "MAU_CHECK" && st.mauPlayerId == uid) {
                Button(
                    onClick = {
                        val ns = st.copy(phase = "PLAYING", mauPlayerId = null, lastActionText = "MAU!")
                        localState = ns; writeOnline(ns)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SandGold),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("MAU! 🂠", fontWeight = FontWeight.Black, fontSize = 18.sp, color = BgDark)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── WISH dialog ──
    if (st.phase == "WISH") {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Farbe wählen", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MM_SUITS.forEach { suit ->
                            val isRed = suit in MM_RED_SUITS
                            Button(
                                onClick = {
                                    if (st.discardPile.isEmpty()) return@Button
                                    val playerIdx = st.currentPlayerIndex
                                    val player2 = st.players[playerIdx]
                                    val ns = when {
                                        player2.hand.isEmpty() -> mmResolveRound(st.copy(wishSuit = suit), playerIdx)
                                        player2.hand.size == 1 -> {
                                            val nextIdx = nextMMIdx(playerIdx, st.direction, st.players)
                                            st.copy(wishSuit = suit, phase = "MAU_CHECK", mauPlayerId = player2.userId, currentPlayerIndex = nextIdx)
                                        }
                                        else -> {
                                            val nextIdx = nextMMIdx(playerIdx, st.direction, st.players)
                                            st.copy(wishSuit = suit, phase = "PLAYING", currentPlayerIndex = nextIdx, lastActionText = "${player2.displayName} wünscht $suit!")
                                        }
                                    }
                                    localState = ns; writeOnline(ns)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isRed) Danger else Color(0xFF1A1A2E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(56.dp),
                            ) { Text(suit, fontSize = 22.sp) }
                        }
                    }
                }
            }
        }
    }

    // ── Round end / Game over sheet ──
    if (st.phase == "ROUND_END" || st.phase == "GAME_OVER") {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            containerColor = SurfaceDark,
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (st.phase == "GAME_OVER") "🏆 Spiel vorbei!" else "Runde vorbei!",
                    style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold,
                )
                val winner = st.players.find { it.userId == if (st.phase == "GAME_OVER") st.gameWinnerId else st.roundWinnerId }
                if (winner != null) {
                    Surface(color = SandGold.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🏆", fontSize = 24.sp)
                            Text("${winner.avatarUrl} ${winner.displayName}", style = MaterialTheme.typography.titleMedium, color = SandGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = Surface2Dark)
                Text("Gesamtpunkte", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    st.players.sortedBy { it.totalScore }.forEach { p ->
                        val isGameWinner = p.userId == st.gameWinnerId
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.avatarUrl} ${p.displayName}${if (p.eliminated) " ❌" else ""}", style = MaterialTheme.typography.bodyMedium, color = if (isGameWinner) SandGold else TextPrimary)
                            Text("${p.totalScore} P", style = MaterialTheme.typography.bodyMedium, color = if (p.totalScore >= 100) Danger else TextSub, fontWeight = if (isGameWinner) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                if (st.phase == "ROUND_END") {
                    if (mode == "online") {
                        Text(
                            if (uid == adminId) "Neue Runde wird gestartet…" else "Warte auf Host…",
                            color = TextMuted, style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Button(
                            onClick = { localState = mmStartNewRound(st); selectedCardId = null; scope.launch { sheetState.hide() } },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeermauViolet),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Nächste Runde 🂠", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MeermauViolet),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Zur Lobby", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Playing card composable ───────────────────────────────────────────────────

@Composable
private fun MMPlayingCard(
    rank: String,
    suit: String,
    faceUp: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val isRed = suit in MM_RED_SUITS
    val cardColor = if (isRed) Danger else Color(0xFF1A1A2E)
    val borderColor = if (selected) MeermauViolet else Color(0xFFDDE0E4)

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(width = if (selected) 2.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .background(if (faceUp) Color(0xFFFFFBF0) else Color(0xFF0D1F3C)),
    ) {
        if (faceUp) {
            Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                Text(rank, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cardColor, lineHeight = 12.sp)
                Text(suit, fontSize = 10.sp, color = cardColor, lineHeight = 11.sp)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(suit, fontSize = 22.sp, color = cardColor)
                }
            }
        } else {
            CardBackScene(modifier = Modifier.fillMaxSize())
        }
    }
}
