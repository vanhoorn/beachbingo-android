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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.raetsel.AiMode
import com.bestfriends.beachbingo.feature.raetsel.KRIEG_FLEET
import com.bestfriends.beachbingo.feature.raetsel.KRIEG_GRID_SIZES
import com.bestfriends.beachbingo.feature.raetsel.KuestenkriegSession
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.feature.raetsel.deserializeBattleState
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class KkOnlineResultItem(
    val opponentId: String,
    val opponentName: String,
    val opponentAvatar: String,
    val myWins: Int,
    val theirWins: Int,
    val totalGames: Int,
    val lastGameAt: Long,
    val lastWinnerId: String,
    val constellationTitle: String,
)

private val KK_CONSTELLATION_NAMES_LOBBY = listOf(
    "Korallenflotte|Sandburgbataillon", "SprottenGirls|DorschBabys",
    "PalmenBoys|SchlauchbootMatrosen", "Wattjäger|Muschelsammler",
    "Möwenpiraten|Krakenflüsterer", "Brandungsreiter|Sandkastenkapitäne",
    "Tintenfischbande|Strandwächter", "Nordseeadler|Wattwurmbrigade",
    "Barrakuda-Crew|Seepferdchen-Staffel", "Wellenreiter|Sanddünenkommando",
    "Heringsjäger|Austernretter", "Salzwasserwölfe|Bademeister-Union",
    "Kormorantruppe|Strandkorbverteidiger", "Anker-Asse|Flaggen-Flatterer",
    "Neptunsgarde|Strandräuber-Koalition", "Krabbenklau-Clan|Muschelpiraten",
    "Tiefseebande|Flachlandmatrosen", "Sturmflut-Staffel|Sandburg-Söldner",
    "Möwenkönige|Plastikenten-Piraten", "Blauwal-Brigade|Minigolf-Miliz",
    "Sardellen-Syndrom|Lachs-Legion", "Schaumkronen-Crew|Treibholz-Truppe",
    "Quallen-Quartier|Sonnencrème-Söldner", "Brandungs-Barbaren|Wellenbrecher",
    "Ebbe-Allianz|Flut-Front",
)

private fun kkConstellationTitle(uid1: String, uid2: String): String {
    val sorted = listOf(uid1, uid2).sorted()
    val key = sorted.joinToString("|")
    var hash = 0L
    for (c in key) hash = ((hash * 31L) + c.code.toLong()) and Long.MAX_VALUE
    val idx = (hash % 25L).toInt()
    val pair = KK_CONSTELLATION_NAMES_LOBBY[idx].split("|")
    return if ((hash / 25L) % 2L == 0L) "${pair[0]} vs. ${pair[1]}" else "${pair[1]} vs. ${pair[0]}"
}

