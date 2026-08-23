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
fun StrandokuGameScreen(
    variant: String,
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<StrandokuPuzzle?>(null) }
    var gs by remember { mutableStateOf<StrandokuState?>(null) }
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
        val p = withContext(Dispatchers.Default) { generateStrandoku(variant, difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeStrandokuState(p, savedState) else createStrandokuState(p)
        elapsed = if (saveId != null) SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) {
        while (running && gs?.solved == false) { delay(1000L); elapsed++ }
    }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            SoloGameSaveManager.recordBestTime(context, "strandoku", variant, difficulty, elapsed)
            SoloGameSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }
    LaunchedEffect(showWin) { if (showWin) audio.playSound("win") }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        SoloGameSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "strandoku", variant = variant,
            difficulty = difficulty, seed = seed, puzzleState = serializeStrandokuState(state),
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
                    Text(
                        "STRANDOKU · ${(STRANDOKU_VARIANT_LABELS[variant] ?: variant).uppercase()}",
                        style = MaterialTheme.typography.labelSmall, color = TextMuted,
                    )
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
            val isSam = p?.isSamurai ?: false
            val gridSize = p?.size ?: 9
            val numPadRows = if (gridSize > 9) 2 else 1

            // Estimate controls height: note-switch + numpad rows + controls row + spacers
            val controlsH = (60f + numPadRows * 44f + 48f + 40f).coerceAtMost(maxHeight.value * 0.38f)
            val availForGrid = (maxHeight.value - controlsH - 32f).coerceAtLeast(150f)
            val availW = (maxWidth.value - 24f).coerceAtLeast(150f)

            val minCellDp = when {
                isSam -> 16f
                gridSize == 16 -> 22f
                gridSize == 12 -> 28f
                else -> 32f
            }
            // Use full available width as primary dimension; limit by height so controls fit
            val cellDp = (minOf(availW, availForGrid) / gridSize).coerceIn(minCellDp, 110f).dp

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (p == null || state == null) {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SkyBlue)
                    }
                } else {
                    Spacer(Modifier.height(8.dp))

                    val density = LocalDensity.current
                    val cellPx = with(density) { cellDp.toPx() }
                    val gapPx = with(density) { 1.dp.toPx() }
                    val boxGapPx = with(density) { 2.dp.toPx() }
                    val (rowStarts, colStarts) = remember(p, cellPx) {
                        computeStrandokuOffsets(p, cellPx, gapPx, boxGapPx)
                    }

                    // ── Grid with border + background ──────────────────────────
                    // Outer Box forces full-width constraint so the grid is always centered
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ZoomableGrid(
                            onTap = tap@{ gx, gy ->
                                val currentState = gs ?: return@tap
                                val rows = rowStarts
                                val cols = colStarts
                                if (rows.isEmpty() || cols.isEmpty()) return@tap
                                val gridH = rows.last() + cellPx
                                val gridW = cols.last() + cellPx
                                if (gy < 0f || gy > gridH || gx < 0f || gx > gridW) return@tap
                                val row = rows.indexOfLast { it <= gy }.coerceIn(0, rows.size - 1)
                                val col = cols.indexOfLast { it <= gx }.coerceIn(0, cols.size - 1)
                                gs = selectStrandokuCell(currentState, row, col)
                            },
                        ) {
                            Surface(
                                color = BgNavyCell,
                                border = BorderStroke(2.dp, TextMuted.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(4.dp),
                            ) {
                                StrandokuGrid(puzzle = p, state = state, cellDp = cellDp)
                            }
                        }
                    } // end centering Box

                    Spacer(Modifier.height(8.dp))

                    // ── Note mode ──────────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text("Notizen", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Switch(
                            checked = state.noteMode,
                            onCheckedChange = { gs = state.copy(noteMode = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SkyBlue,
                                checkedTrackColor = SkyBlue.copy(alpha = 0.4f),
                            ),
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // ── Number pad ─────────────────────────────────────────────
                    val numPad = when {
                        p.isSamurai || p.size == 9 -> (1..9).toList()
                        p.size == 12 -> (1..12).toList()
                        else -> (1..16).toList()
                    }
                    numPad.chunked(9).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            row.forEach { n ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Surface2Dark,
                                    modifier = Modifier.size(36.dp).clickable { gs = enterStrandokuNumber(state, n) },
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (n > 9) ('A' + n - 10).toString() else n.toString(),
                                            fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Controls ───────────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { gs = eraseStrandokuCell(state) },
                            border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f)),
                        ) { Text("⌫", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                val hint = getStrandokuHint(state)
                                if (hint != null) {
                                    audio.playSound("hint")
                                    val s1 = selectStrandokuCell(state, hint.first, hint.second)
                                    gs = enterStrandokuNumber(s1.copy(noteMode = false), p.solution[hint.first][hint.second])
                                }
                            },
                            border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.5f)),
                        ) { Icon(Icons.Filled.Lightbulb, contentDescription = "Tipp", tint = SkyBlue, modifier = Modifier.size(18.dp)) }
                    }

                    Spacer(Modifier.height(4.dp))
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
                    Text("Gelöst!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(
                        "Zeit: ${SoloGameSaveManager.formatElapsed(elapsed)}",
                        fontSize = CellNumber, color = SkyBlue, modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    ) { Text("Zurück zur Lobby", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }

    // ── Rules dialog ────────────────────────────────────────────────────────────
    if (showHelp) {
        ALL_GAME_RULES["strandoku"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showHelp = false; running = true }) }
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

