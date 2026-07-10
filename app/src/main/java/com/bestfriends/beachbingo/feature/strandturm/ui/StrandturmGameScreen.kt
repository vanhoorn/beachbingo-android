package com.bestfriends.beachbingo.feature.strandturm.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ── Constants ─────────────────────────────────────────────────────────────────

private const val VIRT_W   = 400f
private const val VIRT_H   = 580f
private const val PW       = 16f
private const val PH       = 26f
private const val GRAVITY  = 0.48f
private const val MAX_FALL = 11f
private const val WALK_SPD = 2.2f
private const val JUMP_VY  = -9.8f
private const val CLIMB_SPD = 1.7f
private const val COCO_R   = 8f
private const val BONUS_START     = 5000
private const val BONUS_DEC_FRAMES = 6
private const val GOAL_X   = 340f
private const val MOEVE_X  = 55f
private const val MOEVE_Y  = 48f
private const val LADD_W   = 18f
private const val PLAT_H   = 11f

private val StrandturmRed = Color(0xFFDC2626)
private val BgCanvas      = Color(0xFF0A1628)

private data class Plat(val x: Float, val y: Float, val w: Float)
private data class Ladd(val cx: Float, val y1: Float, val y2: Float)

private val PLATS = listOf(
    Plat(10f, 505f, 380f), // P0 bottom / start
    Plat(10f, 420f, 360f), // P1
    Plat(10f, 335f, 360f), // P2
    Plat(10f, 250f, 360f), // P3
    Plat(10f, 165f, 360f), // P4
    Plat(10f,  80f, 380f), // P5 top / goal
)
private val LADDERS = listOf(
    Ladd(355f, 420f, 505f),
    Ladd( 25f, 335f, 420f),
    Ladd(355f, 250f, 335f),
    Ladd( 25f, 165f, 250f),
    Ladd(355f,  80f, 165f),
)
private val ROLL_DIR = floatArrayOf(-1f, 1f, -1f, 1f, -1f, 1f)

private fun bonusForLevel(lvl: Int)  = max(400, BONUS_START - (lvl - 1) * 200)
private fun spawnInterval(lvl: Int)  = max(80,  240          - (lvl - 1) * 20)
private fun cocoSpeed(lvl: Int)      = 1.0f + min(4, lvl - 1) * 0.15f

// ── Game state ────────────────────────────────────────────────────────────────

private class Coco(var id: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, var platIdx: Int)

private class StrandturmState {
    // Player
    var px = 50f;  var py = 505f
    var pvx = 0f;  var pvy = 0f
    var ponGround   = true
    var ponLadder   = false
    var pladderIdx  = -1
    var pfacing     = 1
    var panimTick   = 0
    var pinvTimer   = 0

    // Input flags (written from UI thread, read in game loop on same thread via withFrameNanos)
    var leftHeld    = false
    var rightHeld   = false
    var upHeld      = false
    var downHeld    = false
    var jumpPressed = false   // edge-triggered, consumed once per press

    // Coconuts
    val cocos      = mutableListOf<Coco>()
    var cocoIdCtr  = 0
    var cocoSpawnAcc = 0

    // Compose-observable values (read by Canvas via renderTick + by HUD composables)
    var score      by mutableIntStateOf(0)
    var lives      by mutableIntStateOf(3)
    var level      by mutableIntStateOf(1)
    var bonusTimer by mutableIntStateOf(bonusForLevel(1))
    var phase      by mutableStateOf("PLAYING")   // PLAYING | LIFE_LOST | LEVEL_COMPLETE | GAME_OVER

    // Internal
    var bonusTickAcc = 0
    var phaseTimer   = 0
    var totalFrame   = 0

    fun resetPlayer() {
        px = 50f; py = 505f; pvx = 0f; pvy = 0f
        ponGround = true; ponLadder = false; pladderIdx = -1
    }

    fun softReset() {           // after life lost
        resetPlayer()
        cocos.clear(); cocoSpawnAcc = 0
        bonusTimer = bonusForLevel(level); bonusTickAcc = 0
        pinvTimer  = 120
        phase      = "PLAYING"
    }

    fun hardReset(newLevel: Int, newLives: Int, newScore: Int) {  // new level
        resetPlayer()
        pfacing = 1; panimTick = 0; pinvTimer = 0
        cocos.clear(); cocoIdCtr = 0; cocoSpawnAcc = 0
        score = newScore; lives = newLives; level = newLevel
        bonusTimer = bonusForLevel(newLevel); bonusTickAcc = 0
        phase = "PLAYING"; phaseTimer = 0; totalFrame = 0
    }

