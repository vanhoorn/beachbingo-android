package com.bestfriends.beachbingo.feature.pong.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.sin
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

    val is2P = totalPaddles == 2
    val cw = if (is2P) W2 else SQ
    val ch = if (is2P) H2 else SQ

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
        if (uid != null) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
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

    // Game loop — runs on physics owner, or applies remote state for guests
    LaunchedEffect(loserSide, isPhysicsOwner) {
        if (loserSide != null) return@LaunchedEffect
        while (true) {
            delay(16L)
            frameCount++
            if (manualPaused) continue
            if (isPhysicsOwner) {
                viewModel.tick(frameCount)
            } else {
                viewModel.applyRemoteInterpolation()
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

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {

            // ── Header / Score bar ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2Dark)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToLobby) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Lobby", tint = OceanBlue)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeSides.forEachIndexed { index, side ->
                        if (index > 0) {
                            Text(
                                " · ",
                                color = Color(0xFF1E3050),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        val score = PongGameViewModel.scoreOf(gs, side)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                labelForSide(side).uppercase(),
                                fontSize = 9.sp,
                                color = SIDE_COLOR[side] ?: TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                "$score",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (score >= scoreLimit - 1) Coral else TextPrimary,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
                Text(
                    "/$scoreLimit",
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }

            // ── Canvas ────────────────────────────────────────────────────────
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
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.13f)
                        )
                    }
                }

                Canvas(
                    modifier = canvasModifier.pointerInput(mySide, cw, ch) {
                        // Track all pointer events for paddle control
                        while (true) {
                            val event = awaitPointerEventScope {
                                awaitPointerEvent()
                            }
                            event.changes.firstOrNull()?.let { change ->
                                val pos = change.position
                                val sx = cw / size.width
                                val sy = ch / size.height
                                val lx = pos.x * sx
                                val ly = pos.y * sy
                                val paddle = if (mySide == "left" || mySide == "right") ly.toDouble() else lx.toDouble()
                                viewModel.updateMyPaddle(paddle)
                                change.consume()
                            }
                        }
                    }
                ) {
                    val scaleX = size.width / cw
                    val scaleY = size.height / ch

                    if (is2P) {
                        draw2PField(gs, scaleX, scaleY)
                    } else {
                        drawMultiField(gs, totalPaddles, scaleX, scaleY)
                    }
                }
            }

            // ── HUD bar ───────────────────────────────────────────────────────
            GameHudBar(
                paused = manualPaused,
                onPauseToggle = { manualPaused = !manualPaused },
                onQuit = { manualPaused = true; showQuitDialog = true },
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        activeSides.forEachIndexed { idx, side ->
                            if (idx > 0) Text("·", color = Surface2Dark, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(labelForSide(side).uppercase(), fontSize = 8.sp, color = SIDE_COLOR[side] ?: TextMuted, fontWeight = FontWeight.Bold)
                                Text("${PongGameViewModel.scoreOf(gs, side)}", fontSize = 16.sp, fontWeight = FontWeight.Black,
                                    color = if (PongGameViewModel.scoreOf(gs, side) >= scoreLimit - 1) Coral else TextPrimary)
                            }
                        }
                    }
                }
            }

            // ── Touch hint ────────────────────────────────────────────────────
            Text(
                if (mySide == "left" || mySide == "right") "↕ Ziehe zum Steuern" else "↔ Ziehe zum Steuern",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = (SIDE_COLOR[mySide] ?: TextMuted).copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }

        if (showQuitDialog) {
            QuitConfirmDialog(
                message = "Das laufende Spiel wird beendet.",
                onConfirm = { onNavigateToLobby() },
                onDismiss = { showQuitDialog = false; manualPaused = false },
            )
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
                        fontSize = 72.sp
                    )
                    Text(
                        when {
                            isLoser -> "Du verlierst!"
                            isWinner -> "Du gewinnst!"
                            else -> "${labelForSide(loser)} verliert!"
                        },
                        fontSize = 24.sp,
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
                                    fontSize = 10.sp,
                                    color = SIDE_COLOR[side] ?: TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    "${PongGameViewModel.scoreOf(gs, side)}",
                                    fontSize = 28.sp,
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
}

// ── DrawScope helpers ─────────────────────────────────────────────────────────

