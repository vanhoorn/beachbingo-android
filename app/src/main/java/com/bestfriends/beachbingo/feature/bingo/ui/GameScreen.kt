package com.bestfriends.beachbingo.feature.bingo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.BingoGame
import com.bestfriends.beachbingo.core.model.BingoPlayer
import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.core.model.GameStatus
import com.bestfriends.beachbingo.core.model.User
import com.bestfriends.beachbingo.feature.bingo.ui.components.BingoCardView
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.feature.bingo.viewmodel.BingoViewModel
import com.bestfriends.beachbingo.feature.bingo.viewmodel.TabletUiState
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameId: String,
    onNavigateBack: () -> Unit,
    viewModel: BingoViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }

    val bingoAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val bingoFirestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val bingoUid = bingoAuth.currentUser?.uid

    val game by viewModel.game.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val player2User by viewModel.player2User.collectAsStateWithLifecycle()
    val tabletUiState by viewModel.tabletUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val isAdmin = currentUser?.uid == game.adminId
    val myPlayer = currentUser?.uid?.let { game.players[it] }
    val player2 = player2User?.uid?.let { game.players[it] }
    val winner = game.players.values.firstOrNull { it.hasBingo }

    val audio = remember { BingoAudioManager() }
    var musicStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (bingoUid != null) {
            try {
                val doc = bingoFirestore.collection("users").document(bingoUid).get().await()
                audio.soundEnabled = doc.getBoolean("soundEnabled") ?: true
                audio.musicEnabled = doc.getBoolean("musicEnabled") ?: true
            } catch (_: Exception) {}
        }
        audio.startMusic()
        musicStarted = true
    }
    DisposableEffect(Unit) {
        onDispose { audio.release() }
    }
    LaunchedEffect(game.status) {
        if (!musicStarted) return@LaunchedEffect
        if (game.status == GameStatus.FINISHED) audio.stopMusic()
    }
    LaunchedEffect(game.drawnNumbers.size) {
        if (!musicStarted) return@LaunchedEffect
        if (game.drawnNumbers.isNotEmpty()) audio.playSound("number_drawn")
    }
    LaunchedEffect(game.drawAnimationActive) {
        if (!musicStarted) return@LaunchedEffect
        if (game.drawAnimationActive) audio.playSound("drum_roll")
    }
    LaunchedEffect(uiState.showBingoAnimation) {
        if (!musicStarted) return@LaunchedEffect
        if (uiState.showBingoAnimation) { audio.stopMusic(); audio.playSound("bingo") }
    }
    LaunchedEffect(game.eliminationAnimationActive) {
        if (!musicStarted) return@LaunchedEffect
        if (game.eliminationAnimationActive) audio.playSound("elimination")
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Spiel löschen?") },
            text = { Text("Das Spiel wird für alle Spieler beendet und kann nicht wiederhergestellt werden.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGame()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    if (showQuitDialog) {
        QuitConfirmDialog(
            message = "Du verlässt das Spiel.",
            onConfirm = { viewModel.leaveGame(); onNavigateBack() },
            onDismiss = { showQuitDialog = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("BeachBingo 🏖️") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.leaveGame()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Verlassen")
                        }
                    },
                    actions = {
                        if (isAdmin && game.status == GameStatus.LOBBY) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Spiel löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (game.status == GameStatus.RUNNING) {
                    GameHudBar(
                        paused = false,
                        onPauseToggle = {},
                        onQuit = { showQuitDialog = true },
                    ) {
                        val playerCount = game.players.size
                        val drawnCount = game.drawnNumbers.size
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$playerCount Spieler · $drawnCount Zahlen", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextPrimary)
                            Text("BeachBingo", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when (game.status) {
                GameStatus.LOBBY -> LobbyStateContent(
                    game = game,
                    isAdmin = isAdmin,
                    gameId = gameId,
                    onStartGame = { viewModel.startGame() },
                    isTablet = isTablet,
                    player2User = player2User,
                    tabletUiState = tabletUiState,
                    onPlayer2Login = { email, pw -> viewModel.loginPlayer2(email, pw) },
                    onClearTabletError = { viewModel.clearTabletLoginError() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
                GameStatus.RUNNING -> RunningStateContent(
                    game = game,
                    myPlayer = myPlayer,
                    isAdmin = isAdmin,
                    isDrawing = uiState.isDrawing,
                    isClaimingBingo = uiState.isClaimingBingo,
                    onDrawNumber = { viewModel.drawNumber() },
                    onMarkNumber = { viewModel.markNumber(it) },
                    onClaimBingo = { viewModel.claimBingo() },
                    isTablet = isTablet,
                    player2 = player2,
                    tabletUiState = tabletUiState,
                    onMarkNumberPlayer2 = { viewModel.markNumberPlayer2(it) },
                    onClaimBingoPlayer2 = { viewModel.claimBingoPlayer2() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
                GameStatus.FINISHED -> FinishedStateContent(
                    game = game,
                    onNavigateBack = onNavigateBack,
                    isTablet = isTablet,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }

        // Feuerwerk-Animation auf allen Geräten gleichzeitig
        if (uiState.showBingoAnimation) {
            FireworksOverlay(
                winnerName = winner?.displayName ?: "Jemand",
                winnerAvatar = winner?.avatarUrl ?: "🏆",
                onDismiss = {
                    viewModel.dismissBingoAnimation()
                    if (game.status == GameStatus.FINISHED) onNavigateBack()
                }
            )
        }

        // Boss Level: Teufel-Animation wenn eine Zahl eliminiert wurde
        if (game.eliminationAnimationActive) {
            EliminationOverlay(
                playerName = game.eliminationPlayerName,
                playerAvatar = game.eliminationPlayerAvatar,
                eliminatedNumber = game.eliminationNumber
            )
        }

        // Boss Level: Eliminierungs-Dialog für den ausgewählten Spieler
        if (game.eliminationPendingPlayerId != null &&
            game.eliminationPendingPlayerId == currentUser?.uid &&
            !game.eliminationAnimationActive) {
            EliminationDialog(
                drawnNumbers = game.drawnNumbers,
                onEliminate = { viewModel.eliminateNumber(it) }
            )
        }
    }
}

// ── Fireworks overlay ──────────────────────────────────────────────────────

private data class BurstData(
    val cx: Float, val cy: Float,
    val color: Color,
    val startMs: Long,
    val particles: List<Pair<Float, Float>> // (vx, vy) pairs
)

@Composable
private fun FireworksOverlay(
    winnerName: String,
    winnerAvatar: String,
    onDismiss: () -> Unit
) {
    val fireworkColors = remember {
        listOf(
            Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFE66D),
            Color(0xFF96CEB4), Color(0xFFFF9FF3), Color(0xFF54A0FF),
            Color(0xFFFF9F43), Color(0xFFEE5A24)
        )
    }

    val bursts = remember {
        listOf(0L, 350L, 750L, 1200L, 1800L, 2500L, 3200L).mapIndexed { i, startMs ->
            BurstData(
                cx = 0.1f + Random.nextFloat() * 0.8f,
                cy = 0.08f + Random.nextFloat() * 0.55f,
                color = fireworkColors[i % fireworkColors.size],
                startMs = startMs,
                particles = List(18) { j ->
                    val angle = (j.toDouble() * 2.0 * PI / 18.0).toFloat()
                    val speed = 0.10f + Random.nextFloat() * 0.14f
                    Pair(cos(angle) * speed, sin(angle) * speed - 0.18f)
                }
            )
        }
    }

    var elapsedMs by remember { mutableStateOf(0L) }
    var showDismiss by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        launch {
            delay(2500)
            showDismiss = true
        }
        while (elapsedMs < 5000L) {
            elapsedMs = System.currentTimeMillis() - startTime
            delay(16)
        }
    }

    // Pulsing scale for winner text
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tMs = elapsedMs.toFloat()
            bursts.forEach { burst ->
                val burstAgeMs = tMs - burst.startMs
                if (burstAgeMs < 0f || burstAgeMs > 1800f) return@forEach
                val t = burstAgeMs / 1000f
                val alpha = (1f - burstAgeMs / 1800f).coerceIn(0f, 1f)
                burst.particles.forEach { (vx, vy) ->
                    val gravity = 0.25f * t * t
                    val px = (burst.cx + vx * t) * size.width
                    val py = (burst.cy + vy * t + gravity) * size.height
                    drawCircle(
                        color = burst.color.copy(alpha = alpha),
                        radius = 9f * alpha + 3f,
                        center = Offset(px, py)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = winnerAvatar.ifEmpty { "🏆" },
                fontSize = (72f * scale).sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "BINGO! 🎉",
                fontSize = (32f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFE66D)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = winnerName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "hat gewonnen! 🏖️",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(32.dp))
            AnimatedVisibility(
                visible = showDismiss,
                enter = fadeIn() + scaleIn()
            ) {
                Button(onClick = onDismiss) {
                    Text("Super! 🎉", fontSize = 16.sp)
                }
            }
        }
    }
}

// ── Boss Level: Elimination overlay ───────────────────────────────────────

@Composable
private fun EliminationOverlay(
    playerName: String,
    playerAvatar: String,
    eliminatedNumber: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "devil_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "devil_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC1A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "😈", fontSize = (80f * scale).sp)
            Text(
                text = "${playerAvatar.ifEmpty { "🏄" }} $playerName",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4444),
                textAlign = TextAlign.Center
            )
            Text(
                text = "wirft die",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "$eliminatedNumber",
                fontSize = (56f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFE66D)
            )
            Text(
                text = "zurück in die Lostrommel! 🎒",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Boss Level: Elimination dialog ────────────────────────────────────────

@Composable
private fun EliminationDialog(
    drawnNumbers: List<Int>,
    onEliminate: (Int) -> Unit
) {
    var selectedNumber by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("😈", fontSize = 24.sp)
                Text("Du bist dran!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Wähle eine Zahl, die zurück in die Lostrommel geworfen wird:",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(44.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(drawnNumbers.sorted()) { n ->
                        val isSelected = selectedNumber == n
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.secondaryContainer
                                )
                                .clickable { selectedNumber = n },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = n.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedNumber?.let { onEliminate(it) } },
                enabled = selectedNumber != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Zurückwerfen! 😈")
            }
        }
    )
}

// ── Drum animation (Lostrommel) ────────────────────────────────────────────

@Composable
private fun DrumAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "drum")

    // Äußerer Ring dreht sich
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "drum_rotation"
    )
    // Innerer Ring dreht sich entgegengesetzt
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "drum_inner"
    )
    // Ball hüpft (CSS keyframes nachgebaut)
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 600
                0f    at 0
                -14f  at 180
                4f    at 360
                -6f   at 480
                0f    at 600
            }
        ),
        label = "ball_bounce"
    )
    // Zahl blendet aus/ein
    val numberAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 250
                1f  at 0
                0f  at 100
                0f  at 110
                1f  at 250
            }
        ),
        label = "number_alpha"
    )

    var displayNumber by remember { mutableStateOf(Random.nextInt(1, 76)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            displayNumber = Random.nextInt(1, 76)
        }
    }

    val primary  = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary   // SandGold

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Äußerer drehender Ring mit Segmenten
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension / 2f - 4.dp.toPx()

            // Glow-Ringe
            for (i in 3 downTo 1) {
                drawCircle(
                    color = primary.copy(alpha = 0.06f * i),
                    radius = outerR + i * 4.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Rotierende Segmente
            rotate(rotation, Offset(cx, cy)) {
                val segAngle = 20f
                repeat(18) { i ->
                    drawArc(
                        color = primary.copy(alpha = if (i % 2 == 0) 0.15f else 0f),
                        startAngle = i * segAngle,
                        sweepAngle = segAngle,
                        useCenter = false,
                        topLeft = Offset(cx - outerR, cy - outerR),
                        size = Size(outerR * 2, outerR * 2),
                        style = Stroke(width = outerR * 0.30f)
                    )
                }
                drawCircle(
                    color = primary,
                    radius = outerR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // Innerer Gegenring
            val innerR = outerR - 16.dp.toPx()
            rotate(-rotationInner, Offset(cx, cy)) {
                drawCircle(
                    color = primary.copy(alpha = 0.25f),
                    radius = innerR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Goldener Ball
        Box(
            modifier = Modifier
                .size(112.dp)
                .offset(y = bounceY.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White,
                            0.20f to secondary.copy(alpha = 0.9f),
                            0.55f to secondary,
                            1.00f to Color(0xFFD97706)
                        ),
                        center = Offset(40f, 34f),
                        radius = 120f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayNumber.toString(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0A1628),
                modifier = Modifier.alpha(numberAlpha)
            )
        }
    }
}

// ── Lobby state ────────────────────────────────────────────────────────────

@Composable
private fun LobbyStateContent(
    game: BingoGame,
    isAdmin: Boolean,
    gameId: String,
    onStartGame: () -> Unit,
    isTablet: Boolean = false,
    player2User: User? = null,
    tabletUiState: TabletUiState = TabletUiState(),
    onPlayer2Login: (String, String) -> Unit = { _, _ -> },
    onClearTabletError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isTablet) {
        Row(modifier = modifier) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { LobbyQrCard(gameId = gameId, game = game) }
                item { Text("Spieler (${game.players.size})", style = MaterialTheme.typography.titleMedium) }
                items(game.players.values.toList(), key = { it.userId }) { PlayerRow(it) }
                if (isAdmin) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onStartGame,
                            enabled = game.players.size >= 1,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Spiel starten") }
                    }
                } else {
                    item {
                        Text(
                            "Warten auf den Admin...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            VerticalDivider()
            // Zweiter Spieler Login Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Zweiter Spieler", style = MaterialTheme.typography.titleMedium)
                if (player2User != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(player2User.avatarUrl.ifEmpty { "🏄" }, fontSize = 36.sp)
                            Column {
                                Text(player2User.displayName, style = MaterialTheme.typography.titleSmall)
                                Text("Bereit ✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Player2LoginForm(
                        tabletUiState = tabletUiState,
                        onLogin = onPlayer2Login,
                        onClearError = onClearTabletError
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { LobbyQrCard(gameId = gameId, game = game) }
        item { Text("Spieler (${game.players.size})", style = MaterialTheme.typography.titleMedium) }
        items(game.players.values.toList(), key = { it.userId }) { PlayerRow(it) }
        if (isAdmin) {
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onStartGame,
                    enabled = game.players.size >= 1,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Spiel starten") }
            }
        } else {
            item {
                Text(
                    "Warten auf den Admin...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private const val WEB_APP_BASE_URL = "https://thebeachbingo.netlify.app"

@Composable
private fun LobbyQrCard(gameId: String, game: BingoGame) {
    var selectedTab by remember { mutableStateOf(0) }
    val webUrl = "$WEB_APP_BASE_URL/game/$gameId"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("🤖 Android") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🍎 iPhone") }
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Spiel beitreten", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))
                if (selectedTab == 0) {
                    QrCodeImage(content = gameId, size = 180.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = gameId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "QR-Code scannen oder Code eingeben",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    QrCodeImage(content = webUrl, size = 180.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "QR-Code mit iPhone scannen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Öffnet BeachBingo direkt im Browser",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(4.dp))
                val modeLabel = when (game.gameMode) {
                    GameMode.AUTO_MARK    -> "Level: 1. Rookie"
                    GameMode.MANUAL_MARK  -> "Level: 2. Sniper"
                    GameMode.MINI_BOSS_LEVEL -> "Level: 3. Mini Boss Level 🔵"
                    GameMode.BOSS_LEVEL   -> "Level: 4. Boss Level 😈"
                }
                Text(
                    modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun Player2LoginForm(
    tabletUiState: TabletUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Text(
        "Zweiter Spieler einloggen um mitspielen zu können.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it; onClearError() },
        label = { Text("E-Mail") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; onClearError() },
        label = { Text("Passwort") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    if (tabletUiState.loginError != null) {
        Text(
            tabletUiState.loginError,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    Button(
        onClick = { onLogin(email, password) },
        enabled = !tabletUiState.isLoggingIn && email.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (tabletUiState.isLoggingIn) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Einloggen...")
        } else {
            Text("Einloggen")
        }
    }
}

// ── Running state ──────────────────────────────────────────────────────────

@Composable
private fun RunningStateContent(
    game: BingoGame,
    myPlayer: BingoPlayer?,
    isAdmin: Boolean,
    isDrawing: Boolean,
    isClaimingBingo: Boolean,
    onDrawNumber: () -> Unit,
    onMarkNumber: (Int) -> Unit,
    onClaimBingo: () -> Unit,
    isTablet: Boolean = false,
    player2: BingoPlayer? = null,
    tabletUiState: TabletUiState = TabletUiState(),
    onMarkNumberPlayer2: (Int) -> Unit = {},
    onClaimBingoPlayer2: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isTablet) {
        TabletRunningContent(
            game = game,
            myPlayer = myPlayer,
            player2 = player2,
            isAdmin = isAdmin,
            isDrawing = isDrawing,
            isClaimingBingo = isClaimingBingo,
            isClaimingBingo2 = tabletUiState.isClaimingBingo2,
            onDrawNumber = onDrawNumber,
            onMarkNumber = onMarkNumber,
            onClaimBingo = onClaimBingo,
            onMarkNumberPlayer2 = onMarkNumberPlayer2,
            onClaimBingoPlayer2 = onClaimBingoPlayer2,
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Aktuelle Zahl + Zahl-ziehen-Button in einer kompakten Zeile
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (game.drawAnimationActive) "Zahl wird gezogen..." else "Aktuelle Zahl",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        if (game.drawAnimationActive) {
                            DrumAnimation(modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            AnimatedContent(
                                targetState = game.currentNumber,
                                transitionSpec = {
                                    (fadeIn(tween(300)) + scaleIn(tween(300))) togetherWith fadeOut(tween(150))
                                },
                                label = "number_animation"
                            ) { number ->
                                Text(
                                    text = number?.toString() ?: "–",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    if (isAdmin) {
                        Button(
                            onClick = onDrawNumber,
                            enabled = !isDrawing && !game.drawAnimationActive && !game.eliminationAnimationActive && game.eliminationPendingPlayerId == null && game.drawnNumbers.size < 75,
                            modifier = Modifier.height(52.dp)
                        ) {
                            if (isDrawing || game.drawAnimationActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Ziehen", fontSize = 15.sp)
                                    Text("${game.drawnNumbers.size}/75", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (game.drawnNumbers.isNotEmpty()) {
            item {
                Text("Gezogene Zahlen", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    game.drawnNumbers.reversed().forEach { n ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (n == game.currentNumber) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = n.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (n == game.currentNumber) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        myPlayer?.let { player ->
            item {
                val isAutoMode = game.gameMode == GameMode.AUTO_MARK
                val modeLabel = when (game.gameMode) {
                    GameMode.AUTO_MARK    -> "Deine Karte (automatisch markiert)"
                    GameMode.MANUAL_MARK  -> "Deine Karte (tippe zum Markieren)"
                    GameMode.MINI_BOSS_LEVEL -> "Deine Karte — Mini Boss Level 🔵"
                    GameMode.BOSS_LEVEL   -> "Deine Karte — Boss Level 😈"
                }
                val highlightDrawn = isAutoMode || game.gameMode == GameMode.MINI_BOSS_LEVEL || game.gameMode == GameMode.MANUAL_MARK
                Text(modeLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.height(4.dp))
                BingoCardView(
                    card = player.card,
                    drawnNumbers = game.drawnNumbers,
                    onNumberClick = onMarkNumber,
                    interactive = !isAutoMode,
                    autoMarkWithDrawn = isAutoMode,
                    highlightDrawn = highlightDrawn
                )
            }

            // BINGO!-Button für AUTO_MARK-Modus
            if (game.gameMode == GameMode.AUTO_MARK) {
                item {
                    Button(
                        onClick = onClaimBingo,
                        enabled = !isClaimingBingo && game.drawnNumbers.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        if (isClaimingBingo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Prüfe...", fontSize = 18.sp)
                            }
                        } else {
                            Text("BINGO! 🎉", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Tablet running state ───────────────────────────────────────────────────

@Composable
private fun TabletRunningContent(
    game: BingoGame,
    myPlayer: BingoPlayer?,
    player2: BingoPlayer?,
    isAdmin: Boolean,
    isDrawing: Boolean,
    isClaimingBingo: Boolean,
    isClaimingBingo2: Boolean,
    onDrawNumber: () -> Unit,
    onMarkNumber: (Int) -> Unit,
    onClaimBingo: () -> Unit,
    onMarkNumberPlayer2: (Int) -> Unit,
    onClaimBingoPlayer2: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Aktuelle Zahl + Ziehen-Button (volle Breite)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (game.drawAnimationActive) "Zahl wird gezogen..." else "Aktuelle Zahl",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (game.drawAnimationActive) {
                        DrumAnimation(modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        AnimatedContent(
                            targetState = game.currentNumber,
                            transitionSpec = {
                                (fadeIn(tween(300)) + scaleIn(tween(300))) togetherWith fadeOut(tween(150))
                            },
                            label = "number_animation"
                        ) { number ->
                            Text(
                                text = number?.toString() ?: "–",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (isAdmin) {
                    Button(
                        onClick = onDrawNumber,
                        enabled = !isDrawing && !game.drawAnimationActive && !game.eliminationAnimationActive && game.eliminationPendingPlayerId == null && game.drawnNumbers.size < 75
                    ) {
                        if (isDrawing || game.drawAnimationActive) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ziehen", fontSize = 13.sp)
                                Text("${game.drawnNumbers.size}/75", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Gezogene Zahlen (volle Breite)
        if (game.drawnNumbers.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                game.drawnNumbers.reversed().forEach { n ->
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(
                            if (n == game.currentNumber) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = n.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (n == game.currentNumber) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Zwei Karten nebeneinander
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerCardSection(
                player = myPlayer,
                game = game,
                isClaimingBingo = isClaimingBingo,
                onMarkNumber = onMarkNumber,
                onClaimBingo = onClaimBingo,
                label = myPlayer?.displayName ?: "Spieler 1",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            VerticalDivider()
            PlayerCardSection(
                player = player2,
                game = game,
                isClaimingBingo = isClaimingBingo2,
                onMarkNumber = onMarkNumberPlayer2,
                onClaimBingo = onClaimBingoPlayer2,
                label = player2?.displayName ?: "Spieler 2",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun PlayerCardSection(
    player: BingoPlayer?,
    game: BingoGame,
    isClaimingBingo: Boolean,
    onMarkNumber: (Int) -> Unit,
    onClaimBingo: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxHeight().padding(horizontal = 4.dp)) {
        val isAutoMode = player != null && game.gameMode == GameMode.AUTO_MARK
        // Höhe reserviert für Label + Hinweis + Abstände + ggf. Button
        val reservedH = if (isAutoMode) 150.dp else 90.dp
        // Bingo-Karte: 6 Zeilen × Zellgröße + 5 Lücken à 4dp
        // Höhe ≈ 1.22 × Breite → Breite ≤ verfügbare Höhe / 1.22
        val cardWidth: Dp = minOf(maxWidth, (maxHeight - reservedH) / 1.22f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (player != null) {
                Text(
                    if (isAutoMode) "automatisch markiert" else "tippe zum Markieren",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.width(cardWidth)) {
                    BingoCardView(
                        card = player.card,
                        drawnNumbers = game.drawnNumbers,
                        onNumberClick = onMarkNumber,
                        interactive = !isAutoMode,
                        autoMarkWithDrawn = isAutoMode,
                        highlightDrawn = isAutoMode
                    )
                }
                if (isAutoMode) {
                    Button(
                        onClick = onClaimBingo,
                        enabled = !isClaimingBingo && game.drawnNumbers.isNotEmpty(),
                        modifier = Modifier.width(cardWidth),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        if (isClaimingBingo) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Prüfe...")
                        } else {
                            Text("BINGO! 🎉", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(
                    "Kein Spieler eingeloggt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Finished state ─────────────────────────────────────────────────────────

@Composable
private fun FinishedStateContent(
    game: BingoGame,
    onNavigateBack: () -> Unit,
    isTablet: Boolean = false,
    modifier: Modifier = Modifier
) {
    val winner = game.players.values.firstOrNull { it.hasBingo }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp).then(if (isTablet) Modifier.width(400.dp) else Modifier.fillMaxWidth())
        ) {
            Text("🎉", fontSize = 72.sp)
            Text("Spiel beendet!", style = MaterialTheme.typography.headlineMedium)
            if (winner != null) {
                Text(
                    "${winner.displayName} hat BINGO!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                Text("Zurück zur Lobby")
            }
        }
    }
}

// ── Shared components ──────────────────────────────────────────────────────

@Composable
private fun PlayerRow(player: BingoPlayer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(player.avatarUrl.ifEmpty { "🏄" }, fontSize = 30.sp)
        Spacer(Modifier.width(14.dp))
        Text(player.displayName, style = MaterialTheme.typography.titleSmall)
        if (player.hasBingo) {
            Spacer(Modifier.weight(1f))
            Text("BINGO! 🎉", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
