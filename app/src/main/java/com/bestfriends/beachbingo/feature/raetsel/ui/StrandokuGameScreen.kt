package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (p == null || state == null) {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SdAccent)
                }
            } else {
                Spacer(Modifier.height(8.dp))

                // ── Grid ──────────────────────────────────────────────────────
                StrandokuGrid(puzzle = p, state = state, onCellTap = { r, c ->
                    gs = selectStrandokuCell(state, r, c)
                })

                Spacer(Modifier.height(8.dp))

                // ── Note mode ─────────────────────────────────────────────────
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

                // ── Number pad ────────────────────────────────────────────────
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

                // ── Controls ──────────────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    OutlinedButton(
                        onClick = { gs = eraseStrandokuCell(state) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f)),
                    ) { Text("⌫", color = TextSub, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = {
                            val hint = getStrandokuHint(state)
                            if (hint != null) {
                                val s1 = selectStrandokuCell(state, hint.first, hint.second)
                                gs = enterStrandokuNumber(s1.copy(noteMode = false), p.solution[hint.first][hint.second])
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, SdAccent.copy(alpha = 0.5f)),
                    ) { Text("💡", color = SdAccent, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { running = !running },
                        border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f)),
                    ) { Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { running = false; showQuit = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                    ) { Text("✕", color = Danger, fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

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

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { running = true; showQuit = false },
            title = { Text("Spiel beenden?", color = TextPrimary) },
            text = { Text("Fortschritt wird gespeichert.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = onNavigateBack) { Text("Beenden", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { running = true; showQuit = false }) { Text("Weiterspielen", color = TextSub) }
            },
            containerColor = SurfaceDark,
        )
    }
}

// ── Strandoku Grid Composable ──────────────────────────────────────────────────

@Composable
private fun StrandokuGrid(
    puzzle: StrandokuPuzzle,
    state: StrandokuState,
    onCellTap: (Int, Int) -> Unit,
) {
    val size = puzzle.size
    val isSamurai = puzzle.isSamurai

    // Cell size: Samurai needs small cells; mega16 also smaller
    val cellDp: Dp = when {
        isSamurai -> 16.dp
        size == 16 -> 20.dp
        size == 12 -> 26.dp
        else -> 36.dp
    }
    val fontSp = when {
        isSamurai -> 7.sp
        size == 16 -> 9.sp
        size == 12 -> 11.sp
        else -> 15.sp
    }
    val (bw, bh) = getBoxDimensions(if (isSamurai) 9 else size)

    val sel = state.selected

    Column {
        for (r in 0 until size) {
            // Thick separator before box rows (not for samurai/irregular)
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
                    // Thick separator before box cols
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
                        onTap = { onCellTap(r, c) },
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
    onTap: () -> Unit,
) {
    val sol = puzzle.solution[r][c]
    val isInactive = sol == -1

    // Inactive cell (Samurai gap)
    if (isInactive) {
        Box(Modifier.size(cellDp).background(BgDark))
        return
    }

    val isGiven = puzzle.given[r][c] > 0
    val isSelected = sel == (r to c)
    val hasError = state.errors[r][c]
    val valN = state.board[r][c]
    val notes = state.notes[r][c]

    // Determine highlight (same row/col)
    val isHighlighted = sel != null && (r == sel.first || c == sel.second) && !isSelected
    val isSameNum = sel != null && valN != 0 && state.board[sel.first][sel.second] == valN && !isSelected

    // Background color
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
        isSelected -> SdAccent.copy(alpha = 0.28f)
        isSameNum  -> SdAccent.copy(alpha = 0.14f)
        isHighlighted -> Surface2Dark
        else -> baseBg
    }

    // Killer cage sum overlay
    val killerCage = isCageTopLeft(puzzle, r, c)

    Box(
        modifier = Modifier
            .size(cellDp)
            .background(bg)
            .clickable(enabled = !isGiven || true) { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        // Cage sum (Killer)
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
            notes.isNotEmpty() && !isGiven -> Text(
                notes.sorted().joinToString(""),
                fontSize = (fontSp.value * 0.48f).sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = (fontSp.value * 0.55f).sp,
            )
        }
    }
}
