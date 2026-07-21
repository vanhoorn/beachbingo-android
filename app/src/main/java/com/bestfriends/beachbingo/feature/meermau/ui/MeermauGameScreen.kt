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
import androidx.compose.foundation.layout.PaddingValues
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
private val MauOrange = Color(0xFFE67E22)
private val MauMauGreen = Color(0xFF27AE60)

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

data class MoveLogEntry(val round: Int, val playerName: String, val detail: String, val ts: Long)

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
    val pendingMau: String? = null,
    val pendingMauMau: String? = null,
    val mauMauReady: Boolean = false,
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
    val moveLog: List<MoveLogEntry> = emptyList(),
    val turnId: Int = 0,
)

// ── Pure game logic ───────────────────────────────────────────────────────────

private fun createMMDeck(): List<MMCard> =
    MM_SUITS.flatMap { s -> MM_RANKS.map { r -> MMCard(s, r, "$s$r") } }

private fun shuffleMMDeck(deck: List<MMCard>): List<MMCard> = deck.shuffled()

/** Fix: J is always a wildcard in normal play (no drawPending, no wishSuit). */
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
    return card.rank == "J" || card.suit == top.suit || card.rank == top.rank
}

private fun mmHandPoints(hand: List<MMCard>): Int = hand.sumOf { MM_CARD_POINTS[it.rank] ?: 0 }

private fun bestWishSuitMM(hand: List<MMCard>): String =
    MM_SUITS.associateWith { s -> hand.count { it.suit == s } }.maxByOrNull { it.value }?.key ?: "♠"

private fun mmAIMove(
    hand: List<MMCard>, top: MMCard, wishSuit: String?,
    drawPending: Int, difficulty: String, settings: MMSettings,
): Pair<String?, String?> {
    val playable = hand.filter { canPlayMM(it, top, wishSuit, drawPending, settings) }
    if (difficulty == "ROOKIE") {
        if (playable.isEmpty() || Random.nextFloat() < 0.15f) return null to null
        val chosen = playable.random()
        val ws = if (chosen.rank == "J" || (settings.wildOn10 && chosen.rank == "10"))
            bestWishSuitMM(hand.filter { it.id != chosen.id }) else null
        return chosen.id to ws
    }
    if (playable.isEmpty()) return null to null
    fun weight(r: String) = when (r) { "7" -> 40; "8" -> 30; "J" -> 20; "9" -> 10; else -> 0 }
    val chosen = playable.maxByOrNull { weight(it.rank) + (MM_CARD_POINTS[it.rank] ?: 0) }!!
    val ws = if (chosen.rank == "J" || (settings.wildOn10 && chosen.rank == "10"))
        bestWishSuitMM(hand.filter { it.id != chosen.id }) else null
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
    return shuffleMMDeck(discard.dropLast(1)) to listOf(top)
}

/** If a player played to 1 card but hasn't called MAU before another player acts → 1 penalty card. */
private fun applyPendingMauPenalty(state: MMState): MMState {
    val pendingId = state.pendingMau ?: return state
    if (state.players.getOrNull(state.currentPlayerIndex)?.userId == pendingId) return state
    val pidx = state.players.indexOfFirst { it.userId == pendingId }
    if (pidx < 0) return state.copy(pendingMau = null)
    val pp = state.players[pidx]
    val (draw, discard) = mmReshuffleIfNeeded(state.drawPile, state.discardPile)
    if (draw.isEmpty()) return state.copy(pendingMau = null)
    val newPlayers = state.players.toMutableList().also { it[pidx] = pp.copy(hand = pp.hand + draw.first()) }
    val entry = MoveLogEntry(state.round, pp.displayName, "${pp.displayName} vergisst MAU — zieht 1 Strafkarte", System.currentTimeMillis())
    return state.copy(players = newPlayers, drawPile = draw.drop(1), discardPile = discard, pendingMau = null, moveLog = state.moveLog + entry)
}

