package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.klontausch.*
import com.bestfriends.beachbingo.feature.shared.teamName
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val KlontauschAccent = Color(0xFF8B5CF6)
private const val OFFER_TIMEOUT_SECONDS = 15
private const val TAUSCHEN_ENABLED = false  // flip to true to re-enable Tauschen

private enum class KlonEventType { SWAP, STOLEN, COMPLETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlontauschGameScreen(
    mode: String,
    gameId: String?,
    aiCount: Int,
    difficulty: String,
    saveId: String?,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToResults: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var gameState by remember { mutableStateOf<KlonGameState?>(null) }
    var myTargetIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var paused by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    var aiTargets by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var offerSecondsLeft by remember { mutableIntStateOf(OFFER_TIMEOUT_SECONDS) }
    var showMopsePicker by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    val eventList = remember { mutableStateListOf<Pair<String, KlonEventType>>() }
    val prevCardsHolder = remember { mutableListOf<KlonCard>() }
    val prevCompleteIds = remember { mutableSetOf<String>() }

    fun addKlonEvent(text: String, type: KlonEventType) {
        eventList.add(0, Pair(text, type))
        while (eventList.size > 3) eventList.removeAt(eventList.size - 1)
    }

    // ── Firestore write helper (online) ───────────────────────────────────────
    fun pushState(newState: KlonGameState) {
        if (mode == "AI") {
            gameState = newState
        } else if (gameId != null) {
            val data = mapOf(
                "players"   to newState.players.mapValues { it.value.toFirestoreMap() },
                "playerIds" to newState.playerIds,
                "turnIndex" to newState.turnIndex,
                "offer"     to newState.offer.toFirestoreMap(),
                "status"    to newState.status,
                "winnerId"  to newState.winnerId,
            )
            db.collection("klontauschGames").document(gameId).update(data)
        }
    }

    // ── Offer countdown – auto-cancel after 30 s ──────────────────────────────
    val currentOfferType = gameState?.offer?.type ?: "NONE"
    val currentOfferOwner = gameState?.offer?.fromUserId ?: ""

    LaunchedEffect(currentOfferType, currentOfferOwner) {
        if (currentOfferType != "OPEN") {
            offerSecondsLeft = OFFER_TIMEOUT_SECONDS
            return@LaunchedEffect
        }
        offerSecondsLeft = OFFER_TIMEOUT_SECONDS
        while (offerSecondsLeft > 0) {
            delay(1000L)
            offerSecondsLeft--
        }
        val st2 = gameState ?: return@LaunchedEffect
        if (st2.offer.type == "OPEN" && st2.offer.fromUserId == uid) {
            pushState(cancelOffer(st2))
        }
    }

    // ── Initialize AI game ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (mode != "AI") return@LaunchedEffect
        try {
            val user = db.collection("users").document(uid).get().await()
            val displayName = user.getString("displayName") ?: "Du"
            val avatarUrl = user.getString("avatarUrl") ?: "🦖"

            val playerIds = mutableListOf(uid)
            val playerMap = mutableMapOf<String, KlonPlayerState>()
            playerMap[uid] = KlonPlayerState(uid, displayName, avatarUrl, isAI = false)
            for (i in 0 until aiCount) {
                val aiId = "ai_$i"
                val aiName = AI_KLON_NAMES.getOrElse(i) { "🤖 KI" }
                playerMap[aiId] = KlonPlayerState(aiId, aiName, "🤖", isAI = true)
                playerIds.add(aiId)
            }

            val (dealt, targets) = dealGame(playerMap, playerIds.shuffled())
            aiTargets = targets.filter { it.key != uid }
            myTargetIds = targets[uid] ?: emptyList()

            gameState = KlonGameState(
                players   = dealt,
                playerIds = playerIds.shuffled(),
                status    = "PLAYING",
                adminId   = uid,
            )
        } catch (_: Exception) {}
    }

