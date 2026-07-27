package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val IbAccent = Color(0xFF4ADE80)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InselbrueckeGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var puzzleWithSol by remember { mutableStateOf<HashiPuzzleWithSolution?>(null) }
    var gs by remember { mutableStateOf<HashiState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var selectedIslandId by remember { mutableStateOf<Int?>(null) }
    var zoomScale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val saveIdRef = remember { saveId ?: PuzzleSaveManager.generateId() }

    LaunchedEffect(seed) {
        val ps = withContext(Dispatchers.Default) { generateHashiWithSolution(difficulty, seed.toInt()) }
        puzzleWithSol = ps
        val savedState = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeHashiState(ps.puzzle, ps.solution, savedState) else createHashiState(ps.puzzle)
        elapsed = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) {
        while (running && gs?.solved == false) { delay(1000L); elapsed++ }
    }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            PuzzleSaveManager.recordBestTime(context, "inselbruecke", "standard", difficulty, elapsed)
            PuzzleSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        val ps = puzzleWithSol ?: return@LaunchedEffect
        PuzzleSaveManager.savePuzzle(context, PuzzleSave(
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
            TopAppBar(
                title = {
                    Column {
                        Text("INSELBRÜCKE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${difficulty.replaceFirstChar { it.uppercase() }} · ${PuzzleSaveManager.formatElapsed(elapsed)}",
                            style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { running = false; showQuit = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ps == null || state == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = IbAccent) }
            } else {
                val puzzle = ps.puzzle
                val gridSize = puzzle.gridSize
                val boxPx = with(LocalDensity.current) { 350.dp.toPx() / gridSize }
                val canvasSize = 350.dp

                Canvas(
                    modifier = Modifier
                        .size(canvasSize)
                        .pointerInput(Unit) {
                            val canvasSizePx = size.width.toFloat()
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                val minPan = canvasSizePx * (1f - newScale)
                                zoomScale = newScale
                                panOffset = Offset(
                                    (panOffset.x + pan.x).coerceIn(minPan, 0f),
                                    (panOffset.y + pan.y).coerceIn(minPan, 0f),
                                )
                            }
                        }
                        .pointerInput(state) {
                            detectTapGestures(
                                onDoubleTap = {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                onTap = { rawOffset ->
                                    val col = ((rawOffset.x - panOffset.x) / zoomScale / boxPx).toInt()
                                    val row = ((rawOffset.y - panOffset.y) / zoomScale / boxPx).toInt()
                                    val tapped = puzzle.islands.find { it.row == row && it.col == col }
                                    if (tapped != null) {
                                        val sel = selectedIslandId
                                        if (sel == null || sel == tapped.id) {
                                            selectedIslandId = if (sel == tapped.id) null else tapped.id
                                        } else {
                                            val neighbors = getNeighborIslands(puzzle, puzzle.islands.find { it.id == sel }!!, state.bridges)
                                            if (neighbors.any { it.id == tapped.id }) {
                                                gs = toggleHashiBridge(state, sel, tapped.id)
                                            }
                                            selectedIslandId = null
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    withTransform({
                        translate(panOffset.x, panOffset.y)
                        scale(scaleX = zoomScale, scaleY = zoomScale, pivot = Offset.Zero)
                    }) {
                        // Grid dots
                        val dotPaint = Color(0xFF1E3050)
                        for (r in 0 until gridSize) for (c in 0 until gridSize) {
                            drawCircle(dotPaint, radius = 2f, center = Offset(c * boxPx + boxPx/2, r * boxPx + boxPx/2))
                        }

                        // Bridges
                        for (b in state.bridges) {
                            val a = puzzle.islands.find { it.id == b.from } ?: continue
                            val bb = puzzle.islands.find { it.id == b.to } ?: continue
                            val x1 = a.col * boxPx + boxPx/2; val y1 = a.row * boxPx + boxPx/2
                            val x2 = bb.col * boxPx + boxPx/2; val y2 = bb.row * boxPx + boxPx/2
                            val bridgeOffset = if (b.count == 2) 4f else 0f
                            val isHoriz = a.row == bb.row
                            for (i in 0 until b.count) {
                                val o = if (b.count == 1) 0f else (if (i == 0) -bridgeOffset else bridgeOffset)
                                drawLine(
                                    Color(0xFF4ADE80),
                                    start = if (isHoriz) Offset(x1, y1+o) else Offset(x1+o, y1),
                                    end = if (isHoriz) Offset(x2, y2+o) else Offset(x2+o, y2),
                                    strokeWidth = 3f
                                )
                            }
                        }

                        // Islands
                        for (isl in puzzle.islands) {
                            val cx = isl.col * boxPx + boxPx/2; val cy = isl.row * boxPx + boxPx/2
                            val sum = islandBridgeSum(isl, state.bridges)
                            val done = sum == isl.value
                            val over = sum > isl.value
                            val isSelected = selectedIslandId == isl.id
                            val fillColor = when { done -> IbAccent.copy(alpha = 0.3f); over -> Danger.copy(alpha = 0.3f); else -> Surface2Dark }
                            val strokeColor = when { isSelected -> IbAccent; done -> IbAccent; over -> Danger; else -> TextSub }
                            drawCircle(fillColor, radius = boxPx * 0.4f, center = Offset(cx, cy))
                            drawCircle(strokeColor, radius = boxPx * 0.4f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(if (isSelected) 3f else 2f))
                            val txt = textMeasurer.measure(isl.value.toString(), TextStyle(fontSize = (boxPx * 0.3f).sp, fontWeight = FontWeight.Bold, color = TextPrimary))
                            drawText(txt, topLeft = Offset(cx - txt.size.width/2, cy - txt.size.height/2))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val hint = getHashiHint(state, ps.solution)
                            if (hint != null) gs = toggleHashiBridge(state, hint.first, hint.second)
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, IbAccent.copy(alpha = 0.5f)),
                    ) { Text("💡 Hinweis", color = IbAccent, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { running = !running },
                        border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f)),
                    ) { Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { running = false; showQuit = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                    ) { Text("✕", color = Danger, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showWin) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Alle Inseln verbunden!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}", fontSize = 14.sp, color = IbAccent, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)) {
                        Text("Zurück zur Lobby", fontWeight = FontWeight.Bold, color = BgDark)
                    }
                }
            }
        }
    }

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { running = true; showQuit = false },
            title = { Text("Spiel beenden?", color = TextPrimary) },
            text = { Text("Fortschritt wird gespeichert.", color = TextMuted) },
            confirmButton = { TextButton(onClick = onNavigateBack) { Text("Beenden", color = Danger, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { running = true; showQuit = false }) { Text("Weiterspielen", color = TextSub) } },
            containerColor = SurfaceDark,
        )
    }
}
