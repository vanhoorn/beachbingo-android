package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val SdAccent = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrandokuGameScreen(
    variant: String,
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<StrandokuPuzzle?>(null) }
    var gs by remember { mutableStateOf<StrandokuState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val saveIdRef = remember { saveId ?: PuzzleSaveManager.generateId() }

    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateStrandoku(variant, difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeStrandokuState(p, savedState) else createStrandokuState(p)
        elapsed = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) {
        while (running && gs?.solved == false) { delay(1000L); elapsed++ }
    }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            PuzzleSaveManager.recordBestTime(context, "strandoku", variant, difficulty, elapsed)
            PuzzleSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        PuzzleSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "strandoku", variant = variant,
            difficulty = difficulty, seed = seed, puzzleState = serializeStrandokuState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val p = puzzle; val state = gs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "STRANDOKU · ${(STRANDOKU_VARIANT_LABELS[variant] ?: variant).uppercase()}",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted,
                        )
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
            val cellDp = (minOf(availW, availForGrid) / gridSize).coerceIn(minCellDp, 90f).dp

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (p == null || state == null) {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SdAccent)
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
                                color = Color(0xFF0A1929),
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
                        Text("Notizen", fontSize = 13.sp, color = TextMuted)
                        Switch(
                            checked = state.noteMode,
                            onCheckedChange = { gs = state.copy(noteMode = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SdAccent,
                                checkedTrackColor = SdAccent.copy(alpha = 0.4f),
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
                                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
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
                                    val s1 = selectStrandokuCell(state, hint.first, hint.second)
                                    gs = enterStrandokuNumber(s1.copy(noteMode = false), p.solution[hint.first][hint.second])
                                }
                            },
                            border = BorderStroke(1.dp, SdAccent.copy(alpha = 0.5f)),
                        ) { Text("💡", color = SdAccent, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = false; showHelp = true },
                            border = BorderStroke(1.dp, TextSub.copy(alpha = 0.5f)),
                        ) { Text("?", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = !running },
                            border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f)),
                        ) { Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = false; showQuit = true },
                            border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        ) { Text("✕", color = Danger, fontWeight = FontWeight.Bold) }
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
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Gelöst!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text(
                        "Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}",
                        fontSize = 14.sp, color = SdAccent, modifier = Modifier.padding(top = 4.dp),
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
        val variantLabel = when (variant) {
            "classic" -> "9×9 Classic"
            "mega12" -> "12×12 Mega"
            "mega16" -> "16×16 Mega"
            "diagonal" -> "Diagonal"
            "irregular" -> "Irregular"
            "killer" -> "Killer"
            "samurai" -> "Samurai"
            else -> variant
        }
        Dialog(onDismissRequest = { showHelp = false; running = true }) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "🔢 Strandoku",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                    Text(
                        variantLabel,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SdAccent,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            when (variant) {
                                "mega12"  -> "🔢 Fülle das 12×12-Gitter so, dass jede Zeile, jede Spalte und jedes 3×4-Feld die Zahlen 1–12 (A=10, B=11, C=12) genau einmal enthält."
                                "mega16"  -> "🔢 Fülle das 16×16-Gitter so, dass jede Zeile, jede Spalte und jedes 4×4-Feld die Zahlen 1–16 (A–G für 10–16) genau einmal enthält."
                                "samurai" -> "🔢 Jede Zeile, jede Spalte und jedes 3×3-Feld jedes der fünf Teilgitter enthält die Zahlen 1–9 genau einmal."
                                else      -> "🔢 Fülle das Gitter so, dass jede Zeile, jede Spalte und jedes 3×3-Feld die Zahlen 1–9 genau einmal enthält."
                            },
                            fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp,
                        )
                        if (variant == "killer")    Text("➕ Die Zahlen in jedem Käfig müssen die angegebene Summe ergeben. Eine Zahl darf im selben Käfig nicht zweimal vorkommen.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        if (variant == "diagonal")  Text("↗ Beide Hauptdiagonalen (von Ecke zu Ecke) müssen ebenfalls die Zahlen 1–9 genau einmal enthalten.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        if (variant == "irregular") Text("🔷 Statt quadratischer Boxen gibt es unregelmäßig geformte Regionen. Jede farbige Region muss die Zahlen 1–9 genau einmal enthalten.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        if (variant == "samurai")   Text("🏯 Das Mittelgitter überschneidet sich mit jedem der vier Eckgitter über ein gemeinsames 3×3-Feld. Alle fünf Teilgitter müssen je für sich ein gültiges Sudoku ergeben.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                        Text("Tippe eine Zelle → dann eine Zahl auf dem Zahlenpad. Aktiviere den Notiz-Schalter, um Kandidaten einzutragen. ⌫ löscht die Eingabe.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showHelp = false; running = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SdAccent),
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
            if (r == c || r + c == size - 1) SdAccent.copy(alpha = 0.08f) else SurfaceDark
        }
        else -> SurfaceDark
    }

    val bg = when {
        isSelected   -> SdAccent.copy(alpha = 0.28f)
        isSameNum    -> SdAccent.copy(alpha = 0.14f)
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
                color = TextMuted,
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
                    else     -> SdAccent
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