    // ── Initialize Online game ────────────────────────────────────────────────
    LaunchedEffect(gameId) {
        if (mode != "ONLINE" || gameId == null) return@LaunchedEffect
        db.collection("klontauschGames").document(gameId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val data = snap.data ?: return@addSnapshotListener
                gameState = data.toKlonGameState()
            }
        try {
            val privateSnap = db.collection("klontauschGames").document(gameId)
                .collection("private").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            myTargetIds = (privateSnap.get("targetCharacterIds") as? List<String>) ?: emptyList()
        } catch (_: Exception) {}
    }

    // ── Card change detector (key = user's own cards, not full gameState) ───────
    val detectorMyCards = gameState?.players?.get(uid)?.heldCards ?: emptyList()
    LaunchedEffect(detectorMyCards) {
        if (prevCardsHolder.isEmpty()) {
            // Initial game load – just record baseline, no notifications
            prevCardsHolder.addAll(detectorMyCards)
            return@LaunchedEffect
        }
        val gained = detectorMyCards.filter { c -> prevCardsHolder.none { it.cardId == c.cardId } }
        val lost   = prevCardsHolder.filter { c -> detectorMyCards.none { it.cardId == c.cardId } }

        when {
            gained.isNotEmpty() && lost.isNotEmpty() -> {
                val g = gained.first(); val l = lost.first()
                val gChar = klonCharacterById(g.characterId)
                val lChar = klonCharacterById(l.characterId)
                val isGainedTarget = g.characterId in myTargetIds
                val isLostTarget   = l.characterId in myTargetIds
                addKlonEvent(buildString {
                    append("🔄 +${gChar.name} (${g.part})${if (isGainedTarget) " ⭐" else ""}")
                    append("  /  −${lChar.name} (${l.part})${if (isLostTarget) " 😱" else ""}")
                }, KlonEventType.SWAP)
            }
            gained.isNotEmpty() -> {
                val g = gained.first()
                val gChar = klonCharacterById(g.characterId)
                addKlonEvent(
                    if (g.characterId in myTargetIds)
                        "📥 ${gChar.name} (${g.part}) gemopst – Zielfigur! ⭐"
                    else
                        "📥 ${gChar.name} (${g.part}) gemopst",
                    KlonEventType.SWAP,
                )
            }
            lost.isNotEmpty() -> {
                val l = lost.first()
                val lChar = klonCharacterById(l.characterId)
                val thiefName = gameState?.players?.entries
                    ?.firstOrNull { (pid, p) -> pid != uid && p.heldCards.any { it.cardId == l.cardId } }
                    ?.value?.displayName ?: "Jemand"
                addKlonEvent(
                    if (l.characterId in myTargetIds)
                        "😱 $thiefName mopste ${lChar.name} (${l.part}) – Zielkarte!"
                    else
                        "😱 $thiefName mopste ${lChar.name} (${l.part})",
                    KlonEventType.STOLEN,
                )
            }
        }

        // Complete target check
        val nowComplete = myTargetIds.filter { charId ->
            KlonPart.entries.all { p -> detectorMyCards.any { it.characterId == charId && it.part == p.name } }
        }.toSet()
        val newlyComplete = nowComplete - prevCompleteIds
        if (newlyComplete.isNotEmpty()) {
            val name = klonCharacterById(newlyComplete.first()).name
            addKlonEvent("🏆 $name ist komplett!", KlonEventType.COMPLETE)
        }
        prevCompleteIds.clear(); prevCompleteIds.addAll(nowComplete)

        prevCardsHolder.clear(); prevCardsHolder.addAll(detectorMyCards)
    }

    // ── AI loop: AI's own turn ────────────────────────────────────────────────
    LaunchedEffect(gameState) {
        val st = gameState ?: return@LaunchedEffect
        if (mode != "AI" || paused || st.status != "PLAYING") return@LaunchedEffect

        val turnUid = st.playerIds.getOrNull(st.turnIndex % st.playerIds.size) ?: return@LaunchedEffect
        if (st.players[turnUid]?.isAI != true) return@LaunchedEffect

        delay(1200L)
        val newState = if (!TAUSCHEN_ENABLED) {
            // Tauschen disabled: AI always Mopsen from a random other player
            val others = st.playerIds.filter { it != turnUid }
            val targetUid = others.randomOrNull() ?: return@LaunchedEffect
            val targetTargetIds = if (targetUid == uid) myTargetIds else aiTargets[targetUid] ?: emptyList()
            executeNehmen(st, turnUid, targetUid, targetTargetIds)
        } else {
            val targets = aiTargets[turnUid] ?: emptyList()
            aiDecideMove(st, turnUid, targets)
        }

        val targets = aiTargets[turnUid] ?: emptyList()
        val winner = newState.players[turnUid]
        gameState = if (winner != null && winner.hasWon(targets)) {
            newState.copy(status = "FINISHED", winnerId = turnUid)
        } else {
            newState
        }
    }

