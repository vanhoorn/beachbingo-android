package com.bestfriends.beachbingo.feature.raetsel.ui

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

    LaunchedEffect(running, showWin) { while (running && gs?.solved == false) { delay(1000L); elapsed++ } }

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
                        Text("STRANDOKU · ${(STRANDOKU_VARIANT_LABELS[variant] ?: variant).uppercase()}",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${difficulty.replaceFirstChar { it.uppercase() }} · ${PuzzleSaveManager.formatElapsed(elapsed)}",
                            style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = { IconButton(onClick = { running = false; showQuit = true }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (p == null || state == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SdAccent) }
            } else {
                val size = p.size
                val boxSize = when (size) { 16 -> 20.dp; 12 -> 26.dp; else -> 36.dp }
                val fontSize = when (size) { 16 -> 9.sp; 12 -> 11.sp; else -> 15.sp }
                val numPad = if (size <= 9) 9 else size

                // Grid
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (r in 0 until size) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            for (c in 0 until size) {
                                val isGiven = p.given[r][c] != 0
                                val isSelected = state.selected == r to c
                                val hasError = state.errors[r][c]
                                val val_ = state.board[r][c]
                                val notes = state.notes[r][c]
                                val bgCol = when {
                                    isSelected -> SdAccent.copy(alpha = 0.25f)
                                    hasError -> Danger.copy(alpha = 0.15f)
                                    else -> SurfaceDark
                                }
                                Box(
                                    modifier = Modifier
                                        .size(boxSize)
                                        .background(bgCol, RoundedCornerShape(2.dp))
                                        .border(1.dp, if (isSelected) SdAccent else BorderColor, RoundedCornerShape(2.dp))
                                        .clickable(enabled = !isGiven) { gs = selectStrandokuCell(state, r, c) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (val_ != 0) {
                                        Text(
                                            if (val_ > 9) ('A' + val_ - 10).toString() else val_.toString(),
                                            fontSize = fontSize,
                                            fontWeight = if (isGiven) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (hasError) Danger else if (isGiven) TextPrimary else SdAccent,
                                            textAlign = TextAlign.Center,
                                        )
                                    } else if (notes.isNotEmpty() && !isGiven) {
                                        Text(notes.sorted().joinToString(""), fontSize = (fontSize.value * 0.5f).sp,
                                            color = TextMuted, textAlign = TextAlign.Center, lineHeight = (fontSize.value * 0.6f).sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Note mode toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Notizen", fontSize = 13.sp, color = TextMuted)
                    Switch(checked = state.noteMode, onCheckedChange = { gs = state.copy(noteMode = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SdAccent, checkedTrackColor = SdAccent.copy(alpha = 0.4f)))
                }

                // Number pad
                val nums = (1..numPad).toList()
                nums.chunked(9).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { n ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Surface2Dark,
                                modifier = Modifier.size(36.dp).clickable { gs = enterStrandokuNumber(state, n) }
                            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (n > 9) ('A' + n - 10).toString() else n.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            } }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { gs = eraseStrandokuCell(state) }, border = androidx.compose.foundation.BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f))) {
                        Text("⌫", color = TextSub, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = {
                        val hint = getStrandokuHint(state)
                        if (hint != null) {
                            gs = enterStrandokuNumber(selectStrandokuCell(state, hint.first, hint.second), p.solution[hint.first][hint.second])
                        }
                    }, border = androidx.compose.foundation.BorderStroke(1.dp, SdAccent.copy(alpha = 0.5f))) {
                        Text("💡", color = SdAccent, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { running = !running }, border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlue.copy(alpha = 0.5f))) {
                        Text(if (running) "⏸" else "▶", color = OceanBlue, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { running = false; showQuit = true }, border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.5f))) {
                        Text("✕", color = Danger, fontWeight = FontWeight.Bold)
                    }
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
                    Text("Sudoku gelöst!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("Zeit: ${PuzzleSaveManager.formatElapsed(elapsed)}", fontSize = 14.sp, color = SdAccent, modifier = Modifier.padding(top = 4.dp))
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
