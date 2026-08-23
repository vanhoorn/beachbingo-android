package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.bestfriends.beachbingo.ui.components.GameHudBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun WellensummeGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<KakuroPuzzle?>(null) }
    var gs by remember { mutableStateOf<KakuroState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val saveIdRef = remember { saveId ?: SoloGameSaveManager.generateId() }
    val audio = remember { RaetselAudioManager(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.startMusic(soundEnabled, musicEnabled) }

    BackHandler { running = false; showQuit = true }

    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateKakuro(difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeKakuroState(p, savedState) else createKakuroState(p)
        elapsed = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) { while (running && gs?.solved == false) { delay(1000L); elapsed++ } }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            SoloGameSaveManager.recordBestTime(context, "wellensumme", "standard", difficulty, elapsed)
            SoloGameSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }
    LaunchedEffect(showWin) { if (showWin) audio.playSound("win") }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        SoloGameSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "wellensumme", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeKakuroState(state),
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
                onShowRules = { running = false; showHelp = true },
            ) {
                Column {
                    Text("WELLENSUMME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        "${difficulty.replaceFirstChar { it.uppercase() }} · ${SoloGameSaveManager.formatElapsed(elapsed)}",
                        style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        containerColor = BgDark,
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val gridSize = p?.size ?: 8
            // Reserve space: numpad row + controls row + spacers
            val controlsH = 130f
            val availForGrid = (maxHeight.value - controlsH - 24f).coerceAtLeast(150f)
            val availW = (maxWidth.value - 24f).coerceAtLeast(150f)
            val cellDp = (minOf(availW, availForGrid) / gridSize).coerceIn(28f, 100f).dp

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (p == null || state == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PurpleLight)
                    }
                } else {
                    val size = p.size

                    val density = LocalDensity.current
                    val cellPx = with(density) { cellDp.toPx() }
                    val padPx = with(density) { 2.dp.toPx() }
                    val gapPx = with(density) { 1.dp.toPx() }
                    val pitchPx = cellPx + gapPx

                    // ── Grid with border + background ──────────────────────────
                    ZoomableGrid(
                        onTap = tap@{ gx, gy ->
                            if (gx < padPx || gy < padPx) return@tap
                            val currentState = gs ?: return@tap
                            val row = ((gy - padPx) / pitchPx).toInt()
                            val col = ((gx - padPx) / pitchPx).toInt()
                            if (row !in 0 until size || col !in 0 until size) return@tap
                            if (p.cells[row][col].isBlack) return@tap
                            gs = selectKakuroCell(currentState, row, col)
                        },
                    ) {
                        Surface(
                            color = BgNavyCell,
                            border = BorderStroke(2.dp, TextMuted.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(2.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                for (r in 0 until size) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        for (c in 0 until size) {
                                            val cell = p.cells[r][c]
                                            val isSelected = state.selected == r to c
                                            val hasErr = state.errors[r][c]
                                            if (cell.isBlack) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(cellDp)
                                                        .background(BgNightBlue, RoundedCornerShape(2.dp))
                                                        .border(1.dp, DarkGray, RoundedCornerShape(2.dp)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    cell.downClue?.let { clue ->
                                                        Text(
                                                            clue.toString(),
                                                            fontSize = (cellDp.value * 0.28f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSub,
                                                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                                                        )
                                                    }
                                                    cell.rightClue?.let { clue ->
                                                        Text(
                                                            clue.toString(),
                                                            fontSize = (cellDp.value * 0.28f).sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSub,
                                                            modifier = Modifier.align(Alignment.BottomStart).padding(2.dp),
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(cellDp)
                                                        .background(
                                                            if (isSelected) PurpleLight.copy(alpha = 0.25f)
                                                            else if (hasErr) Danger.copy(alpha = 0.15f)
                                                            else SurfaceDark,
                                                            RoundedCornerShape(2.dp),
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) PurpleLight else if (hasErr) Danger else BorderColor,
                                                            RoundedCornerShape(2.dp),
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    val v = state.board[r][c]
                                                    if (v != 0) Text(
                                                        v.toString(),
                                                        fontSize = (cellDp.value * 0.45f).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (hasErr) Danger else TextPrimary,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Number pad 1–9 ─────────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..9).forEach { n ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Surface2Dark,
                                modifier = Modifier.size(36.dp).clickable { gs = enterKakuroNumber(state, n) },
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(n.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Controls ───────────────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { gs = eraseKakuroCell(state) },
                            border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f)),
                        ) { Text("⌫", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                val hint = getKakuroHint(state)
                                if (hint != null) {
                                    audio.playSound("hint")
                                    gs = enterKakuroNumber(selectKakuroCell(state, hint.first, hint.second), p.cells[hint.first][hint.second].solution ?: 0)
                                }
                            },
                            border = BorderStroke(1.dp, PurpleLight.copy(alpha = 0.5f)),
                        ) { Icon(Icons.Filled.Lightbulb, contentDescription = "Tipp", tint = PurpleLight, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }

    // ── Win dialog ──────────────────────────────────────────────────────────────
    if (showWin) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = DrawNumberTablet)
                    Spacer(Modifier.height(8.dp))
                    Text("Alle Summen stimmen!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${SoloGameSaveManager.formatElapsed(elapsed)}", fontSize = CellNumber, color = PurpleLight, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    ) { Text("Zurück zur Lobby", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }

    // ── Rules dialog ────────────────────────────────────────────────────────────
    if (showHelp) {
        ALL_GAME_RULES["wellensumme"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showHelp = false; running = true }) }
    }

    // ── Quit dialog ───────────────────────────────────────────────────────────
    if (showQuit) {
        GameSaveQuitDialog(
            emoji = "🏖️",
            onContinue = { running = true; showQuit = false },
            onSaveAndQuit = onNavigateBack,
            onQuitWithoutSave = { SoloGameSaveManager.deleteSave(context, saveIdRef); onNavigateBack() },
        )
    }
}
