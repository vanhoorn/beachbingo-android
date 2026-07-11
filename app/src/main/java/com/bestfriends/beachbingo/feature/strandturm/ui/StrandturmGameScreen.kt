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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ── Constants ─────────────────────────────────────────────────────────────────

private const val VIRT_W   = 400f
private const val VIRT_H   = 580f
private const val PW       = 16f
private const val PH       = 26f
private const val GRAVITY  = 0.48f
private const val MAX_FALL = 11f
private const val WALK_SPD = 2.2f
private const val JUMP_VY  = -7.5f
private const val CLIMB_SPD = 1.7f
private const val COCO_R   = 8f
private const val BONUS_START      = 5000
private const val BONUS_DEC_FRAMES = 6
private const val HAMMER_DURATION  = 300
private const val GOAL_X   = 340f
private const val MOEVE_X  = 55f
private const val LADD_W   = 18f
private const val PLAT_H   = 11f
private const val HAMMER_FLOAT     = 30f  // px above platform – requires a jump
private const val EXPLOSION_FRAMES = 18

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
    Ladd(130f, 420f, 505f), // extra
    Ladd( 25f, 335f, 420f),
    Ladd(255f, 335f, 420f), // extra
    Ladd(355f, 250f, 335f),
    Ladd(130f, 250f, 335f), // extra
    Ladd( 25f, 165f, 250f),
    Ladd(255f, 165f, 250f), // extra
    Ladd(355f,  80f, 165f),
    Ladd(130f,  80f, 165f), // extra
)
private val ROLL_DIR = floatArrayOf(-1f, 1f, -1f, 1f, -1f, 1f)

private fun getLevelType(lvl: Int) = ((lvl - 1) % 4) + 1
private val LEVEL_NAMES = mapOf(
    1 to "🏗️ Die Baustelle",
    2 to "🏭 Die Zementfabrik",
    3 to "🛗 Die Aufzüge",
    4 to "🔩 Die Nieten",
)
private const val BELT_SPEED = 1.2f
private data class ConveyorBelt(val platIdx: Int, val x: Float, val w: Float, val vx: Float)
private fun getConveyorBelts(lvl: Int): List<ConveyorBelt> {
    if (getLevelType(lvl) != 2) return emptyList()
    return listOf(
        ConveyorBelt(1, 150f, 200f,  BELT_SPEED),
        ConveyorBelt(2,  20f, 200f, -BELT_SPEED),
        ConveyorBelt(3, 150f, 200f,  BELT_SPEED),
        ConveyorBelt(4,  20f, 200f, -BELT_SPEED),
    )
}

private data class HammerDef(val x: Float, val platIdx: Int)
private val HAMMER_DEFS = listOf(
    HammerDef(190f, 1),
    HammerDef(190f, 3),
)

private fun bonusForLevel(lvl: Int)  = max(400, BONUS_START - (lvl - 1) * 200)
private fun spawnInterval(lvl: Int)  = max(80,  240          - (lvl - 1) * 20)
private fun cocoSpeed(lvl: Int)      = 1.0f + min(4, lvl - 1) * 0.15f

// ── Game state ────────────────────────────────────────────────────────────────

