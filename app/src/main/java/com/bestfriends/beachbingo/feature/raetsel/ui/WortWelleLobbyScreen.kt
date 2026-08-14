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
import com.bestfriends.beachbingo.core.model.ALL_GAMES
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WortWelleLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (difficulty: String, isDaily: Boolean, dailyWord: String, dateStr: String, saveId: String?) -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    var wordBankReady by remember { mutableStateOf(WwWordBank.isReady) }
    var selected by remember { mutableStateOf("mittel") }
    var saves by remember { mutableStateOf(SoloGameSaveManager.getSaves(context).filter { it.gameType == "wortwelle" && it.variant == "random" }) }
    var showStats by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val gameEmoji = ALL_GAMES.first { it.id == "wortwelle" }.emoji

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("wortwelle") == true
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("wortwelle") else FieldValue.arrayRemove("wortwelle")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    LaunchedEffect(Unit) {
        if (!WwWordBank.isReady) {
            withContext(Dispatchers.IO) { WwWordBank.init(context) }
            wordBankReady = true
        }
    }

    val (dailyWord, dateStr) = remember(selected, wordBankReady) {
        if (wordBankReady) getDailyWwWord(selected) else Pair("", "")
    }
    val dailyPlayed = remember(selected, showStats, wordBankReady) {
        if (wordBankReady) hasDailyWwBeenPlayed(context, selected, dateStr) else false
    }

    val cfg = WW_CONFIG[selected]!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onNavigateBack() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text(gameEmoji, fontSize = EmojiMedium)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RÄTSEL", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("WortWelle", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp), color = CyanBright.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, CyanBright.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { showStats = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("🏆", fontSize = MaterialTheme.typography.titleSmall.fontSize) } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) SandGold.copy(alpha = 0.12f) else Surface2Dark,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, if (isFavorite) SandGold.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { toggleFavorite() }
                ) { Box(contentAlignment = Alignment.Center) { Text(if (isFavorite) "★" else "☆", fontSize = MaterialTheme.typography.titleSmall.fontSize, color = if (isFavorite) SandGold else TextSub) } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { showRules = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("?", fontSize = MaterialTheme.typography.titleSmall.fontSize, color = TextSub, fontWeight = FontWeight.Bold) } }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // ── Schwierigkeit ─────────────────────────────────────────────────
            Text("SCHWIERIGKEIT", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            WW_DIFFICULTIES.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val c = WW_CONFIG[d]!!
                        val isSel = selected == d
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) CyanBright.copy(alpha = 0.1f) else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, if (isSel) CyanBright else BorderColor, RoundedCornerShape(12.dp))
                                .clickable { selected = d }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(c.label, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = if (isSel) CyanBright else TextPrimary)
                                Text(c.description, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Wort des Tages ────────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CyanBright.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth().border(1.5.dp, CyanBright.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = BingoCallSize)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wort des Tages", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = CyanBright)
                            Text(dateStr, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted)
                        }
                        if (dailyPlayed) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Success.copy(alpha = 0.15f),
                            ) {
                                Text("✓ Gespielt", fontSize = ChipLabel, color = Success, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onNavigateToGame(selected, true, dailyWord, dateStr, null) },
                        enabled = !dailyPlayed,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            disabledContainerColor = Surface2Dark,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            if (dailyPlayed) "Heute bereits gespielt" else "$gameEmoji Tageswort spielen (${cfg.wordLength} Buchstaben)",
                            fontSize = CellNumber, fontWeight = FontWeight.ExtraBold,
                            color = if (dailyPlayed) TextMuted else BgDark,
                        )
                    }
                }
            }

            // ── Zufälliges Spiel ──────────────────────────────────────────────
            Button(
                onClick = { onNavigateToGame(selected, false, "", "", null) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("$gameEmoji Zufälliges Spiel starten", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = BgDark)
            }

            // ── Gespeicherte Spiele ───────────────────────────────────────────
            val filteredSaves = saves
            if (filteredSaves.isNotEmpty()) {
                Text("GESPEICHERTE SPIELE", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                filteredSaves.forEach { save ->
                    val st = try { deserializeWwState(save.puzzleState) } catch (_: Exception) { null }
                    val guessCount = st?.guesses?.size ?: 0
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${WW_CONFIG[save.difficulty]?.label ?: save.difficulty} · $guessCount Versuch${if (guessCount == 1) "" else "e"}",
                                    fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary,
                                )
                                Text(
                                    SoloGameSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt",
                                    fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp), color = CyanBright.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .border(1.dp, CyanBright.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToGame(save.difficulty, false, "", "", save.id) }
                            ) { Text("→", fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.Bold, color = CyanBright, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp), color = Danger.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        SoloGameSaveManager.deleteSave(context, save.id)
                                        saves = saves.filter { it.id != save.id }
                                    }
                            ) { Text("✕", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = Danger, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    // ── Statistiken-Dialog ─────────────────────────────────────────────────────
    if (showStats) {
        val stats = remember(selected) { getWwStats(context, selected) }
        Dialog(onDismissRequest = { showStats = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("📊 Statistiken", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(cfg.label, fontSize = ChipLabel, color = CyanBright, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val winPct = if (stats.played > 0) (stats.won * 100 / stats.played) else 0
                        listOf(
                            Pair("Gespielt", stats.played.toString()),
                            Pair("Gewonnen %", "$winPct%"),
                            Pair("Streak", stats.currentStreak.toString()),
                            Pair("Max Streak", stats.maxStreak.toString()),
                        ).forEach { (label, value) ->
                            Surface(shape = RoundedCornerShape(10.dp), color = BgDark, modifier = Modifier.weight(1f)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(value, fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                    Text(label, fontSize = StatusTiny, color = TextMuted, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    if (stats.distribution.any { it > 0 }) {
                        Spacer(Modifier.height(16.dp))
                        Text("VERTEILUNG", fontSize = MaterialTheme.typography.labelSmall.fontSize, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        val maxVal = stats.distribution.maxOrNull()?.takeIf { it > 0 } ?: 1
                        stats.distribution.forEachIndexed { i, count ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}", fontSize = ChipLabel, color = TextMuted, modifier = Modifier.width(16.dp))
                                Spacer(Modifier.width(8.dp))
                                val fraction = (count.toFloat().coerceAtLeast(0.05f) / maxVal.toFloat()).coerceIn(0.05f, 1f)
                                Box(modifier = Modifier.weight(1f).height(18.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .fillMaxHeight()
                                            .background(if (count > 0) CyanBright else Surface2Dark, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        if (count > 0) Text("$count", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = BgDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (stats.dailyPlayed > 0) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BgDark,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                Text("🗓 Tageswort", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Gespielt: ${stats.dailyPlayed} · Gewonnen: ${stats.dailyWon} · Streak: ${stats.dailyCurrentStreak}",
                                    fontSize = ChipLabel, color = TextMuted,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showStats = false }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("Schliessen", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }

    // ── Regeln-Dialog ─────────────────────────────────────────────────────────
    if (showRules) {
        Dialog(onDismissRequest = { showRules = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("$gameEmoji WortWelle", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("Wordle auf Deutsch", fontSize = ChipLabel, color = CyanBright,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Errate das versteckte Wort in wenigen Versuchen.", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted, lineHeight = 18.sp)
                        Text("Nach jedem Versuch zeigen die Farben, wie nah du warst:", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted, lineHeight = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(28.dp).background(Success, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center) { Text("S", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                            Text("Grün: Richtige Position!", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(28.dp).background(WwPresent, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center) { Text("T", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                            Text("Gelb: Im Wort, falsche Position.", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(28.dp).background(WwAbsent, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center) { Text("R", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                            Text("Grau: Nicht im Wort.", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted)
                        }
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                        Text("Umlaute werden ersetzt: Ä=AE, Ö=OE, Ü=UE, ß=SS (z.B. BOESE statt BÖSE).", fontSize = ChipLabel, color = TextMuted, lineHeight = 17.sp)
                        Text("Wort des Tages: Weltweit dasselbe Wort — einmal täglich pro Schwierigkeit.", fontSize = ChipLabel, color = TextMuted, lineHeight = 17.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showRules = false }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("Verstanden!", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }
}