/** If a player played their last card without calling MAU MAU before another player acts → 1 penalty card. */
private fun applyPendingMauMauPenalty(state: MMState): MMState {
    val pendingId = state.pendingMauMau ?: return state
    if (state.players.getOrNull(state.currentPlayerIndex)?.userId == pendingId) return state
    val pidx = state.players.indexOfFirst { it.userId == pendingId }
    if (pidx < 0) return state.copy(pendingMauMau = null)
    val pp = state.players[pidx]
    val (draw, discard) = mmReshuffleIfNeeded(state.drawPile, state.discardPile)
    if (draw.isEmpty()) return state.copy(pendingMauMau = null)
    val newPlayers = state.players.toMutableList().also { it[pidx] = pp.copy(hand = pp.hand + draw.first()) }
    val entry = MoveLogEntry(state.round, pp.displayName, "${pp.displayName} vergisst MAU MAU — zieht 1 Strafkarte", System.currentTimeMillis())
    return state.copy(players = newPlayers, drawPile = draw.drop(1), discardPile = discard, pendingMauMau = null, moveLog = state.moveLog + entry)
}

private fun doMMPlay(state: MMState, playerIdx: Int, cardId: String, chosenWishSuit: String? = null): MMState {
    var st = applyPendingMauPenalty(state)
    st = applyPendingMauMauPenalty(st)

    val player = st.players[playerIdx]
    val card = player.hand.find { it.id == cardId } ?: return st
    val newHand = player.hand.filter { it.id != cardId }
    val newDiscard = st.discardPile + card
    val newPlayers = st.players.toMutableList().also { it[playerIdx] = player.copy(hand = newHand) }

    var nextDrawPending = st.drawPending
    var nextWishSuit: String? = if (card.rank == "7" || card.rank == "8") st.wishSuit else null
    var nextDir = st.direction
    var extraSkip = 0
    var action = "${player.displayName} spielt ${card.rank}${card.suit}"

    when (card.rank) {
        "7" -> {
            nextDrawPending = st.drawPending + 2
            action = "${player.displayName} spielt 7 → ${st.drawPending + 2} Karten ziehen!"
        }
        "8" -> {
            if (st.drawPending > 0 && st.settings.stopperOn8) {
                nextDrawPending = 0
                action = "${player.displayName} stoppt den Stapel mit 8!"
            } else {
                extraSkip = 1
                action = "${player.displayName} spielt 8 → ${st.players[nextMMIdx(playerIdx, nextDir, st.players)].displayName} ausgesetzt!"
            }
        }
        "9" -> {
            if (st.settings.reverseOn9) {
                nextDir = -st.direction
                action = "${player.displayName} spielt 9 → Richtung umgekehrt!"
            }
        }
        "J" -> {
            if (player.isAI || chosenWishSuit != null) {
                nextWishSuit = chosenWishSuit ?: bestWishSuitMM(newHand)
                action = "${player.displayName} spielt Bube → wünscht ${nextWishSuit}!"
            } else {
                val entry = MoveLogEntry(st.round, player.displayName, action, System.currentTimeMillis())
                return st.copy(players = newPlayers, discardPile = newDiscard, drawnCard = null,
                    phase = "WISH", lastActionText = "${player.displayName} spielt Bube – Farbe wählen!",
                    moveLog = st.moveLog + entry)
            }
        }
        "10" -> {
            if (st.settings.wildOn10) {
                if (player.isAI || chosenWishSuit != null) {
                    nextWishSuit = chosenWishSuit ?: bestWishSuitMM(newHand)
                    action = "${player.displayName} spielt 10 → wünscht ${nextWishSuit}!"
                } else {
                    val entry = MoveLogEntry(st.round, player.displayName, action, System.currentTimeMillis())
                    return st.copy(players = newPlayers, discardPile = newDiscard, drawnCard = null,
                        phase = "WISH", lastActionText = "${player.displayName} spielt 10 – Farbe wählen!",
                        moveLog = st.moveLog + entry)
                }
            }
        }
    }

    val skippedId = if (extraSkip > 0) st.players[nextMMIdx(playerIdx, nextDir, st.players)].userId else null
    val nextIdx = nextMMIdx(playerIdx, nextDir,
        newPlayers.map { if (it.userId == skippedId) it.copy(eliminated = true) else it })
    val (rDraw, rDiscard) = mmReshuffleIfNeeded(st.drawPile, newDiscard)
    val entry = MoveLogEntry(st.round, player.displayName, action, System.currentTimeMillis())

    // ── 0 cards: MAU MAU path ──
    if (newHand.isEmpty()) {
        val base = st.copy(
            players = newPlayers, drawPile = rDraw, discardPile = rDiscard,
            direction = nextDir, drawPending = nextDrawPending, wishSuit = nextWishSuit,
            drawnCard = null, lastActionText = action, lastSkippedId = skippedId,
            turnId = st.turnId + 1, moveLog = st.moveLog + entry,
        )
        return if (playerIdx != 0 || st.mauMauReady) {
            // AI always wins; human wins if they pre-declared MAU MAU
            mmResolveRound(base.copy(mauMauReady = false, pendingMauMau = null, roundWinnerId = player.userId), playerIdx)
        } else {
            // Human played last card without pressing MAU MAU — penalty pending
            base.copy(currentPlayerIndex = nextIdx, phase = "PLAYING",
                pendingMauMau = player.userId, mauMauReady = false,
                lastActionText = "${player.displayName} spielt letzte Karte!")
        }
    }

    // ── 1 card: MAU path ──
    if (newHand.size == 1) {
        val autoMau = playerIdx != 0 || st.mauPlayerId == player.userId
        val mauTxt = "${player.displayName}: MAU!"
        val logs = if (autoMau) {
            val mauEntry = MoveLogEntry(st.round, player.displayName, mauTxt, System.currentTimeMillis() + 1)
            st.moveLog + entry + mauEntry
        } else {
            st.moveLog + entry
        }
        return st.copy(
            players = newPlayers, drawPile = rDraw, discardPile = rDiscard,
            currentPlayerIndex = nextIdx, direction = nextDir,
            drawPending = nextDrawPending, wishSuit = nextWishSuit,
            drawnCard = null, phase = "PLAYING",
            mauPlayerId = if (autoMau) player.userId else null,
            pendingMau = if (!autoMau) player.userId else null,
            pendingMauMau = null,
            lastActionText = if (autoMau) mauTxt else action,
            aiThinking = false, lastSkippedId = skippedId,
            turnId = st.turnId + 1, moveLog = logs,
        )
    }

    // ── Normal play ──
    return st.copy(
        players = newPlayers, drawPile = rDraw, discardPile = rDiscard,
        currentPlayerIndex = nextIdx, direction = nextDir,
        drawPending = nextDrawPending, wishSuit = nextWishSuit,
        drawnCard = null, lastActionText = action, phase = "PLAYING",
        lastSkippedId = skippedId, turnId = st.turnId + 1, moveLog = st.moveLog + entry,
    )
}

