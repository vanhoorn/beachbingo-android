package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val KkAccent = Color(0xFFFB7185)
private val KkWater = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuestenkriegGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<BattleshipPuzzle?>(null) }
    var gs by remember { mutableStateOf<BattleshipState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(ShipMark.SHIP) }
    val saveIdRef = remember { saveId ?: PuzzleSaveManager.generateId() }

    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateBattleship(difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeBattleshipState(p, savedState) else createBattleshipState(p)
        elapsed = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) { while (running && gs?.solved == false) { delay(1000L); elapsed++ } }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            PuzzleSaveManager.recordBestTime(context, "kuestenkrieg", "standard", difficulty, elapsed)
            PuzzleSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        PuzzleSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "kuestenkrieg", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeBattleshipState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val p = puzzle; val state = gs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("KÜSTENKRIEG", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${difficulty.replaceFirstChar { it.uppercase() }} · ${PuzzleSaveManager.formatElapsed(elapsed)}",
                            style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = { IconButton(onClick = { running = false; showQuit = true }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
        val screenAvailW = maxWidth
        val screenAvailH = maxHeight
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (p == null || state == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KkAccent) }
            } else {
                val size = p.size
                val labelDp: Dp = 22.dp
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
                                Box(modifier = Modifier.size(width = cellDp, height = labelDp), contentAlignment = Alignment.Center) {
                                    Text(p.colClues[c].toString(), fontSize = (cellDp.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (errors.cols[c]) Danger else TextPrimary)
                                }
                            }
                        }

                        // Grid rows
                        (0 until size).forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Row clue
                                Box(modifier = Modifier.size(width = labelDp, height = cellDp), contentAlignment = Alignment.Center) {
                                    Text(p.rowClues[r].toString(), fontSize = (cellDp.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold,
                                        color = if (errors.rows[r]) Danger else TextPrimary)
                                }
                                // Cells
                                (0 until size).forEach { c ->
                                    val mark = state.marks[r][c]
                                    val isGivenShip = p.givenShip[r][c]
                                    val isGivenWater = p.givenWater[r][c]
                                    val bgColor = when {
                                        isGivenShip || mark == ShipMark.SHIP -> KkAccent.copy(alpha = 0.25f)
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
                                            Box(Modifier.size(cellDp * 0.55f).background(if (isGivenShip) KkAccent else KkAccent.copy(alpha = 0.8f), CircleShape))
                                        }
                                        if (mark == ShipMark.WATER || isGivenWater) {
                                            Text("~", fontSize = (cellDp.value * 0.4f).sp, color = KkWater.copy(alpha = if (isGivenWater) 1f else 0.5f))
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
                        val color = if (tool == ShipMark.SHIP) KkAccent else KkWater
                        Surface(shape = RoundedCornerShape(10.dp), color = if (sel) color.copy(alpha = 0.15f) else Surface2Dark,
                            modifier = Modifier.border(1.5.dp, if (sel) color else BorderColor, RoundedCornerShape(10.dp)).clickable { activeTool = tool }
                        ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (sel) color else TextMuted, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
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
                    }, border = androidx.compose.foundation.BorderStroke(1.dp, KkAccent.copy(alpha = 0.5f))) { Text("💡", color = KkAccent, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { running = !running }, border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f))) { Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { running = false; showQuit = true }, border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.5f))) { Text("✕", color = Danger, fontWeight = FontWeight.Bold) }
                }
            }
        }
        } // end BoxWithConstraints
    }

    if (showWin) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Alle Schiffe gefunden!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}", fontSize = 14.sp, color = KkAccent, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)) {
                        Text("Zurück zur Lobby", fontWeight = FontWeight.Bold, color = BgDark)
                    }
                }
            }
        }
    }
    if (showQuit) {
        Dialog(onDismissRequest = { running = true; showQuit = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚓", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Spiel beenden?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { running = true; showQuit = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2Dark),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("Weiterspielen", color = TextPrimary, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KkAccent),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("💾 Speichern & Beenden", color = BgDark, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { PuzzleSaveManager.deleteSave(context, saveIdRef); onNavigateBack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("✕ Beenden ohne Speichern", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
