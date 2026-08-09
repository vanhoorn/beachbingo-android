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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.raetsel.HASHI_GRID_SIZES
import com.bestfriends.beachbingo.feature.raetsel.HASHI_ISLAND_COUNTS
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InselbrueckeLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (difficulty: String, seed: Long, saveId: String?) -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    var selected by remember { mutableStateOf("mittel") }
    var saves by remember { mutableStateOf(SoloGameSaveManager.getSaves(context).filter { it.gameType == "inselbruecke" }) }
    var showStats by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("inselbruecke") == true
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("inselbruecke") else FieldValue.arrayRemove("inselbruecke")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    val difficulties = listOf("leicht", "mittel", "schwer", "experte")
    val diffLabels = mapOf("leicht" to "Leicht", "mittel" to "Mittel", "schwer" to "Schwer", "experte" to "Experte")

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark))).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onNavigateBack() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text("🌉", fontSize = EmojiMedium)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RÄTSEL", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Inselbrücke", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = LimeGreen.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp).border(1.dp, LimeGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).clickable { showStats = true }
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
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Text("Verbinde Inseln mit Brücken. Die Zahl auf jeder Insel zeigt, wie viele Brücken sie braucht. Brücken dürfen sich nicht kreuzen, maximal 2 pro Paar. Alle Inseln müssen verbunden sein.",
                    fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
            }
            Text("SCHWIERIGKEIT", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            difficulties.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val sel = selected == d
                        Surface(shape = RoundedCornerShape(12.dp), color = if (sel) LimeGreen.copy(alpha = 0.1f) else SurfaceDark,
                            modifier = Modifier.weight(1f).border(1.5.dp, if (sel) LimeGreen else BorderColor, RoundedCornerShape(12.dp)).clickable { selected = d }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(diffLabels[d] ?: d, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = if (sel) LimeGreen else TextPrimary)
                                val size = HASHI_GRID_SIZES[d] ?: 9
                                val count = HASHI_ISLAND_COUNTS[d] ?: 14
                                Text("${size}×${size} · ${count} Inseln", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = { onNavigateToGame(selected, System.currentTimeMillis(), null) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Neues Spiel starten", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = BgDark) }
            if (saves.isNotEmpty()) {
                Text("GESPEICHERTE SPIELE", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                saves.forEach { save ->
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${diffLabels[save.difficulty] ?: save.difficulty} · ${HASHI_GRID_SIZES[save.difficulty] ?: 9}×${HASHI_GRID_SIZES[save.difficulty] ?: 9}",
                                    fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(SoloGameSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt", fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = LimeGreen.copy(alpha = 0.1f),
                                modifier = Modifier.border(1.dp, LimeGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onNavigateToGame(save.difficulty, save.seed, save.id) }
                            ) { Text("Fortsetzen", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = LimeGreen, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Danger.copy(alpha = 0.1f),
                                modifier = Modifier.border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { SoloGameSaveManager.deleteSave(context, save.id); saves = saves.filter { it.id != save.id } }
                            ) { Text("✕", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = Danger, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showStats) {
        Dialog(onDismissRequest = { showStats = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("🏆 Bestzeiten", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                    listOf("leicht" to "mittel", "schwer" to "experte").forEach { (d1, d2) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(d1, d2).forEach { d ->
                                val best = SoloGameSaveManager.getBestTimeAny(context, "inselbruecke", d)
                                Surface(shape = RoundedCornerShape(12.dp), color = BgDark, modifier = Modifier.weight(1f)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(14.dp)) {
                                        Text(diffLabels[d] ?: d, fontSize = MaterialTheme.typography.labelSmall.fontSize, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(if (best != null) SoloGameSaveManager.formatElapsed(best) else "—",
                                            fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = if (best != null) LimeGreen else TextMuted)
                                        Text("Bestzeit", fontSize = ChipLabelTiny, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Button(onClick = { showStats = false }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Schliessen", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }
    ALL_GAME_RULES["inselbruecke"]?.let { rule ->
        if (showRules) GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
    }
}