private fun formatGameDate(ms: Long): String =
    SimpleDateFormat("d. MMM", Locale.GERMAN).format(Date(ms))

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
    onNavigateToBattle: () -> Unit = {},
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(KkMode.PUZZLE) }
    var selectedDiff by remember { mutableStateOf("mittel") }
    var selectedAi by remember { mutableStateOf("kapitaen") }
    var puzzleSaves by remember { mutableStateOf(SoloGameSaveManager.getSaves(context).filter { it.gameType == "kuestenkrieg" }) }
    var kiSaves by remember { mutableStateOf(SoloGameSaveManager.getSaves(context).filter { it.gameType == "kuestenkrieg_ki" }) }
    val difficulties = listOf("leicht", "mittel", "schwer", "experte")
    val diffLabels = mapOf("leicht" to "Leicht", "mittel" to "Mittel", "schwer" to "Schwer", "experte" to "Experte")
    var showStats by remember { mutableStateOf(false) }
    var statsTab by remember { mutableStateOf(0) }
    var onlineResultItems by remember { mutableStateOf<List<KkOnlineResultItem>>(emptyList()) }
    var loadingOnline by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("kuestenkrieg") == true
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("kuestenkrieg") else FieldValue.arrayRemove("kuestenkrieg")
        if (uid.isNotBlank()) db.collection("users").document(uid).update("favoriteGames", update)
    }

    LaunchedEffect(showStats) {
        if (!showStats || uid.isBlank()) return@LaunchedEffect
        loadingOnline = true
        try {
            val snaps = db.collection("kuestenkriegResults")
                .whereArrayContains("playerIds", uid)
                .get().await()
            val docs = snaps.documents.sortedByDescending { it.getLong("createdAt") ?: 0L }
            val grouped = mutableMapOf<String, MutableList<com.google.firebase.firestore.DocumentSnapshot>>()
            for (doc in docs) {
                @Suppress("UNCHECKED_CAST")
                val pIds = (doc.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: continue
                val oppId = pIds.find { it != uid } ?: continue
                grouped.getOrPut(oppId) { mutableListOf() }.add(doc)
            }
            onlineResultItems = grouped.entries.map { (oppId, gameDocs) ->
                val myWins = gameDocs.count { it.getString("winnerId") == uid }
                val theirWins = gameDocs.count { it.getString("winnerId") == oppId }
                val lastDoc = gameDocs.first()
                @Suppress("UNCHECKED_CAST")
                val pIds = (lastDoc.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: listOf("", "")
                @Suppress("UNCHECKED_CAST")
                val pNames = (lastDoc.get("playerNames") as? List<*>)?.filterIsInstance<String>() ?: listOf("", "")
                @Suppress("UNCHECKED_CAST")
                val pAvatars = (lastDoc.get("playerAvatars") as? List<*>)?.filterIsInstance<String>() ?: listOf("👤", "👤")
                val oppIdx = pIds.indexOf(oppId).takeIf { it >= 0 } ?: 1
                val oppName = pNames.getOrElse(oppIdx) { "Gegner" }
                val oppAvatar = pAvatars.getOrElse(oppIdx) { "👤" }
                KkOnlineResultItem(
                    opponentId = oppId,
                    opponentName = oppName,
                    opponentAvatar = oppAvatar,
                    myWins = myWins,
                    theirWins = theirWins,
                    totalGames = gameDocs.size,
                    lastGameAt = lastDoc.getLong("createdAt") ?: 0L,
                    lastWinnerId = lastDoc.getString("winnerId") ?: "",
                    constellationTitle = kkConstellationTitle(pIds.getOrElse(0) { "" }, pIds.getOrElse(1) { "" }),
                )
            }.sortedByDescending { it.lastGameAt }
        } catch (_: Exception) {}
        loadingOnline = false
    }

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
                Text("⚓", fontSize = EmojiMedium)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RÄTSEL", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Küstenkrieg", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = RoseRed.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp).border(1.dp, RoseRed.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).clickable { showStats = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("🏆", fontSize = MaterialTheme.typography.titleSmall.fontSize) } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) SandGold.copy(alpha = 0.12f) else Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, if (isFavorite) SandGold.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(10.dp)).clickable { toggleFavorite() }
                ) { Box(contentAlignment = Alignment.Center) { Text(if (isFavorite) "★" else "☆", fontSize = MaterialTheme.typography.titleSmall.fontSize, color = if (isFavorite) SandGold else TextSub) } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).clickable { showRules = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("?", fontSize = MaterialTheme.typography.titleSmall.fontSize, color = TextSub, fontWeight = FontWeight.Bold) } }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // Mode selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SPIELMODUS", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                listOf(
                    Triple(KkMode.PUZZLE, "🧩", "Solo Rätsel" to "Zahlen am Rand verraten die Schiffe"),
                    Triple(KkMode.KI,     "🤖", "Gegen KI"    to "Klassisches Schiffe versenken"),
                    Triple(KkMode.ONLINE, "🌐", "Online"      to "Gegen echten Spieler"),
                ).forEach { (m, emoji, texts) ->
                    val sel = mode == m
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) RoseRed.copy(alpha = 0.08f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth()
                            .border(1.5.dp, if (sel) RoseRed else BorderColor, RoundedCornerShape(12.dp))
                            .clickable { mode = m; onlineError = "" }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(texts.first, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = if (sel) RoseRed else TextPrimary)
                                Text(texts.second, fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (sel) Text("✓", fontSize = MaterialTheme.typography.titleMedium.fontSize, color = RoseRed)
                        }
                    }
                }
            }

            // Puzzle options
            if (mode == KkMode.PUZZLE) {
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                    Text("Schlachtschiff-Rätsel: Zahlen am Rand zeigen Schiffsfelder pro Zeile/Spalte. Schiffe berühren sich nie diagonal. Tippen = Schiff, Lang drücken = Wasser.",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
                }
                Text("SCHWIERIGKEIT", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                difficulties.forEach { d ->
                    val sel = selectedDiff == d
                    Surface(shape = RoundedCornerShape(12.dp), color = if (sel) RoseRed.copy(alpha = 0.1f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) RoseRed else BorderColor, RoundedCornerShape(12.dp)).clickable { selectedDiff = d }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diffLabels[d] ?: d, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = if (sel) RoseRed else TextPrimary)
                                val size = KRIEG_GRID_SIZES[d] ?: 10
                                val fleet = KRIEG_FLEET[d] ?: emptyList()
                                Text("${size}×${size} · ${fleetLabel(fleet)}", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
                            }
                            if (sel) Text("✓", fontSize = MaterialTheme.typography.titleMedium.fontSize, color = RoseRed)
                        }
                    }
                }
                Button(onClick = { onNavigateToGame(selectedDiff, System.currentTimeMillis(), null) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Neues Rätsel", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = BgDark) }

                // Saved puzzle games
                if (puzzleSaves.isNotEmpty()) {
                    Text("GESPEICHERTE RÄTSEL", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    puzzleSaves.forEach { save ->
                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${diffLabels[save.difficulty] ?: save.difficulty} · ${KRIEG_GRID_SIZES[save.difficulty] ?: 10}×${KRIEG_GRID_SIZES[save.difficulty] ?: 10}",
                                        fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(SoloGameSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt", fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = RoseRed.copy(alpha = 0.1f),
                                    modifier = Modifier.border(1.dp, RoseRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onNavigateToGame(save.difficulty, save.seed, save.id) }
                                ) { Text("Fortsetzen", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = RoseRed, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = Danger.copy(alpha = 0.1f),
                                    modifier = Modifier.border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { SoloGameSaveManager.deleteSave(context, save.id); puzzleSaves = puzzleSaves.filter { it.id != save.id } }
                                ) { Text("✕", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = Danger, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                            }
                        }
                    }
                }
            }

            // Online options
            if (mode == KkMode.ONLINE) {
                if (onlineError.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = DangerGlow,
                        modifier = Modifier.fillMaxWidth().border(1.dp, DangerRing, RoundedCornerShape(10.dp))
                    ) { Text(onlineError, fontSize = MaterialTheme.typography.labelMedium.fontSize, color = Danger, modifier = Modifier.padding(12.dp)) }
                }
                Button(
                    onClick = ::createOnlineGame,
                    enabled = !creating && !joining,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed, disabledContainerColor = RoseRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (creating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BgDark, strokeWidth = 2.dp)
                    else Text("Neues Spiel erstellen", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = BgDark)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                    Text("ODER CODE EINGEBEN", fontSize = ChipLabelTiny, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                        focusedBorderColor = RoseRed,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = RoseRed,
                        focusedLabelColor = RoseRed,
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
                    if (joining) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RoseRed, strokeWidth = 2.dp)
                    else Text("Beitreten", fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = RoseRed)
                }
            }

            // KI options
            if (mode == KkMode.KI) {
                Text("KI-SCHWIERIGKEIT", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                AI_OPTIONS.forEach { opt ->
                    val sel = selectedAi == opt.id
                    Surface(shape = RoundedCornerShape(12.dp), color = if (sel) RoseRed.copy(alpha = 0.08f) else SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.5.dp, if (sel) RoseRed else BorderColor, RoundedCornerShape(12.dp)).clickable { selectedAi = opt.id }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(opt.emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opt.label, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = if (sel) RoseRed else TextPrimary)
                                Text(opt.desc, fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (sel) Text("✓", fontSize = MaterialTheme.typography.titleMedium.fontSize, color = RoseRed)
                        }
                    }
                }
                Button(onClick = { onNavigateToPlacement(selectedAi) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Schiffe setzen →", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = BgDark) }

                if (kiSaves.isNotEmpty()) {
                    Text("LAUFENDE KI-GEFECHTE", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    kiSaves.forEach { save ->
                        val aiLabel = when (save.variant) { "matrose" -> "Matrose"; "admiral" -> "Admiral"; else -> "Kapitän" }
                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark, modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("KI: $aiLabel", fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Gefecht läuft", fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = RoseRed.copy(alpha = 0.1f),
                                    modifier = Modifier.border(1.dp, RoseRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            val bs = deserializeBattleState(save.puzzleState ?: "")
                                            if (bs != null) {
                                                KuestenkriegSession.resumedState = bs
                                                KuestenkriegSession.resumedSaveId = save.id
                                                KuestenkriegSession.aiMode = when (save.variant) { "matrose" -> AiMode.MATROSE; "admiral" -> AiMode.ADMIRAL; else -> AiMode.KAPITAEN }
                                                onNavigateToBattle()
                                            }
                                        }
                                ) { Text("Fortsetzen", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = RoseRed, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                                Spacer(Modifier.width(8.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = Danger.copy(alpha = 0.1f),
                                    modifier = Modifier.border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { SoloGameSaveManager.deleteSave(context, save.id); kiSaves = kiSaves.filter { it.id != save.id } }
                                ) { Text("✕", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = Danger, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showStats) {
        Dialog(onDismissRequest = { showStats = false; statsTab = 0 }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("🏆 Statistik", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

                    // Tabs
                    Surface(shape = RoundedCornerShape(8.dp), color = BgDark, modifier = Modifier.fillMaxWidth()) {
                        Row {
                            listOf("Bestzeiten", "Online Duelle").forEachIndexed { i, label ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (statsTab == i) RoseRed else Color.Transparent,
                                    modifier = Modifier.weight(1f).clickable { statsTab = i },
                                ) {
                                    Text(label, fontSize = ChipLabel, fontWeight = FontWeight.Bold,
                                        color = if (statsTab == i) BgDark else TextMuted,
                                        textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (statsTab == 0) {
                        listOf("leicht" to "mittel", "schwer" to "experte").forEach { (d1, d2) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(d1, d2).forEach { d ->
                                    val best = SoloGameSaveManager.getBestTimeAny(context, "kuestenkrieg", d)
                                    Surface(shape = RoundedCornerShape(12.dp), color = BgDark, modifier = Modifier.weight(1f)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(14.dp)) {
                                            Text(diffLabels[d] ?: d, fontSize = MaterialTheme.typography.labelSmall.fontSize, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                            Spacer(Modifier.height(6.dp))
                                            Text(if (best != null) SoloGameSaveManager.formatElapsed(best) else "—",
                                                fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = if (best != null) RoseRed else TextMuted)
                                            Text("Bestzeit", fontSize = ChipLabelTiny, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    } else {
                        if (loadingOnline) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = RoseRed, strokeWidth = 2.dp)
                            }
                        } else if (onlineResultItems.isEmpty()) {
                            Text("Noch keine Online-Duelle gespielt.", fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                onlineResultItems.forEach { item ->
                                    Surface(shape = RoundedCornerShape(12.dp), color = BgDark, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.constellationTitle, fontSize = ChipLabelTiny, color = TextMuted, modifier = Modifier.weight(1f))
                                                Text("${item.totalGames} Spiele", fontSize = ChipLabelTiny, color = TextMuted)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(if (item.myWins >= item.theirWins) "🥇" else "🥈", fontSize = MaterialTheme.typography.labelLarge.fontSize)
                                                Text("Du: ${item.myWins}", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold,
                                                    color = if (item.myWins > item.theirWins) RoseRed else TextSub)
                                                Text("·", color = TextMuted, fontSize = ChipLabel)
                                                Text(item.opponentAvatar, fontSize = MaterialTheme.typography.labelLarge.fontSize)
                                                Text("${item.opponentName}: ${item.theirWins}", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold,
                                                    color = if (item.theirWins > item.myWins) RoseRed else TextSub)
                                            }
                                            Text(
                                                "Letztes: ${if (item.lastWinnerId == uid) "Du" else item.opponentName} gewonnen · ${formatGameDate(item.lastGameAt)}",
                                                fontSize = ChipLabelTiny, color = TextMuted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showStats = false; statsTab = 0 }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Schliessen", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }
    ALL_GAME_RULES["kuestenkrieg"]?.let { rule ->
        if (showRules) GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
    }
}
