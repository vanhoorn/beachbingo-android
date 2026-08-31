package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlin.random.Random
import com.bestfriends.beachbingo.feature.raetsel.GameSave
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val KlontauschAccent = Color(0xFF8B5CF6)
private const val OFFER_TIMEOUT_SECONDS = 15
@Suppress("MayBeConstant")
private val TAUSCHEN_ENABLED = false  // flip to true to re-enable Tauschen

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
    val context = LocalContext.current
    val audio = remember { KlontauschAudioManager(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    var gameState by remember { mutableStateOf<KlonGameState?>(null) }
    var myTargetIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var paused by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var aiTargets by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var offerSecondsLeft by remember { mutableIntStateOf(OFFER_TIMEOUT_SECONDS) }
    var showWinDialog by remember { mutableStateOf(false) }
    val eventList = remember { mutableStateListOf<Pair<String, KlonEventType>>() }
    val prevCardsHolder = remember { mutableListOf<KlonCard>() }
    val prevCompleteIds = remember { mutableSetOf<String>() }

    // Auto-Save on app background (AI mode only)
    DisposableEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        val callback = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                val st = gameState ?: return
                if (mode != "AI" || st.status == "FINISHED") return
                SoloGameSaveManager.saveGame(context, GameSave(
                    id = java.util.UUID.randomUUID().toString(),
                    gameType = "klontausch",
                    difficulty = difficulty,
                    gameState = Json.encodeToString(KlontauschSaveState(st, myTargetIds, aiTargets)),
                    displayLabel = "KI · $aiCount Gegner · Zug ${st.turnIndex}",
                    savedAt = System.currentTimeMillis(),
                ))
            }
        }
        activity?.lifecycle?.addObserver(callback)
        onDispose { activity?.lifecycle?.removeObserver(callback) }
    }

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
            delay(1.seconds)
            offerSecondsLeft--
        }
        val st2 = gameState ?: return@LaunchedEffect
        if (st2.offer.type == "OPEN" && st2.offer.fromUserId == uid) {
            pushState(cancelOffer(st2))
        }
    }

    // ── Initialize AI game ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (mode != "AI" || saveId != null) return@LaunchedEffect
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

    // ── Restore from save (AI mode) ───────────────────────────────────────────
    LaunchedEffect(saveId) {
        if (saveId == null || mode != "AI") return@LaunchedEffect
        val save = SoloGameSaveManager.getGameSave(context, "klontausch")
        if (save == null || save.id != saveId) return@LaunchedEffect
        try {
            val restored = Json.decodeFromString<KlontauschSaveState>(save.gameState)
            gameState = restored.gameState
            myTargetIds = restored.myTargetIds
            aiTargets = restored.aiTargets
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

    // ── Save result to Firestore — called directly at win, never in a coroutine ──
    var resultSaved by remember { mutableStateOf(false) }

    fun saveResult(st: KlonGameState) {
        if (resultSaved) return
        resultSaved = true
        val winnerId  = st.winnerId
        val playerIds = st.playerIds
        db.collection("klontauschResults").add(mapOf(
            "winnerId"    to winnerId,
            "winnerName"  to (st.players[winnerId]?.displayName ?: ""),
            "winnerAvatar" to (st.players[winnerId]?.avatarUrl ?: ""),
            "playerIds"   to playerIds,
            "players"     to playerIds.mapNotNull { pid ->
                val p = st.players[pid] ?: return@mapNotNull null
                mapOf("userId" to pid, "displayName" to p.displayName, "avatarUrl" to p.avatarUrl)
            },
            "teamName"    to teamName(playerIds.sorted().joinToString("|")),
            "mode"        to mode,
            "difficulty"  to difficulty,
            "createdAt"   to System.currentTimeMillis(),
        ))
    }

    LaunchedEffect(Unit) {
        audio.startMusic(soundEnabled, musicEnabled)
    }

    LaunchedEffect(gameState?.status) {
        if (gameState?.status == "FINISHED") {
            audio.stopMusic()
            SoloGameSaveManager.deleteGameSave(context, "klontausch")
            showWinDialog = true
        }
    }

    // ── AI loop: AI's own turn ────────────────────────────────────────────────
    LaunchedEffect(gameState) {
        val st = gameState ?: return@LaunchedEffect
        if (mode != "AI" || paused || st.status != "PLAYING") return@LaunchedEffect

        val turnUid = st.playerIds.getOrNull(st.turnIndex % st.playerIds.size) ?: return@LaunchedEffect
        if (st.players[turnUid]?.isAI != true) return@LaunchedEffect

        delay(1200.milliseconds)
        val others = st.playerIds.filter { it != turnUid }
        val targetUid = others.randomOrNull() ?: return@LaunchedEffect
        val targetTargetIds = if (targetUid == uid) myTargetIds else aiTargets[targetUid] ?: emptyList()
        val newState = executeNehmen(st, turnUid, targetUid, targetTargetIds)

        val targets = aiTargets[turnUid] ?: emptyList()
        val winner = newState.players[turnUid]
        if (winner != null && winner.hasWon(targets)) {
            val finalState = newState.copy(status = "FINISHED", winnerId = turnUid)
            saveResult(finalState)
            gameState = finalState
        } else {
            gameState = newState
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun doMopsen(targetUid: String) {
        val st = gameState ?: return
        val targetTargetIds = aiTargets[targetUid] ?: emptyList()
        val newSt = executeNehmen(st, uid, targetUid, targetTargetIds)
        val me = newSt.players[uid]
        if (me != null && me.hasWon(myTargetIds)) {
            val finalState = newSt.copy(status = "FINISHED", winnerId = uid)
            saveResult(finalState)
            pushState(finalState)
        } else {
            pushState(newSt)
        }
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
        if (me != null && me.hasWon(myTargetIds)) {
            val finalState = newSt.copy(status = "FINISHED", winnerId = uid)
            saveResult(finalState)
            pushState(finalState)
        } else {
            pushState(newSt)
        }
    }

    fun doCancelOffer() {
        val st = gameState ?: return
        if (st.offer.fromUserId == uid) pushState(cancelOffer(st))
    }

    BackHandler {
        if (showWinDialog) onNavigateBack()
        else showQuitDialog = true
    }

    if (showWinDialog) {
        KlontauschConfettiOverlay()
    }
    if (showWinDialog && gameState != null) {
        val finishedState = gameState!!
        KlontauschWinDialog(
            gameState           = finishedState,
            uid                 = uid,
            onToLobby           = onNavigateBack,
            onNavigateToResults = onNavigateToResults,
        )
    }

    if (showRules) {
        ALL_GAME_RULES["klontausch"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }

    if (showQuitDialog) {
        val st = gameState
        if (mode == "AI" && st != null) {
            GameSaveQuitDialog(
                emoji = "🃏",
                message = "KI · $aiCount Gegner · Zug ${st.turnIndex}",
                onContinue = { showQuitDialog = false },
                onSaveAndQuit = {
                    SoloGameSaveManager.saveGame(context, GameSave(
                        id = java.util.UUID.randomUUID().toString(),
                        gameType = "klontausch",
                        difficulty = difficulty,
                        gameState = Json.encodeToString(KlontauschSaveState(st, myTargetIds, aiTargets)),
                        displayLabel = "KI · $aiCount Gegner · Zug ${st.turnIndex}",
                        savedAt = System.currentTimeMillis(),
                    ))
                    showQuitDialog = false
                    onNavigateBack()
                },
                onQuitWithoutSave = {
                    SoloGameSaveManager.deleteGameSave(context, "klontausch")
                    showQuitDialog = false
                    onNavigateBack()
                },
            )
        } else {
            GameSaveQuitDialog(
                emoji = "🃏",
                message = "Klontausch verlassen?",
                onContinue = { showQuitDialog = false },
                onSaveAndQuit = { showQuitDialog = false; onNavigateBack() },
                onQuitWithoutSave = { showQuitDialog = false; onNavigateBack() },
            )
        }
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

        // ── Neueste Meldung (einzeilig, fixe Höhe) ───────────────────────────
        val latestEvent = eventList.firstOrNull()
        val latestEventColor = when (latestEvent?.second) {
            KlonEventType.SWAP     -> Color(0xFF22C55E)
            KlonEventType.STOLEN   -> Crimson
            KlonEventType.COMPLETE -> SandGold
            null                   -> Color.Transparent
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(if (latestEvent != null) SurfaceDark.copy(alpha = 0.6f) else Color.Transparent)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (latestEvent != null) {
                Text(
                    latestEvent.first,
                    color = latestEventColor.copy(alpha = 0.9f),
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── Hauptbereich: Ziel- + Vorratsfiguren (kein Scroll, beide immer sichtbar) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Ziel-Figuren (Pager) — 53 % der verfügbaren Höhe
            if (myTargetIds.isNotEmpty()) {
                val pagerState = rememberPagerState { myTargetIds.size }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.53f),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .border(1.5.dp, if (allDone) KlontauschAccent else KlontauschAccent.copy(0.45f), RoundedCornerShape(12.dp)),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text("${page + 1} / ${myTargetIds.size}", color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                    Text(char.name, color = if (allDone) KlontauschAccent else TextPrimary, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleSmall.fontSize)
                                    if (allDone) Text("✅", fontSize = MaterialTheme.typography.titleSmall.fontSize)
                                }

                                // Figuren-Bereich füllt den Rest der Karte
                                KlontauschFigurePartsBox(
                                    charId     = charId,
                                    hasKopf    = hasKopf,
                                    hasKoerper = hasKoerper,
                                    hasBeine   = hasBeine,
                                    modifier   = Modifier.fillMaxWidth().weight(1f),
                                )
                            }
                        }
                    }

                    // Pager-Punkte
                    Row(
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(myTargetIds.size) { i ->
                            Box(Modifier.size(8.dp).padding(2.dp).clip(CircleShape).background(if (i == pagerState.currentPage) KlontauschAccent else BorderColor))
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Vorrats-Figuren — 47 % (oder 100 % wenn keine Zielfiguren)
            KlontauschStockFigure(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (myTargetIds.isNotEmpty()) 0.47f else 1f),
                stockKopf    = stockKopf,
                stockKoerper = stockKoerper,
                stockBeine   = stockBeine,
            )
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
            iAmResponder || iAmDeclined -> {
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

// ── Figuren-Parts Box (BoxWithConstraints für Proportionen) ─────────────────

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun KlontauschFigurePartsBox(
    charId: String,
    hasKopf: Boolean,
    hasKoerper: Boolean,
    hasBeine: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val partH = maxHeight / 3f
        val partW = maxWidth * 0.70f
        Column(Modifier.width(partW).align(Alignment.Center)) {
            KlonPartSlot(charId, KlonPart.KOPF,    hasKopf,    partH)
            KlonPartSlot(charId, KlonPart.KOERPER, hasKoerper, partH)
            KlonPartSlot(charId, KlonPart.BEINE,   hasBeine,   partH)
        }
    }
}

// ── Vorrats-Figur (3 unabhängige Pager per Körperteil) ───────────────────────

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun KlontauschStockFigure(
    modifier: Modifier = Modifier,
    stockKopf: List<KlonCard>,
    stockKoerper: List<KlonCard>,
    stockBeine: List<KlonCard>,
) {
    val scope        = rememberCoroutineScope()
    val kopfPager    = rememberPagerState { maxOf(stockKopf.size, 1) }
    val koerperPager = rememberPagerState { maxOf(stockKoerper.size, 1) }
    val beinePager   = rememberPagerState { maxOf(stockBeine.size, 1) }
    val totalStock   = stockKopf.size + stockKoerper.size + stockBeine.size

    Column(modifier = modifier.fillMaxWidth()) {
        // Header (Zeile mit Titel + Shuffle-Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OceanBlue, CircleShape)
                        .clickable {
                            scope.launch {
                                if (stockKopf.size > 1) {
                                    val next = stockKopf.indices.filter { it != kopfPager.currentPage }.random()
                                    kopfPager.animateScrollToPage(next)
                                }
                                if (stockKoerper.size > 1) {
                                    val next = stockKoerper.indices.filter { it != koerperPager.currentPage }.random()
                                    koerperPager.animateScrollToPage(next)
                                }
                                if (stockBeine.size > 1) {
                                    val next = stockBeine.indices.filter { it != beinePager.currentPage }.random()
                                    beinePager.animateScrollToPage(next)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎲", fontSize = MaterialTheme.typography.labelLarge.fontSize)
                }
            }
        }

        if (totalStock == 0) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Alle Karten gehören zu deinen Zielfiguren.",
                    color = TextMuted,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            // Figuren-Bereich: füllt den Rest, Höhe wird per BoxWithConstraints berechnet
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                val partH = maxHeight / 3f
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

            // Karten-Zähler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text("${stockKopf.size} Kopf",    color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                Text("${stockKoerper.size} Körper", color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                Text("${stockBeine.size} Beine",  color = TextMuted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
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

// ── Confetti overlay ──────────────────────────────────────────────────────────

@Composable
private fun KlontauschConfettiOverlay() {
    val confettiColors = remember {
        listOf(
            KlontauschAccent, Color(0xFFFB7185), Color(0xFF38BDF8),
            Color(0xFFFBBF24), Color(0xFF4ADE80), Color(0xFFC084FC),
        )
    }
    val particles = remember {
        List(48) {
            floatArrayOf(
                Random.nextFloat(),
                -0.05f - Random.nextFloat() * 0.5f,
                0.25f + Random.nextFloat() * 0.45f,
                (Random.nextFloat() - 0.5f) * 0.06f,
            )
        }
    }
    val colors = remember { List(48) { confettiColors[it % confettiColors.size] } }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (t < 3.5f) {
            t = (System.currentTimeMillis() - start) / 1000f
            delay(16.milliseconds)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEachIndexed { i, p ->
            val rawY = p[1] + p[2] * t
            val y = rawY % 1.1f
            if (y < 0f) return@forEachIndexed
            val x = (p[0] + p[3] * t).let { if (it < 0f) it + 1f else if (it > 1f) it - 1f else it }
            val alpha = ((1f - t / 3.5f) * 1.6f).coerceIn(0f, 0.85f)
            drawCircle(
                color = colors[i].copy(alpha = alpha),
                radius = 8f,
                center = Offset(x * size.width, y * size.height),
            )
        }
    }
}

// ── Winner dialog (undismissable) ────────────────────────────────────────────

@Composable
private fun KlontauschWinDialog(
    gameState: KlonGameState,
    uid: String,
    onToLobby: () -> Unit,
    onNavigateToResults: () -> Unit,
) {
    val winnerId  = gameState.winnerId
    val winner    = gameState.players[winnerId]
    val iAmWinner = winnerId == uid
    val players   = gameState.playerIds.mapNotNull { gameState.players[it] }

    var trophyVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120.milliseconds)
        trophyVisible = true
    }
    val trophyScale by animateFloatAsState(
        targetValue = if (trophyVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "trophy_scale",
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box {
            KlonWinParticles(Modifier.matchParentSize())

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
                    Text(
                        "🏆",
                        fontSize = MaterialTheme.typography.displayMedium.fontSize,
                        modifier = Modifier.scale(trophyScale),
                    )

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
                            val isWin  = p.userId == winnerId
                            val isLast = idx == players.size - 1
                            val emoji  = when {
                                idx == 0            -> "🥇"
                                idx == 1            -> "🥈"
                                idx == 2 && !isLast -> "🥉"
                                isLast              -> "🦀"
                                else                -> "🥉"
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
                        onClick = onNavigateToResults,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KlontauschAccent),
                    ) {
                        Text("Ergebnisse ansehen", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onToLobby,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, KlontauschAccent.copy(0.5f)),
                    ) {
                        Text("Zur Lobby", color = TextSub)
                    }
                }
            }
        }
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun KlonWinParticles(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "klon_particles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "klon_phase",
    )
    val particles = remember {
        List(16) { i ->
            Triple(
                (i / 16f + Random.nextFloat() * 0.055f).coerceIn(0.02f, 0.98f),
                Random.nextFloat(),
                listOf("🃏", "🎭", "🎲", "✨", "🎴", "🃏", "🎲", "🎭")[i % 8],
            )
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        particles.forEach { (x, offset, emoji) ->
            val y = (phase + offset) % 1f
            Text(
                text = emoji,
                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                modifier = Modifier
                    .absoluteOffset(x = w * x, y = h * y)
                    .alpha(0.7f),
            )
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
) {
    val borderColor = if (owned) KlontauschAccent.copy(0.6f) else BorderColor.copy(0.25f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(0.5.dp, borderColor),
        contentAlignment = Alignment.Center,
    ) {
        if (owned) {
            KlontauschCharacterView(
                characterId = characterId,
                part = part,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        } else {
            KlontauschSilhouette(
                part = part,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        }
    }
}
