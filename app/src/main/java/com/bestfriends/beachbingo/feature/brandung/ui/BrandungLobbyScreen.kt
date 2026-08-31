package com.bestfriends.beachbingo.feature.brandung.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BingoCallSize
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.Teal
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.home.ui.SavedGameRow
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import org.json.JSONObject


private data class DifficultyOption(val id: String, val label: String, val emoji: String, val description: String)

private val DIFFICULTIES = listOf(
    DifficultyOption("ROOKIE",     "Rookie",     "🌊", "Macht häufig Fehler – gut zum Üben"),
    DifficultyOption("SNIPER",     "Sniper",     "🎯", "Spielt clever – fordert aber fair"),
    DifficultyOption("BOSS_LEVEL", "Boss Level", "💪", "Fast unbesiegbar – alles oder nichts"),
)

private fun generateGameCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandungLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (mode: String, gameId: String?, aiCount: Int, difficulty: String, saveId: String?) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf("mode") }
    var mode by remember { mutableStateOf("ai") }
    var aiCount by remember { mutableIntStateOf(2) }
    var difficulty by remember { mutableStateOf("SNIPER") }
    var isFavorite by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var savedBrandung by remember { mutableStateOf(SoloGameSaveManager.getGameSave(context, "brandung")) }

    // Online lobby state
    var onlineStep by remember { mutableStateOf("choose") } // choose | waiting
    var gameCode by remember { mutableStateOf("") }
    var gameDocId by remember { mutableStateOf("") }
    var waitingPlayers by remember { mutableStateOf<List<String>>(emptyList()) }
    var creating by remember { mutableStateOf(false) }
    var isJoiner by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("brandung") == true
        } catch (_: Exception) {}
    }

    // Listen for players joining and game start
    LaunchedEffect(gameDocId) {
        if (gameDocId.isBlank()) return@LaunchedEffect
        db.collection("brandungGames").document(gameDocId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val players = (snap.get("playerIds") as? List<String>) ?: emptyList()
                waitingPlayers = players
                if (snap.getString("status") == "RUNNING") {
                    onNavigateToGame("online", gameDocId, 0, "SNIPER", null)
                }
            }
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("brandung") else FieldValue.arrayRemove("brandung")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    fun createOnlineGame() {
        if (uid == null || creating) return
        creating = true
        scope.launch {
            try {
                val user = db.collection("users").document(uid).get().await()
                val displayName = user.getString("displayName") ?: "Spieler"
                val avatarUrl = user.getString("avatarUrl") ?: "🏄"
                val code = generateGameCode()
                val data = mapOf(
                    "gameCode" to code,
                    "status" to "LOBBY",
                    "adminId" to uid,
                    "playerIds" to listOf(uid),
                    "players" to mapOf(
                        uid to mapOf(
                            "userId" to uid,
                            "displayName" to displayName,
                            "avatarUrl" to avatarUrl,
                            "hand" to emptyList<Any>(),
                            "lives" to 3,
                            "eliminated" to false,
                            "isAI" to false,
                        )
                    ),
                    "createdAt" to System.currentTimeMillis(),
                )
                db.collection("brandungGames").document(code).set(data).await()
                gameDocId = code
                gameCode = code
                onlineStep = "waiting"
            } catch (_: Exception) {
            } finally {
                creating = false
            }
        }
    }

    fun startOnlineGame() {
        if (gameDocId.isBlank()) return
        scope.launch {
            try {
                db.collection("brandungGames").document(gameDocId)
                    .update("status", "RUNNING").await()
                onNavigateToGame("online", gameDocId, 0, "SNIPER", null)
            } catch (_: Exception) {}
        }
    }

    fun cancelWaiting() {
        if (gameDocId.isNotBlank() && !isJoiner) {
            db.collection("brandungGames").document(gameDocId).delete()
        }
        gameDocId = ""
        gameCode = ""
        waitingPlayers = emptyList()
        isJoiner = false
        joinCode = ""
        joinError = ""
        onlineStep = "choose"
    }

    fun joinExistingGame(code: String) {
        if (uid == null) return
        joinError = ""
        scope.launch {
            try {
                val normalizedCode = code.trim().uppercase()
                val gameSnap = db.collection("brandungGames").document(normalizedCode).get().await()
                if (!gameSnap.exists()) {
                    joinError = "Spiel nicht gefunden."
                    return@launch
                }
                val status = gameSnap.getString("status") ?: ""
                if (status != "LOBBY" && status != "WAITING") {
                    joinError = "Spiel läuft bereits."
                    return@launch
                }
                val userSnap = db.collection("users").document(uid).get().await()
                val displayName = userSnap.getString("displayName") ?: "Spieler"
                val avatarUrl = userSnap.getString("avatarUrl") ?: "🏄"
                val me = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "avatarUrl" to avatarUrl,
                    "hand" to emptyList<Any>(),
                    "lives" to 3,
                    "eliminated" to false,
                    "isAI" to false,
                )
                db.collection("brandungGames").document(normalizedCode)
                    .update(
                        mapOf(
                            "players.$uid" to me,
                            "playerIds" to FieldValue.arrayUnion(uid),
                        )
                    ).await()
                gameDocId = normalizedCode
                gameCode = normalizedCode
                isJoiner = true
                onlineStep = "waiting"
            } catch (_: Exception) {
                joinError = "Fehler beim Beitreten. Bitte erneut versuchen."
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BRANDUNG", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🌊 Kartenspieler", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToResults) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Ergebnisse", tint = SandGold)
                    }
                    IconButton(onClick = { toggleFavorite() }) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                            color = if (isFavorite) SandGold else TextMuted,
                        )
                    }
                    IconButton(onClick = { showRules = true }) {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Spielanleitung", tint = TextSub)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Step: Mode ──
            if (step == "mode") {
                Text("Spielmodus wählen", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                // Fortsetzen card
                savedBrandung?.let { sg ->
                    val savedAiCount = try { JSONObject(sg.gameState).getJSONArray("players").length() - 1 } catch (_: Exception) { 1 }
                    SavedGameRow(
                        title = "Brandung",
                        subtitle = sg.displayLabel,
                        color = Teal,
                        onResume = { onNavigateToGame("ai", null, savedAiCount, sg.difficulty, sg.id) },
                        onDelete = { SoloGameSaveManager.deleteGameSave(context, "brandung"); savedBrandung = null },
                    )
                }

                BrandungModeCard(
                    emoji = "🤖",
                    title = "Gegen KI",
                    description = "Spiel allein gegen KI-Gegner",
                    color = Teal,
                    onClick = { mode = "ai"; step = "ai_config" },
                )
                BrandungModeCard(
                    emoji = "📱",
                    title = "Online – 2-6 Spieler",
                    description = "Spielt gemeinsam in Echtzeit",
                    color = OceanBlue,
                    onClick = { mode = "online"; step = "online" },
                )
            }

            // ── Step: AI Config ──
            if (step == "ai_config") {
                OutlinedButton(
                    onClick = { step = "mode" },
                    modifier = Modifier.align(Alignment.Start),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                ) { Text("‹ Zurück") }

                Text("KI-Gegner", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                // AI count selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Anzahl KI-Gegner", style = MaterialTheme.typography.labelLarge, color = TextSub)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { count ->
                                val selected = aiCount == count
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            2.dp,
                                            if (selected) Teal else BorderColor,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable { aiCount = count },
                                    color = if (selected) Teal.copy(alpha = 0.2f) else Surface2Dark,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                        Text(
                                            "$count",
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Teal else TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Difficulty selector
                Text("KI-Schwierigkeit", style = MaterialTheme.typography.labelLarge, color = TextSub, modifier = Modifier.padding(start = 4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DIFFICULTIES.forEach { diff ->
                        val selected = difficulty == diff.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    2.dp,
                                    if (selected) Teal else BorderColor,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { difficulty = diff.id },
                            color = if (selected) Teal.copy(alpha = 0.15f) else SurfaceDark,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text(diff.emoji, style = MaterialTheme.typography.headlineSmall)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(diff.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(diff.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                if (selected) {
                                    Text("✓", color = Teal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onNavigateToGame("ai", null, aiCount, difficulty, null) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Spiel starten 🌊", fontWeight = FontWeight.Bold)
                }
            }

            // ── Step: Online ──
            if (step == "online") {
                OutlinedButton(
                    onClick = {
                        if (onlineStep == "waiting") cancelWaiting()
                        else step = "mode"
                    },
                    modifier = Modifier.align(Alignment.Start),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                ) { Text("‹ Zurück") }

                if (onlineStep == "choose") {
                    Text("Online-Spiel", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Spiel erstellen", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "Erstelle ein Spiel und teile den Code mit anderen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            Button(
                                onClick = { createOnlineGame() },
                                enabled = !creating,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (creating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                else Text("Neues Spiel erstellen")
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Spiel beitreten", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "Gib den 6-stelligen Code des Spielers ein, der eingeladen hat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase().take(6); joinError = "" },
                                label = { Text("Spielcode") },
                                placeholder = { Text("z.B. ABC123") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    keyboardType = KeyboardType.Ascii,
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Teal,
                                    focusedLabelColor = Teal,
                                    cursorColor = Teal,
                                    unfocusedTextColor = TextPrimary,
                                    focusedTextColor = TextPrimary,
                                ),
                            )
                            if (joinError.isNotBlank()) {
                                Text(joinError, style = MaterialTheme.typography.bodySmall, color = Danger)
                            }
                            Button(
                                onClick = { if (joinCode.length == 6) joinExistingGame(joinCode) },
                                enabled = joinCode.length == 6,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Beitreten")
                            }
                        }
                    }
                }

                if (onlineStep == "waiting") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            if (isJoiner) "🌊 Beigetreten!" else "⏳ Warte auf Spieler…",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                        )

                        if (!isJoiner) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Surface2Dark,
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("SPIELCODE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.5.sp)
                                    Text(
                                        text = gameCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                                        fontWeight = FontWeight.Black,
                                        color = Teal,
                                        letterSpacing = 6.sp,
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Spielcode", gameCode))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                                    ) { Text("📋 Kopieren") }
                                }
                            }

                            // QR Code
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.padding(4.dp)) {
                                QrCodeImage(
                                    content = "https://beachbande.de/brandung/lobby?join=$gameCode",
                                    size = 160.dp,
                                )
                            }

                            Text(
                                "Spieler scannen den QR-Code oder geben den Code im App ein",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Player list
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${waitingPlayers.size} / 6 Spieler",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                waitingPlayers.forEachIndexed { idx, playerId ->
                                    val isMe = playerId == uid
                                    val isHost = idx == 0
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🌊", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                                        Text(
                                            when {
                                                isMe && isHost -> "Du (Host) 👑"
                                                isMe -> "Du"
                                                isHost -> "Host 👑"
                                                else -> "Spieler ${idx + 1}"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }

                        // Start button (only for host, need >= 2 players)
                        if (!isJoiner && waitingPlayers.size >= 2) {
                            Button(
                                onClick = { startOnlineGame() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Spiel starten (${waitingPlayers.size} Spieler) 🌊", fontWeight = FontWeight.Bold)
                            }
                        } else if (isJoiner) {
                            Text(
                                "Warte auf Spielstart durch den Host...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showRules) {
        ALL_GAME_RULES["brandung"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }
}

@Composable
private fun BrandungModeCard(
    emoji: String,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(emoji, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text("›", fontSize = BingoCallSize, color = TextMuted)
        }
    }
}