    fun loseLife() {
        lives--
        if (lives <= 0) {
            phase = "GAME_OVER"; phaseTimer = 9999
        } else {
            phase = "LIFE_LOST"; phaseTimer = 90
        }
    }

    fun step() {
        // Phase countdown
        if (phase != "PLAYING") {
            phaseTimer--
            if (phaseTimer <= 0) {
                when (phase) {
                    "LEVEL_COMPLETE" -> hardReset(level + 1, lives, score)
                    "LIFE_LOST"      -> softReset()
                }
            }
            return
        }

        totalFrame++

        // ── Bonus timer ────────────────────────────────────────────────────
        bonusTickAcc++
        if (bonusTickAcc >= BONUS_DEC_FRAMES) {
            bonusTickAcc = 0
            bonusTimer = max(0, bonusTimer - 10)
            if (bonusTimer == 0) { loseLife(); return }
        }

        // ── Spawn coconut ──────────────────────────────────────────────────
        cocoSpawnAcc++
        if (cocoSpawnAcc >= spawnInterval(level)) {
            cocoSpawnAcc = 0
            cocos.add(Coco(cocoIdCtr++, MOEVE_X + 35f, PLATS[5].y - COCO_R, cocoSpeed(level), 0f, 5))
        }

        // ── Player physics ─────────────────────────────────────────────────
        val wasOnGround = ponGround

        if (!ponLadder) {
            pvy = if (!wasOnGround) min(pvy + GRAVITY, MAX_FALL) else 0f

            if (jumpPressed && wasOnGround) { pvy = JUMP_VY; ponGround = false }
            jumpPressed = false

            when {
                leftHeld  -> { pvx = -WALK_SPD; pfacing = -1 }
                rightHeld -> { pvx =  WALK_SPD; pfacing =  1 }
                else      -> pvx = 0f
            }

            if (upHeld && wasOnGround) {
                for (i in LADDERS.indices) {
                    val l = LADDERS[i]
                    if (abs(px - l.cx) <= LADD_W / 2 + 4 && abs(py - l.y2) <= 8) {
                        ponLadder = true; pladderIdx = i; ponGround = false; pvx = 0f; pvy = 0f; break
                    }
                }
            }
            if (downHeld && wasOnGround) {
                for (i in LADDERS.indices) {
                    val l = LADDERS[i]
                    if (abs(px - l.cx) <= LADD_W / 2 + 4 && abs(py - l.y1) <= 5) {
                        ponLadder = true; pladderIdx = i; ponGround = false; pvx = 0f; pvy = CLIMB_SPD; break
                    }
                }
            }
        } else {
            jumpPressed = false
            pvx = 0f
            pvy = when { upHeld -> -CLIMB_SPD; downHeld -> CLIMB_SPD; else -> 0f }
            if ((leftHeld || rightHeld) && ponGround) {
                ponLadder = false; pladderIdx = -1
                pvx = if (leftHeld) -WALK_SPD else WALK_SPD
                pfacing = if (leftHeld) -1 else 1
            }
        }

        // ── Move player ────────────────────────────────────────────────────
        val prevPY = py
        px += pvx
        py += pvy
        px = px.coerceIn(PW / 2, VIRT_W - PW / 2)

        // ── Platform collision ─────────────────────────────────────────────
        if (!ponLadder) {
            ponGround = false
            for (p in PLATS) {
                if (px + PW / 2 > p.x && px - PW / 2 < p.x + p.w) {
                    if (pvy >= 0 && prevPY <= p.y + 1 && py >= p.y) {
                        py = p.y; pvy = 0f; ponGround = true; break
                    }
                }
            }
        }

        // ── Ladder exit ────────────────────────────────────────────────────
        if (ponLadder && pladderIdx >= 0) {
            val l = LADDERS[pladderIdx]
            when {
                py <= l.y1 -> { py = l.y1; ponLadder = false; pladderIdx = -1; ponGround = true; pvy = 0f }
                py >= l.y2 -> { py = l.y2; ponLadder = false; pladderIdx = -1; ponGround = true; pvy = 0f }
            }
        }

        // ── Animation tick ─────────────────────────────────────────────────
        if ((pvx != 0f || (ponLadder && pvy != 0f)) && totalFrame % 8 == 0) panimTick++

        // ── Fall off bottom ────────────────────────────────────────────────
        if (py > VIRT_H + 40) { loseLife(); return }

        // ── Goal reached ───────────────────────────────────────────────────
        if (py <= PLATS[5].y + 2 && px >= GOAL_X) {
            score += 300 + bonusTimer
            phase = "LEVEL_COMPLETE"; phaseTimer = 150
            return
        }

        // ── Invincibility countdown ────────────────────────────────────────
        if (pinvTimer > 0) pinvTimer--

        // ── Coconut physics ────────────────────────────────────────────────
        val spd = cocoSpeed(level)
        val toRemove = mutableListOf<Int>()

        for (ci in cocos.indices) {
            val c = cocos[ci]
            if (c.platIdx >= 0) {
                val p = PLATS[c.platIdx]
                c.vx = ROLL_DIR[c.platIdx] * spd
                c.x += c.vx
                c.y  = p.y - COCO_R
                if (c.x < p.x - COCO_R || c.x > p.x + p.w + COCO_R) { c.platIdx = -1; c.vy = 1f }
            } else {
                c.vy = min(c.vy + 0.6f, 14f)
                c.x += c.vx * 0.4f
                c.y += c.vy
                for (pi in PLATS.indices) {
                    val p = PLATS[pi]
                    if (c.x > p.x && c.x < p.x + p.w && c.y + COCO_R >= p.y && c.y + COCO_R <= p.y + COCO_R + 8 && c.vy > 0) {
                        c.y = p.y - COCO_R; c.vy = 0f; c.vx = ROLL_DIR[pi] * spd; c.platIdx = pi; break
                    }
                }
                if (c.y > VIRT_H + 30) toRemove.add(ci)
            }
            if (pinvTimer == 0) {
                val dx = abs(c.x - px)
                val dy = abs(c.y - (py - PH / 2))
                if (dx < PW / 2 + COCO_R - 2 && dy < PH / 2 + COCO_R - 2) { loseLife(); return }
            }
        }
        for (i in toRemove.reversed()) cocos.removeAt(i)
    }
}

