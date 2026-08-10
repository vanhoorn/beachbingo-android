package com.bestfriends.beachbingo.feature.pong.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.feature.pong.viewmodel.BALL_R
import com.bestfriends.beachbingo.feature.pong.viewmodel.CORNER_SIZE
import com.bestfriends.beachbingo.feature.pong.viewmodel.H2
import com.bestfriends.beachbingo.feature.pong.viewmodel.MARGIN
import com.bestfriends.beachbingo.feature.pong.viewmodel.PADDLE_LEN
import com.bestfriends.beachbingo.feature.pong.viewmodel.PADDLE_THICK
import com.bestfriends.beachbingo.feature.pong.viewmodel.PongGS
import com.bestfriends.beachbingo.feature.pong.viewmodel.PongGameViewModel
import com.bestfriends.beachbingo.feature.pong.viewmodel.SQ
import com.bestfriends.beachbingo.feature.pong.viewmodel.W2
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.ChipLabelTiny
import com.bestfriends.beachbingo.ui.theme.DarkGray
import com.bestfriends.beachbingo.ui.theme.EmojiCelebrate
import com.bestfriends.beachbingo.ui.theme.EmojiXLarge
import com.bestfriends.beachbingo.ui.theme.MidGray
import com.bestfriends.beachbingo.ui.theme.NearBlack
import com.bestfriends.beachbingo.ui.theme.StatusTiny
import kotlinx.coroutines.delay

private val SIDE_COLOR = mapOf(
    "left" to OceanBlue,
    "right" to Coral,
    "top" to SandGold,
    "bottom" to Success,
)

