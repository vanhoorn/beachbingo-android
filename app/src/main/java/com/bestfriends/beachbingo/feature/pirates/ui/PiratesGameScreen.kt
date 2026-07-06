package com.bestfriends.beachbingo.feature.pirates.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlin.math.*
import kotlin.random.Random

// ── Constants ─────────────────────────────────────────────────────────────────

private const val CW = 400f
private const val CH = 580f

private const val INVADER_COLS = 7
private const val INVADER_ROWS = 4
private const val INVADER_W = 40f
private const val INVADER_H = 36f
private const val INVADER_TOP = 44f

private const val PLAYER_Y = 530f
private const val PLAYER_W = 44f
private const val PLAYER_SPEED = 5f

private const val BULLET_W = 4f
private const val BULLET_H = 14f
private const val PLAYER_BULLET_SPD = 9f
private const val ENEMY_BULLET_SPD = 4f
private const val MAX_PLAYER_BULLETS = 3

private val SHIELD_XS = floatArrayOf(44f, 140f, 248f, 344f)
private const val SHIELD_COLS = 12
private const val SHIELD_ROWS = 8
private const val BLOCK = 4f
private const val ERASE_R = 2

private val EMOJIS = arrayOf("🪼", "🐚", "🐟", "🦀")
private val BASE_SPEED  = mapOf("ROOKIE" to 3, "SNIPER" to 6, "BOSS_LEVEL" to 10)
private val BASE_FIRING = mapOf("ROOKIE" to 3, "SNIPER" to 6, "BOSS_LEVEL" to 10)

private val PurpleGame   = Color(0xFFA855F7)
private val OrangeBullet = Color(0xFFFB923C)
private val PurpleBullet = Color(0xFFC084FC)

// ── Data classes ──────────────────────────────────────────────────────────────

private data class Invader(val col: Int, val row: Int, var x: Float, var y: Float, var alive: Boolean = true)
private data class Bullet(var x: Float, var y: Float, val dy: Float)

private class Shield(val originX: Float) {
    val blocks: Array<Array<Boolean>> = Array(SHIELD_ROWS) { row ->
        Array(SHIELD_COLS) { col ->
            val center = SHIELD_COLS / 2
            val inArch = col in (center - 2)..(center + 1) && row >= SHIELD_ROWS - 3
            !inArch
        }
    }
}

private enum class Phase { PLAYING, HIT, WAVE_CLEAR, GAME_OVER }

// State-backed fields (score/lives/wave/phase) trigger HUD recomposition automatically.
private class GameState(diffStr: String, val fireRateArg: Int) {
    val baseDiff = diffStr

    var invaders    = mutableListOf<Invader>()
    var playerX     = CW / 2f
    var playerBullets = mutableListOf<Bullet>()
    var enemyBullets  = mutableListOf<Bullet>()
    var shields     = mutableListOf<Shield>()

    var groupDir    = 1
    var moveAccumMs = 0f       // delta-time accumulator for invader steps
    var fireCooldown = 0       // frames since last player shot

    // Compose-observed state — Composables reading these recompose on change
    var score  by mutableIntStateOf(0)
    var lives  by mutableIntStateOf(3)
    var wave   by mutableIntStateOf(1)
    var phase  by mutableStateOf(Phase.PLAYING)
    var phaseTimer = 0

    var keys = booleanArrayOf(false, false) // [left, right]

    // ── Derived difficulty values ─────────────────────────────────────────
    fun currentSpeed()  = min(30, (BASE_SPEED[baseDiff]  ?: 6) + waveBonus(wave))
    fun currentFiring() = min(30, (BASE_FIRING[baseDiff] ?: 6) + waveBonus(wave))

    // Cooldown in frames at 60 fps; clamped to [5, 50]
    fun fireCooldownMax() = max(5, 55 - fireRateArg * 5)

    // Base interval between invader steps in ms (speed 3→~1500ms, speed 30→~66ms)
    fun baseMoveIntervalMs(): Float = max(66f, (93f - currentSpeed() * 3f) * (1000f / 60f))

    fun stepSize()     = max(4f, 4f + currentSpeed() * 0.4f)

    // Per-frame shoot probability per alive invader (~0.0001 … 0.0047 at 60fps)
    fun shootChance()  = 0.0001f + (currentFiring() - 1) * 0.000165f

    fun aliveCount()   = invaders.count { it.alive }

