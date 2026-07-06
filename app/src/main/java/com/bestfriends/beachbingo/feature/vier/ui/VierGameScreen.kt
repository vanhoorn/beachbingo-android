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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.feature.vier.viewmodel.VierGameViewModel
import com.bestfriends.beachbingo.feature.vier.viewmodel.COLS
import com.bestfriends.beachbingo.feature.vier.viewmodel.ROWS
import com.bestfriends.beachbingo.feature.vier.viewmodel.emptyBoard
import com.bestfriends.beachbingo.feature.vier.viewmodel.getAvailableRow
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

private val BoardBg = Color(0xFF0C1F3D)
private val BoardBorder = Color(0xFF1E3A5F)
private val EmptyCellBg = Color(0xFF091525)
private val BeerOrange = Color(0xFFC2410C)

private const val CELL_DP = 44
private const val PIECE_DP = 36

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VierGameScreen(
    mode: String,
    gameId: String?,
    myDrinkId: String,
    aiDrinkId: String?,
    aiDifficulty: String = "SNIPER",
    onNavigateBack: () -> Unit,
    viewModel: VierGameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Initialize game
    LaunchedEffect(mode, gameId) {
        if (mode == "online" && gameId != null) {
            viewModel.initOnline(gameId)
        } else if (mode == "ai") {
            viewModel.initAi(myDrinkId)
        }
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

    fun handleDrop(col: Int) {
        if (gameOver) return
        if (isAiMode) viewModel.dropPieceAi(col, aiDifficulty)
        else if (gameId != null) viewModel.dropPieceOnline(col, gameId)
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
        containerColor = BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    .background(BoardBg, RoundedCornerShape(16.dp))
                    .border(2.dp, BoardBorder, RoundedCornerShape(16.dp))
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
                                    .size(CELL_DP.dp, 40.dp)
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
                                        fontSize = 14.sp,
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
                                        .size(CELL_DP.dp)
                                        .clickable(enabled = !gameOver && myTurn && piece == 0 && getAvailableRow(board, col) == row) {
                                            handleDrop(col)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // Cell background circle (always visible, clipped to circle)
                                    Box(
                                        Modifier
                                            .size(CELL_DP.dp)
                                            .clip(CircleShape)
                                            .background(EmptyCellBg)
                                            .border(2.dp, BoardBorder, CircleShape)
                                    )
                                    // Piece layer — NOT clipped, animates from above
                                    if (drinkId != null) {
                                        key(animKey) {
                                            DroppingPiece(
                                                drinkId = drinkId,
                                                isDropping = isDropping,
                                                dropRow = dropRow,
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
                            Text("🤝", fontSize = 52.sp)
                            Text(
                                "Unentschieden!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                            )
                            Text("Nochmal?", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        } else {
                            Text(if (winnerPlayer == myPiece) "🏆" else "😅", fontSize = 52.sp)
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
    }
}

@Composable
private fun DroppingPiece(
    drinkId: String,
    isDropping: Boolean,
    dropRow: Int,
) {
    if (isDropping && dropRow >= 0) {
        val cellPlusPadding = CELL_DP + 4
        // Distance from above board top (10dp padding + 40dp indicator + 8dp gap) to target cell center
        val boardTopOffset = 10 + 40 + 8
        val totalDrop = boardTopOffset + dropRow * cellPlusPadding + CELL_DP / 2
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
            size = PIECE_DP.dp,
            modifier = Modifier.offset { IntOffset(0, offsetY.value.dp.roundToPx()) },
        )
    } else {
        DrinkPiece(drinkId = drinkId, size = PIECE_DP.dp)
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
    val borderColor = if (isActive || isWinner) drinkColor else Color(0xFF1E3050)

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
                        Text("🏆 Gewonnen", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (isActive && !isWinner)
                        Text("Am Zug", style = MaterialTheme.typography.labelSmall, color = drinkColor, fontWeight = FontWeight.Bold)
                    if (isWinner)
                        Text("🏆 Gewonnen", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                DrinkPiece(drinkId = drinkId, size = 36.dp)
            }
        }
    }
}
