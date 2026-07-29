package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
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
    var showHelp by remember { mutableStateOf(false) }
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
                        Text(
                            "${difficulty.replaceFirstChar { it.uppercase() }} · ${PuzzleSaveManager.formatElapsed(elapsed)}",
                            style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { running = false; showQuit = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Reserve space for button row + spacers
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
                        CircularProgressIndicator(color = IbAccent)
                    }
                } else {
                    val puzzle = ps.puzzle
                    val gridSize = puzzle.gridSize
                    val boxPx = with(LocalDensity.current) { innerCanvasSize.toPx() / gridSize }

                    // Game board with border and slightly lighter background
                    Surface(
                        modifier = Modifier.size(canvasAreaSize),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D1B2E),
                        border = BorderStroke(1.5.dp, BorderColor),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Canvas(
                                modifier = Modifier
                                    .size(innerCanvasSize)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { tapOffset ->
                                                val currentState = gs ?: return@detectTapGestures
                                                val currentPuzzle = puzzleWithSol?.puzzle ?: return@detectTapGestures
                                                val tapX = (tapOffset.x - panOffset.x) / zoomScale
                                                val tapY = (tapOffset.y - panOffset.y) / zoomScale
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
                                            }
                                        )
                                    },
                            ) {
                                withTransform({
                                    translate(panOffset.x, panOffset.y)
                                    scale(scaleX = zoomScale, scaleY = zoomScale, pivot = Offset.Zero)
                                }) {
                                    val dotPaint = Color(0xFF1E3050)
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
                                                Color(0xFF4ADE80),
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
                                        val fillColor = when { done -> IbAccent.copy(alpha = 0.3f); over -> Danger.copy(alpha = 0.3f); else -> Surface2Dark }
                                        val strokeColor = when { isSelected -> IbAccent; done -> IbAccent; over -> Danger; else -> TextSub }
                                        drawCircle(fillColor, radius = boxPx * 0.4f, center = Offset(cx, cy))
                                        drawCircle(strokeColor, radius = boxPx * 0.4f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(if (isSelected) 3f else 2f))
                                        val txt = textMeasurer.measure(isl.value.toString(), TextStyle(fontSize = (boxPx * 0.3f).sp, fontWeight = FontWeight.Bold, color = TextPrimary))
                                        drawText(txt, topLeft = Offset(cx - txt.size.width / 2, cy - txt.size.height / 2))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        OutlinedButton(
                            onClick = { running = !running },
                            border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f)),
                        ) { Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { zoomScale = 1f; panOffset = Offset.Zero },
                            border = BorderStroke(1.dp, TextSub.copy(alpha = 0.5f)),
                        ) { Text("↺", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                val currentState = gs ?: return@OutlinedButton
                                val currentPs = puzzleWithSol ?: return@OutlinedButton
                                val hint = getHashiHint(currentState, currentPs.solution)
                                if (hint != null) gs = toggleHashiBridge(currentState, hint.first, hint.second)
                            },
                            border = BorderStroke(1.dp, IbAccent.copy(alpha = 0.5f)),
                        ) { Text("💡 Hinweis", color = IbAccent, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = false; showHelp = true },
                            border = BorderStroke(1.dp, TextSub.copy(alpha = 0.5f)),
                        ) { Text("?", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = false; showQuit = true },
                            border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        ) { Text("✕ Abbruch", color = Danger, fontWeight = FontWeight.Bold) }
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
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Alle Inseln verbunden!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}", fontSize = 14.sp, color = IbAccent, modifier = Modifier.padding(top = 4.dp))
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
        Dialog(onDismissRequest = { showHelp = false; running = true }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "🌉 Hashiwokakero",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                    Text(
                        "Inselbrücke — Regeln",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IbAccent,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🏝️ Verbinde alle Inseln mit Brücken, sodass jede Insel genau so viele Brücken hat wie ihre Zahl anzeigt.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Text("🔀 Brücken verlaufen nur horizontal oder vertikal und dürfen sich nicht kreuzen.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Text("2️⃣ Zwischen zwei Inseln sind maximal 2 Brücken erlaubt.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Text("🔗 Am Ende müssen alle Inseln miteinander verbunden sein.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                        Text("Tippe eine Insel → dann eine zweite = 1 Brücke. Nochmals = 2 Brücken. Dreimal = entfernen.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showHelp = false; running = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IbAccent),
                    ) { Text("Verstanden!", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }

    // ── Quit dialog (3 options, same as web) ───────────────────────────────────
    if (showQuit) {
        Dialog(onDismissRequest = { running = true; showQuit = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🏖️", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Spiel beenden?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { running = true; showQuit = false },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, TextSub.copy(alpha = 0.4f)),
                        ) { Text("Weiterspielen", color = TextSub, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        ) { Text("💾 Speichern & Beenden", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { PuzzleSaveManager.deleteSave(context, saveIdRef); onNavigateBack() },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        ) { Text("✕ Beenden ohne Speichern", color = Danger, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
