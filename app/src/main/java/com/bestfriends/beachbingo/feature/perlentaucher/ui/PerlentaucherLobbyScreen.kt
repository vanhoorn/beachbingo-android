package com.bestfriends.beachbingo.feature.perlentaucher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.home.ui.SavedGameRow
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerlentaucherLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGame: (level: Int, saveId: String?) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    val highestUnlocked = remember { SoloGameSaveManager.getHighestPerlentaucherLevel(context) }
    var selectedLevel by remember { mutableIntStateOf(1) }
    var isFavorite by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var savedGame by remember { mutableStateOf(SoloGameSaveManager.getGameSave(context, "perlentaucher")) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("perlentaucher") == true
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("perlentaucher") else FieldValue.arrayRemove("perlentaucher")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    fun parseSavedLevel(): Int {
        val save = savedGame ?: return 1
        return try { JSONObject(save.gameState).getInt("levelNumber") } catch (_: Exception) { 1 }
    }

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
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(14.dp))
                Text("🤿", fontSize = EmojiMedium)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RÄTSEL", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text("Perlentaucher", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                // Statistik
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).clickable { showStats = true }
                ) { Box(contentAlignment = Alignment.Center) { Text("🏆", fontSize = MaterialTheme.typography.titleSmall.fontSize) } }
                Spacer(Modifier.width(8.dp))
                // Favorit
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) SandGold.copy(alpha = 0.12f) else Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, if (isFavorite) SandGold.copy(0.5f) else BorderColor, RoundedCornerShape(10.dp)).clickable { toggleFavorite() }
                ) { Box(contentAlignment = Alignment.Center) { Text(if (isFavorite) "★" else "☆", fontSize = MaterialTheme.typography.titleSmall.fontSize, color = if (isFavorite) SandGold else TextSub) } }
                Spacer(Modifier.width(8.dp))
                // Regeln
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).clickable { showRules = true }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = TextSub, modifier = Modifier.size(18.dp)) } }
                Spacer(Modifier.width(8.dp))
                // Einstellungen
                Surface(
                    shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).clickable { onNavigateToSettings() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Settings, "Einstellungen", tint = TextSub, modifier = Modifier.size(18.dp)) } }
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // ── Gespeichertes Spiel ───────────────────────────────────────────
            savedGame?.let { save ->
                val savedLevel = parseSavedLevel()
                val label = save.displayLabel
                Text("FORTSETZEN", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp))
                SavedGameRow(
                    title = "Level $savedLevel",
                    subtitle = label,
                    color = OceanBlue,
                    onResume = { onNavigateToGame(savedLevel, save.id) },
                    onDelete = {
                        SoloGameSaveManager.deleteGameSave(context, "perlentaucher")
                        savedGame = null
                    },
                )
            }

            // ── Level-Auswahl ─────────────────────────────────────────────────
            Text("LEVEL WÄHLEN", fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp))

            // Schnellauswahl-Chips: 1, 10, 20, ... 150
            val quickLevels = listOf(1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150)
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                modifier = Modifier.height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(quickLevels.size) { idx ->
                    val lvl = quickLevels[idx]
                    val sel = selectedLevel == lvl
                    val locked = lvl > highestUnlocked
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (sel && !locked) OceanBlue.copy(alpha = 0.15f) else SurfaceDark,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .border(
                                if (sel && !locked) 1.5.dp else 1.dp,
                                if (sel && !locked) OceanBlue else BorderColor.copy(alpha = if (locked) 0.4f else 1f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(enabled = !locked) { selectedLevel = lvl },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (locked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(lvl.toString(), fontSize = CellNumber, color = TextMuted.copy(alpha = 0.35f))
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = TextMuted.copy(alpha = 0.3f), modifier = Modifier.size(10.dp))
                                }
                            } else {
                                Text(lvl.toString(), fontSize = CellNumber, fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal, color = if (sel) OceanBlue else TextPrimary)
                            }
                        }
                    }
                }
            }

            // Feinauswahl-Slider
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Level", fontSize = ChipLabel, color = TextMuted)
                    Text(selectedLevel.toString(), fontSize = CellNumber, fontWeight = FontWeight.Bold, color = OceanBlue)
                }
                val sliderMax = highestUnlocked.toFloat().coerceAtLeast(1f)
                Slider(
                    value = selectedLevel.toFloat().coerceAtMost(sliderMax),
                    onValueChange = { selectedLevel = it.toInt().coerceIn(1, highestUnlocked) },
                    valueRange = 1f..sliderMax,
                    steps = (highestUnlocked - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = OceanBlue, activeTrackColor = OceanBlue, inactiveTrackColor = Surface2Dark),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1", fontSize = ChipLabelTiny, color = TextMuted)
                    Text(highestUnlocked.toString(), fontSize = ChipLabelTiny, color = TextMuted)
                }
            }

            // Level-Info-Karte
            val config = remember(selectedLevel) {
                com.bestfriends.beachbingo.feature.perlentaucher.PerlentaucherLevelGenerator.generate(selectedLevel)
            }
            Surface(
                shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                modifier = Modifier.fillMaxWidth().border(1.dp, OceanBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LevelStatItem("Züge", config.movesLeft.toString())
                    VerticalDivider(color = BorderColor, modifier = Modifier.height(40.dp))
                    LevelStatItem("Ziel", "${config.targetScore} Pkt.")
                }
            }

            // ── Start-Button ──────────────────────────────────────────────────
            Button(
                onClick = { onNavigateToGame(selectedLevel, null) },
                enabled = selectedLevel <= highestUnlocked,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Neues Spiel — Level $selectedLevel", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = BgDark) }
        }

        Spacer(Modifier.height(32.dp))
    }

    ALL_GAME_RULES["perlentaucher"]?.let { rule ->
        if (showRules) GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
    }

    if (showStats) {
        AlertDialog(
            onDismissRequest = { showStats = false },
            title = { Text("🏆 Statistik", fontWeight = FontWeight.Bold) },
            text = {
                Text("Freigeschaltet bis Level: $highestUnlocked / 150", color = OceanBlue, fontWeight = FontWeight.Bold)
            },
            confirmButton = {
                TextButton(onClick = { showStats = false }) { Text("Schließen") }
            },
        )
    }
}

@Composable
private fun LevelStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = OceanBlue)
        Text(label, fontSize = ChipLabelTiny, color = TextMuted)
    }
}
