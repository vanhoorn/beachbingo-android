package com.bestfriends.beachbingo.feature.klontausch

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// ── Enums ───────────────────────────────────────────────────────────────────

enum class KlonPart { KOPF, KOERPER, BEINE }

// ── Data classes ────────────────────────────────────────────────────────────

@Serializable
data class KlonCard(
    val cardId: String,
    val characterId: String,
    val part: String, // KlonPart name
)

@Serializable
data class KlonOffer(
    val type: String = "NONE",       // "NONE" | "OPEN"
    val fromUserId: String = "",
    val part: String = "",           // KlonPart name
    val committedCardId: String = "",
    val responderIds: List<String> = emptyList(),  // accepted
    val declinedIds: List<String> = emptyList(),   // declined
    val selectedResponderId: String = "",
    val responderCardId: String = "",
)

@Serializable
data class KlonPlayerState(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val heldCards: List<KlonCard> = emptyList(),
    val cardCount: Int = 0,
    val isAI: Boolean = false,
    val isEliminated: Boolean = false,
)

@Serializable
data class KlonGameState(
    val players: Map<String, KlonPlayerState> = emptyMap(),
    val playerIds: List<String> = emptyList(),
    val turnIndex: Int = 0,
    val offer: KlonOffer = KlonOffer(),
    val status: String = "LOBBY",    // "LOBBY" | "PLAYING" | "FINISHED"
    val winnerId: String = "",
    val adminId: String = "",
)

@Serializable
data class KlontauschSaveState(
    val gameState: KlonGameState,
    val myTargetIds: List<String>,
    val aiTargets: Map<String, List<String>>,
)

// ── Setup helpers ────────────────────────────────────────────────────────────

fun klonPartOf(card: KlonCard): KlonPart = KlonPart.valueOf(card.part)

private val DECK_JSON = Json { ignoreUnknownKeys = true }

/** Build a deck of 3 cards (KOPF/KOERPER/BEINE) per character, for the given character pool. */
fun buildDeck(characterIds: List<String>): List<KlonCard> {
    val cards = mutableListOf<KlonCard>()
    for (charId in characterIds) {
        for (part in KlonPart.entries) {
            cards.add(KlonCard(
                cardId = "${charId}_${part.name}_${(1..99999).random()}",
                characterId = charId,
                part = part.name,
            ))
        }
    }
    return cards.shuffled()
}

/**
 * Draw player count × 3 characters from the library, build 9 cards per player,
 * deal 9 to each player, and return the initial players map + private target lists.
 *
 * Returns Pair<Map<uid, KlonPlayerState>, Map<uid, List<String>>>
 *   second value = target character IDs per uid
 */
fun dealGame(
    playerMap: Map<String, KlonPlayerState>,
    playerIds: List<String>,
): Pair<Map<String, KlonPlayerState>, Map<String, List<String>>> {
    val n = playerIds.size
    val pool = ALL_KLON_CHARACTERS.shuffled().take(n * 3)
    val deck = buildDeck(pool.map { it.id }).toMutableList()

    val targets = mutableMapOf<String, List<String>>()
    val dealt = mutableMapOf<String, KlonPlayerState>()

    // assign 3 target characters per player (non-overlapping from pool thirds)
    playerIds.forEachIndexed { i, uid ->
        val myTargets = pool.subList(i * 3, i * 3 + 3).map { it.id }
        targets[uid] = myTargets
    }

    // deal 9 cards to each player (FIFO from shuffled deck)
    playerIds.forEach { uid ->
        val hand = deck.take(9)
        repeat(9) { deck.removeFirst() }
        dealt[uid] = playerMap.getValue(uid).copy(heldCards = hand, cardCount = hand.size)
    }

    return dealt to targets
}