private fun DrawScope.drawBackground(sx: Float, sy: Float) {
    // Sandy beach gradient
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFC8A86B), Color(0xFFD4B87A), Color(0xFFC09050)),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height)
        )
    )
    // Subtle sand texture
    val lineColor = Color(0x128B6A30)
    var y = 0f
    while (y < size.height) {
        val offset = sin(y * 0.3f) * 1.5f
        drawLine(lineColor, Offset(0f, y + offset), Offset(size.width, y + offset), 1.dp.toPx())
        y += 6f
    }
    // Court boundary
    val bx = (MARGIN + PADDLE_THICK + 4) * sx
    val bTop = 8f * sy
    drawRect(
        color = Color(0x59FFFFFF),
        topLeft = Offset(bx, bTop),
        size = Size(size.width - 2 * bx, size.height - 2 * bTop),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.draw2PField(g: PongGS, sx: Float, sy: Float) {
    drawBackground(sx, sy)

    // Top and bottom walls — sand planks (ball bounces off these, MARGIN units thick)
    val wallH = MARGIN * sy
    val wallColor = Color(0xCC8B6530)
    drawRect(wallColor, Offset(0f, 0f), Size(size.width, wallH))
    drawRect(wallColor, Offset(0f, size.height - wallH), Size(size.width, wallH))

    // Net bar + horizontal stripes
    val nx = size.width / 2f
    drawRect(Color(0x4D000000), Offset(nx - 3f, 0f), Size(6f, size.height))   // shadow
    drawRect(Color(0xFF7A5C28), Offset(nx - 2f, 0f), Size(4f, size.height))
    val stripeStep = 9f * sy
    var ys = 8f * sy
    while (ys < size.height) {
        drawLine(Color(0x8CFFFFFF), Offset(nx - 6f, ys), Offset(nx + 6f, ys), 1.dp.toPx())
        ys += stripeStep
    }

    drawSurfboard(
        x = MARGIN * sx,
        y = (g.paddleLeft - PADDLE_LEN / 2).toFloat() * sy,
        w = PADDLE_THICK * sx, h = PADDLE_LEN * sy,
        color = OceanBlue, vertical = true
    )
    drawSurfboard(
        x = size.width - (MARGIN + PADDLE_THICK) * sx,
        y = (g.paddleRight - PADDLE_LEN / 2).toFloat() * sy,
        w = PADDLE_THICK * sx, h = PADDLE_LEN * sy,
        color = Coral, vertical = true
    )
    drawVolleyball(g, sx, sy)
}

private fun DrawScope.drawMultiField(g: PongGS, totalPaddles: Int, sx: Float, sy: Float) {
    val s = size.width
    val wall = g.wallSide

    drawBackground(sx, sy)

    // Dashed cross net
    val dashLen = 5.dp.toPx()
    val dashGap = 6.dp.toPx()
    var yy = 0f
    while (yy < s) {
        drawLine(Color(0x4CFFFFFF), Offset(s / 2f, yy), Offset(s / 2f, (yy + dashLen).coerceAtMost(s)), 1.5.dp.toPx())
        yy += dashLen + dashGap
    }
    var xx = 0f
    while (xx < s) {
        drawLine(Color(0x4CFFFFFF), Offset(xx, s / 2f), Offset((xx + dashLen).coerceAtMost(s), s / 2f), 1.5.dp.toPx())
        xx += dashLen + dashGap
    }

    // Corner deflectors (4P) — sand-colored
    if (totalPaddles == 4) {
        val cs = CORNER_SIZE * sx
        val cc = Color(0xBB8B6530)
        drawPath(Path().apply { moveTo(0f, 0f); lineTo(cs, 0f); lineTo(0f, cs); close() }, cc)
        drawPath(Path().apply { moveTo(s, 0f); lineTo(s - cs, 0f); lineTo(s, cs); close() }, cc)
        drawPath(Path().apply { moveTo(0f, s); lineTo(cs, s); lineTo(0f, s - cs); close() }, cc)
        drawPath(Path().apply { moveTo(s, s); lineTo(s - cs, s); lineTo(s, s - cs); close() }, cc)
    }

    // Wall — sand plank
    if (wall != null) {
        val wc = Color(0xCC8B6530)
        val thickness = (PADDLE_THICK + MARGIN) * sx
        when (wall) {
            "left"   -> drawRect(wc, Offset(0f, 0f),             Size(thickness, s))
            "right"  -> drawRect(wc, Offset(s - thickness, 0f),  Size(thickness, s))
            "top"    -> drawRect(wc, Offset(0f, 0f),             Size(s, thickness))
            "bottom" -> drawRect(wc, Offset(0f, s - thickness),  Size(s, thickness))
        }
    }

    // Paddles
    val activeSides = PongGameViewModel.sidesForPaddles(totalPaddles, wall)
    activeSides.forEach { side ->
        val color = SIDE_COLOR[side] ?: OceanBlue
        val pos = PongGameViewModel.paddleOf(g, side).toFloat()
        when (side) {
            "left"   -> drawSurfboard(MARGIN * sx, (pos - PADDLE_LEN / 2) * sy, PADDLE_THICK * sx, PADDLE_LEN * sy, color, vertical = true)
            "right"  -> drawSurfboard(s - (MARGIN + PADDLE_THICK) * sx, (pos - PADDLE_LEN / 2) * sy, PADDLE_THICK * sx, PADDLE_LEN * sy, color, vertical = true)
            "top"    -> drawSurfboard((pos - PADDLE_LEN / 2) * sx, MARGIN * sy, PADDLE_LEN * sx, PADDLE_THICK * sy, color, vertical = false)
            "bottom" -> drawSurfboard((pos - PADDLE_LEN / 2) * sx, s - (MARGIN + PADDLE_THICK) * sy, PADDLE_LEN * sx, PADDLE_THICK * sy, color, vertical = false)
        }
    }

    drawVolleyball(g, sx, sy)
}

private fun DrawScope.drawSurfboard(x: Float, y: Float, w: Float, h: Float, color: Color, vertical: Boolean) {
    val cx = x + w / 2f
    val cy = y + h / 2f
    val hw = w / 2f
    val hh = h / 2f

    val path = Path()
    if (vertical) {
        path.moveTo(cx, cy - hh)
        path.cubicTo(cx + hw * 2.2f, cy - hh * 0.6f, cx + hw * 2.2f, cy + hh * 0.6f, cx, cy + hh)
        path.cubicTo(cx - hw * 2.2f, cy + hh * 0.6f, cx - hw * 2.2f, cy - hh * 0.6f, cx, cy - hh)
        path.close()
        drawPath(path, brush = Brush.linearGradient(
            colors = listOf(shadeColor(color, -0.08f), color, shadeColor(color, -0.08f)),
            start = Offset(x, cy), end = Offset(x + w, cy)
        ))
        drawLine(Color(0x59FFFFFF), Offset(cx, cy - hh * 0.7f), Offset(cx, cy + hh * 0.7f), 1.5.dp.toPx())
    } else {
        path.moveTo(cx - hw, cy)
        path.cubicTo(cx - hw * 0.6f, cy - hh * 2.2f, cx + hw * 0.6f, cy - hh * 2.2f, cx + hw, cy)
        path.cubicTo(cx + hw * 0.6f, cy + hh * 2.2f, cx - hw * 0.6f, cy + hh * 2.2f, cx - hw, cy)
        path.close()
        drawPath(path, brush = Brush.linearGradient(
            colors = listOf(shadeColor(color, -0.08f), color, shadeColor(color, -0.08f)),
            start = Offset(cx, y), end = Offset(cx, y + h)
        ))
        drawLine(Color(0x59FFFFFF), Offset(cx - hw * 0.7f, cy), Offset(cx + hw * 0.7f, cy), 1.5.dp.toPx())
    }
}

private fun shadeColor(color: Color, delta: Float): Color = Color(
    red   = (color.red   + delta).coerceIn(0f, 1f),
    green = (color.green + delta).coerceIn(0f, 1f),
    blue  = (color.blue  + delta).coerceIn(0f, 1f),
    alpha = color.alpha
)

private fun DrawScope.drawVolleyball(g: PongGS, sx: Float, sy: Float) {
    if (g.paused && g.pauseTimer >= 30) return
    val bx = g.bx.toFloat() * sx
    val by = g.by.toFloat() * sy
    val r  = BALL_R * sx
    val r2 = r * 1.05f
    val seam = Stroke(width = 1.4.dp.toPx())

    // Drop shadow
    drawCircle(Color(0x66000000), radius = r * 1.1f, center = Offset(bx + 1f, by + 2f))
    // Ball base (off-white)
    drawCircle(Color(0xFFF5F2E6), radius = r, center = Offset(bx, by))

    // Blue seam — arc centered at (bx - r*0.3, by - r*0.3), from 99° sweep 135°
    drawArc(Color(0xFF2563EB), 99f, 135f, false,
        Offset(bx - r * 0.3f - r2, by - r * 0.3f - r2), Size(r2 * 2, r2 * 2), style = seam)
    // Orange seam — arc centered at (bx + r*0.3, by), from 279° sweep 126°
    drawArc(Color(0xFFF59E0B), 279f, 126f, false,
        Offset(bx + r * 0.3f - r2, by - r2), Size(r2 * 2, r2 * 2), style = seam)
    // Green seam — arc centered at (bx, by + r*0.35), from 9° sweep 162°
    drawArc(Color(0xFF16A34A), 9f, 162f, false,
        Offset(bx - r2, by + r * 0.35f - r2), Size(r2 * 2, r2 * 2), style = seam)

    // Outer border
    drawCircle(Color(0x26000000), radius = r, center = Offset(bx, by), style = Stroke(1.dp.toPx()))
    // Highlight
    drawCircle(Color(0x8CFFFFFF), radius = r * 0.3f, center = Offset(bx - r * 0.28f, by - r * 0.28f))
}
