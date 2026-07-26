package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

private val DsAccent = Color(0xFFFBBF24)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuenenschattenGameScreen(
    difficulty: String,
    seed: Long,
    saveId: String?,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf<HitoriPuzzle?>(null) }
    var gs by remember { mutableStateOf<HitoriState?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var showWin by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    val saveIdRef = remember { saveId ?: PuzzleSaveManager.generateId() }

    // Generate puzzle in background
    LaunchedEffect(seed) {
        val p = withContext(Dispatchers.Default) { generateHitori(difficulty, seed.toInt()) }
        puzzle = p
        val savedState = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.puzzleState else null
        gs = if (savedState != null) deserializeHitoriState(p, savedState) else createHitoriState(p)
        val savedElapsed = if (saveId != null) PuzzleSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0 else 0
        elapsed = savedElapsed
        running = true
    }

    // Timer
    LaunchedEffect(running, showWin) {
        while (running && gs?.solved == false) {
            delay(1000L)
            elapsed++
        }
    }

    // Win detection
    LaunchedEffect(gs?.solved) {
        if (gs?.solved == true && !showWin) {
            running = false
            val best = PuzzleSaveManager.getBestTime(context, "duenenschatten", "standard", difficulty)
            PuzzleSaveManager.recordBestTime(context, "duenenschatten", "standard", difficulty, elapsed)
            PuzzleSaveManager.deleteSave(context, saveIdRef)
            showWin = true
        }
    }

    // Auto-save
    LaunchedEffect(gs) {
        val state = gs ?: return@LaunchedEffect
        if (state.solved || showWin) return@LaunchedEffect
        val p = puzzle ?: return@LaunchedEffect
        PuzzleSaveManager.savePuzzle(context, PuzzleSave(
            id = saveIdRef, gameType = "duenenschatten", variant = "standard",
            difficulty = difficulty, seed = seed, puzzleState = serializeHitoriState(state),
            startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
        ))
    }

    val p = puzzle
    val state = gs

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DÜNENSC­HATTEN", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${difficulty.replaceFirstChar { it.uppercase() }} · ${PuzzleSaveManager.formatElapsed(elapsed)}",
                            style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { running = false; showQuit = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextSub)
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
            if (p == null || state == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DsAccent)
                }
            } else {
                val conflicts = computeConflicts(state)
                val size = p.size
                val cellDp: Dp = ((350 - size * 2) / size).coerceIn(28, 52).dp

                // Grid
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (r in 0 until size) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (c in 0 until size) {
                                val mark = state.marks[r][c]
                                val isBlack = mark == CellMark.BLACK
                                val isDot = mark == CellMark.DOT
                                val isConflict = (r to c) in conflicts.adjacentBlacks || (r to c) in conflicts.duplicateWhites
                                val bgColor = when {
                                    isBlack -> Color(0xFF111827)
                                    isConflict -> Danger.copy(alpha = 0.15f)
                                    else -> SurfaceDark
                                }
                                val borderCol = when {
                                    isConflict -> Danger
                                    else -> BorderColor
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellDp)
                                        .background(bgColor, RoundedCornerShape(4.dp))
                                        .border(1.dp, borderCol, RoundedCornerShape(4.dp))
                                        .pointerInput(r, c) {
                                            detectTapGestures(
                                                onTap = { gs = toggleMark(state, r, c) },
                                                onLongPress = {
                                                    val newMark = if (state.marks[r][c] == CellMark.DOT) CellMark.WHITE else CellMark.DOT
                                                    gs = setMark(state, r, c, newMark)
                                                }
                                            )
                                        },
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
                                                .background(DsAccent, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Tippen = schwarz/Punkt · Lang drücken = Punkt", fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val hint = getHitoriHint(state)
                            if (hint != null) {
                                val correct = if (p.solution[hint.first][hint.second]) CellMark.BLACK else CellMark.WHITE
                                gs = setMark(state, hint.first, hint.second, correct)
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, DsAccent.copy(alpha = 0.5f)),
                    ) { Text("💡 Hinweis", color = DsAccent, fontWeight = FontWeight.Bold) }

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
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Rätsel gelöst!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}", fontSize = 14.sp, color = DsAccent, modifier = Modifier.padding(top = 4.dp))
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