private fun mmResolveRound(state: MMState, winnerIdx: Int): MMState {
    val winner = state.players[winnerIdx]
    val newScores = state.roundScores.toMutableMap()
    state.players.forEachIndexed { idx, p ->
        if (!p.eliminated && idx != winnerIdx) {
            newScores[p.userId] = (newScores[p.userId] ?: 0) + mmHandPoints(p.hand)
        }
    }
    val newPlayers = state.players.map { p ->
        val total = newScores[p.userId] ?: p.totalScore
        p.copy(totalScore = total, eliminated = total >= 100 || (p.eliminated && p.userId != winner.userId))
    }
    val alive = newPlayers.filter { !it.eliminated }
    val winEntry = MoveLogEntry(state.round, winner.displayName, "🏆 ${winner.displayName} gewinnt die Runde!", System.currentTimeMillis())
    val cleared = state.copy(mauMauReady = false, pendingMau = null, pendingMauMau = null, mauPlayerId = null)
    return if (alive.size <= 1) {
        cleared.copy(
            players = newPlayers, roundScores = newScores, roundWinnerId = winner.userId,
            gameWinnerId = (alive.firstOrNull() ?: winner).userId,
            phase = "GAME_OVER", lastActionText = "🏆 ${winner.displayName} gewinnt das Spiel!",
            moveLog = state.moveLog + winEntry,
        )
    } else {
        cleared.copy(
            players = newPlayers, roundScores = newScores, roundWinnerId = winner.userId,
            phase = "ROUND_END", lastActionText = "${winner.displayName} ist raus!",
            moveLog = state.moveLog + winEntry,
        )
    }
}