    fun initWave() {
        val startX = 50f
        invaders.clear()
        for (row in 0 until INVADER_ROWS)
            for (col in 0 until INVADER_COLS)
                invaders.add(Invader(col, row,
                    startX + col * (INVADER_W + 10f),
                    INVADER_TOP + row * (INVADER_H + 10f)))
        groupDir    = 1
        moveAccumMs = 0f
        enemyBullets.clear()
        shields.clear()
        SHIELD_XS.forEach { shields.add(Shield(it)) }
    }

    init { initWave() }
}

private fun waveBonus(wave: Int): Int {
    if (wave <= 1) return 0
    val base  = min(wave - 1, 4)
    val extra = max(0, wave - 5) * 2
    return base + extra
}

private fun rectsOverlap(ax: Float, ay: Float, aw: Float, ah: Float,
                          bx: Float, by: Float, bw: Float, bh: Float) =
    ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PiratesGameScreen(
    difficulty: String,
    fireRate: Int,
    controlMode: String,
    onNavigateToResults: (score: Int, wave: Int, highScore: Int, newHighScore: Boolean) -> Unit,
    onNavigateToLobby: () -> Unit,
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid

    val gs = remember { GameState(difficulty, fireRate) }

    // renderTick: only used to force Canvas redraws; HUD uses mutableStateOf fields directly
    var renderTick    by remember { mutableLongStateOf(0L) }
    var paused        by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var resultHandled  by remember { mutableStateOf(false) }
    var isFavorite    by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        val snap = try { firestore.collection("users").document(uid).get().await() } catch (_: Exception) { return@LaunchedEffect }
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("pirates") == true
    }

    // Game loop — delta-time aware
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                val deltaMs = if (lastNanos == 0L) 16f
                              else ((frameNanos - lastNanos) / 1_000_000f).coerceIn(1f, 50f)
                lastNanos = frameNanos
                if (!paused) updateGame(gs, deltaMs)
                renderTick++
            }
            // Check game-over after frame
            if (gs.phase == Phase.GAME_OVER && gs.phaseTimer > 120 && !resultHandled && uid != null) {
                resultHandled = true
                val finalScore = gs.score
                val prev = try {
                    val snap = firestore.collection("users").document(uid).get().await()
                    @Suppress("UNCHECKED_CAST")
                    (snap.get("piratesHighScores") as? Map<String, Long>)?.get(difficulty)?.toInt() ?: 0
                } catch (_: Exception) { 0 }
                val newHs = maxOf(prev, finalScore)
                val isNew = finalScore > prev
                if (isNew) try {
                    firestore.collection("users").document(uid)
                        .update("piratesHighScores.$difficulty", newHs.toLong()).await()
                } catch (_: Exception) {}
                onNavigateToResults(finalScore, gs.wave, newHs, isNew)
                break
            }
        }
    }

    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding(),
    ) {
        // ── HUD ────────────────────────────────────────────────────────────
        GameHudBar(
            paused          = paused,
            isFavorite      = isFavorite,
            onPauseToggle   = { paused = !paused },
            onQuit          = { paused = true; showQuitDialog = true },
            onFavoriteToggle = {
                isFavorite = !isFavorite
                if (uid != null) {
                    val update = if (isFavorite) FieldValue.arrayUnion("pirates") else FieldValue.arrayRemove("pirates")
                    firestore.collection("users").document(uid).update("favoriteGames", update)
                }
            },
        ) {
            HudCell(value = "${gs.score}", label = "Score", color = PurpleGame, modifier = Modifier.weight(1.4f))
            HudCell(value = "W${gs.wave}", label = "Welle", color = OceanBlue, modifier = Modifier.weight(0.8f))
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1.2f),
            ) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(3) { i -> Text(if (i < gs.lives) "🐙" else "💀", fontSize = 13.sp) }
                }
                Text("Leben", fontSize = 8.sp, color = TextMuted)
            }
            HudBar2("⚡", gs.currentSpeed(), 30, SandGold)
            HudBar2("🔱", gs.currentFiring(), 30, PurpleGame)
        }

        // ── Game canvas ────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val widthPx = with(density) { maxWidth.toPx() }
                val scale   = widthPx / CW
                val canvasH = CH * scale

                Canvas(
                    modifier = Modifier
                        .width(maxWidth)
                        .height(with(density) { canvasH.toDp() })
                        .then(
                            if (controlMode == "TOUCH")
                                Modifier.pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.any { it.pressed }
                                            if (pressed) {
                                                val x = event.changes.firstOrNull()?.position?.x ?: 0f
                                                gs.keys[0] = x < widthPx / 2
                                                gs.keys[1] = x >= widthPx / 2
                                            } else {
                                                gs.keys[0] = false; gs.keys[1] = false
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            else Modifier
                        ),
                ) {
                    // scale the whole 400×580 virtual canvas to fit the screen width
                    drawIntoCanvas { c ->
                        c.save()
                        c.scale(scale, scale)
                        drawGame(this, gs, renderTick, paused)
                        c.restore()
                    }
                }
            }
        }

        // ── Control buttons ────────────────────────────────────────────────
        if (controlMode == "BUTTONS") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ControlButton("◀", OceanBlue, Modifier.weight(1f).fillMaxHeight(),
                    { gs.keys[0] = true }, { gs.keys[0] = false })
                ControlButton("▶", Coral,      Modifier.weight(1f).fillMaxHeight(),
                    { gs.keys[1] = true }, { gs.keys[1] = false })
            }
        }
    }

    // ── Quit dialog ────────────────────────────────────────────────────────
    if (showQuitDialog) {
        QuitConfirmDialog(
            message   = "Score: ${gs.score} — Fortschritt geht verloren.",
            onConfirm = onNavigateToLobby,
            onDismiss = { showQuitDialog = false; paused = false },
        )
    }
}

