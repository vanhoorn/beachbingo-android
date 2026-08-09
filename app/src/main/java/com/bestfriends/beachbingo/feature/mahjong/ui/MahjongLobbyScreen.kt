package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.home.ui.SavedGameRow
import com.bestfriends.beachbingo.feature.mahjong.LAYOUT_DEFS
import com.bestfriends.beachbingo.feature.mahjong.LAYOUT_ORDER
import com.bestfriends.beachbingo.feature.mahjong.LayoutId
import com.bestfriends.beachbingo.feature.mahjong.MahjongDifficulty
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahjongLobbyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGame: (layout: String, difficulty: String, seed: Long, saveId: String?) -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var difficulty by remember { mutableStateOf(MahjongDifficulty.ROOKIE) }
    var layout by remember { mutableStateOf(LayoutId.SCHILDKROETE) }
    var saves by remember { mutableStateOf(SoloGameSaveManager.getSaves(context).filter { it.gameType == "mahjong" }) }
    var showStats by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("mahjong") == true
            // Load preferred defaults
            snap.getString("preferredMahjongDifficulty")?.let { s ->
                runCatching { MahjongDifficulty.valueOf(s) }.getOrNull()?.let { difficulty = it }
            }
            snap.getString("preferredMahjongLayout")?.let { s ->
                runCatching { LayoutId.valueOf(s) }.getOrNull()?.let { layout = it }
            }
        } catch (_: Exception) {}
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("mahjong") else FieldValue.arrayRemove("mahjong")
        if (uid != null) db.collection("users").document(uid).update("favoriteGames", update)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Surface2Dark,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateBack() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text("🀄", fontSize = ScoreLarge)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SPIEL", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("GezeitenSteine", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                // Einstellungen
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Surface2Dark,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .clickable { onNavigateToSettings() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, null, tint = TextSub, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Stats
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MahjongGold.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MahjongGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { showStats = true },
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("🏆", fontSize = MaterialTheme.typography.titleSmall.fontSize) }
                }
                Spacer(Modifier.width(8.dp))
                // Regeln
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MahjongGold.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, MahjongGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { showRules = true },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, null, tint = MahjongGold, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Favorit
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFavorite) MahjongGold.copy(0.2f) else Surface2Dark,
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, if (isFavorite) MahjongGold else BorderColor, RoundedCornerShape(10.dp))
                        .clickable { toggleFavorite() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            color = if (isFavorite) MahjongGold else TextMuted,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Schwierigkeitsgrad ────────────────────────────────────────────
            Text("Schwierigkeitsgrad", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)

            // (label, emoji, desc)
            val difficulties = listOf(
                Triple(MahjongDifficulty.ROOKIE, "🐚", "ROOKIE") to "Freie Steine leuchten · Unbegrenzt Hinweise & Mischungen · Kein Timer",
                Triple(MahjongDifficulty.SNIPER, "🎯", "SNIPER") to "Kein Highlight · 3 Hinweise · 1 Mischung · Timer",
                Triple(MahjongDifficulty.BOSS,   "💀", "BOSS LEVEL") to "Keine Hilfen · Kein Mischen · Timer · Nur Highscore",
            )

            difficulties.forEach { (meta, desc) ->
                val (d, emoji, label) = meta
                val sel = difficulty == d
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (sel) MahjongGold.copy(0.15f) else SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (sel) 2.dp else 1.dp,
                            color = if (sel) MahjongGold else BorderColor,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable { difficulty = d },
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = if (sel) MahjongGold else TextPrimary)
                            Spacer(Modifier.height(3.dp))
                            Text(desc, fontSize = ChipLabel, color = TextMuted, lineHeight = 16.sp)
                        }
                        if (sel) {
                            Spacer(Modifier.width(8.dp))
                            Text("✓", fontSize = MaterialTheme.typography.titleMedium.fontSize, color = MahjongGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Layout-Auswahl ────────────────────────────────────────────────
            if (difficulty != MahjongDifficulty.ROOKIE) {
                Text("Layout", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LAYOUT_ORDER.forEach { l ->
                        val sel = layout == l
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) MahjongGold.copy(0.15f) else SurfaceDark,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (sel) 2.dp else 1.dp,
                                    color = if (sel) MahjongGold else BorderColor,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { layout = l },
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                LayoutPreview(
                                    layoutId = l,
                                    active   = sel,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${l.emoji} ${l.label}",
                                    fontSize = StatusTiny,
                                    color = if (sel) MahjongGold else TextMuted,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            // ── Starten ───────────────────────────────────────────────────────
            Button(
                onClick = {
                    val chosenLayout = if (difficulty == MahjongDifficulty.ROOKIE) LayoutId.SCHILDKROETE else layout
                    onNavigateToGame(chosenLayout.name, difficulty.name, System.currentTimeMillis(), null)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MahjongGold),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Spiel starten", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }

            // ── Gespeicherte Spiele ───────────────────────────────────────────
            if (saves.isNotEmpty()) {
                Text("Gespeicherte Spiele", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                saves.forEach { save ->
                    val saveLayout = runCatching { LayoutId.valueOf(save.variant) }.getOrNull()
                    val saveDiff   = runCatching { MahjongDifficulty.valueOf(save.difficulty) }.getOrNull()
                    SavedGameRow(
                        title    = "${saveLayout?.emoji ?: "🀄"} ${saveLayout?.label ?: save.variant}",
                        subtitle = "${saveDiff?.name ?: save.difficulty} · ${SoloGameSaveManager.formatElapsed(save.elapsedSeconds)}",
                        color    = MahjongGold,
                        onResume = { onNavigateToGame(save.variant, save.difficulty, save.seed, save.id) },
                        onDelete = {
                            SoloGameSaveManager.deleteSave(context, save.id)
                            saves = SoloGameSaveManager.getSaves(context).filter { it.gameType == "mahjong" }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ── Stats-Dialog ──────────────────────────────────────────────────────────
    if (showStats) {
        Dialog(onDismissRequest = { showStats = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = MaterialTheme.typography.headlineLarge.fontSize)
                    Spacer(Modifier.height(8.dp))
                    Text("Boss Level · Bestzeiten", fontSize = MaterialTheme.typography.bodyLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    LAYOUT_ORDER.forEach { l ->
                        val best = SoloGameSaveManager.getBestTime(context, "mahjong", l.name, MahjongDifficulty.BOSS.name)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${l.emoji} ${l.label}", fontSize = CellNumber, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                if (best != null) SoloGameSaveManager.formatElapsed(best) else "—",
                                fontSize = CellNumber,
                                fontWeight = FontWeight.Bold,
                                color = if (best != null) MahjongGold else TextMuted,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showStats = false },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, TextSub.copy(0.4f)),
                    ) {
                        Text("Schließen", color = TextSub)
                    }
                }
            }
        }
    }

    // ── Spielregeln ───────────────────────────────────────────────────────────
    if (showRules) {
        val rule = ALL_GAME_RULES["mahjong"]
        if (rule != null) {
            GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
        }
    }
}

@Composable
private fun LayoutPreview(
    layoutId: LayoutId,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val positions = LAYOUT_DEFS[layoutId]?.positions ?: return
    val layer0 = positions.filter { it.third == 0 }
    if (layer0.isEmpty()) return

    val minC = layer0.minOf { it.first }
    val maxC = layer0.maxOf { it.first }
    val minR = layer0.minOf { it.second }
    val maxR = layer0.maxOf { it.second }
    val spanC = ((maxC - minC) / 2 + 1).toFloat()

    val density  = LocalDensity.current
    val maxDotPx = with(density) { 5.dp.toPx() }
    val dotColor = if (active) MahjongGold else TextMuted
    val alpha    = if (active) 0.9f else 0.45f

    Canvas(modifier = modifier) {
        val dotW = minOf(maxDotPx, size.width / spanC)
        val dotH = dotW * 1.3f
        layer0.forEach { pos ->
            val x = (pos.first  - minC) / 2f * dotW
            val y = (pos.second - minR) / 2f * dotH
            drawRect(
                color    = dotColor.copy(alpha = alpha),
                topLeft  = Offset(x, y),
                size     = Size(dotW - 0.5f, dotH - 0.5f),
            )
        }
    }
}
