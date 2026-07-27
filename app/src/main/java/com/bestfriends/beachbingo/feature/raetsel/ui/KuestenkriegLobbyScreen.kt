package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.raetsel.KRIEG_FLEET
import com.bestfriends.beachbingo.feature.raetsel.KRIEG_GRID_SIZES
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSaveManager
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val KkAccent = Color(0xFFFB7185)

private fun fleetLabel(fleet: List<Int>): String {
    val counts = fleet.groupBy { it }
    return counts.entries.sortedByDescending { it.key }.joinToString(", ") { "${it.value.size}×${it.key}er" }
}

private fun generateKkGameCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

private enum class KkMode { PUZZLE, KI, ONLINE }

private data class AiOption(val id: String, val label: String, val desc: String, val emoji: String)

private val AI_OPTIONS = listOf(
    AiOption("matrose",  "Matrose",  "Schießt zufällig",             "🌊"),
    AiOption("kapitaen", "Kapitän",  "Wahrscheinlichkeitsbasiert",   "⚓"),
    AiOption("admiral",  "Admiral",  "Sucht & zielt — stärkste KI", "🏴‍☠️"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuestenkriegLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (difficulty: String, seed: Long, saveId: String?) -> Unit,
    onNavigateToPlacement: (aiMode: String) -> Unit,
    onNavigateToOnlineLobby: (code: String) -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(KkMode.PUZZLE) }
    var selectedDiff by remember { mutableStateOf("mittel") }
    var selectedAi by remember { mutableStateOf("kapitaen") }
    val saves = remember { PuzzleSaveManager.getSaves(context).filter { it.gameType == "kuestenkrieg" } }
    val difficulties = listOf("leicht", "mittel", "schwer", "experte")
    val diffLabels = mapOf("leicht" to "Leicht", "mittel" to "Mittel", "schwer" to "Schwer", "experte" to "Experte")

    // Online state
    var creating by remember { mutableStateOf(false) }
    var joining by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var onlineError by remember { mutableStateOf("") }

    fun createOnlineGame() {
        if (uid.isBlank() || creating) return
        creating = true
        onlineError = ""
        scope.launch {
            try {
                val user = db.collection("users").document(uid).get().await()
                val displayName = user.getString("displayName") ?: "Spieler"
                val avatarUrl = user.getString("avatarUrl") ?: "👤"
                val code = generateKkGameCode()
                val data = mapOf(
                    "gameId" to code,
                    "adminId" to uid,
                    "status" to "LOBBY",
                    "playerIds" to listOf(uid),
                    "players" to mapOf(
                        uid to mapOf(
                            "userId" to uid,
                            "displayName" to displayName,
                            "avatarUrl" to avatarUrl,
                            "fleet" to emptyList<Any>(),
                            "fleetReady" to false,
                        )
                    ),
                    "shots" to emptyMap<String, Any>(),
                    "turn" to "",
                    "winner" to null,
                    "createdAt" to System.currentTimeMillis(),
                )
                db.collection("kuestenkriegGames").document(code).set(data).await()
                onNavigateToOnlineLobby(code)
            } catch (_: Exception) {
                onlineError = "Erstellen fehlgeschlagen. Bitte erneut versuchen."
            } finally {
                creating = false
            }
        }
    }

    fun joinOnlineGame() {
        val code = joinCode.trim().uppercase()
        if (code.isBlank() || uid.isBlank() || joining) return
        joining = true
        onlineError = ""
        scope.launch {
            try {
                val snap = db.collection("kuestenkriegGames").document(code).get().await()
                if (!snap.exists()) {
                    onlineError = "Kein Spiel mit diesem Code gefunden."
                    joining = false
                    return@launch
                }
                val gameStatus = snap.getString("status") ?: ""
                if (gameStatus == "FINISHED") {
                    onlineError = "Dieses Spiel ist bereits beendet."
                    joining = false
                    return@launch
                }
                @Suppress("UNCHECKED_CAST")
                val pIds = (snap.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (pIds.size >= 2 && !pIds.contains(uid)) {
                    onlineError = "Das Spiel ist voll."
                    joining = false
                    return@launch
                }
                if (!pIds.contains(uid)) {
                    val user = db.collection("users").document(uid).get().await()
                    val displayName = user.getString("displayName") ?: "Spieler"
                    val avatarUrl = user.getString("avatarUrl") ?: "👤"
                    db.collection("kuestenkriegGames").document(code).update(
                        mapOf(
                            "playerIds" to com.google.firebase.firestore.FieldValue.arrayUnion(uid),
                            "players.$uid" to mapOf(
                                "userId" to uid,
                                "displayName" to displayName,
                                "avatarUrl" to avatarUrl,
                                "fleet" to emptyList<Any>(),
                                "fleetReady" to false,
                            ),
                        )
                    ).await()
                }
                onNavigateToOnlineLobby(code)
            } catch (_: Exception) {
                onlineError = "Beitreten fehlgeschlagen."
            } finally {
                joining = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark))).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onNavigateBack() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text("⚓", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("RÄTSEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Küstenkrieg", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Mode selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SPIELMODUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                listOf(
                    Triple(KkMode.PUZZLE, "🧩", "Solo Rätsel" to "Zahlen am Rand verraten die Schiffe"),
                    Triple(KkMode.KI,     "🤖", "Gegen KI"    to "Klassisches Schiffe versenken"),
                    Triple(KkMode.ONLINE, "🌐", "Online"      to "Gegen echten Spieler"),
                ).forEach { (m, emoji, texts) ->
                    val sel = mode == m
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) KkAccent.copy(alpha = 0.08f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                            .border(1.5.dp, if (sel) KkAccent else BorderColor, RoundedCornerShape(12.dp))
                            .clickable { mode = m; onlineError = "" }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(texts.first, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) KkAccent else TextPrimary)
                                Text(texts.second, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (sel) Text("✓", fontSize = 18.sp, color = KkAccent)
                        }
                    }
                }
            }

            // Puzzle options
            if (mode == KkMode.PUZZLE) {
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                    Text("Schlachtschiff-Rätsel: Zahlen am Rand zeigen Schiffsfelder pro Zeile/Spalte. Schiffe berühren sich nie diagonal. Tippen = Schiff, Lang drücken = Wasser.",
                        fontSize = 13.sp, color = TextMuted, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
                }
                Text("SCHWIERIGKEIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                difficulties.forEach { d ->
                    val sel = selectedDiff == d
                    Surface(shape = RoundedCornerShape(12.dp), color = if (sel) KkAccent.copy(alpha = 0.1f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) KkAccent else BorderColor, RoundedCornerShape(12.dp)).clickable { selectedDiff = d }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diffLabels[d] ?: d, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) KkAccent else TextPrimary)
                                val size = KRIEG_GRID_SIZES[d] ?: 10
                                val fleet = KRIEG_FLEET[d] ?: emptyList()
                                Text("${size}×${size} · ${fleetLabel(fleet)}", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
                            }
                            if (sel) Text("✓", fontSize = 18.sp, color = KkAccent)
                        }
                    }
                }
                Button(onClick = { onNavigateToGame(selectedDiff, System.currentTimeMillis(), null) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KkAccent),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Neues Rätsel", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark) }

                // Saved puzzle games
                if (saves.isNotEmpty()) {
                    Text("GESPEICHERTE RÄTSEL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    saves.forEach { save ->
                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${diffLabels[save.difficulty] ?: save.difficulty} · ${KRIEG_GRID_SIZES[save.difficulty] ?: 10}×${KRIEG_GRID_SIZES[save.difficulty] ?: 10}",
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(PuzzleSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = KkAccent.copy(alpha = 0.1f),
                                    modifier = Modifier.border(1.dp, KkAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onNavigateToGame(save.difficulty, save.seed, save.id) }
                                ) { Text("Fortsetzen", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KkAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                            }
                        }
                    }
                }
            }

            // Online options
            if (mode == KkMode.ONLINE) {
                if (onlineError.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0x22EF4444),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x55EF4444), RoundedCornerShape(10.dp))
                    ) { Text(onlineError, fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.padding(12.dp)) }
                }
                Button(
                    onClick = ::createOnlineGame,
                    enabled = !creating && !joining,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KkAccent, disabledContainerColor = KkAccent.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (creating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BgDark, strokeWidth = 2.dp)
                    else Text("Neues Spiel erstellen", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                    Text("ODER CODE EINGEBEN", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                }

                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().take(6); onlineError = "" },
                    label = { Text("Spielcode") },
                    placeholder = { Text("ABCD12", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KkAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = KkAccent,
                        focusedLabelColor = KkAccent,
                        unfocusedLabelColor = TextMuted,
                    ),
                )
                Button(
                    onClick = ::joinOnlineGame,
                    enabled = joinCode.trim().length >= 4 && !joining && !creating,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2Dark, disabledContainerColor = Surface2Dark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (joining) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KkAccent, strokeWidth = 2.dp)
                    else Text("Beitreten", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = KkAccent)
                }
            }

            // KI options
            if (mode == KkMode.KI) {
                Text("KI-SCHWIERIGKEIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                AI_OPTIONS.forEach { opt ->
                    val sel = selectedAi == opt.id
                    Surface(shape = RoundedCornerShape(12.dp), color = if (sel) KkAccent.copy(alpha = 0.08f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) KkAccent else BorderColor, RoundedCornerShape(12.dp)).clickable { selectedAi = opt.id }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(opt.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opt.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) KkAccent else TextPrimary)
                                Text(opt.desc, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (sel) Text("✓", fontSize = 18.sp, color = KkAccent)
                        }
                    }
                }
                Button(onClick = { onNavigateToPlacement(selectedAi) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KkAccent),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Schiffe setzen →", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark) }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