// ── Draw helpers ──────────────────────────────────────────────────────────────

private fun DrawScope.drawPlatform(p: Plat, s: Float) {
    val brown      = Color(0xFF7C3F1A)
    val highlight  = Color(0xFFA05A2C)
    val shadow     = Color(0xFF4A2409)
    val grain      = Color(0xFF6B3416)
    drawRect(brown,     Offset(p.x * s, p.y * s), Size(p.w * s, PLAT_H * s))
    drawRect(highlight, Offset(p.x * s, p.y * s), Size(p.w * s, 2f * s))
    drawRect(shadow,    Offset(p.x * s, (p.y + PLAT_H - 2) * s), Size(p.w * s, 2f * s))
    var gx = p.x + 8f
    while (gx < p.x + p.w - 4f) {
        drawLine(grain, Offset(gx * s, (p.y + 2) * s), Offset(gx * s, (p.y + PLAT_H - 2) * s), strokeWidth = 0.5f * s)
        gx += 16f
    }
}

private fun DrawScope.drawLadder(l: Ladd, s: Float) {
    val railColor = Color(0xFF8B6534)
    val lx1 = (l.cx - LADD_W / 2 + 2) * s
    val lx2 = (l.cx + LADD_W / 2 - 2) * s
    drawLine(railColor, Offset(lx1, l.y1 * s), Offset(lx1, l.y2 * s), strokeWidth = 2.5f * s)
    drawLine(railColor, Offset(lx2, l.y1 * s), Offset(lx2, l.y2 * s), strokeWidth = 2.5f * s)
    var ry = l.y2 - 9f
    while (ry >= l.y1 + 4f) {
        drawLine(railColor, Offset((l.cx - LADD_W / 2 + 3) * s, ry * s), Offset((l.cx + LADD_W / 2 - 3) * s, ry * s), strokeWidth = 1.8f * s)
        ry -= 11f
    }
}

