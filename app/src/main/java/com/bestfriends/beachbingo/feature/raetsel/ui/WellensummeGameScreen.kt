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

private val WsAccent = Color(0xFFC084FC)
private val KakuroBg = Color(0xFF111827)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellensummeGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<KakuroPuzzle?>(null) }
    var gs by remember { mutableStateOf<KakuroState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val saveIdRef = remember { saveId ?: PuzzleSaveManager.generateId() }

    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateKakuro(difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeKakuroState(p, savedState) else createKakuroState(p)
        elapsed = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        running = true
    }

    LaunchedEffect(running, showWin) { while (running && gs?.solved == false) { delay(1000L); elapsed++ } }

    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            PuzzleSaveManager.recordBestTime(context, "wellensumme", "standard", difficulty, elapsed)
            PuzzleSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }

    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        PuzzleSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "wellensumme", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeKakuroState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val p = puzzle; val state = gs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WELLENSUMME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
            val gridSize = p?.size ?: 8
            // Reserve space: numpad row + controls row + spacers
            val controlsH = 130f
            val availForGrid = (maxHeight.value - controlsH - 24f).coerceAtLeast(150f)
            val availW = (maxWidth.value - 24f).coerceAtLeast(150f)
            val cellDp = (minOf(availW, availForGrid) / gridSize).coerceIn(28f, 72f).dp

            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (p == null || state == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WsAccent)
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
                            color = Color(0xFF0A1929),
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
                                                        .background(KakuroBg, RoundedCornerShape(2.dp))
                                                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp)),
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
                                                            if (isSelected) WsAccent.copy(alpha = 0.25f)
                                                            else if (hasErr) Danger.copy(alpha = 0.15f)
                                                            else SurfaceDark,
                                                            RoundedCornerShape(2.dp),
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) WsAccent else if (hasErr) Danger else BorderColor,
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
                                    Text(n.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                if (hint != null) gs = enterKakuroNumber(selectKakuroCell(state, hint.first, hint.second), p.cells[hint.first][hint.second].solution ?: 0)
                            },
                            border = BorderStroke(1.dp, WsAccent.copy(alpha = 0.5f)),
                        ) { Text("💡 Hinweis", color = WsAccent, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = false; showHelp = true },
                            border = BorderStroke(1.dp, TextSub.copy(alpha = 0.5f)),
                        ) { Text("? Regeln", color = TextSub, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = !running },
                            border = BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f)),
                        ) { Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { running = false; showQuit = true },
                            border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        ) { Text("✕", color = Danger, fontWeight = FontWeight.Bold) }
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
                    Text("Alle Summen stimmen!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}", fontSize = 14.sp, color = WsAccent, modifier = Modifier.padding(top = 4.dp))
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
                        "🌊 Kakuro",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                    Text(
                        "Wellensumme — Regeln",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WsAccent,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔢 Fülle alle weißen Zellen mit Zahlen von 1–9.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Text("➕ Jeder Zahlenblock muss genau die angegebene Summe ergeben — die schwarzen Felder zeigen sie an (oben = vertikal, unten = horizontal).", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Text("🚫 Innerhalb eines Blocks darf jede Zahl nur einmal vorkommen.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                        Text("Tippe eine Zelle → dann eine Zahl (1–9). ⌫ löscht die Zelle.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showHelp = false; running = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WsAccent),
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