private class Coco(var id: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, var platIdx: Int)
private data class Explosion(val id: Int, val x: Float, val y: Float, var frame: Int = 0)

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

    // Hammer power-up
    var hasHammer    = false
    var hammerTimer  = 0
    val hammerPickups = MutableList(HAMMER_DEFS.size) { false }

    // Jump-over bonus tracking
    val jumpedCocoIds = mutableSetOf<Int>()

    // Explosion effects
    val explosions    = mutableListOf<Explosion>()
    var explosionIdCtr = 0

    // Conveyor belts active this level (empty for non-L2 level types)
    val conveyorBelts = mutableListOf<ConveyorBelt>()

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
        hasHammer = false; hammerTimer = 0
        hammerPickups.fill(false)
        jumpedCocoIds.clear()
        explosions.clear()
        phase      = "PLAYING"
    }

    fun hardReset(newLevel: Int, newLives: Int, newScore: Int) {  // new level
        resetPlayer()
        pfacing = 1; panimTick = 0; pinvTimer = 0
        cocos.clear(); cocoIdCtr = 0; cocoSpawnAcc = 0
        score = newScore; lives = newLives; level = newLevel
        bonusTimer = bonusForLevel(newLevel); bonusTickAcc = 0
        phase = "PLAYING"; phaseTimer = 0; totalFrame = 0
        hasHammer = false; hammerTimer = 0
        hammerPickups.fill(false)
        jumpedCocoIds.clear()
        conveyorBelts.clear(); conveyorBelts.addAll(getConveyorBelts(newLevel))
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

        // ── Hammer pickup ─────────────────────────────────────────────────
        if (!hasHammer) {
            for (hi in HAMMER_DEFS.indices) {
                if (hammerPickups[hi]) continue
                val h = HAMMER_DEFS[hi]
                val hy = PLATS[h.platIdx].y - HAMMER_FLOAT // floated above platform
                if (abs(px - h.x) < 20f && abs(py - hy) < 18f) {
                    hasHammer = true
                    hammerTimer = HAMMER_DURATION
                    hammerPickups[hi] = true
                    score += 500
                }
            }
        } else {
            hammerTimer--
            if (hammerTimer <= 0) { hasHammer = false; hammerTimer = 0 }
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
                    if (abs(px - l.cx) <= LADD_W / 2 + 14 && abs(py - l.y2) <= 12) {
                        ponLadder = true; pladderIdx = i; ponGround = false; pvx = 0f; pvy = 0f
                        py = l.y2 - 2f
                        px = l.cx
                        break
                    }
                }
            }
            if (downHeld && wasOnGround) {
                for (i in LADDERS.indices) {
                    val l = LADDERS[i]
                    if (abs(px - l.cx) <= LADD_W / 2 + 14 && abs(py - l.y1) <= 8) {
                        ponLadder = true; pladderIdx = i; ponGround = false; pvx = 0f; pvy = CLIMB_SPD
                        px = l.cx
                        break
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

        // ── Conveyor belt effect (Level 2 mechanic) ───────────────────────
        if (ponGround && !ponLadder && conveyorBelts.isNotEmpty()) {
            for (belt in conveyorBelts) {
                if (abs(py - PLATS[belt.platIdx].y) < 2f
                    && px >= belt.x && px <= belt.x + belt.w) {
                    px = (px + belt.vx).coerceIn(PW / 2, VIRT_W - PW / 2)
                    break
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

        // ── Explosion update ───────────────────────────────────────────────
        explosions.removeAll { e -> e.frame++; e.frame >= EXPLOSION_FRAMES }

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
                if (c.x < p.x - COCO_R || c.x > p.x + p.w + COCO_R) {
                    val nextPIdx = c.platIdx - 1
                    if (nextPIdx >= 0) {
                        val np = PLATS[nextPIdx]
                        c.x = c.x.coerceIn(np.x + COCO_R, np.x + np.w - COCO_R)
                    }
                    c.y = p.y + PLAT_H + 1f
                    c.platIdx = -1; c.vy = 1f; c.vx = 0f
                }
            } else {
                c.vy = min(c.vy + 0.6f, 14f)
                c.y += c.vy
                for (pi in PLATS.indices) {
                    val p = PLATS[pi]
                    if (c.x > p.x && c.x < p.x + p.w && c.y + COCO_R >= p.y && c.y + COCO_R <= p.y + COCO_R + 8 && c.vy > 0) {
                        c.y = p.y - COCO_R; c.vy = 0f; c.vx = ROLL_DIR[pi] * spd; c.platIdx = pi; break
                    }
                }
                if (c.y > VIRT_H + 30) toRemove.add(ci)
            }

            // Jump-over bonus
            if (c.platIdx >= 0 && !ponGround && !ponLadder && !jumpedCocoIds.contains(c.id)
                && abs(c.x - px) < PW / 2 + COCO_R + 4 && py < c.y - COCO_R) {
                jumpedCocoIds.add(c.id)
                score += 100
            }

            if (pinvTimer == 0) {
                val dx = abs(c.x - px)
                val dy = abs(c.y - (py - PH / 2))
                if (dx < PW / 2 + COCO_R - 2 && dy < PH / 2 + COCO_R - 2) {
                    if (hasHammer) {
                        toRemove.add(ci)
                        score += 300
                        explosions.add(Explosion(explosionIdCtr++, c.x, c.y))
                    } else {
                        loseLife(); return
                    }
                }
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

private fun DrawScope.drawSeeloewe(frame: Int, s: Float) {
    val mx = MOEVE_X
    val gy = PLATS[5].y
    val flipRaise = kotlin.math.sin(frame * 0.1).toFloat() * 5f

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas

        fun fillEllipse(cx: Float, cy: Float, rx: Float, ry: Float, angleDeg: Float, colorArgb: Int) {
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb; style = android.graphics.Paint.Style.FILL
            }
            nc.save()
            nc.translate(cx * s, cy * s)
            nc.rotate(angleDeg)
            nc.drawOval(-rx * s, -ry * s, rx * s, ry * s, p)
            nc.restore()
        }

        fun fillCircle(cx: Float, cy: Float, r: Float, colorArgb: Int) {
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb; style = android.graphics.Paint.Style.FILL
            }
            nc.drawCircle(cx * s, cy * s, r * s, p)
        }

        fun strokeLine(x1: Float, y1: Float, x2: Float, y2: Float, colorArgb: Int, w: Float) {
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb; style = android.graphics.Paint.Style.STROKE
                strokeWidth = w * s; strokeCap = android.graphics.Paint.Cap.ROUND
            }
            nc.drawLine(x1 * s, y1 * s, x2 * s, y2 * s, p)
        }

        val dark   = android.graphics.Color.argb(255, 0x2A, 0x20, 0x18)
        val body   = android.graphics.Color.argb(255, 0x4A, 0x38, 0x28)
        val belly  = android.graphics.Color.argb(255, 0x6B, 0x52, 0x40)
        val head   = android.graphics.Color.argb(255, 0x5A, 0x45, 0x35)
        val ear    = android.graphics.Color.argb(255, 0x3D, 0x2E, 0x20)
        val snout  = android.graphics.Color.argb(255, 0x7A, 0x60, 0x50)
        val nose   = android.graphics.Color.argb(255, 0x1A, 0x0F, 0x0A)
        val eyeC   = android.graphics.Color.argb(255, 0x0A, 0x0A, 0x12)
        val brow   = android.graphics.Color.argb(255, 0x1A, 0x0F, 0x0A)
        val whisk  = android.graphics.Color.argb(255, 0xC8, 0xB8, 0xA0)

        // Hind flippers
        fillEllipse(mx - 7, gy + 5, 11f, 5f, Math.toDegrees(-0.25).toFloat(), dark)
        fillEllipse(mx + 7, gy + 5, 11f, 5f, Math.toDegrees( 0.25).toFloat(), dark)
        // Main body
        fillEllipse(mx, gy - 15, 15f, 17f, 0f, body)
        // Belly patch
        fillEllipse(mx + 2, gy - 13, 8f, 11f, 0f, belly)
        // Left front flipper
        fillEllipse(mx - 16, gy - 17, 8f, 4f, Math.toDegrees(0.6).toFloat(), dark)
        // Right front flipper (throwing pose, animated)
        fillEllipse(mx + 17, gy - 24 - flipRaise, 9f, 4f, Math.toDegrees(-0.7).toFloat(), dark)
        // Neck
        fillEllipse(mx + 4, gy - 34, 8f, 9f, Math.toDegrees(0.15).toFloat(), body)
        // Head
        fillCircle(mx + 6, gy - 46, 10f, head)
        // Ear bumps
        fillCircle(mx + 1, gy - 55, 3.5f, ear)
        fillCircle(mx + 10, gy - 54, 3f, ear)
        // Snout
        fillEllipse(mx + 13, gy - 46, 6f, 4f, 0f, snout)
        // Nose
        fillEllipse(mx + 18, gy - 46, 2.2f, 1.5f, 0f, nose)
        // Eye
        fillCircle(mx + 10, gy - 50, 3f, eyeC)
        fillCircle(mx + 11, gy - 51, 1.1f, android.graphics.Color.WHITE)
        // Grumpy brow
        strokeLine(mx + 6, gy - 54, mx + 13, gy - 52, brow, 1.5f)
        // Whiskers
        for (i in 0..3) {
            val wy = gy - 49 + i * 1.4f
            strokeLine(mx + 15, wy, mx + 28, wy - 1 + i * 0.3f, whisk, 0.9f)
            strokeLine(mx + 8,  wy, mx - 4,  wy - 0.5f + i * 0.3f, whisk, 0.9f)
        }
    }
}

private fun DrawScope.drawExplosion(e: Explosion, s: Float) {
    val t = e.frame.toFloat() / EXPLOSION_FRAMES
    val r = (4f + t * 20f) * s
    val a = 1f - t
    // Bright core
    drawCircle(Color(1f, 1f, 0.78f, a * 0.95f), r * 0.35f, Offset(e.x * s, e.y * s))
    // Orange fill
    drawCircle(Color(0.98f, 0.57f, 0.24f, a * 0.75f), r * 0.7f, Offset(e.x * s, e.y * s))
    // Red expanding ring
    drawCircle(Color(0.94f, 0.27f, 0.27f, a), r, Offset(e.x * s, e.y * s), style = Stroke(2f * s))
    // 8 flying sparks
    val unitR = r / s
    for (i in 0..7) {
        val ang = (i.toDouble() * Math.PI * 2 / 8).toFloat()
        val sx = e.x + cos(ang) * unitR * 1.4f
        val sy = e.y + sin(ang) * unitR * 1.4f
        drawCircle(Color(0.98f, 0.75f, 0.14f, a), 2.5f * (1 - t * 0.7f) * s, Offset(sx * s, sy * s))
    }
}

private fun DrawScope.drawHammerPickup(x: Float, y: Float, s: Float) {
    // Handle
    drawRect(Color(0xFF92400E), Offset((x - 2) * s, (y - 17) * s), Size(3f * s, 13f * s))
    // Head
    drawRect(Color(0xFF94A3B8), Offset((x - 7) * s, (y - 22) * s), Size(13f * s, 6f * s))
    // Glint
    drawRect(Color(0x59FFFFFF), Offset((x - 6) * s, (y - 21) * s), Size(5f * s, 2f * s))
    // Glow
    drawCircle(Color(0x40FBB124), 10f * s, Offset(x * s, (y - 16) * s))
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

    // Hammer held above head
    if (gs.hasHammer) {
        val swingUp = gs.totalFrame % 24 < 12
        val hx = px + f * 6f
        val hy = py - PH - (if (swingUp) 8f else 3f)
        drawRect(Color(0xFF92400E), Offset((hx - 1) * s, hy * s), Size(3f * s, 11f * s))
        drawRect(Color(0xFF94A3B8), Offset((hx - 6) * s, (hy - (if (swingUp) 6f else 2f)) * s), Size(12f * s, 5f * s))
        if (gs.hammerTimer > 0 && gs.totalFrame % 6 < 3) {
            drawCircle(Color(0xFFFBBF24), 2f * s, Offset((hx + 7) * s, (hy - 4) * s))
        }
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

private fun DrawScope.drawConveyorBelt(belt: ConveyorBelt, frame: Int, s: Float) {
    val py = PLATS[belt.platIdx].y
    val period = 18f
    val rawOff = (frame * 0.5f) % period
    val scrollX = if (belt.vx > 0) rawOff else period - rawOff
    // Belt surface – industrial steel-gray, 5px overlaying the platform top edge
    drawRect(Color(0xE0334155.toInt()), Offset(belt.x * s, py * s), Size(belt.w * s, 5f * s))
    // Animated diagonal stripes via native canvas clip
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.save()
        nc.clipRect(belt.x * s, py * s, (belt.x + belt.w) * s, (py + 5f) * s)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x6B94A3B8.toInt()
            style = android.graphics.Paint.Style.FILL
        }
        var sx = belt.x - period * 2 + scrollX
        while (sx < belt.x + belt.w + period) {
            val np = android.graphics.Path().apply {
                moveTo(sx * s,                     py * s)
                lineTo((sx + period * 0.55f) * s,  (py + 5f) * s)
                lineTo((sx + period) * s,           (py + 5f) * s)
                lineTo((sx + period * 0.45f) * s,   py * s)
                close()
            }
            nc.drawPath(np, paint)
            sx += period
        }
        nc.restore()
    }
    // Direction edge highlight (amber = right, blue = left)
    val edgeColor = if (belt.vx > 0) Color(0xD9FBBF24.toInt()) else Color(0xD960A5FA.toInt())
    drawLine(edgeColor, Offset(belt.x * s, py * s), Offset((belt.x + belt.w) * s, py * s), 1.5f * s)
}

private fun DrawScope.drawWanne(c: Coco, s: Float) {
    val r = COCO_R
    val bodyPath = Path().apply {
        moveTo((c.x - r + 1) * s,    (c.y - r * 0.55f) * s)
        lineTo((c.x + r - 1) * s,    (c.y - r * 0.55f) * s)
        lineTo((c.x + r - 3) * s,    (c.y + r * 0.6f) * s)
        lineTo((c.x - r + 3) * s,    (c.y + r * 0.6f) * s)
        close()
    }
    drawPath(bodyPath, Color(0xFF78716C.toInt()))
    val cementPath = Path().apply {
        moveTo((c.x - r + 2) * s,    (c.y - r * 0.55f) * s)
        lineTo((c.x + r - 2) * s,    (c.y - r * 0.55f) * s)
        lineTo((c.x + r - 3.5f) * s, (c.y - r * 0.05f) * s)
        lineTo((c.x - r + 3.5f) * s, (c.y - r * 0.05f) * s)
        close()
    }
    drawPath(cementPath, Color(0xFFA8A29E.toInt()))
    val handlePath = Path().apply {
        moveTo((c.x - r + 2) * s,    (c.y - r * 0.55f) * s)
        lineTo((c.x - r) * s,        (c.y - r * 1.15f) * s)
        lineTo((c.x + r) * s,        (c.y - r * 1.15f) * s)
        lineTo((c.x + r - 2) * s,    (c.y - r * 0.55f) * s)
    }
    drawPath(handlePath, Color(0xFF57534E.toInt()), style = Stroke(2f * s))
    drawRect(Color(0xFF44403C.toInt()), Offset((c.x - r + 3) * s, (c.y + r * 0.25f) * s),
        Size((r - 3) * 2 * s, r * 0.35f * s))
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
    // Conveyor belts (overlaid on platform surface, under ladders – Level 2 mechanic)
    for (belt in gs.conveyorBelts) drawConveyorBelt(belt, gs.totalFrame, s)
    // Ladders
    for (l in LADDERS) drawLadder(l, s)
    // Goal
    drawGoal(s)
    // Hammer pickups (floated above platform – jump to reach)
    for (hi in HAMMER_DEFS.indices) {
        if (!gs.hammerPickups[hi]) {
            val h = HAMMER_DEFS[hi]
            drawHammerPickup(h.x, PLATS[h.platIdx].y - HAMMER_FLOAT, s)
        }
    }
    // Seelöwe
    drawSeeloewe(gs.totalFrame, s)
    // Obstacles (coconuts in L1, cement troughs in L2)
    val levelType = getLevelType(gs.level)
    for (c in gs.cocos) if (levelType == 2) drawWanne(c, s) else drawCoco(c, s)
    // Explosions (above coconuts, below player)
    for (e in gs.explosions) drawExplosion(e, s)
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
                            LEVEL_NAMES[getLevelType(gs.level + 1)]?.let { name ->
                                Text(name, fontSize = 13.sp, color = SandGold)
                            }
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

        // Controls
        when (controlMode) {
            "BUTTONS" -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val sideW = 84.dp
                val sideH = 62.dp
                val midW  = 62.dp
                val midH  = 58.dp
                val gap   = 5.dp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(sideW).height(midH))
                        HoldButton("▲", Modifier.width(midW).height(midH),
                            onPress = { gs.upHeld = true; gs.jumpPressed = true }, onRelease = { gs.upHeld = false })
                        Spacer(Modifier.width(sideW).height(midH))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        HoldButton("◄", Modifier.width(sideW).height(sideH),
                            onPress = { gs.leftHeld = true }, onRelease = { gs.leftHeld = false })
                        Spacer(Modifier.width(midW).height(sideH))
                        HoldButton("►", Modifier.width(sideW).height(sideH),
                            onPress = { gs.rightHeld = true }, onRelease = { gs.rightHeld = false })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(sideW).height(midH))
                        HoldButton("▼", Modifier.width(midW).height(midH),
                            onPress = { gs.downHeld = true }, onRelease = { gs.downHeld = false })
                        Spacer(Modifier.width(sideW).height(midH))
                    }
                }
            }
            "SPLIT" -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left thumb: ◄ ►
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HoldButton("◄", Modifier.size(78.dp, 62.dp),
                        onPress = { gs.leftHeld = true }, onRelease = { gs.leftHeld = false })
                    HoldButton("►", Modifier.size(78.dp, 62.dp),
                        onPress = { gs.rightHeld = true }, onRelease = { gs.rightHeld = false })
                }
                // Right thumb: ▲ ▼
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HoldButton("▲", Modifier.size(72.dp, 62.dp),
                        onPress = { gs.upHeld = true; gs.jumpPressed = true }, onRelease = { gs.upHeld = false })
                    HoldButton("▼", Modifier.size(72.dp, 62.dp),
                        onPress = { gs.downHeld = true }, onRelease = { gs.downHeld = false })
                }
            }
            else -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Links / Rechts tippen  ·  Oben tippen = Springen / Klettern",
                    fontSize = 11.sp, color = TextMuted)
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
                while (true) {
                    val down: PointerInputChange = awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                    }
                    down.consume()
                    onPress()
                    awaitPointerEventScope { waitForUpOrCancellation() }
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 24.sp, color = TextPrimary)
    }
}