private fun DrawScope.drawMoeve(frame: Int, s: Float) {
    val mx = MOEVE_X; val my = MOEVE_Y
    val wing = if (frame % 60 < 30) -6f else 0f

    val bodyColor  = Color(0xFFE2E8F0)
    val wingColor  = Color(0xFFCBD5E1)
    val beakColor  = Color(0xFFF97316)
    val eyeColor   = Color(0xFF0F172A)

    // Body ellipse
    drawOval(bodyColor, Offset((mx - 20) * s, (my + 8 - 11) * s), Size(40f * s, 22f * s))

    // Left wing
    val lwPath = Path().apply {
        moveTo((mx - 5) * s, (my + 6) * s)
        quadraticBezierTo((mx - 26) * s, (my + wing) * s, (mx - 36) * s, (my + 3 + wing) * s)
        quadraticBezierTo((mx - 26) * s, (my + 10) * s,  (mx - 5) * s,  (my + 14) * s)
        close()
    }
    drawPath(lwPath, wingColor)

    // Right wing
    val rwPath = Path().apply {
        moveTo((mx + 5) * s, (my + 6) * s)
        quadraticBezierTo((mx + 26) * s, (my + wing) * s, (mx + 36) * s, (my + 3 + wing) * s)
        quadraticBezierTo((mx + 26) * s, (my + 10) * s,   (mx + 5) * s,  (my + 14) * s)
        close()
    }
    drawPath(rwPath, wingColor)

    // Beak triangle
    val beakPath = Path().apply {
        moveTo((mx + 18) * s, (my + 7) * s)
        lineTo((mx + 26) * s, (my + 4) * s)
        lineTo((mx + 18) * s, (my + 13) * s)
        close()
    }
    drawPath(beakPath, beakColor)

    // Eye
    drawCircle(eyeColor, 2.5f * s, Offset((mx + 10) * s, (my + 5) * s))
    drawCircle(Color.White, 0.9f * s, Offset((mx + 11) * s, (my + 4.5f) * s))
}

private fun DrawScope.drawPlayer(gs: StrandturmState, s: Float) {
    if (gs.pinvTimer > 0 && (gs.totalFrame / 5) % 2 == 0) return

    val px = gs.px; val py = gs.py
    val f  = gs.pfacing

    val capColor  = Color(0xFFFBBF24)
    val headColor = Color(0xFFFDE68A)
    val legColor  = Color(0xFFFDE68A)

    // Swim cap
    drawOval(capColor, Offset((px - 7) * s, (py - PH + 5 - 5.5f) * s), Size(14f * s, 11f * s))
    // Head
    drawCircle(headColor, 7f * s, Offset(px * s, (py - PH + 12) * s))
    // Torso
    drawRect(StrandturmRed, Offset((px - 5) * s, (py - PH + 18) * s), Size(10f * s, 11f * s))

    // Legs
    val legSW = 3f * s
    if (gs.ponLadder) {
        drawLine(legColor, Offset((px - 5) * s, (py - PH + 21) * s), Offset((px - 10) * s, (py - PH + 16) * s), strokeWidth = legSW)
        drawLine(legColor, Offset((px + 5) * s, (py - PH + 21) * s), Offset((px + 10) * s, (py - PH + 16) * s), strokeWidth = legSW)
        val legOff = if (gs.panimTick % 2 == 0) 3f else -3f
        drawLine(legColor, Offset((px - 3) * s, (py - PH + 29) * s), Offset((px - 3 + legOff) * s, py * s), strokeWidth = legSW)
        drawLine(legColor, Offset((px + 3) * s, (py - PH + 29) * s), Offset((px + 3 - legOff) * s, py * s), strokeWidth = legSW)
    } else if (gs.pvy < -1f) {
        drawLine(legColor, Offset((px - 4) * s, (py - PH + 29) * s), Offset((px - 7) * s, (py - 5) * s), strokeWidth = legSW)
        drawLine(legColor, Offset((px + 4) * s, (py - PH + 29) * s), Offset((px + 7) * s, (py - 5) * s), strokeWidth = legSW)
    } else {
        val swing = if (gs.panimTick % 2 == 0) 5f else -5f
        drawLine(legColor, Offset((px - 3) * s, (py - PH + 29) * s), Offset((px - 3 + swing * f) * s, py * s), strokeWidth = legSW)
        drawLine(legColor, Offset((px + 3) * s, (py - PH + 29) * s), Offset((px + 3 - swing * f) * s, py * s), strokeWidth = legSW)
    }
}

