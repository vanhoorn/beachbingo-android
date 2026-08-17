package com.bestfriends.beachbingo.feature.perlentaucher.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.perlentaucher.*
import com.bestfriends.beachbingo.feature.raetsel.GameSave
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Composable
fun PerlentaucherGameScreen(
    level: Int,
    saveId: String?,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    onNavigateBack: () -> Unit,
    onNavigateToGame: (level: Int, saveId: String?) -> Unit = { _, _ -> onNavigateBack() },
    onNavigateToResults: (level: Int, score: Int, movesLeft: Int, bestScore: Int, newBestScore: Boolean) -> Unit = { _, _, _, _, _ -> onNavigateBack() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun vibrate(ms: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    val config = remember(level) { PerlentaucherLevelGenerator.generate(level) }
    val saveIdRef = remember { saveId ?: SoloGameSaveManager.generateId() }

    // ── Initial state from save or fresh ─────────────────────────────────────
    data class InitState(val boardArr: IntArray?, val score: Int, val movesLeft: Int)
    val init = remember(saveId) {
        if (saveId != null) {
            val save = SoloGameSaveManager.getGameSave(context, "perlentaucher")
            if (save != null) {
                try {
                    val json = JSONObject(save.gameState)
                    val arr = json.getJSONArray("board")
                    InitState(
                        boardArr = IntArray(arr.length()) { arr.getInt(it) },
                        score = json.getInt("score"),
                        movesLeft = json.getInt("movesLeft"),
                    )
                } catch (_: Exception) { InitState(null, 0, config.movesLeft) }
            } else InitState(null, 0, config.movesLeft)
        } else InitState(null, 0, config.movesLeft)
    }

    val boardModel = remember {
        PerlentaucherBoardModel(config.seed).also { m ->
            init.boardArr?.let { m.loadFromIntArray(it) }
        }
    }

    var score by remember { mutableIntStateOf(init.score) }
    var movesLeft by remember { mutableIntStateOf(init.movesLeft) }
    var boardTick by remember { mutableIntStateOf(0) }
    var boardSnapshot by remember { mutableStateOf(snapshotBoard(boardModel)) }
    var fallenCols by remember { mutableStateOf(emptyMap<Int, Int>()) }
    var swapAnimData by remember { mutableStateOf<SwapAnimData?>(null) }
    var explosionCells by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }

    var paused by remember { mutableStateOf(false) }
    var showQuit by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var gameLost by remember { mutableStateOf(false) }
    var resultsBestScore by remember { mutableIntStateOf(0) }
    var resultsNewBest by remember { mutableStateOf(false) }
    var resultsMovesLeft by remember { mutableIntStateOf(0) }

    val shakeAnim = remember { Animatable(0f) }

    val audio = remember { PerlentaucherAudioManager() }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.startMusic(soundEnabled, musicEnabled) }

    BackHandler { if (!gameWon && !gameLost) { paused = true; showQuit = true } }

    fun autoSave() {
        if (gameWon || gameLost) return
        val boardArr = boardModel.boardToIntArray()
        val jsonArr = JSONArray().apply { boardArr.forEach { put(it) } }
        val stateJson = JSONObject().apply {
            put("levelNumber", level)
            put("score", score)
            put("movesLeft", movesLeft)
            put("board", jsonArr)
        }.toString()
        SoloGameSaveManager.saveGame(context, GameSave(
            id = saveIdRef,
            gameType = "perlentaucher",
            difficulty = "level_$level",
            gameState = stateJson,
            displayLabel = "Level $level · Score: $score · Züge: $movesLeft",
            savedAt = System.currentTimeMillis(),
        ))
    }

    // ── Game loop ─────────────────────────────────────────────────────────────
    LaunchedEffect(boardTick) {
        boardSnapshot = snapshotBoard(boardModel)

        when (boardModel.phase) {
            BoardPhase.MATCHING -> {
                delay(80)
                val result = boardModel.applyMatches()

                result.specialGenCells.forEach { (pos, st) ->
                    val (r, c) = pos
                    val piece = boardModel.board[r][c]
                    if (piece != null) boardModel.placeSpecial(r, c, piece.type, st)
                }

                if (result.clearedCells.isNotEmpty()) {
                    val hasSpecial = result.specialGenCells.isNotEmpty()
                    val hasBig = result.matches.any { it.cells.size >= 4 }
                    vibrate(if (hasSpecial) 120L else if (hasBig) 80L else 40L)
                    explosionCells = result.clearedCells.toList()

                    if (boardModel.score - score > 0 && result.matches.size > 1) {
                        scope.launch {
                            shakeAnim.snapTo(6f)
                            shakeAnim.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 400f))
                        }
                    }
                    audio.playSound(if (hasBig || hasSpecial) "match4" else "match3")
                    score = boardModel.score
                    delay(450)
                    explosionCells = emptyList()
                } else {
                    audio.playCascade(0)
                    score = boardModel.score
                }
                boardTick++
            }

            BoardPhase.FALLING -> {
                val prevSnap = snapshotBoard(boardModel)
                val changed = boardModel.applyGravity()
                if (changed) {
                    val cols = (0 until BOARD_SIZE).mapNotNull { c ->
                        val dist = (0 until BOARD_SIZE).count { r -> boardModel.board[r][c] != prevSnap[r][c] }
                        if (dist > 0) c to dist else null
                    }.toMap()
                    val maxRows = cols.values.maxOrNull() ?: 1
                    val animMs = (80L + maxRows * 60L).coerceIn(140L, 440L)
                    fallenCols = cols
                    delay(animMs)
                    fallenCols = emptyMap()
                }
                boardTick++
            }

            BoardPhase.FILLING -> {
                boardModel.fillBoard()
                boardTick++
            }

            BoardPhase.CHECK_DEADLOCK -> {
                boardModel.checkDeadlock()
                boardTick++
            }

            BoardPhase.SHUFFLE -> {
                audio.playSound("shuffle")
                boardModel.shuffle()
                delay(280)
                boardTick++
            }

            BoardPhase.IDLE -> {
                boardSnapshot = snapshotBoard(boardModel)
                when {
                    score >= config.targetScore -> {
                        val oldBest = SoloGameSaveManager.getBestPerlentaucherScore(context, level) ?: 0
                        SoloGameSaveManager.saveBestPerlentaucherScore(context, level, score)
                        resultsNewBest = score > oldBest
                        resultsBestScore = SoloGameSaveManager.getBestPerlentaucherScore(context, level) ?: score
                        resultsMovesLeft = movesLeft
                        gameWon = true
                        SoloGameSaveManager.deleteGameSave(context, "perlentaucher")
                        SoloGameSaveManager.saveHighestPerlentaucherLevel(context, level + 1)
                        audio.playSound("win")
                    }
                    movesLeft <= 0 -> {
                        gameLost = true
                        SoloGameSaveManager.deleteGameSave(context, "perlentaucher")
                        audio.playSound("loss")
                    }
                    else -> autoSave()
                }
            }

            BoardPhase.SWAPPING -> { /* transient */ }
        }
    }

    Scaffold(
        topBar = {
            GameHudBar(
                paused = paused,
                onPauseToggle = { paused = !paused },
                onQuit = { paused = true; showQuit = true },
                onShowRules = { showRules = true },
            ) {
                Text("🤿", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Text(
                        "Level $level",
                        fontSize = ChipLabelTiny,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$score Pkt.",
                            fontSize = CellNumber,
                            fontWeight = FontWeight.ExtraBold,
                            color = OceanBlue,
                        )
                        Text(
                            "Ziel: ${config.targetScore}",
                            fontSize = ChipLabelTiny,
                            color = TextMuted,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${movesLeft} Züge",
                            fontSize = ChipLabel,
                            fontWeight = FontWeight.Bold,
                            color = if (movesLeft <= 5) Danger else TextSub,
                        )
                    }
                }
            }
        },
        containerColor = BgDark,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer { translationX = shakeAnim.value },
        ) {
            PerlentaucherBoardView(
                board = boardSnapshot,
                fallenCols = fallenCols,
                targetScore = config.targetScore,
                currentScore = score,
                enabled = !paused && !gameWon && !gameLost && boardModel.phase == BoardPhase.IDLE,
                modifier = Modifier.fillMaxSize(),
                onSwap = { r1, c1, r2, c2 ->
                    if (movesLeft > 0 && boardModel.phase == BoardPhase.IDLE && !paused && !gameWon && !gameLost) {
                        val p1 = boardModel.board[r1][c1]
                        val p2 = boardModel.board[r2][c2]
                        val result = boardModel.trySwap(r1, c1, r2, c2)
                        if (result != null) {
                            swapAnimData = SwapAnimData(Pair(r1, c1), Pair(r2, c2), p1, p2)
                            if (result.clearedCells.isNotEmpty()) explosionCells = result.clearedCells.toList()
                            score = boardModel.score
                            movesLeft--
                            scope.launch {
                                delay(420)
                                swapAnimData = null
                                explosionCells = emptyList()
                                boardTick++
                            }
                        } else {
                            vibrate(30L)
                        }
                    }
                },
                onPieceSelected = { vibrate(15L) },
                swapAnimData = swapAnimData,
                explosionCells = explosionCells,
            )

            // ── Pause overlay ─────────────────────────────────────────────────
            if (paused && !showQuit) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏸", fontSize = EmojiXLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("Pausiert", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { paused = false },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("▶  Weiterspielen", color = BgDark, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }

            // ── Win overlay ───────────────────────────────────────────────────
            if (gameWon) {
                var displayedScore by remember { mutableIntStateOf(0) }
                LaunchedEffect(Unit) { displayedScore = score }
                val animatedScore by animateIntAsState(
                    targetValue = displayedScore,
                    animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    label = "winScore",
                )
                val winParties = remember {
                    val colors = listOf(
                        0xFFF5EFE0.toInt(), 0xFF00BCD4.toInt(), 0xFFE91E8C.toInt(),
                        0xFFFF5722.toInt(), 0xFF4CAF50.toInt(), 0xFF0EA5E9.toInt(), 0xFFFFD700.toInt(),
                    )
                    listOf(
                        Party(
                            speed = 0f, maxSpeed = 35f, damping = 0.9f,
                            angle = 270, spread = 360,
                            colors = colors,
                            emitter = Emitter(duration = 4, TimeUnit.SECONDS).perSecond(50),
                            position = Position.Relative(0.5, 0.0),
                        ),
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🎉", fontSize = EmojiXLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Ziel erreicht!", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "$animatedScore Punkte · Level $level",
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = FontWeight.Bold,
                                color = OceanBlue,
                            )
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    onNavigateToResults(level, score, resultsMovesLeft, resultsBestScore, resultsNewBest)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("Ergebnisse anzeigen", fontWeight = FontWeight.ExtraBold, color = BgDark) }
                        }
                    }
                    // Konfetti on top of the dialog
                    KonfettiView(parties = winParties, modifier = Modifier.fillMaxSize())
                }
            }

            // ── Loss overlay ──────────────────────────────────────────────────
            if (gameLost) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🫧", fontSize = EmojiXLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Keine Züge mehr!", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Erreicht: $score von ${config.targetScore} Punkten",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { onNavigateBack() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("Nochmal versuchen", fontWeight = FontWeight.ExtraBold, color = BgDark) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onNavigateBack() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TextSub.copy(0.4f)),
                            ) { Text("Zurück zur Lobby", color = TextSub) }
                        }
                    }
                }
            }
        }
    }

    if (showQuit) {
        GameSaveQuitDialog(
            emoji = "🤿",
            onContinue = { showQuit = false; paused = false },
            onSaveAndQuit = { autoSave(); onNavigateBack() },
            onQuitWithoutSave = {
                SoloGameSaveManager.deleteGameSave(context, "perlentaucher")
                onNavigateBack()
            },
        )
    }

    ALL_GAME_RULES["perlentaucher"]?.let { rule ->
        if (showRules) GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
    }
}

private fun snapshotBoard(model: PerlentaucherBoardModel): Array<Array<PerlentaucherPiece?>> =
    Array(BOARD_SIZE) { r -> Array(BOARD_SIZE) { c -> model.board[r][c] } }

fun createFreshPerlentaucherSave(context: android.content.Context, level: Int) {
    val config = PerlentaucherLevelGenerator.generate(level)
    val model = PerlentaucherBoardModel(config.seed)
    val boardArr = model.boardToIntArray()
    val jsonArr = JSONArray().apply { boardArr.forEach { put(it) } }
    val stateJson = JSONObject().apply {
        put("levelNumber", level)
        put("score", 0)
        put("movesLeft", config.movesLeft)
        put("board", jsonArr)
    }.toString()
    SoloGameSaveManager.saveGame(context, GameSave(
        id = SoloGameSaveManager.generateId(),
        gameType = "perlentaucher",
        difficulty = "level_$level",
        gameState = stateJson,
        displayLabel = "Level $level · Score: 0 · Züge: ${config.movesLeft}",
        savedAt = System.currentTimeMillis(),
    ))
}