private fun mmStartNewRound(state: MMState): MMState {
    val alive = state.players.filter { !it.eliminated }
    val (hands, drawPile, topCard) = mmDealCards(alive.size)
    val newPlayers = state.players.mapIndexed { _, p ->
        val aliveIdx = alive.indexOfFirst { it.userId == p.userId }
        if (!p.eliminated && aliveIdx >= 0) p.copy(hand = hands[aliveIdx]) else p
    }
    val firstIdx = newPlayers.indexOfFirst { !it.eliminated }.coerceAtLeast(0)
    val roundEntry = MoveLogEntry(state.round + 1, "System", "── Runde ${state.round + 1} beginnt ──", System.currentTimeMillis())
    return state.copy(
        players = newPlayers, drawPile = drawPile, discardPile = listOf(topCard),
        currentPlayerIndex = firstIdx, direction = 1, drawPending = 0, wishSuit = null,
        phase = "PLAYING", mauPlayerId = null, pendingMau = null, pendingMauMau = null,
        mauMauReady = false, drawnCard = null, roundWinnerId = null,
        roundScores = state.players.associate { it.userId to it.totalScore },
        round = state.round + 1, lastActionText = "Runde ${state.round + 1} beginnt!",
        aiThinking = false, lastSkippedId = null, moveLog = state.moveLog + roundEntry,
    )
}