// ── Update (delta-time based) ─────────────────────────────────────────────────

private fun updateGame(gs: GameState, deltaMs: Float) {
    when (gs.phase) {
        Phase.HIT -> {
            gs.phaseTimer++
            if (gs.phaseTimer >= 90) {
                if (gs.lives <= 0) { gs.phase = Phase.GAME_OVER; gs.phaseTimer = 0 }
                else {
                    gs.phase = Phase.PLAYING; gs.phaseTimer = 0
                    gs.playerX = CW / 2f
                    gs.playerBullets.clear(); gs.enemyBullets.clear()
                }
            }
            return
        }
        Phase.WAVE_CLEAR -> {
            gs.phaseTimer++
            if (gs.phaseTimer >= 90) {
                gs.wave++; gs.phase = Phase.PLAYING; gs.phaseTimer = 0
                gs.playerBullets.clear(); gs.initWave()
            }
            return
        }
        Phase.GAME_OVER -> { gs.phaseTimer++; return }
        Phase.PLAYING   -> {}
    }

    // ── Player movement ───────────────────────────────────────────────────
    if (gs.keys[0]) gs.playerX = max(PLAYER_W / 2f,       gs.playerX - PLAYER_SPEED)
    if (gs.keys[1]) gs.playerX = min(CW - PLAYER_W / 2f,  gs.playerX + PLAYER_SPEED)

    // ── Auto fire — clamped cooldown avoids burst ─────────────────────────
    gs.fireCooldown++
    val maxCd = gs.fireCooldownMax()
    if (gs.fireCooldown >= maxCd) {
        if (gs.playerBullets.size < MAX_PLAYER_BULLETS) {
            gs.playerBullets.add(Bullet(gs.playerX, PLAYER_Y - 26f, -PLAYER_BULLET_SPD))
            gs.fireCooldown = 0
        } else {
            gs.fireCooldown = maxCd  // clamp — don't accumulate past max
        }
    }

    // ── Player bullets ────────────────────────────────────────────────────
    val pbIter = gs.playerBullets.iterator()
    outer@ while (pbIter.hasNext()) {
        val b = pbIter.next(); b.y += b.dy
        if (b.y < -BULLET_H) { pbIter.remove(); continue }

        // vs. shields
        for (shield in gs.shields) {
            val sy = CH - 160f
            for (row in 0 until SHIELD_ROWS) for (col in 0 until SHIELD_COLS) {
                if (!shield.blocks[row][col]) continue
                if (rectsOverlap(b.x - BULLET_W/2, b.y, BULLET_W, BULLET_H,
                        shield.originX + col*BLOCK, sy + row*BLOCK, BLOCK, BLOCK)) {
                    eraseShield(shield, col, row); pbIter.remove(); continue@outer
                }
            }
        }
        // vs. invaders
        for (inv in gs.invaders) {
            if (!inv.alive) continue
            if (rectsOverlap(b.x - BULLET_W/2, b.y, BULLET_W, BULLET_H,
                    inv.x - INVADER_W/2, inv.y - INVADER_H/2, INVADER_W, INVADER_H)) {
                inv.alive = false
                gs.score += when (inv.row) { 0 -> 40; 1 -> 30; 2 -> 20; else -> 10 }
                pbIter.remove(); continue@outer
            }
        }
    }

    // ── Invader movement (delta-time) ─────────────────────────────────────
    val alive = gs.aliveCount()
    if (alive == 0) { gs.phase = Phase.WAVE_CLEAR; gs.phaseTimer = 0; return }

    val speedFactor  = alive.toFloat() / (INVADER_COLS * INVADER_ROWS).toFloat()
    val intervalMs   = gs.baseMoveIntervalMs() * speedFactor
    gs.moveAccumMs  += deltaMs
    if (gs.moveAccumMs >= intervalMs) {
        gs.moveAccumMs -= intervalMs

        val step = gs.stepSize() * gs.groupDir
        var hitEdge = false
        for (inv in gs.invaders) {
            if (!inv.alive) continue
            val nx = inv.x + step
            if (nx < INVADER_W/2 + 5f || nx > CW - INVADER_W/2 - 5f) { hitEdge = true; break }
        }
        if (hitEdge) {
            gs.groupDir *= -1
            for (inv in gs.invaders) { if (inv.alive) inv.y += INVADER_H * 0.6f }
        } else {
            for (inv in gs.invaders) { if (inv.alive) inv.x += step }
        }
        for (inv in gs.invaders) {
            if (inv.alive && inv.y + INVADER_H/2 >= PLAYER_Y) {
                gs.lives = 0; gs.phase = Phase.HIT; gs.phaseTimer = 0; return
            }
        }
    }

    // ── Enemy fire ────────────────────────────────────────────────────────
    val chance = gs.shootChance()
    for (inv in gs.invaders) {
        if (inv.alive && Random.nextFloat() < chance)
            gs.enemyBullets.add(Bullet(inv.x, inv.y + INVADER_H/2, ENEMY_BULLET_SPD))
    }

    // ── Enemy bullets ─────────────────────────────────────────────────────
    val ebIter = gs.enemyBullets.iterator()
    outer@ while (ebIter.hasNext()) {
        val b = ebIter.next(); b.y += b.dy
        if (b.y > CH + BULLET_H) { ebIter.remove(); continue }

        for (shield in gs.shields) {
            val sy = CH - 160f
            for (row in 0 until SHIELD_ROWS) for (col in 0 until SHIELD_COLS) {
                if (!shield.blocks[row][col]) continue
                if (rectsOverlap(b.x - BULLET_W/2, b.y, BULLET_W, BULLET_H,
                        shield.originX + col*BLOCK, sy + row*BLOCK, BLOCK, BLOCK)) {
                    eraseShield(shield, col, row); ebIter.remove(); continue@outer
                }
            }
        }
        if (rectsOverlap(b.x - BULLET_W/2, b.y, BULLET_W, BULLET_H,
                gs.playerX - PLAYER_W/2, PLAYER_Y - PLAYER_W/2, PLAYER_W, PLAYER_W)) {
            gs.lives--; gs.phase = Phase.HIT; gs.phaseTimer = 0
            ebIter.remove()
        }
    }
}