private fun DrawScope.drawCoco(c: Coco, s: Float) {
    val mainColor = Color(0xFF5C2D0A)
    val spotColor = Color(0xFF3D1A06)
    val hiColor   = Color(0x21FFFFFF)
    drawCircle(mainColor, COCO_R * s, Offset(c.x * s, c.y * s))
    drawCircle(spotColor, COCO_R * 0.38f * s, Offset((c.x - 2) * s, (c.y - 2) * s))
    drawCircle(spotColor, COCO_R * 0.30f * s, Offset((c.x + 3) * s, (c.y + 2) * s))
    drawCircle(spotColor, COCO_R * 0.33f * s, Offset((c.x - 1) * s, (c.y + 3) * s))
    drawCircle(hiColor,   COCO_R * 0.38f * s, Offset((c.x - 2) * s, (c.y - 3) * s))
}

private fun DrawScope.drawGoal(s: Float) {
    val goalX = GOAL_X
    val goalY = PLATS[5].y
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize  = 22f * s
        }
        canvas.nativeCanvas.drawText("🛟", (goalX + 15) * s, (goalY - 16) * s + paint.textSize * 0.4f, paint)
    }
}

private fun DrawScope.drawGame(gs: StrandturmState, s: Float) {
    // Background
    drawRect(BgCanvas, size = Size(VIRT_W * s, VIRT_H * s))

    // Ocean gradient at bottom
    drawRect(
        Color(0x260EA5E9),
        Offset(0f, (VIRT_H - 60) * s),
        Size(VIRT_W * s, 60f * s),
    )

    // Platforms
    for (p in PLATS) drawPlatform(p, s)
    // Ladders
    for (l in LADDERS) drawLadder(l, s)
    // Goal
    drawGoal(s)
    // Möwe
    drawMoeve(gs.totalFrame, s)
    // Coconuts
    for (c in gs.cocos) drawCoco(c, s)
    // Player
    drawPlayer(gs, s)
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun StrandturmGameScreen(
    controlMode: String,
    onNavigateToResults: (score: Int, level: Int, highScore: Int, bestLevel: Int, newHighScore: Boolean, newBestLevel: Boolean) -> Unit,
    onNavigateToLobby: () -> Unit,
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid

    val gs = remember { StrandturmState() }

    var renderTick     by remember { mutableLongStateOf(0L) }
    var paused         by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showGameOver   by remember { mutableStateOf(false) }
    var savedHighScore by remember { mutableIntStateOf(0) }
    var savedBestLevel by remember { mutableIntStateOf(0) }
    var isNewHighScore by remember { mutableStateOf(false) }
    var isNewBestLevel by remember { mutableStateOf(false) }

    // Game loop at 60fps
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { _ ->
                if (!paused && gs.phase != "GAME_OVER") gs.step()
                renderTick++
            }

            if (gs.phase == "GAME_OVER" && !showGameOver) {
                val finalScore = gs.score
                val finalLevel = gs.level
                if (uid != null) {
                    try {
                        val snap   = firestore.collection("users").document(uid).get().await()
                        val prev   = snap.getLong("strandturmHighScore")?.toInt() ?: 0
                        val prevLv = snap.getLong("strandturmBestLevel")?.toInt() ?: 0
                        isNewHighScore = finalScore > prev
                        isNewBestLevel = finalLevel > prevLv
                        savedHighScore = max(prev, finalScore)
                        savedBestLevel = max(prevLv, finalLevel)
                        val updates = mutableMapOf<String, Any>()
                        if (isNewHighScore) updates["strandturmHighScore"] = finalScore.toLong()
                        if (isNewBestLevel) updates["strandturmBestLevel"] = finalLevel.toLong()
                        if (updates.isNotEmpty()) {
                            firestore.collection("users").document(uid).update(updates).await()
                        }
                    } catch (_: Exception) {
                        savedHighScore = finalScore; savedBestLevel = finalLevel
                    }
                }
                showGameOver = true
                break
            }
        }
    }

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // HUD bar
        GameHudBar(
            paused        = paused,
            onPauseToggle = { if (gs.phase == "PLAYING" || paused) paused = !paused },
            onQuit        = {
                if (showGameOver) onNavigateToLobby()
                else { paused = true; showQuitDialog = true }
            },
        ) {
            Text("${gs.score}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = StrandturmRed)
            Text("Pts", fontSize = 10.sp, color = TextMuted)
            Spacer(Modifier.width(8.dp))
            Text("Lv.${gs.level}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Spacer(Modifier.width(8.dp))
            repeat(3) { i ->
                Text(if (i < gs.lives) "❤️" else "🖤", fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "⏱ ${gs.bonusTimer}",
                fontSize = 12.sp,
                color = if (gs.bonusTimer < 1000) StrandturmRed else TextMuted,
            )
        }

        // Canvas area
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val pxW   = with(density) { maxWidth.toPx() }
                val scale = pxW / VIRT_W

                val touchMod = if (controlMode == "TOUCH") {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val wasUp = gs.upHeld
                                var newLeft = false; var newRight = false; var newUp = false
                                event.changes.filter { it.pressed }.forEach { ch ->
                                    val ly = ch.position.y
                                    val lx = ch.position.x
                                    when {
                                        ly < size.height * 0.35f -> newUp    = true
                                        lx < size.width  / 2f   -> newLeft  = true
                                        else                     -> newRight = true
                                    }
                                }
                                if (newUp && !wasUp) gs.jumpPressed = true
                                gs.upHeld    = newUp
                                gs.leftHeld  = newLeft
                                gs.rightHeld = newRight
                            }
                        }
                    }
                } else Modifier

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { (VIRT_H * scale).toDp() })
                        .then(touchMod),
                ) {
                    @Suppress("UNUSED_EXPRESSION")
                    renderTick
                    drawGame(gs, scale)
                }
            }

            // Pause overlay
            if (paused && !showGameOver) {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("⏸", fontSize = 40.sp)
                            Text("Pause", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Drücke ⏸ zum Weiterspielen", fontSize = 13.sp, color = TextMuted)
                        }
                    }
                }
            }

            // LIFE_LOST overlay (brief flash)
            if (gs.phase == "LIFE_LOST") {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("💥", fontSize = 40.sp)
                            Text("Autsch!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = StrandturmRed)
                            Text("Noch ${gs.lives} Leben", fontSize = 14.sp, color = TextMuted)
                        }
                    }
                }
            }

            // LEVEL_COMPLETE overlay
            if (gs.phase == "LEVEL_COMPLETE") {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("🎉", fontSize = 40.sp)
                            Text("Geschafft!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SandGold)
                            Text("+${gs.bonusTimer} Bonus", fontSize = 14.sp, color = TextMuted)
                            Text("Level ${gs.level + 1} startet …", fontSize = 12.sp, color = TextSub)
                        }
                    }
                }
            }

            // GAME_OVER overlay
            if (showGameOver) {
                Box(
                    modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(260.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("🗼", fontSize = 40.sp)
                            Text("Game Over", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            if (isNewHighScore) Text("🏆 Neuer Rekord!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SandGold)
                            Text("${gs.score}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = StrandturmRed)
                            Text("Punkte · Level ${gs.level}", fontSize = 12.sp, color = TextMuted)
                            Button(
                                onClick = {
                                    onNavigateToResults(
                                        gs.score, gs.level,
                                        savedHighScore, savedBestLevel,
                                        isNewHighScore, isNewBestLevel,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StrandturmRed),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Weiter →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // D-Pad (BUTTONS mode)
        if (controlMode == "BUTTONS") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val btnSize = 54.dp
                val btnMod  = Modifier.size(btnSize)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.size(btnSize))
                        HoldButton("▲", btnMod,
                            onPress   = { gs.upHeld = true; gs.jumpPressed = true },
                            onRelease = { gs.upHeld = false },
                        )
                        Spacer(Modifier.size(btnSize))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HoldButton("◄", btnMod,
                            onPress   = { gs.leftHeld = true },
                            onRelease = { gs.leftHeld = false },
                        )
                        Spacer(Modifier.size(btnSize))
                        HoldButton("►", btnMod,
                            onPress   = { gs.rightHeld = true },
                            onRelease = { gs.rightHeld = false },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Spacer(Modifier.size(btnSize))
                        HoldButton("▼", btnMod,
                            onPress   = { gs.downHeld = true },
                            onRelease = { gs.downHeld = false },
                        )
                        Spacer(Modifier.size(btnSize))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Links / Rechts tippen  ·  Oben tippen = Springen / Klettern",
                    fontSize = 11.sp, color = TextMuted,
                )
            }
        }
    }

    if (showQuitDialog) {
        QuitConfirmDialog(
            message   = "Score: ${gs.score} Pts · Level ${gs.level}. Fortschritt geht verloren.",
            onConfirm = onNavigateToLobby,
            onDismiss = { showQuitDialog = false; paused = false },
        )
    }
}

@Composable
private fun HoldButton(
    label: String,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            when {
                                change.pressed && !change.previousPressed  -> onPress()
                                !change.pressed && change.previousPressed  -> onRelease()
                            }
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 22.sp, color = TextPrimary)
    }
}