private fun doMMDraw(state: MMState, playerIdx: Int): MMState {
    var st = applyPendingMauPenalty(state)
    st = applyPendingMauMauPenalty(st)

    val player = st.players[playerIdx]
    var (draw, discard) = mmReshuffleIfNeeded(st.drawPile, st.discardPile)
    if (draw.isEmpty()) return st.copy(lastActionText = "Stapel leer – kein Ziehen möglich")

    val newMauPlayerId = if (player.userId == st.mauPlayerId) null else st.mauPlayerId

    if (st.drawPending > 0) {
        val count = minOf(st.drawPending, draw.size)
        val drawnCards = draw.take(count)
        draw = draw.drop(count)
        val newPlayers = st.players.toMutableList().also {
            it[playerIdx] = player.copy(hand = player.hand + drawnCards)
        }
        val nextIdx = nextMMIdx(playerIdx, st.direction, st.players)
        val txt = "${player.displayName} zieht $count Karten (Strafe)"
        val entry = MoveLogEntry(st.round, player.displayName, txt, System.currentTimeMillis())
        return st.copy(
            players = newPlayers, drawPile = draw, discardPile = discard,
            currentPlayerIndex = nextIdx, drawPending = 0, drawnCard = null,
            mauPlayerId = newMauPlayerId, lastActionText = txt,
            turnId = st.turnId + 1, moveLog = st.moveLog + entry,
        )
    }

    val drawnCard = draw.first()
    draw = draw.drop(1)
    val txt = "${player.displayName} zieht eine Karte"
    val entry = MoveLogEntry(st.round, player.displayName, txt, System.currentTimeMillis())
    return st.copy(drawPile = draw, discardPile = discard, drawnCard = drawnCard,
        mauPlayerId = newMauPlayerId, lastActionText = txt, moveLog = st.moveLog + entry)
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

// ── Deck helper ───────────────────────────────────────────────────────────────

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
    val logSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var localState by remember { mutableStateOf<MMState?>(null) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var adminId by remember { mutableStateOf("") }
    var onlinePlayerIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLog by remember { mutableStateOf(false) }

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

    // ── Init ──────────────────────────────────────────────────────────────────
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

    // ── Online snapshot listener ───────────────────────────────────────────────
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

    // ── Online: admin starts new round after ROUND_END ─────────────────────────
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

    // ── AI turn ───────────────────────────────────────────────────────────────
    LaunchedEffect(localState?.currentPlayerIndex, localState?.phase, localState?.drawnCard, localState?.turnId) {
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

    // ── AI drawn-card decision ─────────────────────────────────────────────────
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
            val ws = if (drawnCard.rank == "J" || (st.settings.wildOn10 && drawnCard.rank == "10"))
                bestWishSuitMM(current.hand) else null
            val stWithCard = st.copy(
                players = st.players.toMutableList().also {
                    it[st.currentPlayerIndex] = current.copy(hand = current.hand + drawnCard)
                },
                drawnCard = null,
            )
            doMMPlay(stWithCard, stWithCard.currentPlayerIndex, drawnCard.id, ws)
        } else {
            val newHand = current.hand + drawnCard
            val nextIdx = nextMMIdx(st.currentPlayerIndex, st.direction, st.players)
            val newPlayers = st.players.toMutableList().also {
                it[st.currentPlayerIndex] = current.copy(hand = newHand)
            }
            st.copy(players = newPlayers, drawnCard = null, currentPlayerIndex = nextIdx,
                lastActionText = "${current.displayName} behält die Karte")
        }
    }

    // ── Save result on game over ───────────────────────────────────────────────
    LaunchedEffect(localState?.phase) {
        val st = localState ?: return@LaunchedEffect
        if (st.phase != "GAME_OVER") return@LaunchedEffect
        val winnerId = st.gameWinnerId ?: return@LaunchedEffect
        try {
            db.collection("meermauResults").add(
                mapOf(
                    "winnerId" to winnerId,
                    "playerIds" to st.players.map { it.userId },
                    "players" to st.players.map {
                        mapOf("userId" to it.userId, "displayName" to it.displayName, "avatarUrl" to it.avatarUrl)
                    },
                    "rounds" to st.round,
                    "mode" to mode,
                    "difficulty" to difficulty,
                    "createdAt" to System.currentTimeMillis(),
                )
            ).await()
        } catch (_: Exception) {}
    }

    // ── Render ────────────────────────────────────────────────────────────────
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
    val isMyTurn = currentPlayer?.userId == uid && !st.aiThinking
    val opponents = st.players.filter { it.userId != uid }
    val drawnCard = st.drawnCard

    val playableIds = if (isMyTurn && st.phase == "PLAYING" && topCard != null)
        myHand.filter { canPlayMM(it, topCard, st.wishSuit, st.drawPending, st.settings) }.map { it.id }.toSet()
    else emptySet()

    val canPlaySelected = selectedCardId != null && playableIds.contains(selectedCardId)
    val hasAnyPlayable = playableIds.isNotEmpty()

    // ── MAU / MAU-MAU handlers ─────────────────────────────────────────────
    fun handleMau() {
        val s = localState ?: return
        val postPlay = s.pendingMau == uid
        val prePlay = isMyTurn && s.phase == "PLAYING" && myHand.size == 2 && canPlaySelected && drawnCard == null
        if (!postPlay && !prePlay) return
        val mp = s.players[0]
        val mauTxt = "${mp.displayName}: MAU!"
        val entry = MoveLogEntry(s.round, mp.displayName, mauTxt, System.currentTimeMillis())
        localState = s.copy(pendingMau = null, mauPlayerId = uid, lastActionText = mauTxt, moveLog = s.moveLog + entry)
    }

    fun handleMauMau() {
        val s = localState ?: return
        if (s.pendingMauMau == uid) {
            val ph = s.players.find { it.userId == uid } ?: return
            val entry = MoveLogEntry(s.round, ph.displayName, "🏆 ${ph.displayName}: MAU MAU!", System.currentTimeMillis())
            val winnerIdx = s.players.indexOfFirst { it.userId == uid }
            localState = mmResolveRound(
                s.copy(pendingMauMau = null, roundWinnerId = uid,
                    lastActionText = "🏆 ${ph.displayName}: MAU MAU!",
                    moveLog = s.moveLog + entry),
                winnerIdx,
            )
            return
        }
        if (!isMyTurn || s.phase != "PLAYING" || myHand.size != 1 || !hasAnyPlayable || s.mauMauReady || drawnCard != null) return
        val mp = s.players[0]
        val mauTxt = "${mp.displayName}: MAU MAU! (bereit)"
        val entry = MoveLogEntry(s.round, mp.displayName, mauTxt, System.currentTimeMillis())
        localState = s.copy(mauMauReady = true, moveLog = s.moveLog + entry)
    }

    val statusText = when {
        st.pendingMauMau == uid -> "⚡ Schnell MAU MAU drücken!"
        st.pendingMau == uid    -> "⚡ Schnell MAU drücken!"
        st.aiThinking           -> "${currentPlayer?.displayName ?: ""} denkt…"
        isMyTurn && st.phase == "PLAYING" -> "Du bist dran"
        st.phase == "PLAYING"   -> "${currentPlayer?.displayName ?: ""} ist dran"
        else                    -> ""
    }

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
                actions = {
                    Box {
                        IconButton(onClick = { showLog = true }) {
                            Text("📋", fontSize = 18.sp)
                        }
                        if (st.moveLog.isNotEmpty()) {
                            Surface(
                                color = MeermauViolet, shape = RoundedCornerShape(50),
                                modifier = Modifier.size(16.dp).align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (st.moveLog.size > 99) "99+" else "${st.moveLog.size}",
                                        fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                opponents.forEach { opp ->
                    val isOppTurn = currentPlayer?.userId == opp.userId
                    val isMau = opp.userId == st.mauPlayerId
                    Surface(
                        color = if (isOppTurn) MeermauViolet.copy(alpha = 0.18f) else SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.5.dp, if (isOppTurn) MeermauViolet else Color(0xFF1E3050), RoundedCornerShape(12.dp)),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(opp.avatarUrl, fontSize = 22.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(opp.displayName, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                if (isMau) Text("MAU!", style = MaterialTheme.typography.labelSmall, color = MeermauViolet, fontWeight = FontWeight.Black)
                            }
                            Text("${opp.hand.size} Karten · ${opp.totalScore}P", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (opp.eliminated) Text("❌ Aus", style = MaterialTheme.typography.labelSmall, color = Danger)
                            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp), modifier = Modifier.padding(top = 6.dp)) {
                                opp.hand.take(5).forEach { _ ->
                                    Box(modifier = Modifier.size(width = 28.dp, height = 40.dp).clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, MeermauViolet.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) {
                                        CardBackScene(modifier = Modifier.fillMaxSize())
                                    }
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
                shape = RoundedCornerShape(60.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1a5c2e)),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFF8B7355)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(width = 56.dp, height = 80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MeermauViolet.copy(alpha = 0.4f), RoundedCornerShape(8.dp))) {
                                CardBackScene(modifier = Modifier.fillMaxSize())
                            }
                            Text("${st.drawPile.size} 🂠", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            if (st.drawPending > 0) Text("+${st.drawPending}", style = MaterialTheme.typography.labelMedium, color = Danger, fontWeight = FontWeight.Bold)
                        }
                        Text(if (st.direction == 1) "→" else "←", fontSize = 24.sp, color = MeermauViolet)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (topCard != null) MMPlayingCard(rank = topCard.rank, suit = topCard.suit, faceUp = true, selected = false)
                            Text("Ablage", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
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
                text = statusText,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    st.pendingMauMau == uid || st.pendingMau == uid -> MauOrange
                    isMyTurn -> MeermauViolet
                    else -> TextSub
                },
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
                    "Du · ${myHand.size + if (drawnCard != null && isMyTurn) 1 else 0} Karten · ${myPlayer?.totalScore ?: 0} Punkte",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    myHand.forEach { card ->
                        val canPlay = topCard != null && isMyTurn && st.phase == "PLAYING" && playableIds.contains(card.id)
                        MMPlayingCard(
                            rank = card.rank, suit = card.suit, faceUp = true,
                            selected = selectedCardId == card.id,
                            modifier = Modifier
                                .clickable(enabled = isMyTurn && st.phase == "PLAYING") {
                                    selectedCardId = if (selectedCardId == card.id) null
                                    else if (canPlay || selectedCardId != null) card.id
                                    else null
                                }
                                .alpha(if (canPlay || selectedCardId == card.id || !isMyTurn) 1f else 0.45f)
                                .offset(y = if (selectedCardId == card.id) (-8).dp else 0.dp),
                        )
                    }
                    if (drawnCard != null && isMyTurn) {
                        val canPlayDrawn = topCard != null && canPlayMM(drawnCard, topCard, st.wishSuit, st.drawPending, st.settings)
                        Box {
                            MMPlayingCard(
                                rank = drawnCard.rank, suit = drawnCard.suit, faceUp = true,
                                selected = selectedCardId == drawnCard.id,
                                modifier = Modifier
                                    .clickable(enabled = isMyTurn && canPlayDrawn) {
                                        selectedCardId = if (selectedCardId == drawnCard.id) null else drawnCard.id
                                    }
                                    .alpha(if (isMyTurn && canPlayDrawn) 1f else 0.5f)
                                    .offset(y = if (selectedCardId == drawnCard.id) (-8).dp else 0.dp),
                            )
                            Surface(color = MeermauViolet, shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)) {
                                Text("NEU", fontSize = 7.sp, color = Color.White, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
            }

            // ── Action buttons ──
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Ziehen/Behalten + Spielen
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (drawnCard != null && isMyTurn) {
                        OutlinedButton(
                            onClick = {
                                val newHand = myHand + drawnCard
                                val nextIdx = nextMMIdx(st.currentPlayerIndex, st.direction, st.players)
                                val newPlayers = st.players.toMutableList().also {
                                    it[st.currentPlayerIndex] = it[st.currentPlayerIndex].copy(hand = newHand)
                                }
                                val ns = st.copy(players = newPlayers, drawnCard = null, currentPlayerIndex = nextIdx, lastActionText = "Du behältst die Karte")
                                localState = ns; selectedCardId = null; writeOnline(ns)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                        ) { Text("Behalten") }
                        val canPlayDrawn = topCard != null && canPlayMM(drawnCard, topCard, st.wishSuit, st.drawPending, st.settings)
                        Button(
                            onClick = {
                                val humanPlayer2 = st.players[st.currentPlayerIndex]
                                val stWithCard = st.copy(
                                    players = st.players.toMutableList().also {
                                        it[st.currentPlayerIndex] = humanPlayer2.copy(hand = humanPlayer2.hand + drawnCard)
                                    },
                                    drawnCard = null,
                                )
                                val s = doMMPlay(stWithCard, stWithCard.currentPlayerIndex, drawnCard.id)
                                localState = s; selectedCardId = null; writeOnline(s)
                            },
                            enabled = canPlayDrawn,
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeermauViolet, disabledContainerColor = MeermauViolet.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Spielen", fontWeight = FontWeight.Bold) }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (!isMyTurn || st.phase != "PLAYING") return@OutlinedButton
                                localState = doMMDraw(st, st.currentPlayerIndex); selectedCardId = null
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
                                localState = s; selectedCardId = null; writeOnline(s)
                            },
                            enabled = isMyTurn && st.phase == "PLAYING" && canPlaySelected && drawnCard == null,
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeermauViolet, disabledContainerColor = MeermauViolet.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Spielen", fontWeight = FontWeight.Bold) }
                    }
                }

                // Row 2: MAU + MAU MAU buttons
                val showMau = (isMyTurn && st.phase == "PLAYING" && myHand.size == 2 && canPlaySelected && drawnCard == null)
                    || st.pendingMau == uid
                val showMauMau = ((isMyTurn && st.phase == "PLAYING" && myHand.size == 1 && hasAnyPlayable && !st.mauMauReady && drawnCard == null)
                    || st.pendingMauMau == uid)
                val showMauMauReady = st.mauMauReady && isMyTurn && st.phase == "PLAYING" && myHand.size == 1

                if (showMau || showMauMau || showMauMauReady) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showMau) {
                            Button(
                                onClick = { handleMau() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MauOrange),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("🂠 MAU!", fontWeight = FontWeight.Black, fontSize = 16.sp) }
                        }
                        if (showMauMau) {
                            Button(
                                onClick = { handleMauMau() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MauMauGreen),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("🏆 MAU MAU!", fontWeight = FontWeight.Black, fontSize = 16.sp) }
                        }
                        if (showMauMauReady) {
                            Surface(
                                color = MauMauGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(50.dp).border(1.dp, MauMauGreen, RoundedCornerShape(12.dp)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓ MAU MAU gerufen!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MauMauGreen)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Spielverlauf sheet ────────────────────────────────────────────────────
    if (showLog) {
        ModalBottomSheet(
            onDismissRequest = { showLog = false },
            sheetState = logSheetState,
            containerColor = SurfaceDark,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📋 Spielverlauf", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { showLog = false; scope.launch { logSheetState.hide() } }) {
                        Text("✕", color = TextMuted, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (st.moveLog.isEmpty()) {
                        Text("Noch keine Spielzüge", color = TextMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                    } else {
                        st.moveLog.reversed().forEach { entry ->
                            val isSystem = entry.playerName == "System"
                            val isMAU = entry.detail.contains("MAU")
                            val isDraw = entry.detail.contains("zieht")
                            val isWin = entry.detail.contains("gewinnt") || entry.detail.contains("🏆")
                            if (isSystem) {
                                Text(entry.detail, style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                            } else {
                                val borderColor = when {
                                    isWin || isMAU -> MeermauViolet
                                    isDraw -> Danger
                                    else -> Color(0xFF1E3050)
                                }
                                val textColor = when {
                                    isWin || isMAU -> MeermauViolet
                                    isDraw -> Danger
                                    else -> TextPrimary
                                }
                                Surface(
                                    color = when {
                                        isWin -> MeermauViolet.copy(alpha = 0.12f)
                                        isMAU -> MeermauViolet.copy(alpha = 0.08f)
                                        isDraw -> Danger.copy(alpha = 0.08f)
                                        else -> Surface2Dark
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = textColor, fontWeight = if (isWin || isMAU) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            "Runde ${entry.round} · ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.GERMAN).format(java.util.Date(entry.ts))}",
                                            style = MaterialTheme.typography.labelSmall, color = TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ── WISH dialog ───────────────────────────────────────────────────────────
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
                                        player2.hand.isEmpty() -> {
                                            if (playerIdx != 0 || st.mauMauReady) {
                                                mmResolveRound(st.copy(wishSuit = suit, mauMauReady = false, pendingMauMau = null, roundWinnerId = player2.userId), playerIdx)
                                            } else {
                                                val ni = nextMMIdx(playerIdx, st.direction, st.players)
                                                st.copy(wishSuit = suit, phase = "PLAYING", currentPlayerIndex = ni,
                                                    pendingMauMau = player2.userId, mauMauReady = false,
                                                    lastActionText = "${player2.displayName} spielt letzte Karte!")
                                            }
                                        }
                                        player2.hand.size == 1 -> {
                                            val ni = nextMMIdx(playerIdx, st.direction, st.players)
                                            val autoMau = playerIdx != 0 || st.mauPlayerId == player2.userId
                                            val mauTxt = "${player2.displayName}: MAU!"
                                            if (autoMau) {
                                                val mauEntry = MoveLogEntry(st.round, player2.displayName, mauTxt, System.currentTimeMillis())
                                                st.copy(wishSuit = suit, phase = "PLAYING", currentPlayerIndex = ni,
                                                    mauPlayerId = player2.userId, pendingMau = null,
                                                    lastActionText = mauTxt, moveLog = st.moveLog + mauEntry)
                                            } else {
                                                st.copy(wishSuit = suit, phase = "PLAYING", currentPlayerIndex = ni,
                                                    mauPlayerId = null, pendingMau = player2.userId,
                                                    lastActionText = "${player2.displayName} wünscht $suit!")
                                            }
                                        }
                                        else -> {
                                            val ni = nextMMIdx(playerIdx, st.direction, st.players)
                                            st.copy(wishSuit = suit, phase = "PLAYING", currentPlayerIndex = ni,
                                                lastActionText = "${player2.displayName} wünscht $suit!")
                                        }
                                    }
                                    localState = ns; writeOnline(ns)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF5F5F5),
                                    contentColor = if (isRed) Color(0xFFCC0000) else Color(0xFF111111),
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(64.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(suit, fontSize = 28.sp, lineHeight = 28.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Round end / Game over sheet ───────────────────────────────────────────
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
