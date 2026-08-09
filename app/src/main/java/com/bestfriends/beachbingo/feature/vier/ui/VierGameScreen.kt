package com.bestfriends.beachbingo.feature.vier.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.feature.raetsel.GameSave
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.feature.vier.viewmodel.VierGameViewModel
import com.bestfriends.beachbingo.feature.vier.viewmodel.COLS
import com.bestfriends.beachbingo.feature.vier.viewmodel.ROWS
import com.bestfriends.beachbingo.feature.vier.viewmodel.emptyBoard
import com.bestfriends.beachbingo.feature.vier.viewmodel.getAvailableRow
import com.bestfriends.beachbingo.ui.theme.BeerOrange
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BoardBlueDark
import com.bestfriends.beachbingo.ui.theme.BoardBlueDeep
import com.bestfriends.beachbingo.ui.theme.BoardBlueMid
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.CellNumber
import com.bestfriends.beachbingo.ui.theme.ChipLabel
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.EmojiHuge
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.StatusTiny
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt


private val CELL_DP_MIN = 36.dp
private val CELL_DP_MAX = 120.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VierGameScreen(
    mode: String,
    gameId: String?,
    myDrinkId: String,
    aiDrinkId: String?,
    aiDifficulty: String = "SNIPER",
    saveId: String? = null,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    onNavigateBack: () -> Unit,
    viewModel: VierGameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val context = LocalContext.current

    // Initialize game
    LaunchedEffect(mode, gameId) {
        if (mode == "online" && gameId != null) {
            viewModel.initOnline(gameId)
        } else if (mode == "ai") {
            viewModel.initAi(myDrinkId)
        }
    }

    // Restore from save (AI mode only)
    LaunchedEffect(saveId) {
        if (saveId == null || mode != "ai") return@LaunchedEffect
        val save = SoloGameSaveManager.getGameSave(context, "vier") ?: return@LaunchedEffect
        try {
            val obj = JSONObject(save.gameState)
            val arr = obj.getJSONArray("board")
            val board = (0 until arr.length()).map { arr.getInt(it) }
            val currentPlayer = obj.getInt("currentPlayer")
            viewModel.loadSave(board, currentPlayer, aiDifficulty)
        } catch (_: Exception) {}
    }

    // Derived display values
    val board: List<Int>
    val winCells: List<Int>
    val winnerPlayer: Int?   // 1 or 2
    val draw: Boolean
    val myPiece: Int
    val opponentDrinkId: String
    val myTurn: Boolean
    val aiThinking: Boolean
    val isAiMode = mode == "ai"

    if (isAiMode) {
        board = uiState.board
        winCells = uiState.winCells
        winnerPlayer = uiState.winner
        draw = uiState.draw
        myPiece = 1
        opponentDrinkId = aiDrinkId ?: "whisky"
        myTurn = uiState.currentPlayer == 1
        aiThinking = uiState.aiThinking
    } else {
        board = uiState.board
        winCells = emptyList()
        winnerPlayer = if (uiState.onlineWinnerId != null) {
            val idx = uiState.onlinePlayers.indexOfFirst { it["userId"] == uiState.onlineWinnerId }
            if (idx >= 0) idx + 1 else null
        } else null
        draw = uiState.onlineIsDraw
        myPiece = (uiState.onlinePlayers.indexOfFirst { it["userId"] == uid }.takeIf { it >= 0 } ?: 0) + 1
        opponentDrinkId = uiState.onlinePlayers.firstOrNull { it["userId"] != uid }?.get("drinkId") ?: "whisky"
        myTurn = uiState.onlineCurrentTurn == uid
        aiThinking = false
    }

    val gameOver = winnerPlayer != null || draw

    var manualPaused by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val vierUid = auth.currentUser?.uid

    val audio = remember { VierAudioManager() }
    var musicStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        audio.soundEnabled = soundEnabled
        audio.musicEnabled = musicEnabled
        audio.startMusic()
        musicStarted = true
    }
    DisposableEffect(Unit) {
        onDispose { audio.release() }
    }
    LaunchedEffect(manualPaused) {
        if (!musicStarted) return@LaunchedEffect
        if (manualPaused) audio.stopMusic() else audio.startMusic()
    }
    LaunchedEffect(gameOver) {
        if (!musicStarted || !gameOver) return@LaunchedEffect
        audio.stopMusic()
        if (draw) audio.playSound("draw") else audio.playSound("win")
    }
    // Play piece_drop for every drop (human AND AI) by watching dropAnimKey.
    LaunchedEffect(uiState.dropAnimKey) {
        if (!musicStarted || uiState.dropAnimKey == 0) return@LaunchedEffect
        audio.playSound("piece_drop")
    }

    fun handleDrop(col: Int) {
        if (gameOver) return
        if (isAiMode) viewModel.dropPieceAi(col, aiDifficulty)
        else if (gameId != null) viewModel.dropPieceOnline(col, gameId)
    }

    if (showRules) {
        ALL_GAME_RULES["vier"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }

    if (showQuitDialog) {
        if (isAiMode && !gameOver) {
            GameSaveQuitDialog(
                emoji = "🍺",
                message = "KI-Partie · ${uiState.board.count { it != 0 }} Steine gesetzt",
                onContinue = { showQuitDialog = false; manualPaused = false },
                onSaveAndQuit = {
                    val boardJson = JSONArray(uiState.board.map { it.toLong() })
                    SoloGameSaveManager.saveGame(
                        context,
                        GameSave(
                            id = java.util.UUID.randomUUID().toString(),
                            gameType = "vier",
                            difficulty = aiDifficulty,
                            gameState = JSONObject()
                                .put("board", boardJson)
                                .put("currentPlayer", uiState.currentPlayer)
                                .put("myDrinkId", myDrinkId)
                                .put("aiDrinkId", aiDrinkId ?: "whisky")
                                .toString(),
                            displayLabel = "KI · ${uiState.board.count { it != 0 }} Steine",
                            savedAt = System.currentTimeMillis(),
                        )
                    )
                    onNavigateBack()
                },
                onQuitWithoutSave = {
                    SoloGameSaveManager.deleteGameSave(context, "vier")
                    onNavigateBack()
                },
            )
        } else {
            QuitConfirmDialog(
                emoji = "🍺",
                message = "Das laufende Spiel wird beendet.",
                onConfirm = { onNavigateBack() },
                onDismiss = { showQuitDialog = false; manualPaused = false },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isAiMode) "vs KI" else "Code: $gameId",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        bottomBar = {
            GameHudBar(
                paused = manualPaused,
                onPauseToggle = { manualPaused = !manualPaused },
                onQuit = { manualPaused = true; showQuitDialog = true },
                onShowRules = { showRules = true },
            ) {
                val turnLabel = when { gameOver -> "Fertig"; myTurn -> "Du bist dran"; else -> "Gegner denkt..." }
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(turnLabel, fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (isAiMode) "vs KI" else "Online", fontSize = StatusTiny, color = TextMuted)
                }
            }
        },
        containerColor = BgDark,
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Cell size from width: col padding 24dp, board padding 20dp, gaps (COLS-1)*4dp
            val totalGaps = ((COLS - 1) * 4).dp
            val cellFromWidth = (maxWidth - 24.dp - 20.dp - totalGaps) / COLS
            // Cell size from height: ~130dp for UI chrome (player bars, status, spacers),
            // board overhead 68dp (padding 20dp + arrow row 40dp + gap 8dp), row gaps (ROWS-1)*4dp
            val rowGaps = ((ROWS - 1) * 4).dp
            val cellFromHeight = (maxHeight - 130.dp - 68.dp - rowGaps) / ROWS
            val cellDp = minOf(cellFromWidth, cellFromHeight).coerceIn(CELL_DP_MIN, CELL_DP_MAX)
            val pieceDp = (cellDp.value * 0.82f).dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Player indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlayerBar(
                    drinkId = myDrinkId,
                    label = "Du",
                    isActive = !gameOver && myTurn,
                    isWinner = winnerPlayer == myPiece,
                    drinkColor = getDrink(myDrinkId).color,
                    modifier = Modifier.weight(1f),
                )
                Text("vs", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                PlayerBar(
                    drinkId = opponentDrinkId,
                    label = if (isAiMode) "KI" else "Gegner",
                    isActive = !gameOver && !myTurn,
                    isWinner = winnerPlayer != null && winnerPlayer != myPiece,
                    drinkColor = getDrink(opponentDrinkId).color,
                    modifier = Modifier.weight(1f),
                    flip = true,
                )
            }

            // Board — no clip so pieces can fall in from above
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp), clip = false)
                    .background(BoardBlueDark, RoundedCornerShape(16.dp))
                    .border(2.dp, BoardBlueMid, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Column tap indicators — large, clearly tappable
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        for (col in 0 until COLS) {
                            val available = getAvailableRow(board, col) != -1
                            val canDrop = !gameOver && myTurn && available
                            Box(
                                modifier = Modifier
                                    .size(cellDp, 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (canDrop) getDrink(myDrinkId).color.copy(alpha = 0.18f)
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (canDrop) Modifier.border(1.dp, getDrink(myDrinkId).color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        else Modifier
                                    )
                                    .clickable(enabled = canDrop) { handleDrop(col) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (canDrop) {
                                    Text(
                                        text = "▼",
                                        fontSize = CellNumber,
                                        color = getDrink(myDrinkId).color,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    // Grid rows
                    for (row in 0 until ROWS) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = if (row < ROWS - 1) Modifier.padding(bottom = 4.dp) else Modifier,
                        ) {
                            for (col in 0 until COLS) {
                                val cellIdx = row * COLS + col
                                val piece = board.getOrElse(cellIdx) { 0 }
                                val isWinCell = winCells.contains(cellIdx)
                                val drinkId = when (piece) {
                                    1 -> if (myPiece == 1) myDrinkId else opponentDrinkId
                                    2 -> if (myPiece == 2) myDrinkId else opponentDrinkId
                                    else -> null
                                }

                                val isDropping = uiState.lastDroppedCell == cellIdx
                                val dropRow = uiState.lastDroppedRow
                                val animKey = if (isDropping) uiState.dropAnimKey else cellIdx

                                // Outer Box: no clip, so piece can overflow upward during animation
                                Box(
                                    modifier = Modifier
                                        .size(cellDp)
                                        .clickable(enabled = !gameOver && myTurn && piece == 0 && getAvailableRow(board, col) == row) {
                                            handleDrop(col)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // Cell background circle (always visible, clipped to circle)
                                    Box(
                                        Modifier
                                            .size(cellDp)
                                            .clip(CircleShape)
                                            .background(BoardBlueDeep)
                                            .border(2.dp, BoardBlueMid, CircleShape)
                                    )
                                    // Piece layer — NOT clipped, animates from above
                                    if (drinkId != null) {
                                        key(animKey) {
                                            DroppingPiece(
                                                drinkId = drinkId,
                                                isDropping = isDropping,
                                                dropRow = dropRow,
                                                cellDp = cellDp,
                                                pieceDp = pieceDp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status text
            if (!gameOver) {
                Text(
                    text = when {
                        aiThinking -> "🤖 KI denkt nach…"
                        myTurn -> "Dein Zug — wähle eine Spalte"
                        else -> "Gegner ist dran…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }

            // Game Over panel
            if (gameOver) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (draw) {
                            Text("🤝", fontSize = EmojiHuge)
                            Text(
                                "Unentschieden!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                            )
                            Text("Nochmal?", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        } else {
                            Text(if (winnerPlayer == myPiece) "🏆" else "😅", fontSize = EmojiHuge)
                            Text(
                                text = when {
                                    winnerPlayer == myPiece -> "Du gewinnst!"
                                    isAiMode -> "KI gewinnt!"
                                    else -> "Gegner gewinnt!"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                            )
                            val winDrinkId = if (winnerPlayer == myPiece) myDrinkId else opponentDrinkId
                            DrinkPiece(drinkId = winDrinkId, size = 56.dp)
                            Text(
                                text = if (winnerPlayer == myPiece) "Prost! 🍺" else "Beim nächsten Mal!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (isAiMode) {
                                Button(
                                    onClick = { viewModel.restartAi() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BeerOrange),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Nochmal spielen")
                                }
                            }
                            OutlinedButton(
                                onClick = onNavigateBack,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            ) {
                                Text("Zur Lobby")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
        } // BoxWithConstraints
    }
}

@Composable
private fun DroppingPiece(
    drinkId: String,
    isDropping: Boolean,
    dropRow: Int,
    cellDp: Dp,
    pieceDp: Dp,
) {
    if (isDropping && dropRow >= 0) {
        val cellInt = cellDp.value.toInt()
        val cellPlusPadding = cellInt + 4
        // Distance from above board top (10dp padding + 40dp indicator + 8dp gap) to target cell center
        val boardTopOffset = 10 + 40 + 8
        val totalDrop = boardTopOffset + dropRow * cellPlusPadding + cellInt / 2
        val offsetY = remember { Animatable(-totalDrop.toFloat()) }
        val duration = (120 + dropRow * 55).coerceAtLeast(200)

        LaunchedEffect(Unit) {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = duration
                    // Land at 80%
                    0f at (duration * 0.80f).toInt() with LinearEasing
                    // Bounce up slightly at 90%
                    -7f at (duration * 0.90f).toInt() with LinearEasing
                    // Settle down at 96%
                    3f at (duration * 0.96f).toInt() with LinearEasing
                    // Final rest
                    0f at duration with LinearEasing
                },
            )
        }

        DrinkPiece(
            drinkId = drinkId,
            size = pieceDp,
            modifier = Modifier.offset { IntOffset(0, offsetY.value.dp.roundToPx()) },
        )
    } else {
        DrinkPiece(drinkId = drinkId, size = pieceDp)
    }
}

@Composable
private fun PlayerBar(
    drinkId: String,
    label: String,
    isActive: Boolean,
    isWinner: Boolean,
    drinkColor: Color,
    modifier: Modifier = Modifier,
    flip: Boolean = false,
) {
    val bgColor = when {
        isWinner -> drinkColor.copy(alpha = 0.15f)
        isActive -> drinkColor.copy(alpha = 0.08f)
        else -> SurfaceDark
    }
    val borderColor = if (isActive || isWinner) drinkColor else BorderColor

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (flip) Arrangement.End else Arrangement.Start,
        ) {
            if (!flip) {
                DrinkPiece(drinkId = drinkId, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (isActive && !isWinner)
                        Text("Am Zug", style = MaterialTheme.typography.labelSmall, color = drinkColor, fontWeight = FontWeight.Bold)
                    if (isWinner)
                        Text("🏆 Gewonnen", style = MaterialTheme.typography.labelSmall, color = SandGold, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (isActive && !isWinner)
                        Text("Am Zug", style = MaterialTheme.typography.labelSmall, color = drinkColor, fontWeight = FontWeight.Bold)
                    if (isWinner)
                        Text("🏆 Gewonnen", style = MaterialTheme.typography.labelSmall, color = SandGold, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                DrinkPiece(drinkId = drinkId, size = 36.dp)
            }
        }
    }
}