// ── Firestore serialization ──────────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toKlonGameState(): KlonGameState {
    val playerIds = (this["playerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val adminId   = this["adminId"] as? String ?: ""
    val turnIndex = (this["turnIndex"] as? Long)?.toInt() ?: 0
    val status    = this["status"] as? String ?: "LOBBY"
    val winnerId  = this["winnerId"] as? String ?: ""

    val rawOffer  = this["offer"] as? Map<String, Any>
    val offer = if (rawOffer != null) rawOffer.toKlonOffer() else KlonOffer()

    val rawPlayers = this["players"] as? Map<*, *> ?: emptyMap<String, Any>()
    val players = rawPlayers.entries.mapNotNull { (k, v) ->
        val uid = k as? String ?: return@mapNotNull null
        val pm  = v as? Map<String, Any> ?: return@mapNotNull null
        uid to pm.toKlonPlayerState(uid)
    }.toMap()

    return KlonGameState(players, playerIds, turnIndex, offer, status, winnerId, adminId)
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.toKlonPlayerState(uid: String): KlonPlayerState {
    val displayName = this["displayName"] as? String ?: ""
    val avatarUrl   = this["avatarUrl"] as? String ?: ""
    val isAI        = this["isAI"] as? Boolean ?: false
    val cardCount   = (this["cardCount"] as? Long)?.toInt() ?: 0
    val rawCards    = this["heldCards"] as? List<*> ?: emptyList<Any>()
    val heldCards   = rawCards.mapNotNull { raw ->
        (raw as? Map<String, Any>)?.let {
            KlonCard(
                cardId      = it["cardId"] as? String ?: "",
                characterId = it["characterId"] as? String ?: "",
                part        = it["part"] as? String ?: "KOPF",
            )
        }
    }
    return KlonPlayerState(uid, displayName, avatarUrl, heldCards, cardCount, isAI)
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.toKlonOffer(): KlonOffer {
    return KlonOffer(
        type               = this["type"] as? String ?: "NONE",
        fromUserId         = this["fromUserId"] as? String ?: "",
        part               = this["part"] as? String ?: "",
        committedCardId    = this["committedCardId"] as? String ?: "",
        responderIds       = (this["responderIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        declinedIds        = (this["declinedIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        selectedResponderId= this["selectedResponderId"] as? String ?: "",
        responderCardId    = this["responderCardId"] as? String ?: "",
    )
}

fun KlonOffer.toFirestoreMap(): Map<String, Any> = mapOf(
    "type"                to type,
    "fromUserId"          to fromUserId,
    "part"                to part,
    "committedCardId"     to committedCardId,
    "responderIds"        to responderIds,
    "declinedIds"         to declinedIds,
    "selectedResponderId" to selectedResponderId,
    "responderCardId"     to responderCardId,
)

fun KlonPlayerState.toFirestoreMap(): Map<String, Any> = mapOf(
    "userId"       to userId,
    "displayName"  to displayName,
    "avatarUrl"    to avatarUrl,
    "heldCards"    to heldCards.map { c ->
        mapOf("cardId" to c.cardId, "characterId" to c.characterId, "part" to c.part)
    },
    "cardCount"    to heldCards.size,
    "isAI"         to isAI,
    "isEliminated" to isEliminated,
)

// ── Game actions ─────────────────────────────────────────────────────────────

/**
 * Returns the character IDs for which this player holds all 3 parts AND the character
 * is in [targetIds] (the player's own target characters).
 * Only completed TARGET characters are "safe" — accidentally completed non-target sets
 * are not protected and can still be mopsed.
 */
fun safeCharacterIds(player: KlonPlayerState, targetIds: Collection<String>): Set<String> {
    if (targetIds.isEmpty()) return emptySet()
    val allParts = KlonPart.entries.map { it.name }.toSet()
    return player.heldCards
        .filter { it.characterId in targetIds }
        .groupBy { it.characterId }
        .filter { (_, cards) -> cards.map { it.part }.toSet() == allParts }
        .keys
        .toSet()
}

/**
 * Checks if a player has all 3 parts of each of their target characters.
 * Returns true if the player has won.
 */
fun KlonPlayerState.hasWon(targetCharacterIds: List<String>): Boolean {
    if (targetCharacterIds.size < 3) return false
    return targetCharacterIds.all { charId ->
        val partsHeld = heldCards.filter { it.characterId == charId }.map { it.part }.toSet()
        KlonPart.entries.all { p -> p.name in partsHeld }
    }
}

/**
 * Execute a "Mopsen" (take) action: the active player takes one random non-safe card
 * from the explicitly chosen [targetUid].
 */
fun executeNehmen(
    state: KlonGameState,
    activeUid: String,
    targetUid: String,
    targetPlayerTargetIds: List<String> = emptyList(),
): KlonGameState {
    val targetP  = state.players[targetUid] ?: return state
    if (targetP.heldCards.isEmpty()) return state.copy(turnIndex = state.turnIndex + 1, offer = KlonOffer())

    val safeIds  = safeCharacterIds(targetP, targetPlayerTargetIds)
    val takeable = targetP.heldCards.filter { it.characterId !in safeIds }
    if (takeable.isEmpty()) return state.copy(turnIndex = state.turnIndex + 1, offer = KlonOffer())

    val takenCard       = takeable.random()
    val newTargetCards  = targetP.heldCards - takenCard
    val activeP         = state.players[activeUid] ?: return state
    val newActiveCards  = activeP.heldCards + takenCard

    val newPlayers = state.players.toMutableMap()
    newPlayers[targetUid] = targetP.copy(heldCards = newTargetCards, cardCount = newTargetCards.size)
    newPlayers[activeUid] = activeP.copy(heldCards = newActiveCards, cardCount = newActiveCards.size)

    return state.copy(
        players   = newPlayers,
        turnIndex = state.turnIndex + 1,
        offer     = KlonOffer(),
    )
}

/**
 * Active player opens a trade offer for a specific card.
 * Cards from completed character sets cannot be offered.
 */
fun openOffer(
    state: KlonGameState,
    activeUid: String,
    cardId: String,
): KlonGameState {
    val activeP = state.players[activeUid] ?: return state
    val card = activeP.heldCards.find { it.cardId == cardId } ?: return state
    if (card.characterId in safeCharacterIds(activeP, emptyList())) return state
    val offer = KlonOffer(
        type            = "OPEN",
        fromUserId      = activeUid,
        part            = card.part,
        committedCardId = cardId,
    )
    return state.copy(offer = offer)
}

/**
 * A player responds to the active offer.
 * They must have a non-safe matching card to meld.
 */
fun respondToOffer(
    state: KlonGameState,
    responderId: String,
): KlonGameState {
    val offer = state.offer
    if (offer.type != "OPEN" || responderId in offer.responderIds) return state
    if (responderId == offer.fromUserId) return state
    val responder = state.players[responderId] ?: return state
    val safeIds   = safeCharacterIds(responder, emptyList())
    val hasPart   = responder.heldCards.any { it.part == offer.part && it.characterId !in safeIds }
    if (!hasPart) return state
    return state.copy(offer = offer.copy(responderIds = offer.responderIds + responderId))
}

/**
 * Withdraw own response from offer (switch back to undecided).
 */
fun withdrawResponse(
    state: KlonGameState,
    responderId: String,
): KlonGameState {
    val offer = state.offer
    if (offer.type != "OPEN") return state
    return state.copy(offer = offer.copy(
        responderIds = offer.responderIds - responderId,
        declinedIds  = offer.declinedIds  - responderId,
    ))
}

/**
 * Player declines the open offer (they don't want to swap).
 */
fun declineOffer(
    state: KlonGameState,
    responderId: String,
): KlonGameState {
    val offer = state.offer
    if (offer.type != "OPEN" || responderId == offer.fromUserId) return state
    if (responderId in offer.declinedIds) return state
    return state.copy(offer = offer.copy(
        responderIds = offer.responderIds - responderId,
        declinedIds  = offer.declinedIds  + responderId,
    ))
}

/**
 * Offering player selects a partner and the swap happens.
 * Only non-safe cards are eligible on the responder's side.
 */
fun selectPartnerAndSwap(
    state: KlonGameState,
    selectedResponderId: String,
): KlonGameState {
    val offer = state.offer
    if (offer.type != "OPEN") return state
    val offerer   = state.players[offer.fromUserId] ?: return state
    val responder = state.players[selectedResponderId] ?: return state

    val offererCard = offerer.heldCards.find { it.cardId == offer.committedCardId } ?: return state
    val safeIds     = safeCharacterIds(responder, emptyList())
    val responderCandidates = responder.heldCards.filter { it.part == offer.part && it.characterId !in safeIds }
    if (responderCandidates.isEmpty()) return state
    val responderCard = responderCandidates.random()

    val newOffererCards   = offerer.heldCards - offererCard + responderCard
    val newResponderCards = responder.heldCards - responderCard + offererCard

    val newPlayers = state.players.toMutableMap()
    newPlayers[offer.fromUserId]    = offerer.copy(heldCards = newOffererCards, cardCount = newOffererCards.size)
    newPlayers[selectedResponderId] = responder.copy(heldCards = newResponderCards, cardCount = newResponderCards.size)

    return state.copy(
        players   = newPlayers,
        turnIndex = state.turnIndex + 1,
        offer     = KlonOffer(),
    )
}

/**
 * Cancel/skip an open offer without swapping (offerer changes mind).
 */
fun cancelOffer(state: KlonGameState): KlonGameState =
    state.copy(offer = KlonOffer(), turnIndex = state.turnIndex + 1)

// ── AI ────────────────────────────────────────────────────────────────────────

val AI_KLON_NAMES = listOf("🤖 Möwe", "🤖 Krabbe", "🤖 Fisch", "🤖 Hai", "🤖 Delfin")

/**
 * Decide AI move. Returns updated state after AI acts.
 * AI logic (1 level):
 *  - If there's an open offer from someone else AND AI has a matching card → 70% chance to respond.
 *  - On own turn → 50% Nehmen / 50% Tauschen (random card of random part).
 */
fun aiDecideMove(
    state: KlonGameState,
    aiUid: String,
    targetIds: List<String>,
): KlonGameState {
    val activeTurnUid = state.playerIds.getOrNull(state.turnIndex % state.playerIds.size) ?: return state
    val offer = state.offer

    // Not this AI's turn but there's an open offer from someone else → accept or decline
    if (activeTurnUid != aiUid && offer.type == "OPEN" && offer.fromUserId != aiUid) {
        if (aiUid in offer.responderIds || aiUid in offer.declinedIds) return state
        val aiPlayer = state.players[aiUid] ?: return state
        val safeIds  = safeCharacterIds(aiPlayer, targetIds)
        val hasPart  = aiPlayer.heldCards.any { it.part == offer.part && it.characterId !in safeIds }
        return if (hasPart && Math.random() < 0.70) {
            respondToOffer(state, aiUid)
        } else {
            declineOffer(state, aiUid)
        }
    }

    // AI's own turn
    if (activeTurnUid != aiUid) return state

    // AI already has an open offer — check for responders
    if (offer.type == "OPEN" && offer.fromUserId == aiUid) {
        return if (offer.responderIds.isNotEmpty()) {
            selectPartnerAndSwap(state, offer.responderIds.random())
        } else {
            state // still waiting
        }
    }

    val aiPlayer = state.players[aiUid] ?: return state
    if (aiPlayer.heldCards.isEmpty()) {
        return state.copy(turnIndex = state.turnIndex + 1, offer = KlonOffer())
    }

    fun randomTarget(): String =
        state.playerIds.filter { it != aiUid }.randomOrNull() ?: aiUid

    return if (Math.random() < 0.50) {
        executeNehmen(state, aiUid, randomTarget())
    } else {
        // Only offer non-safe cards; prefer cards that don't match targets
        val safeIds       = safeCharacterIds(aiPlayer, targetIds)
        val offerable     = aiPlayer.heldCards.filter { it.characterId !in safeIds }
        if (offerable.isEmpty()) return executeNehmen(state, aiUid, randomTarget())
        val nonTargetCards = offerable.filter { c -> targetIds.none { tId -> c.characterId == tId } }
        val candidates     = nonTargetCards.ifEmpty { offerable }
        val card = candidates.random()
        openOffer(state, aiUid, card.cardId)
    }
}

// ── Game code generator ───────────────────────────────────────────────────────

fun generateKlonGameCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}