    // ── AI loop: respond to human's open offer (only when Tauschen enabled) ──
    LaunchedEffect(gameState) {
        if (!TAUSCHEN_ENABLED) return@LaunchedEffect
        val st = gameState ?: return@LaunchedEffect
        if (mode != "AI" || paused || st.status != "PLAYING") return@LaunchedEffect
        if (st.offer.type != "OPEN" || st.offer.fromUserId != uid) return@LaunchedEffect

        val pendingAIs = st.playerIds.filter { pid ->
            st.players[pid]?.isAI == true &&
            pid !in st.offer.responderIds &&
            pid !in st.offer.declinedIds
        }
        if (pendingAIs.isEmpty()) return@LaunchedEffect

        delay(900L)
        var current = st
        for (aiId in pendingAIs) {
            val aiTargetList = aiTargets[aiId] ?: emptyList()
            current = aiDecideMove(current, aiId, aiTargetList)
        }
        if (current != st) gameState = current
    }

    // ── Save result to Firestore when game finishes ───────────────────────────
    var resultSaved by remember { mutableStateOf(false) }
    LaunchedEffect(gameState?.status) {
        val st = gameState ?: return@LaunchedEffect
        if (st.status != "FINISHED") return@LaunchedEffect
        showWinDialog = true
        if (resultSaved) return@LaunchedEffect
        resultSaved = true
        val winnerId   = st.winnerId
        val playerIds  = st.playerIds
        val teamKey    = playerIds.sorted().joinToString("|")
        try {
            db.collection("klontauschResults").add(mapOf(
                "winnerId"    to winnerId,
                "winnerName"  to (st.players[winnerId]?.displayName ?: ""),
                "winnerAvatar" to (st.players[winnerId]?.avatarUrl ?: ""),
                "playerIds"   to playerIds,
                "players"     to playerIds.mapNotNull { pid ->
                    val p = st.players[pid] ?: return@mapNotNull null
                    mapOf("userId" to pid, "displayName" to p.displayName, "avatarUrl" to p.avatarUrl)
                },
                "teamName"    to teamName(teamKey),
                "mode"        to mode,
                "difficulty"  to difficulty,
                "createdAt"   to System.currentTimeMillis(),
            )).await()
        } catch (_: Exception) {}
    }