private fun eraseShield(shield: Shield, hitCol: Int, hitRow: Int) {
    for (dr in -ERASE_R..ERASE_R) for (dc in -ERASE_R..ERASE_R) {
        val r = hitRow + dr; val c = hitCol + dc
        if (r in 0 until SHIELD_ROWS && c in 0 until SHIELD_COLS) shield.blocks[r][c] = false
    }
}

// ── Drawing ───────────────────────────────────────────────────────────────────

private fun drawGame(scope: DrawScope, gs: GameState, tick: Long, paused: Boolean) {
    with(scope) {
        // Background gradient
        drawRect(
            Brush.linearGradient(listOf(Color(0xFF07072a), Color(0xFF0a1628)),
                Offset.Zero, Offset(0f, CH)),
            size = Size(CW, CH),
        )

        // Stars (seed-fixed, always same positions)
        val rng = Random(42)
        repeat(35) {
            drawRect(Color.White.copy(alpha = 0.55f),
                topLeft = Offset(rng.nextFloat() * CW, rng.nextFloat() * CH),
                size = Size(1.5f, 1.5f))
        }

        // Shields
        val shieldTopY = CH - 160f
        for (shield in gs.shields) {
            for (row in 0 until SHIELD_ROWS) for (col in 0 until SHIELD_COLS) {
                if (!shield.blocks[row][col]) continue
                val shade = 0.5f + row.toFloat() / SHIELD_ROWS * 0.5f
                drawRect(
                    color = Color(0xFFF97316).copy(alpha = shade),
                    topLeft = Offset(shield.originX + col * BLOCK, shieldTopY + row * BLOCK),
                    size = Size(BLOCK - 0.5f, BLOCK - 0.5f),
                )
            }
        }

        // Player bullets (purple glow)
        for (b in gs.playerBullets)
            drawRect(
                Brush.linearGradient(listOf(PurpleBullet, Color(0xFF7C3AED)),
                    Offset(b.x, b.y), Offset(b.x, b.y + BULLET_H)),
                topLeft = Offset(b.x - BULLET_W / 2, b.y),
                size = Size(BULLET_W, BULLET_H),
            )

        // Enemy bullets (orange)
        for (b in gs.enemyBullets)
            drawRect(
                Brush.linearGradient(listOf(OrangeBullet, Color(0xFFEA580C)),
                    Offset(b.x, b.y), Offset(b.x, b.y + BULLET_H)),
                topLeft = Offset(b.x - BULLET_W / 2, b.y),
                size = Size(BULLET_W, BULLET_H),
            )

        // Emojis via native canvas (inherits the scale transform set by caller)
        drawIntoCanvas { c ->
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            for (inv in gs.invaders) {
                if (!inv.alive) continue
                paint.textSize = 22f
                c.nativeCanvas.drawText(EMOJIS[inv.row], inv.x, inv.y + 9f, paint)
            }
            val showPlayer = gs.phase != Phase.HIT || (tick / 6) % 2 == 0L
            if (showPlayer) {
                paint.textSize = 38f
                c.nativeCanvas.drawText("🐙", gs.playerX, PLAYER_Y + 15f, paint)
            }
        }

        // Phase overlays
        when {
            gs.phase == Phase.WAVE_CLEAR ->
                drawOverlay(this, "🌊", "Welle ${gs.wave} geschafft!", "Nächste Welle…")
            gs.phase == Phase.GAME_OVER  ->
                drawOverlay(this, "💀", "Game Over", "Score: ${gs.score}")
            paused && gs.phase == Phase.PLAYING ->
                drawOverlay(this, "⏸", "Pause", "Tippe ▶ zum Weiterspielen")
        }
    }
}

