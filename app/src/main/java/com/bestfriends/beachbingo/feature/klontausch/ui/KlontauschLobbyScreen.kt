package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.klontausch.*
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val KlontauschAccent = Color(0xFF8B5CF6)

private data class KlonDiff(val id: String, val emoji: String, val label: String, val description: String)
private val KLON_DIFFS = listOf(
    KlonDiff("ROOKIE",     "🐣", "Rookie",     "Mopst zufällig, keine Strategie"),
    KlonDiff("SNIPER",     "🎯", "Sniper",     "Taktisch klug, bevorzugt Zielkarten"),
    KlonDiff("BOSS_LEVEL", "💀", "Boss Level", "Unerbittlich – kennt deine Schwächen"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlontauschLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (mode: String, gameId: String?, aiCount: Int, difficulty: String, saveId: String?) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf("mode") }          // "mode" | "ai_config" | "online"
    var onlineStep by remember { mutableStateOf("choose") }  // "choose" | "waiting"
    var aiCount by remember { mutableIntStateOf(1) }
    var difficulty by remember { mutableStateOf("SNIPER") }
    var isFavorite by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    // Online state
    var gameCode by remember { mutableStateOf("") }
    var waitingPlayers by remember { mutableStateOf<List<String>>(emptyList()) }
    var creating by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("klontausch") == true
        } catch (_: Exception) {}
    }

    // Watch waiting room
    LaunchedEffect(gameCode) {
        if (gameCode.isBlank()) return@LaunchedEffect
        db.collection("klontauschGames").document(gameCode)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                waitingPlayers = (snap.get("playerIds") as? List<String>) ?: emptyList()
            }
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val upd = if (isFavorite) FieldValue.arrayUnion("klontausch") else FieldValue.arrayRemove("klontausch")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", upd)
    }

    fun createOnlineGame() {
        if (uid == null || creating) return
        creating = true; createError = ""
        scope.launch {
            try {
                val user = db.collection("users").document(uid).get().await()
                val displayName = user.getString("displayName") ?: "Spieler"
                val avatarUrl = user.getString("avatarUrl") ?: "🦖"
                val code = generateKlonGameCode()
                val playerState = mapOf(
                    "userId" to uid, "displayName" to displayName, "avatarUrl" to avatarUrl,
                    "heldCards" to emptyList<Any>(), "cardCount" to 0,
                    "isAI" to false, "isEliminated" to false,
                )
                db.collection("klontauschGames").document(code).set(mapOf(
                    "gameCode" to code, "status" to "LOBBY", "mode" to "ONLINE",
                    "adminId" to uid, "playerIds" to listOf(uid),
                    "players" to mapOf(uid to playerState),
                    "turnIndex" to 0,
                    "offer" to mapOf(
                        "type" to "NONE", "fromUserId" to "", "part" to "",
                        "committedCardId" to "", "responderIds" to emptyList<String>(),
                        "declinedIds" to emptyList<String>(),
                        "selectedResponderId" to "", "responderCardId" to "",
                    ),
                    "winnerId" to "", "createdAt" to System.currentTimeMillis(),
                )).await()
                gameCode = code
                onlineStep = "waiting"
            } catch (_: Exception) {
                createError = "Erstellen fehlgeschlagen. Nochmal versuchen."
            } finally {
                creating = false
            }
        }
    }

    fun startOnlineGame() {
        if (gameCode.isBlank() || starting) return
        starting = true
        scope.launch {
            try {
                val snap = db.collection("klontauschGames").document(gameCode).get().await()
                @Suppress("UNCHECKED_CAST")
                val currentIds = (snap.get("playerIds") as? List<String>) ?: return@launch
                @Suppress("UNCHECKED_CAST")
                val rawPlayers = (snap.get("players") as? Map<*, *>) ?: return@launch

                val playerMap = rawPlayers.entries.associate { (k, v) ->
                    val pUid = k as String
                    @Suppress("UNCHECKED_CAST")
                    val pm = v as Map<String, Any>
                    pUid to KlonPlayerState(
                        userId = pUid,
                        displayName = pm["displayName"] as? String ?: pUid,
                        avatarUrl = pm["avatarUrl"] as? String ?: "🦖",
                        isAI = false,
                    )
                }

                val (dealt, targets) = dealGame(playerMap, currentIds)

                // Write private targets for each player
                targets.forEach { (pUid, targetIds) ->
                    db.collection("klontauschGames").document(gameCode)
                        .collection("private").document(pUid)
                        .set(mapOf("targetCharacterIds" to targetIds)).await()
                }

                // Write full dealt state + set status PLAYING
                db.collection("klontauschGames").document(gameCode).update(mapOf(
                    "status" to "PLAYING",
                    "players" to dealt.mapValues { it.value.toFirestoreMap() },
                    "turnIndex" to 0,
                )).await()

                onNavigateToGame("ONLINE", gameCode, 0, difficulty, null)
            } catch (_: Exception) {
                starting = false
            }
        }
    }

    fun cancelWaiting() {
        if (gameCode.isNotBlank()) db.collection("klontauschGames").document(gameCode).delete()
        gameCode = ""; waitingPlayers = emptyList(); onlineStep = "choose"
    }

    if (showRules) {
        ALL_GAME_RULES["klontausch"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Surface2Dark,
                    modifier = Modifier.size(40.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable {
                            when {
                                step == "ai_config"          -> step = "mode"
                                step == "online" && onlineStep == "waiting" -> cancelWaiting()
                                step == "online"             -> step = "mode"
                                else                         -> onNavigateBack()
                            }
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text("🃏", fontSize = EmojiMedium)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("KARTENSPIEL", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Klontausch", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToResults() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🏆", fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) SandGold.copy(0.12f) else Surface2Dark,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, if (isFavorite) SandGold.copy(0.5f) else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { toggleFavorite() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (isFavorite) "★" else "☆",
                            fontSize = MaterialTheme.typography.titleSmall.fontSize,
                            color = if (isFavorite) SandGold else TextSub)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { showRules = true },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("?", fontSize = MaterialTheme.typography.titleSmall.fontSize,
                            color = TextSub, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToSettings() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Settings, "Einstellungen", tint = TextSub, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── STEP: MODE ────────────────────────────────────────────────────
            if (step == "mode") {
                Text("Spielmodus wählen", style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, modifier = Modifier.padding(start = 4.dp))

                KlonModeCard("🤖", "Gegen KI", "Spiel allein gegen KI-Gegner") { step = "ai_config" }
                KlonModeCard("📱", "Online – 2–4 Spieler", "Spielt gemeinsam per QR-Code") { step = "online" }
            }

            // ── STEP: AI CONFIG ───────────────────────────────────────────────
            if (step == "ai_config") {
                Text("KI-Gegner konfigurieren", style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, modifier = Modifier.padding(start = 4.dp))

                // Player count
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Anzahl KI-Gegner", style = MaterialTheme.typography.labelLarge, color = TextSub)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3).forEach { count ->
                                val sel = aiCount == count
                                Surface(
                                    modifier = Modifier.weight(1f)
                                        .border(2.dp, if (sel) KlontauschAccent else BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { aiCount = count },
                                    color = if (sel) KlontauschAccent.copy(0.18f) else Surface2Dark,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                        Text("$count", style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sel) KlontauschAccent else TextPrimary)
                                    }
                                }
                            }
                        }
                        Text("Du spielst gegen $aiCount KI-Gegner (${aiCount + 1} Spieler gesamt)",
                            color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Difficulty
                Text("KI-Schwierigkeit", style = MaterialTheme.typography.labelLarge, color = TextSub,
                    modifier = Modifier.padding(start = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KLON_DIFFS.forEach { diff ->
                        val sel = difficulty == diff.id
                        Surface(
                            modifier = Modifier.fillMaxWidth()
                                .border(2.dp, if (sel) KlontauschAccent else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { difficulty = diff.id },
                            color = if (sel) KlontauschAccent.copy(0.15f) else SurfaceDark,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(diff.emoji, style = MaterialTheme.typography.headlineSmall)
                                Column(Modifier.weight(1f)) {
                                    Text(diff.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(diff.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                if (sel) Text("✓", color = KlontauschAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onNavigateToGame("AI", null, aiCount, difficulty, null) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KlontauschAccent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Spiel starten 🃏", fontWeight = FontWeight.Bold)
                }
            }

            // ── STEP: ONLINE ──────────────────────────────────────────────────
            if (step == "online") {
                if (onlineStep == "choose") {
                    Text("Online-Spiel", style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary, modifier = Modifier.padding(start = 4.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Spiel erstellen", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Erstelle eine Lobby und lade Freunde per QR-Code ein (2–4 Spieler).",
                                style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            if (createError.isNotBlank()) {
                                Text(createError, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { createOnlineGame() }, enabled = !creating,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KlontauschAccent),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (creating) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                else Text("Lobby erstellen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (onlineStep == "waiting") {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("⏳ Warte auf Spieler…", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold, color = TextPrimary)

                        // Code display
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Surface2Dark) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("SPIELCODE", style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted, letterSpacing = 1.5.sp)
                                Text(gameCode, fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black, color = KlontauschAccent, letterSpacing = 6.sp)
                            }
                        }

                        // QR code
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.padding(4.dp)) {
                            QrCodeImage(
                                content = "https://thebeachbingo.netlify.app/klontausch/lobby?join=$gameCode",
                                size = 180.dp,
                            )
                        }
                        Text("Spieler scannen den QR-Code oder geben den Code ein",
                            style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center)

                        // Player list
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${waitingPlayers.size} / 4 Spieler",
                                    style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                waitingPlayers.forEachIndexed { idx, _ ->
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🃏", style = MaterialTheme.typography.titleMedium)
                                        Text(if (idx == 0) "Du (Host)" else "Spieler ${idx + 1}",
                                            style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        if (waitingPlayers.size >= 2) {
                            Button(
                                onClick = { startOnlineGame() },
                                enabled = !starting,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KlontauschAccent),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (starting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                else Text("Spiel starten (${waitingPlayers.size} Spieler) 🃏",
                                    fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { cancelWaiting() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                        ) { Text("Lobby abbrechen") }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun KlonModeCard(emoji: String, label: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = TextSub)
        }
    }
}
