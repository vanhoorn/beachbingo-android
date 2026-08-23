package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.bestfriends.beachbingo.ui.components.GameHudBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
fun InselbrueckeGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var puzzleWithSol by remember { mutableStateOf<HashiPuzzleWithSolution?>(null) }
    var gs by remember { mutableStateOf<HashiState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val showConnectHint by remember { derivedStateOf { gs?.let { !it.solved && hashiAllSumsCorrect(it.puzzle, it.bridges) } ?: false } }
    var selectedIslandId by remember { mutableStateOf<Int?>(null) }
    // Incrementing this key forces ZoomableGrid to recreate its state (= zoom reset)
    var zoomResetKey by remember { mutableIntStateOf(0) }
    val saveIdRef = remember { saveId ?: SoloGameSaveManager.generateId() }
    val audio = remember { RaetselAudioManager(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.startMusic(soundEnabled, musicEnabled) }

    BackHandler { running = false; showQuit = true }

    LaunchedEffect(seed) {
        val ps = withContext(Dispatchers.Default) { generateHashiWithSolution(difficulty, seed.toInt()) }
        puzzleWithSol = ps
        val savedState = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeHashiState(ps.puzzle, ps.solution, savedState) else createHashiState(ps.puzzle)
        elapsed = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) {
        while (running && gs?.solved == false) { delay(1000L); elapsed++ }
    }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            SoloGameSaveManager.recordBestTime(context, "inselbruecke", "standard", difficulty, elapsed)
            SoloGameSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }
    LaunchedEffect(showWin) { if (showWin) audio.playSound("win") }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        val ps = puzzleWithSol ?: return@LaunchedEffect
        SoloGameSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "inselbruecke", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeHashiState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val ps = puzzleWithSol
    val state = gs
    val textMeasurer = rememberTextMeasurer()

    Scaffold(
        topBar = {
            GameHudBar(
                paused = !running,
                onPauseToggle = { running = !running },
                onQuit = { running = false; showQuit = true },
                onShowRules = { running = false; showHelp = true },
            ) {
                Column {
                    Text("INSELBRÜCKE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
            val controlsH = 76f
            val canvasAreaSize = minOf(maxWidth.value - 24f, maxHeight.value - controlsH - 24f)
                .coerceAtLeast(240f).dp
            val innerCanvasSize = (canvasAreaSize.value - 24f).coerceAtLeast(200f).dp

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (ps == null || state == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LimeGreen)
                    }
                } else {
                    val puzzle = ps.puzzle
                    val gridSize = puzzle.gridSize
                    val boxPx = with(LocalDensity.current) { innerCanvasSize.toPx() / gridSize }

                    Surface(
                        modifier = Modifier.size(canvasAreaSize),
                        shape = RoundedCornerShape(12.dp),
                        color = BgDeepNavy,
                        border = BorderStroke(1.5.dp, BorderColor),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            key(zoomResetKey) {
                                ZoomableGrid(
                                    modifier = Modifier.size(innerCanvasSize),
                                    onTap = { tapX, tapY ->
                                        val currentState = gs ?: return@ZoomableGrid
                                        val currentPuzzle = puzzleWithSol?.puzzle ?: return@ZoomableGrid
                                        val maxDistSq = (boxPx * 0.6f) * (boxPx * 0.6f)
                                        val tapped = currentPuzzle.islands.minByOrNull { isl ->
                                            val cx = isl.col * boxPx + boxPx / 2f
                                            val cy = isl.row * boxPx + boxPx / 2f
                                            (tapX - cx) * (tapX - cx) + (tapY - cy) * (tapY - cy)
                                        }?.takeIf { isl ->
                                            val cx = isl.col * boxPx + boxPx / 2f
                                            val cy = isl.row * boxPx + boxPx / 2f
                                            (tapX - cx) * (tapX - cx) + (tapY - cy) * (tapY - cy) <= maxDistSq
                                        }
                                        if (tapped != null) {
                                            val sel = selectedIslandId
                                            if (sel == null || sel == tapped.id) {
                                                selectedIslandId = if (sel == tapped.id) null else tapped.id
                                            } else {
                                                val selIsland = currentPuzzle.islands.find { it.id == sel }
                                                if (selIsland != null) {
                                                    val neighbors = getNeighborIslands(currentPuzzle, selIsland, currentState.bridges)
                                                    if (neighbors.any { it.id == tapped.id }) {
                                                        gs = toggleHashiBridge(currentState, sel, tapped.id)
                                                    }
                                                }
                                                selectedIslandId = null
                                            }
                                        }
                                    },
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val dotPaint = BorderColor
                                        for (r in 0 until gridSize) for (c in 0 until gridSize) {
                                            drawCircle(dotPaint, radius = 2f, center = Offset(c * boxPx + boxPx / 2, r * boxPx + boxPx / 2))
                                        }
                                        for (b in state.bridges) {
                                            val a = puzzle.islands.find { it.id == b.from } ?: continue
                                            val bb = puzzle.islands.find { it.id == b.to } ?: continue
                                            val x1 = a.col * boxPx + boxPx / 2; val y1 = a.row * boxPx + boxPx / 2
                                            val x2 = bb.col * boxPx + boxPx / 2; val y2 = bb.row * boxPx + boxPx / 2
                                            val bridgeOffset = if (b.count == 2) 4f else 0f
                                            val isHoriz = a.row == bb.row
                                            for (i in 0 until b.count) {
                                                val o = if (b.count == 1) 0f else (if (i == 0) -bridgeOffset else bridgeOffset)
                                                drawLine(
                                                    LimeGreen,
                                                    start = if (isHoriz) Offset(x1, y1 + o) else Offset(x1 + o, y1),
                                                    end = if (isHoriz) Offset(x2, y2 + o) else Offset(x2 + o, y2),
                                                    strokeWidth = 3f,
                                                )
                                            }
                                        }
                                        for (isl in puzzle.islands) {
                                            val cx = isl.col * boxPx + boxPx / 2
                                            val cy = isl.row * boxPx + boxPx / 2
                                            val sum = islandBridgeSum(isl, state.bridges)
                                            val done = sum == isl.value
                                            val over = sum > isl.value
                                            val isSelected = selectedIslandId == isl.id
                                            val fillColor = when { done -> LimeGreen.copy(alpha = 0.3f); over -> Danger.copy(alpha = 0.3f); else -> Surface2Dark }
                                            val strokeColor = when { isSelected -> LimeGreen; done -> LimeGreen; over -> Danger; else -> TextSub }
                                            drawCircle(fillColor, radius = boxPx * 0.4f, center = Offset(cx, cy))
                                            drawCircle(strokeColor, radius = boxPx * 0.4f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(if (isSelected) 3f else 2f))
                                            val txt = textMeasurer.measure(isl.value.toString(), TextStyle(fontSize = (boxPx * 0.3f).sp, fontWeight = FontWeight.Bold, color = TextPrimary))
                                            drawText(txt, topLeft = Offset(cx - txt.size.width / 2, cy - txt.size.height / 2))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    if (showConnectHint) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberBrown.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "✓ Alle Zahlen stimmen – aber die Inseln sind noch nicht alle verbunden!",
                                fontSize = ChipLabel,
                                color = YellowLight,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                lineHeight = 16.sp,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        OutlinedButton(
                            onClick = { zoomResetKey++ },
                            border = BorderStroke(1.dp, TextSub.copy(alpha = 0.5f)),
                        ) { Text("↺", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                val currentState = gs ?: return@OutlinedButton
                                val currentPs = puzzleWithSol ?: return@OutlinedButton
                                val hint = getHashiHint(currentState, currentPs.solution)
                                if (hint != null) {
                                    audio.playSound("hint")
                                    gs = toggleHashiBridge(currentState, hint.first, hint.second)
                                }
                            },
                            border = BorderStroke(1.dp, LimeGreen.copy(alpha = 0.5f)),
                        ) { Icon(Icons.Filled.Lightbulb, contentDescription = "Tipp", tint = LimeGreen, modifier = Modifier.size(18.dp)) }
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
                    Text("Alle Inseln verbunden!", fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${SoloGameSaveManager.formatElapsed(elapsed)}", fontSize = CellNumber, color = LimeGreen, modifier = Modifier.padding(top = 4.dp))
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
        ALL_GAME_RULES["inselbruecke"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showHelp = false; running = true }) }
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
