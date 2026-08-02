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
import com.bestfriends.beachbingo.feature.raetsel.HITORI_SIZES
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSaveManager
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val DsAccent = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuenenschattenLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (difficulty: String, seed: Long, saveId: String?) -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    var selected by remember { mutableStateOf("mittel") }
    var saves by remember { mutableStateOf(PuzzleSaveManager.getSaves(context).filter { it.gameType == "duenenschatten" }) }
    var showStats by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("duenenschatten") == true
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("duenenschatten") else FieldValue.arrayRemove("duenenschatten")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    val difficulties = listOf("leicht", "mittel", "schwer", "experte")
    val diffLabels = mapOf("leicht" to "Leicht", "mittel" to "Mittel", "schwer" to "Schwer", "experte" to "Experte")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
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
                Text("◼", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RÄTSEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("DünenSchatten", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = DsAccent.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp).border(1.dp, DsAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).clickable { showStats = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("🏆", fontSize = 16.sp) } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) SandGold.copy(alpha = 0.12f) else Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, if (isFavorite) SandGold.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(10.dp)).clickable { toggleFavorite() }
                ) { Box(contentAlignment = Alignment.Center) { Text(if (isFavorite) "★" else "☆", fontSize = 16.sp, color = if (isFavorite) SandGold else TextSub) } }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).clickable { showRules = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("?", fontSize = 16.sp, color = TextSub, fontWeight = FontWeight.Bold) } }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Info
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            ) {
                Text(
                    "Hitori: Tippe Felder an, um sie schwarz zu markieren. Keine zwei schwarzen Felder dürfen nebeneinander liegen. Alle weißen Felder müssen verbunden sein. Jede Zahl darf in einer Zeile/Spalte nur einmal weiß sein.",
                    fontSize = 13.sp, color = TextMuted, lineHeight = 20.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Difficulty
            Text("SCHWIERIGKEIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
            difficulties.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        val isSelected = selected == d
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) DsAccent.copy(alpha = 0.1f) else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, if (isSelected) DsAccent else BorderColor, RoundedCornerShape(12.dp))
                                .clickable { selected = d }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(diffLabels[d] ?: d, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DsAccent else TextPrimary)
                                val size = HITORI_SIZES[d] ?: 7
                                Text("${size}×${size}", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            // Start button
            Button(
                onClick = { onNavigateToGame(selected, System.currentTimeMillis(), null) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DsAccent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Neues Spiel starten", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark)
            }

            // Saved games
            if (saves.isNotEmpty()) {
                Text("GESPEICHERTE SPIELE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                saves.forEach { save ->
                    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${diffLabels[save.difficulty] ?: save.difficulty} · ${HITORI_SIZES[save.difficulty] ?: 7}×${HITORI_SIZES[save.difficulty] ?: 7}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(PuzzleSaveManager.formatElapsed(save.elapsedSeconds) + " gespielt",
                                    fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DsAccent.copy(alpha = 0.1f),
                                modifier = Modifier.border(1.dp, DsAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { onNavigateToGame(save.difficulty, save.seed, save.id) }
                            ) { Text("Fortsetzen", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DsAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Danger.copy(alpha = 0.1f),
                                modifier = Modifier.border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { PuzzleSaveManager.deleteSave(context, save.id); saves = saves.filter { it.id != save.id } }
                            ) { Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Danger, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
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
                    Text("🏆 Bestzeiten", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                    listOf("leicht" to "mittel", "schwer" to "experte").forEach { (d1, d2) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(d1, d2).forEach { d ->
                                val best = PuzzleSaveManager.getBestTimeAny(context, "duenenschatten", d)
                                Surface(shape = RoundedCornerShape(12.dp), color = BgDark, modifier = Modifier.weight(1f)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(14.dp)) {
                                        Text(diffLabels[d] ?: d, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(if (best != null) PuzzleSaveManager.formatElapsed(best) else "—",
                                            fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (best != null) DsAccent else TextMuted)
                                        Text("Bestzeit", fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Button(onClick = { showStats = false }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DsAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Schliessen", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }
    ALL_GAME_RULES["duenenschatten"]?.let { rule ->
        if (showRules) GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
    }
}
