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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.outlined.HelpOutline
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet

private val BrandungTeal = Color(0xFF0D9488)

private data class DifficultyOption(val id: String, val label: String, val emoji: String, val description: String)

private val DIFFICULTIES = listOf(
    DifficultyOption("ROOKIE",     "Rookie",     "😅", "Macht häufig Fehler – gut zum Üben"),
    DifficultyOption("SNIPER",     "Sniper",     "🎯", "Spielt clever – fordert aber fair"),
    DifficultyOption("BOSS_LEVEL", "Boss Level", "💀", "Fast unbesiegbar – alles oder nichts"),
)

private fun generateGameCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandungLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (mode: String, gameId: String?, aiCount: Int, difficulty: String) -> Unit,
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

    // Online lobby state
    var onlineStep by remember { mutableStateOf("choose") } // choose | waiting
    var gameCode by remember { mutableStateOf("") }
    var gameDocId by remember { mutableStateOf("") }
    var waitingPlayers by remember { mutableStateOf<List<String>>(emptyList()) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("brandung") == true
        } catch (_: Exception) {}
    }

    // Listen for players joining online game
    LaunchedEffect(gameDocId) {
        if (gameDocId.isBlank()) return@LaunchedEffect
        db.collection("brandungGames").document(gameDocId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val players = (snap.get("playerIds") as? List<String>) ?: emptyList()
                waitingPlayers = players
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
                    "status" to "WAITING",
                    "adminId" to uid,
                    "playerIds" to listOf(uid),
                    "players" to listOf(mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "avatarUrl" to avatarUrl,
                        "lives" to 3,
                        "isAI" to false,
                    )),
                    "createdAt" to System.currentTimeMillis(),
                )
                val ref = db.collection("brandungGames").add(data).await()
                gameDocId = ref.id
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
                onNavigateToGame("online", gameDocId, 0, "SNIPER")
            } catch (_: Exception) {}
        }
    }

    fun cancelWaiting() {
        if (gameDocId.isNotBlank()) {
            db.collection("brandungGames").document(gameDocId).delete()
        }
        gameDocId = ""
        gameCode = ""
        waitingPlayers = emptyList()
        onlineStep = "choose"
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
                            fontSize = 22.sp,
                            color = if (isFavorite) SandGold else TextMuted,
                        )
                    }
                    IconButton(onClick = { showRules = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Spielanleitung", tint = TextSub)
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

                BrandungModeCard(
                    emoji = "🤖",
                    title = "Gegen KI",
                    description = "Spiel allein gegen KI-Gegner",
                    color = BrandungTeal,
                    onClick = { mode = "ai"; step = "ai_config" },
                )
                BrandungModeCard(
                    emoji = "📱",
                    title = "Online – 2-6 Spieler",
                    description = "Spielt gemeinsam in Echtzeit",
                    color = Color(0xFF0EA5E9),
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
                                            if (selected) BrandungTeal else Color(0xFF1E3050),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .clickable { aiCount = count },
                                    color = if (selected) BrandungTeal.copy(alpha = 0.2f) else Surface2Dark,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                                        Text(
                                            "$count",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) BrandungTeal else TextPrimary,
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
                                    if (selected) BrandungTeal else Color(0xFF1E3050),
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { difficulty = diff.id },
                            color = if (selected) BrandungTeal.copy(alpha = 0.15f) else SurfaceDark,
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
                                    Text("✓", color = BrandungTeal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onNavigateToGame("ai", null, aiCount, difficulty) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandungTeal),
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
                                colors = ButtonDefaults.buttonColors(containerColor = BrandungTeal),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (creating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                else Text("Neues Spiel erstellen")
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
                            "⏳ Warte auf Spieler…",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                        )

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
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandungTeal,
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
                                content = "https://thebeachbingo.netlify.app/brandung/lobby?join=$gameCode",
                                size = 160.dp,
                            )
                        }

                        Text(
                            "Spieler scannen den QR-Code oder geben den Code ein",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )

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
                                waitingPlayers.forEachIndexed { idx, _ ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🌊", fontSize = 18.sp)
                                        Text(
                                            if (idx == 0) "Du (Host)" else "Spieler ${idx + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }

                        // Start button (only for host, need >= 2 players)
                        if (waitingPlayers.size >= 2) {
                            Button(
                                onClick = { startOnlineGame() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandungTeal),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Spiel starten (${waitingPlayers.size} Spieler) 🌊", fontWeight = FontWeight.Bold)
                            }
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
                    Text(emoji, fontSize = 28.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text("›", fontSize = 20.sp, color = TextMuted)
        }
    }
}
