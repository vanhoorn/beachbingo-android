package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun DuenenschattenGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<HitoriPuzzle?>(null) }
    var gs by remember { mutableStateOf<HitoriState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val saveIdRef = remember { saveId ?: SoloGameSaveManager.generateId() }
    val audio = remember { RaetselAudioManager() }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.startMusic(soundEnabled, musicEnabled) }

    BackHandler { running = false; showQuit = true }

    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateHitori(difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeHitoriState(p, savedState) else createHitoriState(p)
        val savedElapsed = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        elapsed = savedElapsed
        running = true
    }

    LaunchedEffect(running, showWin) {
        while (running && gs?.solved == false) { delay(1000L); elapsed++ }
    }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            SoloGameSaveManager.recordBestTime(context, "duenenschatten", "standard", difficulty, elapsed)
            SoloGameSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }
    LaunchedEffect(showWin) { if (showWin) audio.playSound("win") }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        val p = puzzle ?: return@LaunchedEffect
        SoloGameSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "duenenschatten", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeHitoriState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val p = puzzle
    val state = gs

    Scaffold(
        topBar = {
            GameHudBar(
                paused = !running,
                onPauseToggle = { running = !running },
                onQuit = { running = false; showQuit = true },
                onShowRules = { running = false; showHelp = true },
            ) {
                Column {
                    Text("DÜNENSC­HATTEN", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
            // Reserve: hint text + spacer + controls row + bottom padding
            val controlsH = 100f
            val availForGrid = (maxHeight.value - controlsH - 32f).coerceAtLeast(150f)
            val availW = (maxWidth.value - 24f).coerceAtLeast(150f)
            val cellDp = (minOf(availW, availForGrid) / gridSize).coerceIn(28f, 100f).dp

            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (p == null || state == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SandGoldLight)
                    }
                } else {
                    val conflicts = computeConflicts(state)
                    val size = p.size

                    val density = LocalDensity.current
                    val cellPx = with(density) { cellDp.toPx() }
                    val padPx = with(density) { 4.dp.toPx() }
                    val gapPx = with(density) { 2.dp.toPx() }
                    val pitchPx = cellPx + gapPx

                    // ── Grid with border + background ──────────────────────────
                    ZoomableGrid(
                        onTap = tap@{ gx, gy ->
                            if (gx < padPx || gy < padPx) return@tap
                            val currentState = gs ?: return@tap
                            val row = ((gy - padPx) / pitchPx).toInt()
                            val col = ((gx - padPx) / pitchPx).toInt()
                            if (row !in 0 until size || col !in 0 until size) return@tap
                            gs = toggleMark(currentState, row, col)
                        },
                        onLongPress = lp@{ gx, gy ->
                            if (gx < padPx || gy < padPx) return@lp
                            val currentState = gs ?: return@lp
                            val row = ((gy - padPx) / pitchPx).toInt()
                            val col = ((gx - padPx) / pitchPx).toInt()
                            if (row !in 0 until size || col !in 0 until size) return@lp
                            val newMark = if (currentState.marks[row][col] == CellMark.DOT) CellMark.WHITE else CellMark.DOT
                            gs = setMark(currentState, row, col, newMark)
                        },
                    ) {
                        Surface(
                            color = BgNavyCell,
                            border = BorderStroke(2.dp, TextMuted.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                for (r in 0 until size) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        for (c in 0 until size) {
                                            val mark = state.marks[r][c]
                                            val isBlack = mark == CellMark.BLACK
                                            val isDot = mark == CellMark.DOT
                                            val isConflict = (r to c) in conflicts.adjacentBlacks || (r to c) in conflicts.duplicateWhites
                                            val bgColor = when {
                                                isBlack    -> BgNightBlue
                                                isConflict -> Danger.copy(alpha = 0.15f)
                                                else       -> SurfaceDark
                                            }
                                            val borderCol = when {
                                                isConflict -> Danger
                                                else       -> BorderColor
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(cellDp)
                                                    .background(bgColor, RoundedCornerShape(4.dp))
                                                    .border(1.dp, borderCol, RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (!isBlack) {
                                                    Text(
                                                        p.grid[r][c].toString(),
                                                        fontSize = (cellDp.value * 0.42f).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isConflict) Danger else TextSub,
                                                    )
                                                }
                                                if (isDot) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(cellDp * 0.28f)
                                                            .align(Alignment.BottomEnd)
                                                            .offset((-3).dp, (-3).dp)
                                                            .background(SandGoldLight, CircleShape),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tippen = schwarz/Punkt · Lang drücken = Punkt",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted, textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Controls ───────────────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val hint = getHitoriHint(state)
                                if (hint != null) {
                                    audio.playSound("hint")
                                    val correct = if (p.solution[hint.first][hint.second]) CellMark.BLACK else CellMark.WHITE
                                    gs = setMark(state, hint.first, hint.second, correct)
                                }
                            },
                            border = BorderStroke(1.dp, SandGoldLight.copy(alpha = 0.5f)),
                        ) { Icon(Icons.Filled.Lightbulb, contentDescription = "Tipp", tint = SandGoldLight, modifier = Modifier.size(18.dp)) }
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
                    Text("Rätsel gelöst!", fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${SoloGameSaveManager.formatElapsed(elapsed)}", fontSize = CellNumber, color = SandGoldLight, modifier = Modifier.padding(top = 4.dp))
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
        ALL_GAME_RULES["duenenschatten"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showHelp = false; running = true }) }
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