// ── Strandoku Grid Composable ──────────────────────────────────────────────────

@Composable
private fun StrandokuGrid(
    puzzle: StrandokuPuzzle,
    state: StrandokuState,
    cellDp: Dp,
) {
    val size = puzzle.size
    val isSamurai = puzzle.isSamurai
    val fontSp = (cellDp.value * 0.50f).sp
    val (bw, bh) = getBoxDimensions(if (isSamurai) 9 else size)
    val sel = state.selected

    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        for (r in 0 until size) {
            if (r > 0 && !isSamurai && puzzle.variant !in listOf("irregular", "killer")) {
                val isBoxBoundary = r % bh == 0
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isBoxBoundary) 2.dp else 1.dp)
                        .background(if (isBoxBoundary) TextMuted.copy(alpha = 0.6f) else BorderColor),
                )
            } else if (r > 0) {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
            }

            Row {
                for (c in 0 until size) {
                    if (c > 0 && !isSamurai && puzzle.variant !in listOf("irregular", "killer")) {
                        val isBoxBoundary = c % bw == 0
                        Spacer(
                            Modifier
                                .width(if (isBoxBoundary) 2.dp else 1.dp)
                                .height(cellDp)
                                .background(if (isBoxBoundary) TextMuted.copy(alpha = 0.6f) else BorderColor),
                        )
                    } else if (c > 0) {
                        Spacer(Modifier.width(1.dp).height(cellDp).background(BorderColor))
                    }

                    StrandokuCell(
                        puzzle = puzzle,
                        state = state,
                        r = r, c = c,
                        cellDp = cellDp,
                        fontSp = fontSp,
                        sel = sel,
                    )
                }
            }
        }
    }
}

