package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.bestfriends.beachbingo.ui.components.GameHudBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KEYBOARD_ROWS = listOf(
    listOf("Q","W","E","R","T","Z","U","I","O","P"),
    listOf("A","S","D","F","G","H","J","K","L"),
    listOf("Y","X","C","V","B","N","M"),
)

@Composable
fun WortWelleGameScreen(
    difficulty: String,
    isDaily: Boolean,
    dailyWord: String?,
    dateStr: String?,
    saveId: String?,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val cfg = WW_CONFIG[difficulty] ?: WW_CONFIG["mittel"]!!

    // ── Initialzustand laden ────────────────────────────────────────────────
    val init = remember {
        when {
            isDaily && !dailyWord.isNullOrEmpty() -> WwInitState(dailyWord, emptyList(), "", "playing", 0)
            saveId != null -> {
                val save = SoloGameSaveManager.getSaves(context).find { it.id == saveId }
                if (save != null) {
                    val s = deserializeWwState(save.puzzleState)
                    s.copy(elapsedSeconds = save.elapsedSeconds)
                } else WwInitState(getWwRandomWord(difficulty), emptyList(), "", "playing", 0)
            }
            else -> WwInitState(getWwRandomWord(difficulty), emptyList(), "", "playing", 0)
        }
    }

    val targetWord = remember { init.targetWord }
    var guesses    by remember { mutableStateOf(init.guesses) }
    var cells      by remember {
        mutableStateOf(List(cfg.wordLength) { i ->
            init.currentInput.getOrNull(i)?.let { if (it.isLetter()) it.toString() else "" } ?: ""
        })
    }
    var cursorPos  by remember {
        val f = (0 until cfg.wordLength).firstOrNull { i ->
            init.currentInput.getOrNull(i)?.isLetter() != true
        } ?: (cfg.wordLength - 1)
        mutableIntStateOf(f)
    }
    var gameStatus by remember { mutableStateOf(init.gameStatus) }
    var elapsed    by remember { mutableIntStateOf(init.elapsedSeconds) }
    var running    by remember { mutableStateOf(true) }
    var paused     by remember { mutableStateOf(false) }

    var showResult  by remember { mutableStateOf(false) }
    var showQuit    by remember { mutableStateOf(false) }
    var showRules   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var shakeRow    by remember { mutableIntStateOf(-1) }
    val shakeX      = remember { Animatable(0f) }
    val resultSaved = remember { mutableStateOf(false) }
    val saveIdRef   = remember { if (!isDaily) saveId ?: SoloGameSaveManager.generateId() else "" }

    // ── Flip-Animation ───────────────────────────────────────────────────────
    val preRevealedRows = remember { (0 until init.guesses.size).toSet() }
    val flipScales = remember { List(cfg.maxGuesses) { List(cfg.wordLength) { Animatable(1f) } } }
    var revealedSet by remember {
        mutableStateOf(
            preRevealedRows.flatMap { row ->
                (0 until cfg.wordLength).map { col -> row * 100 + col }
            }.toSet()
        )
    }

    val bestTime = remember { SoloGameSaveManager.getBestTimeAny(context, "wortwelle", difficulty) }
    val keyStatuses = remember(guesses) { computeWwKeyStatuses(guesses, targetWord) }

    // ── Timer ───────────────────────────────────────────────────────────────
    LaunchedEffect(running, showResult, paused) {
        while (running && !paused && gameStatus == "playing") { delay(1000L); elapsed++ }
    }

    // ── Spielende erkennen ───────────────────────────────────────────────────
    LaunchedEffect(gameStatus) {
        if (resultSaved.value) return@LaunchedEffect
        if (gameStatus == "won" || gameStatus == "lost") {
            resultSaved.value = true
            running = false
            if (gameStatus == "won") SoloGameSaveManager.recordBestTime(context, "wortwelle", difficulty, difficulty, elapsed)
            recordWwResult(context, difficulty, gameStatus == "won", guesses.size, isDaily, dateStr)
            if (!isDaily && saveIdRef.isNotEmpty()) SoloGameSaveManager.deleteSave(context, saveIdRef)
            delay(cfg.wordLength * 150L + 200L)
            showResult = true
        }
    }

    // ── Flip-Animation nach Submit ────────────────────────────────────────────
    LaunchedEffect(guesses.size) {
        val row = guesses.size - 1
        if (row < 0 || row in preRevealedRows) return@LaunchedEffect
        coroutineScope {
            for (col in 0 until cfg.wordLength) {
                launch {
                    delay(col * 150L)
                    flipScales[row][col].animateTo(0f, tween(150))
                    revealedSet = revealedSet + (row * 100 + col)
                    flipScales[row][col].animateTo(1f, tween(150))
                }
            }
        }
    }

    // ── Shake-Animation ──────────────────────────────────────────────────────
    LaunchedEffect(shakeRow) {
        if (shakeRow < 0) return@LaunchedEffect
        repeat(3) {
            shakeX.animateTo(9f,  animationSpec = tween(55, easing = LinearEasing))
            shakeX.animateTo(-9f, animationSpec = tween(55, easing = LinearEasing))
        }
        shakeX.animateTo(0f, animationSpec = tween(55))
        shakeRow = -1
    }

    // ── Fehlernachricht ausblenden ────────────────────────────────────────────
    LaunchedEffect(errorMsg) {
        if (errorMsg != null) { delay(2000L); errorMsg = null }
    }

    // ── Auto-Save (Zufallsmodus, bei jedem Raten) ────────────────────────────
    LaunchedEffect(guesses) {
        if (!isDaily && gameStatus == "playing" && saveIdRef.isNotEmpty()) {
            SoloGameSaveManager.savePuzzle(context, PuzzleSave(
                id = saveIdRef, gameType = "wortwelle", variant = "random",
                difficulty = difficulty, seed = 0L,
                puzzleState = serializeWwState(targetWord, guesses, cells.joinToString(""), gameStatus),
                startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
            ))
        }
    }

    // ── Tastatureingabe verarbeiten ───────────────────────────────────────────
    fun handleKey(key: String) {
        if (gameStatus != "playing") return
        when (key) {
            "←" -> {
                if (cells[cursorPos].isNotEmpty()) {
                    cells = cells.toMutableList().also { it[cursorPos] = "" }
                } else if (cursorPos > 0) {
                    val newPos = cursorPos - 1
                    cells = cells.toMutableList().also { it[newPos] = "" }
                    cursorPos = newPos
                }
            }
            "↵" -> {
                if (cells.any { it.isEmpty() }) {
                    errorMsg = "Bitte ${cfg.wordLength} Buchstaben eingeben."
                    shakeRow = guesses.size
                    return
                }
                val input = cells.joinToString("")
                if (!isValidWwGuess(input, difficulty)) {
                    errorMsg = "Unbekanntes Wort!"
                    shakeRow = guesses.size
                    return
                }
                if (cfg.hardMode) {
                    val err = validateWwHardMode(input, guesses, targetWord)
                    if (err != null) {
                        errorMsg = err
                        shakeRow = guesses.size
                        return
                    }
                }
                val newGuesses = guesses + input
                guesses = newGuesses
                cells = List(cfg.wordLength) { "" }
                cursorPos = 0
                if (newGuesses.last() == targetWord) gameStatus = "won"
                else if (newGuesses.size >= cfg.maxGuesses) gameStatus = "lost"
            }
            else -> {
                cells = cells.toMutableList().also { it[cursorPos] = key }
                if (cursorPos < cfg.wordLength - 1) cursorPos++
            }
        }
    }

    Scaffold(
        topBar = {
            GameHudBar(
                paused = !running,
                onPauseToggle = { running = !running },
                onQuit = { running = false; showQuit = true },
                onShowRules = { running = false; showRules = true },
            ) {
                Column {
                    Text("WORTWELLE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    val bestLabel = if (bestTime != null) "  ⏱ ${SoloGameSaveManager.formatElapsed(bestTime)}" else ""
                    Text(
                        "${cfg.label}${if (isDaily) " · Tageswort" else ""}  ·  ${SoloGameSaveManager.formatElapsed(elapsed)}$bestLabel",
                        style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        },
        containerColor = BgDark,
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val availW = maxWidth.value
            val availH = maxHeight.value

            val keyboardH   = 226f
            val controlsH   = 52f
            val errorH      = 30f
            val gridPad     = 16f
            val gridGapDp   = 5f
            val gridAvailW  = (availW - gridPad * 2 - gridGapDp * (cfg.wordLength - 1)).coerceAtLeast(80f)
            val gridAvailH  = (availH - keyboardH - controlsH - errorH - gridPad * 2 - gridGapDp * (cfg.maxGuesses - 1)).coerceAtLeast(80f)
            val cellByW     = gridAvailW / cfg.wordLength
            val cellByH     = gridAvailH / cfg.maxGuesses
            val cellSize    = minOf(cellByW, cellByH).coerceIn(36f, 72f)
            val cellDp      = cellSize.dp
            val fontSize    = (cellSize * 0.42f).sp

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = gridPad.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {

                // ── Gitter ───────────────────────────────────────────────────
                Column(
                    verticalArrangement = Arrangement.spacedBy(gridGapDp.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    for (row in 0 until cfg.maxGuesses) {
                        val isCurrentRow = row == guesses.size
                        val guess = guesses.getOrNull(row)
                        val statuses = if (guess != null) computeWwStatuses(guess, targetWord) else null
                        val offsetX = if (row == shakeRow) shakeX.value else 0f

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gridGapDp.dp),
                            modifier = Modifier.offset { IntOffset((offsetX * density).toInt(), 0) },
                        ) {
                            for (col in 0 until cfg.wordLength) {
                                val cellStr = if (isCurrentRow) cells.getOrElse(col) { "" } else ""
                                val isRevealed = (row * 100 + col) in revealedSet
                                val char: Char? = when {
                                    guess != null -> guess.getOrNull(col)
                                    isCurrentRow  -> cellStr.firstOrNull()
                                    else          -> null
                                }
                                val status: WwLetterStatus = when {
                                    guess != null && isRevealed -> statuses?.getOrElse(col) { WwLetterStatus.ABSENT } ?: WwLetterStatus.ABSENT
                                    guess != null && !isRevealed -> WwLetterStatus.TYPING
                                    isCurrentRow && cellStr.isNotEmpty() -> WwLetterStatus.TYPING
                                    else -> WwLetterStatus.EMPTY
                                }
                                val bg = when (status) {
                                    WwLetterStatus.CORRECT -> Success
                                    WwLetterStatus.PRESENT -> WwPresent
                                    WwLetterStatus.ABSENT  -> WwAbsent
                                    else -> Color.Transparent
                                }
                                val isCursorCell = isCurrentRow && col == cursorPos && gameStatus == "playing"
                                val borderColor = when {
                                    isCursorCell -> CyanBright
                                    status == WwLetterStatus.TYPING -> CyanBright
                                    status == WwLetterStatus.EMPTY  -> BorderColor
                                    else -> Color.Transparent
                                }
                                val flipScale = flipScales[row][col].value
                                Box(
                                    modifier = Modifier
                                        .size(cellDp)
                                        .scale(scaleX = 1f, scaleY = flipScale)
                                        .background(bg, RoundedCornerShape(6.dp))
                                        .border(2.dp, borderColor, RoundedCornerShape(6.dp))
                                        .then(
                                            if (isCurrentRow && gameStatus == "playing")
                                                Modifier.clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                ) { cursorPos = col }
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (char != null) {
                                        Text(
                                            char.toString(), fontSize = fontSize,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (status == WwLetterStatus.EMPTY || status == WwLetterStatus.TYPING) TextPrimary else Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Fehlermeldung ─────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().height(errorH.dp), contentAlignment = Alignment.Center) {
                    if (errorMsg != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = WwAbsentBg) {
                            Text(errorMsg ?: "", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextPrimary, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), textAlign = TextAlign.Center)
                        }
                    }
                }

                // ── Controls-Leiste ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(controlsH.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isDaily) {
                        OutlinedButton(
                            onClick = {
                                if (saveIdRef.isNotEmpty()) {
                                    SoloGameSaveManager.savePuzzle(context, PuzzleSave(
                                        id = saveIdRef, gameType = "wortwelle", variant = "random",
                                        difficulty = difficulty, seed = 0L,
                                        puzzleState = serializeWwState(targetWord, guesses, cells.joinToString(""), gameStatus),
                                        startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
                                    ))
                                }
                                onNavigateBack()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, OceanBlue.copy(alpha = 0.33f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = OceanBlue.copy(alpha = 0.13f),
                                contentColor = OceanBlue,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text("💾", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold) }
                    }
                    if (gameStatus == "playing") {
                        OutlinedButton(
                            onClick = { paused = !paused },
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.33f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CyanBright.copy(alpha = 0.13f),
                                contentColor = CyanBright,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text(if (paused) "▶" else "⏸", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold) }
                    }
                    OutlinedButton(
                        onClick = { running = false; showRules = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextSub.copy(alpha = 0.33f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TextSub.copy(alpha = 0.13f),
                            contentColor = TextSub,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) { Text("?", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { running = false; showQuit = true },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.33f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Danger.copy(alpha = 0.13f),
                            contentColor = Danger,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) { Text("✕", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold) }
                }

                // ── QWERTZ-Tastatur ───────────────────────────────────────────
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                ) {
                    KEYBOARD_ROWS.forEach { rowKeys ->
                        val availKeyW = (availW - gridPad * 2 - 4f * (rowKeys.size - 1)) / rowKeys.size.toFloat()
                        val keyH = 50.dp

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowKeys.forEach { key ->
                                val keyStatus = keyStatuses[key.firstOrNull()]
                                val keyBg = when (keyStatus) {
                                    WwLetterStatus.CORRECT -> Success
                                    WwLetterStatus.PRESENT -> WwPresent
                                    WwLetterStatus.ABSENT  -> WwAbsentBg
                                    else -> Surface2Dark
                                }
                                val keyTextColor = when (keyStatus) {
                                    WwLetterStatus.CORRECT, WwLetterStatus.PRESENT, WwLetterStatus.ABSENT -> Color.White
                                    else -> TextPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .width(availKeyW.dp)
                                        .height(keyH)
                                        .background(keyBg, RoundedCornerShape(8.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { handleKey(key) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = keyTextColor,
                                    )
                                }
                            }
                        }
                    }
                    // Aktionsleiste
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .height(52.dp)
                                .background(Danger.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { handleKey("←") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕  Löschen", fontSize = CellNumber, fontWeight = FontWeight.Bold, color = Danger)
                        }
                        Box(
                            modifier = Modifier
                                .weight(3f)
                                .height(52.dp)
                                .background(Success.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, Success.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { handleKey("↵") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✓  Bestätigen", fontSize = CellNumber, fontWeight = FontWeight.Bold, color = Success)
                        }
                    }
                }
            }
        }
    }

    // ── Ergebnis-Dialog ───────────────────────────────────────────────────────
    if (showResult) {
        val finalStats = remember(showResult) { getWwStats(context, difficulty) }
        val winPct = if (finalStats.played > 0) finalStats.won * 100 / finalStats.played else 0
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (gameStatus == "won") "🎉" else "😔", fontSize = DrawNumberTablet)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (gameStatus == "won") "Glückwunsch!" else "Verloren!",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Das Wort war:", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        targetWord.forEach { ch ->
                            Box(
                                modifier = Modifier.size(36.dp).background(Success, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text(ch.toString(), fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${SoloGameSaveManager.formatElapsed(elapsed)}  ·  $winPct% Gewonnen  ·  Streak: ${finalStats.currentStreak}",
                        fontSize = MaterialTheme.typography.labelMedium.fontSize, color = CyanBright, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (isDaily && dateStr != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Tageswort vom $dateStr", fontSize = MaterialTheme.typography.labelSmall.fontSize, color = TextMuted)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBright),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text("Zurück zur Lobby", fontWeight = FontWeight.Bold, color = BgDark) }
                }
            }
        }
    }

    // ── Regeln-Dialog ─────────────────────────────────────────────────────────
    if (showRules) {
        ALL_GAME_RULES["wortwelle"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false; running = true }) }
    }

    // ── Beenden-Dialog ────────────────────────────────────────────────────────
    if (showQuit) {
        GameSaveQuitDialog(
            emoji = "🏖️",
            hideSave = isDaily,
            onContinue = { running = true; showQuit = false },
            onSaveAndQuit = {
                if (saveIdRef.isNotEmpty()) {
                    SoloGameSaveManager.savePuzzle(context, PuzzleSave(
                        id = saveIdRef, gameType = "wortwelle", variant = "random",
                        difficulty = difficulty, seed = 0L,
                        puzzleState = serializeWwState(targetWord, guesses, cells.joinToString(""), gameStatus),
                        startedAt = System.currentTimeMillis(), elapsedSeconds = elapsed,
                    ))
                }
                onNavigateBack()
            },
            onQuitWithoutSave = {
                if (!isDaily && saveIdRef.isNotEmpty()) SoloGameSaveManager.deleteSave(context, saveIdRef)
                onNavigateBack()
            },
        )
    }
}