@Composable
fun PongGameScreen(
    gameId: String?,
    totalPaddles: Int,
    humanCount: Int,
    difficulty: String,
    scoreLimit: Int,
    isHost: Boolean,
    mySide: String,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    onNavigateToLobby: () -> Unit,
    viewModel: PongGameViewModel = hiltViewModel()
) {
    val diff = runCatching { PongDifficulty.valueOf(difficulty) }.getOrDefault(PongDifficulty.ROOKIE)

    LaunchedEffect(gameId) {
        viewModel.init(gameId, totalPaddles, humanCount, diff, scoreLimit, isHost, mySide)
    }

    val gs by viewModel.gs.collectAsStateWithLifecycle()
    val loserSide by viewModel.loserSide.collectAsStateWithLifecycle()
    val opponentNames by viewModel.opponentNames.collectAsStateWithLifecycle()
    val isGameActive by viewModel.isGameActive.collectAsStateWithLifecycle()
    val hostDisconnected by viewModel.hostDisconnected.collectAsStateWithLifecycle()

    val is2P = totalPaddles == 2
    val cw = if (is2P) W2 else SQ
    val ch = if (is2P) H2 else SQ
    val zoneHeight = if (LocalConfiguration.current.screenWidthDp >= 600) 160.dp else 120.dp

    var frameCount by remember { mutableIntStateOf(0) }
    val isPhysicsOwner = humanCount == 1 || isHost
    var manualPaused by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }

    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    val audio = remember { PongAudioManager() }
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
    LaunchedEffect(loserSide) {
        if (!musicStarted || loserSide == null) return@LaunchedEffect
        audio.stopMusic()
        audio.playSound("win")
    }
    // Detect ball hits and score events by observing gs velocity/score changes
    LaunchedEffect(audio) {
        var prevBvx = 0.0
        var prevBvy = 0.0
        var prevTotal = 0
        snapshotFlow { gs }.collect { cur ->
            val curBvx = cur.bvx
            val curBvy = cur.bvy
            val curTotal = PongGameViewModel.sidesForPaddles(totalPaddles, cur.wallSide)
                .sumOf { PongGameViewModel.scoreOf(cur, it) }
            when {
                curTotal > prevTotal -> audio.playSound("score")
                prevBvx != 0.0 && curBvx != 0.0 && prevBvx * curBvx < 0.0 ->
                    audio.playSound("ball_hit")
                prevBvy != 0.0 && curBvy != 0.0 && prevBvy * curBvy < 0.0 ->
                    audio.playSound("wall_hit")
            }
            prevBvx = curBvx
            prevBvy = curBvy
            prevTotal = curTotal
        }
    }

    // Game loop — only runs once the game is active (host set IN_PROGRESS)
    LaunchedEffect(loserSide, isPhysicsOwner, isGameActive) {
        if (loserSide != null || !isGameActive) return@LaunchedEffect
        while (true) {
            delay(16L)
            frameCount++
            if (manualPaused) continue
            if (isPhysicsOwner) {
                viewModel.tick(frameCount)
            } else {
                viewModel.applyRemoteInterpolation(frameCount)
            }
        }
    }

    fun labelForSide(side: String): String {
        return when {
            side == mySide -> "Du"
            humanCount == 1 -> "KI"
            else -> opponentNames[side] ?: "Gegner"
        }
    }

    val activeSides = PongGameViewModel.sidesForPaddles(totalPaddles, gs.wallSide)

    Scaffold(
        containerColor = BgDark,
        topBar = {
            GameHudBar(
                paused = manualPaused,
                onPauseToggle = { manualPaused = !manualPaused },
                onQuit = { manualPaused = true; showQuitDialog = true },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    activeSides.forEachIndexed { index, side ->
                        if (index > 0) {
                            Text(
                                " · ",
                                color = BorderColor,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        val score = PongGameViewModel.scoreOf(gs, side)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                labelForSide(side).uppercase(),
                                fontSize = StatusTiny,
                                color = SIDE_COLOR[side] ?: TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            )
                            Text(
                                "$score",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = if (score >= scoreLimit - 1) Coral else TextPrimary,
                                lineHeight = 24.sp,
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).navigationBarsPadding()) {

                // ── Canvas ────────────────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Explicit sizing avoids aspect-ratio issues with tight constraints on tablets
                val canvasModifier = if (is2P) {
                    if (maxWidth.value * ch / cw <= maxHeight.value) {
                        Modifier.size(maxWidth, (maxWidth.value * ch / cw).dp)
                    } else {
                        Modifier.size((maxHeight.value * cw / ch).dp, maxHeight)
                    }
                } else {
                    val sq = minOf(maxWidth, maxHeight)
                    Modifier.size(sq, sq)
                }

                // Countdown overlay (composable text, since DrawScope can't easily draw text)
                if (gs.paused && gs.pauseTimer > 30) {
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${((gs.pauseTimer + 29) / 30).coerceIn(1, 3)}",
                            fontSize = EmojiCelebrate,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.13f)
                        )
                    }
                }

                Canvas(modifier = canvasModifier) {
                    val scaleX = size.width / cw
                    val scaleY = size.height / ch

                    if (is2P) {
                        draw2PField(gs, scaleX, scaleY)
                    } else {
                        drawMultiField(gs, totalPaddles, scaleX, scaleY)
                    }
                }
            }

            // ── Zone control strip ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(zoneHeight)
                    .background(NearBlack)
                    .pointerInput(mySide, humanCount, is2P, cw, ch) {
                        while (true) {
                            val event = awaitPointerEventScope { awaitPointerEvent() }
                            event.changes.forEach { change ->
                                val pos = change.position
                                val zW = size.width.toFloat()
                                val zH = size.height.toFloat()
                                val frac  = (pos.y / zH).coerceIn(0f, 1f)
                                val fracX = (pos.x / zW).coerceIn(0f, 1f)
                                val side = if (humanCount == 1) mySide
                                           else if (pos.x < zW / 2f) "left" else "right"
                                val isVert = side == "left" || side == "right"
                                val wallOff = if (is2P && isVert) MARGIN.toDouble() else 0.0
                                val axisSize = (if (isVert) ch else cw).toDouble()
                                val pMin = PADDLE_LEN / 2.0 + wallOff
                                val pMax = axisSize - PADDLE_LEN / 2.0 - wallOff
                                val paddle = if (isVert) pMin + frac * (pMax - pMin)
                                             else pMin + fracX * (pMax - pMin)
                                viewModel.updatePaddleSide(side, paddle)
                                change.consume()
                            }
                        }
                    }
            ) {
                if (humanCount >= 2) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(3.dp).height(28.dp).background(OceanBlue, RoundedCornerShape(2.dp)))
                            Text("↕", fontSize = ChipLabelTiny, color = MidGray, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("↕", fontSize = ChipLabelTiny, color = MidGray, fontWeight = FontWeight.Bold)
                            Box(Modifier.width(3.dp).height(28.dp).background(Coral, RoundedCornerShape(2.dp)))
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val paddleColor = SIDE_COLOR[mySide] ?: OceanBlue
                        val isVert = mySide == "left" || mySide == "right"
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isVert) {
                                Box(Modifier.width(3.dp).height(32.dp).background(paddleColor, RoundedCornerShape(2.dp)))
                                Text("↕", style = MaterialTheme.typography.labelSmall, color = MidGray, fontWeight = FontWeight.Bold)
                            } else {
                                Box(Modifier.width(32.dp).height(3.dp).background(paddleColor, RoundedCornerShape(2.dp)))
                                Text("↔", style = MaterialTheme.typography.labelSmall, color = MidGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        }

        if (showQuitDialog) {
            QuitConfirmDialog(
                emoji = "🏓",
                message = "Das laufende Spiel wird beendet.",
                onConfirm = { onNavigateToLobby() },
                onDismiss = { showQuitDialog = false; manualPaused = false },
            )
        }

        // ── Host-disconnect overlay (guest only) ─────────────────────────────
        if (hostDisconnected && loserSide == null && isGameActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark.copy(alpha = 0.93f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🔌", fontSize = EmojiXLarge)
                    Text(
                        "Host nicht erreichbar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                    )
                    Text(
                        "Verbindung zum Host unterbrochen.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onNavigateToLobby,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2Dark)
                    ) { Text("Zur Lobby", color = TextPrimary) }
                }
            }
        }

        // ── Waiting-for-host overlay (guest only, before IN_PROGRESS) ────────
        if (!isGameActive && !isHost && gameId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark.copy(alpha = 0.93f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🏓", fontSize = EmojiXLarge)
                    Text(
                        "Warte auf Host...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                    )
                    Text(
                        "Das Spiel beginnt, sobald der Host startet.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onNavigateToLobby,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2Dark)
                    ) { Text("Zurück zur Lobby", color = TextPrimary) }
                }
            }
        }

        // ── Winner/Loser overlay ──────────────────────────────────────────────
        if (loserSide != null) {
            val loser = loserSide!!
            val winnerSides = activeSides.filter { it != loser }
            val isWinner = mySide in winnerSides
            val isLoser = mySide == loser

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark.copy(alpha = 0.93f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        when {
                            isWinner -> "🏆"
                            isLoser -> "😅"
                            else -> "🏓"
                        },
                        fontSize = EmojiCelebrate
                    )
                    Text(
                        when {
                            isLoser -> "Du verlierst!"
                            isWinner -> "Du gewinnst!"
                            else -> "${labelForSide(loser)} verliert!"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    // Score summary
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        activeSides.forEach { side ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    labelForSide(side).uppercase(),
                                    fontSize = ChipLabelTiny,
                                    color = SIDE_COLOR[side] ?: TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    "${PongGameViewModel.scoreOf(gs, side)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (side == loser) Coral else TextPrimary
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (humanCount == 1 || isHost) {
                            Button(
                                onClick = { viewModel.resetGame() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Coral)
                            ) {
                                Text(
                                    "🔄 Nochmal",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                        Button(
                            onClick = onNavigateToLobby,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Surface2Dark)
                        ) {
                            Text(
                                "Lobby",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    } // Scaffold
}

// ── DrawScope helpers ─────────────────────────────────────────────────────────

private fun DrawScope.draw2PField(g: PongGS, sx: Float, sy: Float) {
    val w = size.width
    val h = size.height
    val wallH = MARGIN * sy

    // Black background
    drawRect(Color.Black)

    // White walls
    drawRect(Color.White, Offset(0f, 0f), Size(w, wallH))
    drawRect(Color.White, Offset(0f, h - wallH), Size(w, wallH))

    // Dashed center line
    val dashLen = 12.dp.toPx()
    val dashGap = 10.dp.toPx()
    val nx = w / 2f
    var yy = wallH
    while (yy < h - wallH) {
        val end = (yy + dashLen).coerceAtMost(h - wallH)
        drawLine(Color.White, Offset(nx, yy), Offset(nx, end), 4.dp.toPx())
        yy += dashLen + dashGap
    }

    // Left paddle
    drawRect(
        color = Color.White,
        topLeft = Offset(MARGIN * sx, (g.paddleLeft - PADDLE_LEN / 2).toFloat() * sy),
        size = Size(PADDLE_THICK * sx, PADDLE_LEN * sy)
    )
    // Right paddle
    drawRect(
        color = Color.White,
        topLeft = Offset(w - (MARGIN + PADDLE_THICK) * sx, (g.paddleRight - PADDLE_LEN / 2).toFloat() * sy),
        size = Size(PADDLE_THICK * sx, PADDLE_LEN * sy)
    )

    drawClassicBall(g, sx, sy)
}

private fun DrawScope.drawMultiField(g: PongGS, totalPaddles: Int, sx: Float, sy: Float) {
    val s = size.width
    val wall = g.wallSide

    // Black background
    drawRect(Color.Black)

    // Dashed cross
    val dashLen = 10.dp.toPx()
    val dashGap = 8.dp.toPx()
    var yy = 0f
    while (yy < s) {
        val end = (yy + dashLen).coerceAtMost(s)
        drawLine(Color.White, Offset(s / 2f, yy), Offset(s / 2f, end), 3.dp.toPx())
        yy += dashLen + dashGap
    }
    var xx = 0f
    while (xx < s) {
        val end = (xx + dashLen).coerceAtMost(s)
        drawLine(Color.White, Offset(xx, s / 2f), Offset(end, s / 2f), 3.dp.toPx())
        xx += dashLen + dashGap
    }

    // Corner triangles (4P)
    if (totalPaddles == 4) {
        val cs = CORNER_SIZE * sx
        val cc = DarkGray
        drawPath(Path().apply { moveTo(0f, 0f); lineTo(cs, 0f); lineTo(0f, cs); close() }, cc)
        drawPath(Path().apply { moveTo(s, 0f); lineTo(s - cs, 0f); lineTo(s, cs); close() }, cc)
        drawPath(Path().apply { moveTo(0f, s); lineTo(cs, s); lineTo(0f, s - cs); close() }, cc)
        drawPath(Path().apply { moveTo(s, s); lineTo(s - cs, s); lineTo(s, s - cs); close() }, cc)
    }

    // Wall
    if (wall != null) {
        val thickness = (PADDLE_THICK + MARGIN) * sx
        when (wall) {
            "left"   -> drawRect(DarkGray, Offset(0f, 0f),            Size(thickness, s))
            "right"  -> drawRect(DarkGray, Offset(s - thickness, 0f), Size(thickness, s))
            "top"    -> drawRect(DarkGray, Offset(0f, 0f),            Size(s, thickness))
            "bottom" -> drawRect(DarkGray, Offset(0f, s - thickness), Size(s, thickness))
        }
    }

    // Paddles
    val activeSides = PongGameViewModel.sidesForPaddles(totalPaddles, wall)
    activeSides.forEach { side ->
        val color = SIDE_COLOR[side] ?: Color.White
        val pos = PongGameViewModel.paddleOf(g, side).toFloat()
        when (side) {
            "left"   -> drawRect(color, Offset(MARGIN * sx, (pos - PADDLE_LEN / 2) * sy), Size(PADDLE_THICK * sx, PADDLE_LEN * sy))
            "right"  -> drawRect(color, Offset(s - (MARGIN + PADDLE_THICK) * sx, (pos - PADDLE_LEN / 2) * sy), Size(PADDLE_THICK * sx, PADDLE_LEN * sy))
            "top"    -> drawRect(color, Offset((pos - PADDLE_LEN / 2) * sx, MARGIN * sy), Size(PADDLE_LEN * sx, PADDLE_THICK * sy))
            "bottom" -> drawRect(color, Offset((pos - PADDLE_LEN / 2) * sx, s - (MARGIN + PADDLE_THICK) * sy), Size(PADDLE_LEN * sx, PADDLE_THICK * sy))
        }
    }

    drawClassicBall(g, sx, sy)
}

private fun DrawScope.drawClassicBall(g: PongGS, sx: Float, sy: Float) {
    if (g.paused && g.pauseTimer >= 30) return
    drawCircle(Color.White, radius = BALL_R * sx, center = Offset(g.bx.toFloat() * sx, g.by.toFloat() * sy))
}