private fun drawOverlay(scope: DrawScope, emoji: String, title: String, subtitle: String) {
    with(scope) {
        drawRect(Color.Black.copy(alpha = 0.70f), size = Size(CW, CH))
        // Card background
        val cardW = 300f; val cardH = 110f
        val cardX = (CW - cardW) / 2f; val cardY = CH / 2f - cardH / 2f
        drawRoundRect(
            color = Color(0xFF1E3050),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
        )
        drawRoundRect(
            color = Color(0xFFA855F7).copy(alpha = 0.4f),
            topLeft = Offset(cardX, cardY),
            size = Size(cardW, cardH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
        )
        drawIntoCanvas { c ->
            val emojiPaint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                textSize = 28f
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
                textSize = 22f
            }
            val subPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                textSize = 15f
            }
            val cx = CW / 2f
            c.nativeCanvas.drawText(emoji, cx, cardY + 34f, emojiPaint)
            c.nativeCanvas.drawText(title, cx, cardY + 64f, titlePaint)
            c.nativeCanvas.drawText(subtitle, cx, cardY + 88f, subPaint)
        }
    }
}


@Composable
private fun HudCell(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 8.sp, color = TextMuted)
    }
}

@Composable
private fun HudBar2(label: String, value: Int, max: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(26.dp).height(5.dp)
                .background(Surface2Dark, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(26.dp * (value.toFloat() / max))
                    .background(color, RoundedCornerShape(3.dp)),
            )
        }
    }
}

// ── Control button ────────────────────────────────────────────────────────────

@Composable
private fun ControlButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) onDown() else onUp()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 30.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