    // ── Close picker when turn passes ─────────────────────────────────────────
    val currentTurnUidForPicker = gameState?.playerIds?.getOrNull(
        (gameState?.turnIndex ?: 0) % (gameState?.playerIds?.size?.coerceAtLeast(1) ?: 1)
    )
    LaunchedEffect(currentTurnUidForPicker) {
        if (currentTurnUidForPicker != uid) showMopsePicker = false
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun doMopsen(targetUid: String) {
        val st = gameState ?: return
        val targetTargetIds = aiTargets[targetUid] ?: emptyList()
        val newSt = executeNehmen(st, uid, targetUid, targetTargetIds)
        val me = newSt.players[uid]
        pushState(
            if (me != null && me.hasWon(myTargetIds)) newSt.copy(status = "FINISHED", winnerId = uid)
            else newSt
        )
    }

    fun doTauschen() {
        val st = gameState ?: return
        val cardId = selectedCardId ?: return
        pushState(openOffer(st, uid, cardId))
        selectedCardId = null
    }

    fun doMelden() {
        val st = gameState ?: return
        pushState(respondToOffer(st, uid))
    }

    fun doWithdraw() {
        val st = gameState ?: return
        pushState(withdrawResponse(st, uid))
    }

    fun doDecline() {
        val st = gameState ?: return
        pushState(declineOffer(st, uid))
    }

    fun doSelectPartner(responderId: String) {
        val st = gameState ?: return
        val newSt = selectPartnerAndSwap(st, responderId)
        val me = newSt.players[uid]
        pushState(
            if (me != null && me.hasWon(myTargetIds)) newSt.copy(status = "FINISHED", winnerId = uid)
            else newSt
        )
    }

    fun doCancelOffer() {
        val st = gameState ?: return
        if (st.offer.fromUserId == uid) pushState(cancelOffer(st))
    }

    BackHandler {
        if (showWinDialog) onNavigateBack()
        else showQuitDialog = true
    }

    if (showWinDialog && gameState != null) {
        val finishedState = gameState!!
        KlontauschWinDialog(
            gameState   = finishedState,
            uid         = uid,
            onToLobby   = onNavigateBack,
        )
    }

    if (showRules) {
        ALL_GAME_RULES["klontausch"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }

    if (showQuitDialog) {
        GameSaveQuitDialog(
            emoji = "🃏",
            message = "Klontausch · ${gameState?.players?.size ?: 0} Spieler",
            onContinue = { showQuitDialog = false },
            onSaveAndQuit = { showQuitDialog = false; onNavigateBack() },
            onQuitWithoutSave = { showQuitDialog = false; onNavigateBack() },
        )
    }

    val st = gameState
    if (st == null) {
        Box(Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KlontauschAccent)
        }
        return
    }

    val turnUid = st.playerIds.getOrNull(st.turnIndex % st.playerIds.size) ?: uid
    val isMyTurn = turnUid == uid
    val myPlayer = st.players[uid]
    val offer = st.offer
    val iAmOfferer   = offer.type == "OPEN" && offer.fromUserId == uid
    val iAmResponder = offer.type == "OPEN" && uid in offer.responderIds
    val iAmDeclined  = offer.type == "OPEN" && uid in offer.declinedIds
    val myCards = myPlayer?.heldCards ?: emptyList()
    val mySafeIds = remember(myCards, myTargetIds) { myPlayer?.let { safeCharacterIds(it, myTargetIds) } ?: emptySet() }

    // For each target char + part: if not owned, pick a fill card (same part preferred, random)
    val targetFillCards = remember(myCards, myTargetIds) {
        val stockCards = myCards.filter { it.characterId !in myTargetIds }
        myTargetIds.associateWith { charId ->
            KlonPart.entries.associateWith { part ->
                val owned = myCards.any { it.characterId == charId && it.part == part.name }
                if (owned) null
                else stockCards.filter { it.part == part.name }.randomOrNull()
                    ?: stockCards.randomOrNull()
            }
        }
    }

    val stockKopf    = remember(myCards, myTargetIds) { myCards.filter { it.characterId !in myTargetIds && it.part == KlonPart.KOPF.name } }
    val stockKoerper = remember(myCards, myTargetIds) { myCards.filter { it.characterId !in myTargetIds && it.part == KlonPart.KOERPER.name } }
    val stockBeine   = remember(myCards, myTargetIds) { myCards.filter { it.characterId !in myTargetIds && it.part == KlonPart.BEINE.name } }

    // ── Main layout ───────────────────────────────────────────────────────────
    Column(Modifier.fillMaxSize().background(BgDark)) {

        GameHudBar(
            paused = paused,
            onPauseToggle = { paused = !paused },
            onQuit = { showQuitDialog = true },
            onShowRules = { showRules = true },
        ) {
            val turnName = st.players[turnUid]?.displayName ?: "..."
            Text(
                if (isMyTurn) "Dein Zug" else "$turnName ist dran",
                color = if (isMyTurn) KlontauschAccent else TextSub,
                fontWeight = if (isMyTurn) FontWeight.Bold else FontWeight.Normal,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
        }

        if (paused) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pause", color = TextSub, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
            }
            return@Column
        }

        // ── Spieler-Leiste + Mopsen (unter HUD) ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            st.playerIds.forEach { pid ->
                val p = st.players[pid] ?: return@forEach
                val isActive = pid == turnUid
                val canMopse = isMyTurn && pid != uid
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canMopse) OceanBlue.copy(alpha = 0.08f) else Color.Transparent)
                        .border(1.dp, if (canMopse) OceanBlue.copy(0.5f) else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable(enabled = canMopse) { doMopsen(pid) }
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                ) {
                    Box {
                        Text(p.avatarUrl, fontSize = MaterialTheme.typography.titleSmall.fontSize)
                        if (isActive) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(KlontauschAccent)
                                    .align(Alignment.TopEnd),
                            )
                        }
                    }
                    Text(
                        if (pid == uid) "Du" else p.displayName.take(6),
                        color = if (isActive) KlontauschAccent else if (canMopse) OceanBlue else TextMuted,
                        fontWeight = if (isActive || canMopse) FontWeight.Bold else FontWeight.Normal,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${p.cardCount} 🃏",
                        color = if (canMopse) OceanBlue.copy(0.8f) else TextMuted,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                }
            }
        }

        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

        // ── Ereignisliste (pinned) ────────────────────────────────────────────
        if (eventList.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                eventList.forEach { (text, type) ->
                    val eventColor = when (type) {
                        KlonEventType.SWAP     -> Color(0xFF22C55E)
                        KlonEventType.STOLEN   -> Crimson
                        KlonEventType.COMPLETE -> SandGold
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(eventColor))
                        Text(
                            text,
                            color = eventColor.copy(alpha = 0.9f),
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }

        // ── Scrollbarer Hauptbereich: Ziel- + Vorratsfiguren ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // Ziel-Figuren (Pager)
            if (myTargetIds.isNotEmpty()) {
                val pagerState = rememberPagerState { myTargetIds.size }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    val charId = myTargetIds[page]
                    val char = klonCharacterById(charId)
                    val hasKopf    = myCards.any { it.characterId == charId && it.part == KlonPart.KOPF.name }
                    val hasKoerper = myCards.any { it.characterId == charId && it.part == KlonPart.KOERPER.name }
                    val hasBeine   = myCards.any { it.characterId == charId && it.part == KlonPart.BEINE.name }
                    val allDone    = hasKopf && hasKoerper && hasBeine

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (allDone) KlontauschAccent.copy(0.12f) else SurfaceDark,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .border(1.dp, if (allDone) KlontauschAccent.copy(0.6f) else BorderColor, RoundedCornerShape(12.dp)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text("${page + 1} / ${myTargetIds.size}", color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                Text(char.name, color = if (allDone) KlontauschAccent else TextPrimary, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleSmall.fontSize)
                                if (allDone) Text("✅", fontSize = MaterialTheme.typography.titleSmall.fontSize)
                            }

                            BoxWithConstraints(Modifier.fillMaxWidth().height(216.dp)) {
                                val partH = maxHeight / 3f
                                val partW = maxWidth * 0.65f
                                val fillForChar = targetFillCards[charId]
                                Column(Modifier.width(partW).align(Alignment.Center)) {
                                    KlonPartSlot(charId, KlonPart.KOPF,    hasKopf,    partH, fillCard = if (!hasKopf)    fillForChar?.get(KlonPart.KOPF)    else null)
                                    KlonPartSlot(charId, KlonPart.KOERPER, hasKoerper, partH, fillCard = if (!hasKoerper) fillForChar?.get(KlonPart.KOERPER) else null)
                                    KlonPartSlot(charId, KlonPart.BEINE,   hasBeine,   partH, fillCard = if (!hasBeine)   fillForChar?.get(KlonPart.BEINE)   else null)
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(myTargetIds.size) { i ->
                        Box(Modifier.size(8.dp).padding(2.dp).clip(CircleShape).background(if (i == pagerState.currentPage) KlontauschAccent else BorderColor))
                    }
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Vorrats-Figuren
            KlontauschStockFigure(
                stockKopf    = stockKopf,
                stockKoerper = stockKoerper,
                stockBeine   = stockBeine,
            )

            Spacer(Modifier.height(8.dp))
        }

        // ── Aktionsbereich (fixiert unten) ────────────────────────────────────
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when {
                TAUSCHEN_ENABLED && offer.type == "OPEN" && !iAmOfferer -> {
                    OfferCardCompact(
                        offererName    = st.players[offer.fromUserId]?.displayName ?: "?",
                        part           = offer.part,
                        responderCount = offer.responderIds.size,
                        declinedCount  = offer.declinedIds.size,
                        secondsLeft    = offerSecondsLeft,
                        canMeld        = myCards.any { it.part == offer.part && it.characterId !in mySafeIds },
                        iAmResponder   = iAmResponder,
                        iAmDeclined    = iAmDeclined,
                        onAccept       = { doMelden() },
                        onDecline      = { doDecline() },
                        onWithdraw     = { doWithdraw() },
                    )
                }
                TAUSCHEN_ENABLED && iAmOfferer -> {
                    MyOfferCard(
                        part          = offer.part,
                        secondsLeft   = offerSecondsLeft,
                        responderIds  = offer.responderIds,
                        declinedCount = offer.declinedIds.size,
                        playerNames   = st.players.mapValues { it.value.displayName },
                        onSelectPartner = { doSelectPartner(it) },
                        onCancel      = { doCancelOffer() },
                    )
                }
                isMyTurn -> {
                    Text(
                        "Dein Zug – tippe auf einen Mitspieler zum Mopsen",
                        color = KlontauschAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(vertical = 10.dp),
                    )
                }
                else -> {
                    val waitName = st.players[turnUid]?.displayName ?: "..."
                    Text(
                        "⏳  $waitName überlegt…",
                        color = TextSub,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        modifier = Modifier.align(Alignment.Center).padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

// ── Action buttons composable ─────────────────────────────────────────────────

@Composable
private fun ActionButtons(
    onMopsen: () -> Unit,
    // TAUSCHEN_DISABLED: onTauschen, canTauschen, selectedCardLabel
) {
    Button(
        onClick = onMopsen,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
    ) {
        Text("Mopsen", fontWeight = FontWeight.Bold)
    }
}

// ── Mopsen: player picker ─────────────────────────────────────────────────────

@Composable
private fun MopsePicker(
    players: Map<String, KlonPlayerState>,
    playerIds: List<String>,
    onPickPlayer: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, OceanBlue.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Von wem mopsen?",
            color = OceanBlue,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        )
        playerIds.forEach { pid ->
            val p = players[pid] ?: return@forEach
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = OceanBlue.copy(0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OceanBlue.copy(0.35f), RoundedCornerShape(10.dp))
                    .clickable { onPickPlayer(pid) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(p.avatarUrl, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    Column(Modifier.weight(1f)) {
                        Text(p.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize)
                        Text("${p.cardCount} Karten", color = TextMuted,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                    Text("→", color = OceanBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Abbrechen", color = TextSub, fontSize = MaterialTheme.typography.labelMedium.fontSize)
        }
    }
}

// ── Offer card (from someone else) ───────────────────────────────────────────

@Composable
private fun OfferCardCompact(
    offererName: String,
    part: String,
    responderCount: Int,
    declinedCount: Int,
    secondsLeft: Int,
    canMeld: Boolean,
    iAmResponder: Boolean,
    iAmDeclined: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val timerColor = when {
        secondsLeft <= 5  -> Crimson
        secondsLeft <= 10 -> SandGold
        else              -> TextSub
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, SandGold.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "🤝 $offererName bietet $part-Tausch",
                color = SandGold,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
            Text(
                "${secondsLeft}s",
                color = timerColor,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
            )
        }
        if (responderCount > 0 || declinedCount > 0) {
            Text(
                "✅ $responderCount annehmen  •  ❌ $declinedCount ablehnen",
                color = TextMuted,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
        }
        when {
            iAmResponder -> {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                ) {
                    Text("Entscheidung ändern")
                }
            }
            iAmDeclined -> {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                ) {
                    Text("Entscheidung ändern")
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canMeld) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        ) {
                            Text("Annehmen", color = BgDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Crimson),
                        border = BorderStroke(1.dp, Crimson.copy(0.6f)),
                    ) {
                        Text("Ablehnen")
                    }
                }
                if (!canMeld) {
                    Text(
                        "Du hast keine $part-Karte — nur Ablehnen möglich.",
                        color = TextMuted,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                }
            }
        }
    }
}

// ── My own open offer ─────────────────────────────────────────────────────────

@Composable
private fun MyOfferCard(
    part: String,
    secondsLeft: Int,
    responderIds: List<String>,
    declinedCount: Int,
    playerNames: Map<String, String>,
    onSelectPartner: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val timerColor = when {
        secondsLeft <= 5  -> Crimson
        secondsLeft <= 10 -> SandGold
        else              -> TextSub
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, KlontauschAccent.copy(0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Mein Angebot: $part-Karte",
                color = KlontauschAccent,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
            Text(
                "${secondsLeft}s",
                color = timerColor,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
            )
        }

        if (declinedCount > 0 || responderIds.isNotEmpty()) {
            Text(
                "✅ ${responderIds.size} annehmen  •  ❌ $declinedCount ablehnen",
                color = TextMuted,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
        }
        if (responderIds.isEmpty()) {
            Text(
                "Warte auf Antworten…",
                color = TextMuted,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
        } else {
            Text(
                "Partner wählen:",
                color = TextSub,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(responderIds) { rid ->
                    val name = playerNames[rid] ?: rid
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OceanBlue.copy(0.15f),
                        modifier = Modifier
                            .border(1.dp, OceanBlue.copy(0.5f), RoundedCornerShape(10.dp))
                            .clickable { onSelectPartner(rid) },
                    ) {
                        Text(
                            name,
                            color = OceanBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(contentColor = Crimson),
        ) {
            Text("Angebot zurückziehen")
        }
    }
}

// ── Vorrats-Figur (3 unabhängige Pager per Körperteil) ───────────────────────

@Composable
private fun KlontauschStockFigure(
    stockKopf: List<KlonCard>,
    stockKoerper: List<KlonCard>,
    stockBeine: List<KlonCard>,
) {
    val scope        = rememberCoroutineScope()
    val kopfPager    = rememberPagerState { maxOf(stockKopf.size, 1) }
    val koerperPager = rememberPagerState { maxOf(stockKoerper.size, 1) }
    val beinePager   = rememberPagerState { maxOf(stockBeine.size, 1) }
    val totalStock   = stockKopf.size + stockKoerper.size + stockBeine.size

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Vorrat ($totalStock Karten)",
                color = TextSub,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
            )
            if (totalStock > 0) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OceanBlue, CircleShape)
                        .clickable {
                            scope.launch {
                                if (stockKopf.isNotEmpty())    kopfPager.animateScrollToPage(stockKopf.indices.random())
                                if (stockKoerper.isNotEmpty()) koerperPager.animateScrollToPage(stockKoerper.indices.random())
                                if (stockBeine.isNotEmpty())   beinePager.animateScrollToPage(stockBeine.indices.random())
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎲", fontSize = MaterialTheme.typography.labelLarge.fontSize)
                }
            }
        }

        if (totalStock == 0) {
            Text(
                "Alle Karten gehören zu deinen Zielfiguren.",
                color = TextMuted,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                val partH = 88.dp
                val partW = maxWidth * 0.62f

                Column(Modifier.width(partW).align(Alignment.Center)) {
                    // KOPF
                    Box(Modifier.fillMaxWidth().height(partH)) {
                        HorizontalPager(state = kopfPager, modifier = Modifier.fillMaxSize()) { page ->
                            val card = stockKopf.getOrNull(page)
                            if (card != null) KlontauschCharacterView(card.characterId, KlonPart.KOPF, Modifier.fillMaxSize().padding(1.dp))
                            else KlontauschSilhouette(KlonPart.KOPF, Modifier.fillMaxSize().padding(1.dp))
                        }
                        if (stockKopf.size > 1) StockPartDots(stockKopf.size, kopfPager.currentPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                    }
                    // KÖRPER
                    Box(Modifier.fillMaxWidth().height(partH)) {
                        HorizontalPager(state = koerperPager, modifier = Modifier.fillMaxSize()) { page ->
                            val card = stockKoerper.getOrNull(page)
                            if (card != null) KlontauschCharacterView(card.characterId, KlonPart.KOERPER, Modifier.fillMaxSize().padding(1.dp))
                            else KlontauschSilhouette(KlonPart.KOERPER, Modifier.fillMaxSize().padding(1.dp))
                        }
                        if (stockKoerper.size > 1) StockPartDots(stockKoerper.size, koerperPager.currentPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                    }
                    // BEINE
                    Box(Modifier.fillMaxWidth().height(partH)) {
                        HorizontalPager(state = beinePager, modifier = Modifier.fillMaxSize()) { page ->
                            val card = stockBeine.getOrNull(page)
                            if (card != null) KlontauschCharacterView(card.characterId, KlonPart.BEINE, Modifier.fillMaxSize().padding(1.dp))
                            else KlontauschSilhouette(KlonPart.BEINE, Modifier.fillMaxSize().padding(1.dp))
                        }
                        if (stockBeine.size > 1) StockPartDots(stockBeine.size, beinePager.currentPage, Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text("${stockKopf.size} Kopf", color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                Text("${stockKoerper.size} Koerper", color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                Text("${stockBeine.size} Beine", color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        }
    }
}

@Composable
private fun StockPartDots(count: Int, currentPage: Int, modifier: Modifier = Modifier) {
    val visible = minOf(count, 5)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(visible) { i ->
            val active = i == currentPage || (i == 4 && currentPage >= 4)
            Box(Modifier.size(4.dp).clip(CircleShape).background(if (active) OceanBlue else BorderColor))
        }
    }
}

// ── Winner dialog (undismissable) ────────────────────────────────────────────

@Composable
private fun KlontauschWinDialog(
    gameState: KlonGameState,
    uid: String,
    onToLobby: () -> Unit,
) {
    val winnerId   = gameState.winnerId
    val winner     = gameState.players[winnerId]
    val iAmWinner  = winnerId == uid
    val players    = gameState.playerIds.mapNotNull { gameState.players[it] }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, KlontauschAccent.copy(0.6f), RoundedCornerShape(24.dp)),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("🏆", fontSize = MaterialTheme.typography.displayMedium.fontSize)

                val winnerDisplayName = winner?.displayName ?: "?"
                Text(
                    if (iAmWinner) "Du hast gewonnen!" else "$winnerDisplayName hat gewonnen!",
                    color = SandGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    textAlign = TextAlign.Center,
                )

                if (winner != null) {
                    Text(
                        "${winner.avatarUrl}  ${winner.displayName}",
                        color = SandGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    )
                }

                HorizontalDivider(color = BorderColor)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    players.forEachIndexed { idx, p ->
                        val isWin = p.userId == winnerId
                        val isLast = idx == players.size - 1
                        val emoji = when {
                            idx == 0 -> "🥇"
                            idx == 1 -> "🥈"
                            idx == 2 && !isLast -> "🥉"
                            isLast   -> "🦀"
                            else     -> "🥉"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(emoji, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                            Text(p.avatarUrl, fontSize = MaterialTheme.typography.titleSmall.fontSize)
                            Text(
                                p.displayName + if (p.userId == uid) " (Du)" else "",
                                color = if (isWin) SandGold else TextSub,
                                fontWeight = if (isWin) FontWeight.Bold else FontWeight.Normal,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onToLobby,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KlontauschAccent),
                ) {
                    Text("Zur Lobby", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ── Part slot (in target figure pager) ───────────────────────────────────────

@Composable
private fun KlonPartSlot(
    characterId: String,
    part: KlonPart,
    owned: Boolean,
    height: androidx.compose.ui.unit.Dp,
    fillCard: KlonCard? = null,
) {
    val borderColor = when {
        owned    -> KlontauschAccent.copy(0.6f)
        fillCard != null -> BorderColor.copy(0.35f)
        else     -> BorderColor.copy(0.25f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(0.5.dp, borderColor),
        contentAlignment = Alignment.Center,
    ) {
        when {
            owned -> KlontauschCharacterView(
                characterId = characterId,
                part = part,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
            fillCard != null -> KlontauschCharacterView(
                characterId = fillCard.characterId,
                part = KlonPart.valueOf(fillCard.part),
                modifier = Modifier.fillMaxSize().padding(2.dp).alpha(0.35f),
            )
            else -> KlontauschSilhouette(
                part = part,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        }
    }
}