@Composable
private fun StrandokuCell(
    puzzle: StrandokuPuzzle,
    state: StrandokuState,
    r: Int, c: Int,
    cellDp: Dp,
    fontSp: androidx.compose.ui.unit.TextUnit,
    sel: Pair<Int, Int>?,
) {
    val sol = puzzle.solution[r][c]
    val isInactive = sol == -1

    if (isInactive) {
        Box(Modifier.size(cellDp).background(BgDark))
        return
    }

    val isGiven = puzzle.given[r][c] > 0
    val isSelected = sel == (r to c)
    val hasError = state.errors[r][c]
    val valN = state.board[r][c]
    val notes = state.notes[r][c]

    val isHighlighted = sel != null && (r == sel.first || c == sel.second) && !isSelected
    val isSameNum = sel != null && valN != 0 && state.board[sel.first][sel.second] == valN && !isSelected

    val baseBg: Color = when (puzzle.variant) {
        "irregular" -> {
            val regionId = puzzle.regions!![r][c]
            val argb = REGION_COLORS_ARGB[regionId % REGION_COLORS_ARGB.size]
            Color(((argb and 0xFF0000L shr 16).toInt()), ((argb and 0xFF00L shr 8).toInt()), (argb and 0xFFL).toInt(), 0x40)
        }
        "killer" -> {
            val cageIdx = getCageIndex(puzzle, r, c)
            if (cageIdx >= 0) {
                val argb = CAGE_COLORS_ARGB[cageIdx % CAGE_COLORS_ARGB.size]
                Color(((argb and 0xFF0000L shr 16).toInt()), ((argb and 0xFF00L shr 8).toInt()), (argb and 0xFFL).toInt(), 0x35)
            } else SurfaceDark
        }
        "diagonal" -> {
            val size = puzzle.size
            if (r == c || r + c == size - 1) SkyBlue.copy(alpha = 0.08f) else SurfaceDark
        }
        else -> SurfaceDark
    }

    val bg = when {
        isSelected   -> SkyBlue.copy(alpha = 0.28f)
        isSameNum    -> SkyBlue.copy(alpha = 0.14f)
        isHighlighted -> Surface2Dark
        else         -> baseBg
    }

    val killerCage = isCageTopLeft(puzzle, r, c)

    Box(
        modifier = Modifier
            .size(cellDp)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (killerCage != null) {
            Text(
                killerCage.sum.toString(),
                fontSize = (fontSp.value * 0.52f).sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 1.dp, top = 1.dp),
                lineHeight = fontSp,
            )
        }

        when {
            valN != 0 -> Text(
                if (valN > 9) ('A' + valN - 10).toString() else valN.toString(),
                fontSize = fontSp,
                fontWeight = if (isGiven) FontWeight.ExtraBold else FontWeight.Normal,
                color = when {
                    hasError -> Danger
                    isGiven  -> TextPrimary
                    else     -> SkyBlue
                },
                textAlign = TextAlign.Center,
            )
            notes.isNotEmpty() && !isGiven -> {
                val noteFontSp = (cellDp.value * 0.30f).sp
                if (puzzle.size <= 9) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (subRow in 0..2) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (subCol in 0..2) {
                                    val digit = subRow * 3 + subCol + 1
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                        if (digit in notes) {
                                            Text(
                                                digit.toString(),
                                                fontSize = noteFontSp,
                                                color = TextMuted,
                                                lineHeight = noteFontSp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        notes.sorted().joinToString(""),
                        fontSize = noteFontSp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = (fontSp.value * 0.72f).sp,
                    )
                }
            }
        }
    }
}

// ── Zoom support ───────────────────────────────────────────────────────────────

private fun computeStrandokuOffsets(
    puzzle: StrandokuPuzzle,
    cellPx: Float,
    gapPx: Float,
    boxGapPx: Float,
): Pair<FloatArray, FloatArray> {
    val size = puzzle.size
    val isSamurai = puzzle.isSamurai
    val isIrregularOrKiller = puzzle.variant in listOf("irregular", "killer")
    val (bw, bh) = getBoxDimensions(if (isSamurai) 9 else size)

    val rowStarts = FloatArray(size)
    var y = 0f
    for (r in 0 until size) {
        rowStarts[r] = y
        if (r < size - 1) {
            val spacer = if (!isSamurai && !isIrregularOrKiller && (r + 1) % bh == 0) boxGapPx else gapPx
            y += cellPx + spacer
        }
    }

    val colStarts = FloatArray(size)
    var x = 0f
    for (c in 0 until size) {
        colStarts[c] = x
        if (c < size - 1) {
            val spacer = if (!isSamurai && !isIrregularOrKiller && (c + 1) % bw == 0) boxGapPx else gapPx
            x += cellPx + spacer
        }
    }

    return rowStarts to colStarts
}
