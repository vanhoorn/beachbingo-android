package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.bestfriends.beachbingo.ui.components.GameHudBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.raetsel.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun KuestenkriegGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<BattleshipPuzzle?>(null) }
    var gs by remember { mutableStateOf<BattleshipState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(ShipMark.SHIP) }
    val saveIdRef = remember { saveId ?: SoloGameSaveManager.generateId() }
    val audio = remember { KuestenkriegAudioManager(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.startMusic(soundEnabled, musicEnabled) }

    BackHandler { running = false; showQuit = true }

    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateBattleship(difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeBattleshipState(p, savedState) else createBattleshipState(p)
        elapsed = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) { while (running && gs?.solved == false) { delay(1000L); elapsed++ } }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            SoloGameSaveManager.recordBestTime(context, "kuestenkrieg", "standard", difficulty, elapsed)
            SoloGameSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }
    LaunchedEffect(showWin) { if (showWin) audio.playSound("win") }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        SoloGameSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "kuestenkrieg", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeBattleshipState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val p = puzzle; val state = gs

    Scaffold(
        topBar = {
            GameHudBar(
                paused = !running,
                onPauseToggle = { running = !running },
                onQuit = { running = false; showQuit = true },
                onShowRules = { running = false; showRules = true },
            ) {
                Column {
                    Text("KÜSTENKRIEG", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        "${difficulty.replaceFirstChar { it.uppercase() }} · ${SoloGameSaveManager.formatElapsed(elapsed)}",
                        style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        containerColor = BgDark
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
        val screenAvailW = maxWidth
        val screenAvailH = maxHeight
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (p == null || state == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RoseRed) }
            } else {
                val size = p.size
                val labelDp: Dp = 28.dp
                // Dynamic cell size: limited by both available width and height
                val cellFromW = (screenAvailW - labelDp - 16.dp) / size
                val cellFromH = (screenAvailH - labelDp - 130.dp) / size
                val cellDp: Dp = minOf(cellFromW, cellFromH).coerceAtLeast(24.dp)
                val errors = computeKriegErrors(state)

                val density = LocalDensity.current
                val labelPx = with(density) { labelDp.toPx() }
                val cellPx = with(density) { cellDp.toPx() }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ZoomableGrid(
                    onTap = tap@{ gx, gy ->
                        if (gx < labelPx || gy < labelPx) return@tap
                        val currentState = gs ?: return@tap
                        val row = ((gy - labelPx) / cellPx).toInt()
                        val col = ((gx - labelPx) / cellPx).toInt()
                        if (row !in 0 until size || col !in 0 until size) return@tap
                        val isGiven = p.givenShip[row][col] || p.givenWater[row][col]
                        if (!isGiven) gs = setKriegMark(currentState, row, col, activeTool)
                    },
                    onLongPress = lp@{ gx, gy ->
                        if (gx < labelPx || gy < labelPx) return@lp
                        val currentState = gs ?: return@lp
                        val row = ((gy - labelPx) / cellPx).toInt()
                        val col = ((gx - labelPx) / cellPx).toInt()
                        if (row !in 0 until size || col !in 0 until size) return@lp
                        val isGiven = p.givenShip[row][col] || p.givenWater[row][col]
                        if (!isGiven) gs = setKriegMark(currentState, row, col, ShipMark.WATER)
                    },
                ) {
                    Column {
                        // Col clues row
                        Row(modifier = Modifier.padding(start = labelDp)) {
                            (0 until size).forEach { c ->
                                Box(modifier = Modifier.size(width = cellDp, height = labelDp).background(BgDark), contentAlignment = Alignment.Center) {
                                    Text(p.colClues[c].toString(), fontSize = (cellDp.value * 0.38f).coerceAtMost(14f).sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (errors.cols[c]) Danger else TextPrimary)
                                }
                            }
                        }

                        // Grid rows
                        (0 until size).forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Row clue
                                Box(modifier = Modifier.size(width = labelDp, height = cellDp).background(BgDark), contentAlignment = Alignment.Center) {
                                    Text(p.rowClues[r].toString(), fontSize = (cellDp.value * 0.38f).coerceAtMost(14f).sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (errors.rows[r]) Danger else TextPrimary)
                                }
                                // Cells
                                (0 until size).forEach { c ->
                                    val mark = state.marks[r][c]
                                    val isGivenShip = p.givenShip[r][c]
                                    val isGivenWater = p.givenWater[r][c]
                                    val bgColor = when {
                                        isGivenShip || mark == ShipMark.SHIP -> RoseRed.copy(alpha = 0.25f)
                                        isGivenWater || mark == ShipMark.WATER -> Surface2Dark
                                        else -> SurfaceDark
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(cellDp)
                                            .background(bgColor, RoundedCornerShape(2.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (mark == ShipMark.SHIP || isGivenShip) {
                                            Box(Modifier.size(cellDp * 0.55f).background(if (isGivenShip) RoseRed else RoseRed.copy(alpha = 0.8f), CircleShape))
                                        }
                                        if (mark == ShipMark.WATER || isGivenWater) {
                                            Text("~", fontSize = (cellDp.value * 0.4f).sp, color = SkyBlue.copy(alpha = if (isGivenWater) 1f else 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                } // end centering Box

                Spacer(Modifier.height(10.dp))

                // Tool selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ShipMark.SHIP to "🚢 Schiff", ShipMark.WATER to "🌊 Wasser").forEach { (tool, label) ->
                        val sel = activeTool == tool
                        val color = if (tool == ShipMark.SHIP) RoseRed else SkyBlue
                        Surface(shape = RoundedCornerShape(10.dp), color = if (sel) color.copy(alpha = 0.15f) else Surface2Dark,
                            modifier = Modifier.border(1.5.dp, if (sel) color else BorderColor, RoundedCornerShape(10.dp)).clickable { activeTool = tool }
                        ) { Text(label, fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = if (sel) color else TextMuted, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val hint = getKriegHint(state)
                        if (hint != null) {
                            val correct = if (p.solution[hint.first][hint.second]) ShipMark.SHIP else ShipMark.WATER
                            gs = setKriegMark(state, hint.first, hint.second, correct)
                        }
                    }, border = androidx.compose.foundation.BorderStroke(1.dp, RoseRed.copy(alpha = 0.5f))) { Icon(Icons.Filled.Lightbulb, contentDescription = "Tipp", tint = RoseRed, modifier = Modifier.size(18.dp)) }
                }
            }
        }
        } // end BoxWithConstraints
    }

    if (showWin) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = DrawNumberTablet)
                    Spacer(Modifier.height(8.dp))
                    Text("Alle Schiffe gefunden!", fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${SoloGameSaveManager.formatElapsed(elapsed)}", fontSize = CellNumber, color = RoseRed, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)) {
                        Text("Zurück zur Lobby", fontWeight = FontWeight.Bold, color = BgDark)
                    }
                }
            }
        }
    }
    // ── Rules dialog ────────────────────────────────────────────────────────────
    if (showRules) {
        ALL_GAME_RULES["kuestenkrieg"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false; running = true }) }
    }

    // ── Quit dialog ───────────────────────────────────────────────────────────
    if (showQuit) {
        GameSaveQuitDialog(
            emoji = "⚓",
            onContinue = { running = true; showQuit = false },
            onSaveAndQuit = onNavigateBack,
            onQuitWithoutSave = { SoloGameSaveManager.deleteSave(context, saveIdRef); onNavigateBack() },
        )
    }
}
