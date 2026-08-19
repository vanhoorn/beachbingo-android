package com.bestfriends.beachbingo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.CardBack
import com.bestfriends.beachbingo.ui.theme.CardBorderLight
import com.bestfriends.beachbingo.ui.theme.CardColorDark
import com.bestfriends.beachbingo.ui.theme.CardFace
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.PalmDark
import com.bestfriends.beachbingo.ui.theme.PalmDeep
import com.bestfriends.beachbingo.ui.theme.PalmMid
import com.bestfriends.beachbingo.ui.theme.SandBeach
import com.bestfriends.beachbingo.ui.theme.SandBeachLight
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.SunBright
import com.bestfriends.beachbingo.ui.theme.SunCore
import com.bestfriends.beachbingo.ui.theme.SunGlow
import com.bestfriends.beachbingo.ui.theme.Teal
import com.bestfriends.beachbingo.ui.theme.TrunkBrown
import com.bestfriends.beachbingo.ui.theme.WaveDark
import com.bestfriends.beachbingo.ui.theme.WaveDeep
import com.bestfriends.beachbingo.ui.theme.WaveLight
import com.bestfriends.beachbingo.ui.theme.WaveMid
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Suit helpers ─────────────────────────────────────────────────────────────

fun suitColor(suit: String): Color = when (suit) {
    "♥" -> SandGold   // Sonne
    "♦" -> Teal        // Welle
    "♠" -> Success     // Palme
    "♣" -> Coral       // Muschel
    else -> CardColorDark
}

fun suitName(suit: String): String = when (suit) {
    "♥" -> "Sonne"
    "♦" -> "Welle"
    "♠" -> "Palme"
    "♣" -> "Muschel"
    else -> suit
}

// ── Beach suit icons ──────────────────────────────────────────────────────────

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSun(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.22f), radius = r * 0.50f, center = Offset(cx, cy))
    drawCircle(color, radius = r * 0.30f, center = Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.35f), radius = r * 0.14f, center = Offset(cx - r * 0.08f, cy - r * 0.09f))
    val ri = r * 0.38f; val ro = r * 0.62f
    for (i in 0 until 8) {
        val a = i * Math.PI / 4.0
        drawLine(color, Offset(cx + cos(a).toFloat() * ri, cy + sin(a).toFloat() * ri),
            Offset(cx + cos(a).toFloat() * ro, cy + sin(a).toFloat() * ro),
            strokeWidth = r * 0.13f, cap = StrokeCap.Round)
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(cx: Float, cy: Float, r: Float, color: Color) {
    val p1 = Path()
    p1.moveTo(cx - r * 0.85f, cy - r * 0.18f)
    p1.quadraticBezierTo(cx - r * 0.42f, cy - r * 0.56f, cx, cy - r * 0.18f)
    p1.quadraticBezierTo(cx + r * 0.42f, cy + r * 0.20f, cx + r * 0.85f, cy - r * 0.18f)
    drawPath(p1, color = color, style = Stroke(width = r * 0.18f, cap = StrokeCap.Round))
    val p2 = Path()
    p2.moveTo(cx - r * 0.85f, cy + r * 0.30f)
    p2.quadraticBezierTo(cx - r * 0.42f, cy - r * 0.08f, cx, cy + r * 0.30f)
    p2.quadraticBezierTo(cx + r * 0.42f, cy + r * 0.68f, cx + r * 0.85f, cy + r * 0.30f)
    drawPath(p2, color = color.copy(alpha = 0.60f), style = Stroke(width = r * 0.14f, cap = StrokeCap.Round))
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPalm(cx: Float, cy: Float, r: Float, color: Color) {
    val tx = cx + r * 0.05f; val ty = cy - r * 0.25f
    val bx = cx - r * 0.04f; val by = cy + r * 0.68f
    drawLine(TrunkBrown, Offset(bx, by), Offset(tx, ty), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
    listOf(
        Offset(cx - r * 0.80f, cy - r * 0.72f),
        Offset(cx + r * 0.82f, cy - r * 0.65f),
        Offset(cx + r * 0.05f, cy - r * 0.95f),
    ).forEach { end ->
        val mx = (tx + end.x) / 2f; val my = (ty + end.y) / 2f - r * 0.10f
        val p = Path(); p.moveTo(tx, ty); p.quadraticBezierTo(mx, my, end.x, end.y)
        drawPath(p, color = color, style = Stroke(width = r * 0.16f, cap = StrokeCap.Round))
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShell(cx: Float, cy: Float, r: Float, color: Color) {
    val botY = cy + r * 0.48f; val rad = r * 0.70f
    drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - rad, botY - rad), size = Size(rad * 2f, rad * 2f),
        style = Stroke(width = r * 0.14f, cap = StrokeCap.Round))
    for (i in 0..4) {
        val a = Math.PI * (1.0 - i * 0.25)
        drawLine(color.copy(alpha = 0.55f), Offset(cx, botY),
            Offset((cx + cos(a) * rad * 0.82).toFloat(), (botY - sin(a) * rad * 0.82).toFloat()),
            strokeWidth = r * 0.09f, cap = StrokeCap.Round)
    }
    drawCircle(color, radius = r * 0.10f, center = Offset(cx, botY))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeachSuit(suit: String, cx: Float, cy: Float, r: Float, color: Color) {
    when (suit) {
        "♥" -> drawSun(cx, cy, r, color)
        "♦" -> drawWave(cx, cy, r, color)
        "♠" -> drawPalm(cx, cy, r, color)
        "♣" -> drawShell(cx, cy, r, color)
    }
}

@Composable
fun BeachSuitIcon(suit: String, sizeDp: Dp) {
    val color = suitColor(suit)
    Canvas(modifier = Modifier.size(sizeDp)) {
        val r = minOf(size.width, size.height) / 2f
        drawBeachSuit(suit, size.width / 2f, size.height / 2f, r, color)
    }
}

// ── CardBackScene ─────────────────────────────────────────────────────────────

@Composable
fun CardBackScene(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Daytime sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(WaveDark, WaveLight),
                startY = 0f, endY = h * 0.56f,
            ),
            size = Size(w, h * 0.56f),
        )
        // Ocean
        drawRect(
            brush = Brush.verticalGradient(
                listOf(WaveMid, WaveDeep),
                startY = h * 0.56f, endY = h,
            ),
            topLeft = Offset(0f, h * 0.56f),
            size = Size(w, h * 0.44f),
        )

        // Sun (upper-right)
        val sCx = w * 0.78f; val sCy = h * 0.115f
        drawCircle(SunGlow.copy(alpha = 0.28f), radius = w * 0.165f, center = Offset(sCx, sCy))
        drawCircle(SunCore, radius = w * 0.10f, center = Offset(sCx, sCy))
        drawCircle(SunBright, radius = w * 0.060f, center = Offset(sCx - w * 0.012f, sCy - h * 0.008f))
        val sRi = w * 0.13f; val sRo = w * 0.195f
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4.0
            val ca = cos(angle).toFloat(); val sa = sin(angle).toFloat()
            drawLine(
                color = SunCore.copy(alpha = 0.7f),
                start = Offset(sCx + ca * sRi, sCy + sa * sRi),
                end = Offset(sCx + ca * sRo, sCy + sa * sRo),
                strokeWidth = 2.5f,
            )
        }

        // Wave lines
        listOf(Triple(0.68f, 1.2f, 0.35f), Triple(0.79f, 1.0f, 0.25f), Triple(0.90f, 0.9f, 0.18f))
            .forEach { (yf, lw, alpha) ->
                val y = h * yf
                val p = Path(); p.moveTo(0f, y); var x = 0f
                while (x < w) {
                    val dx = w * 0.26f
                    p.quadraticBezierTo(x + dx * 0.25f, y - h * 0.022f, x + dx * 0.5f, y)
                    p.quadraticBezierTo(x + dx * 0.75f, y + h * 0.022f, x + dx, y)
                    x += dx
                }
                drawPath(p, color = Color.White.copy(alpha = alpha), style = Stroke(width = lw))
            }

        // Island
        drawOval(SandBeach, topLeft = Offset(w * 0.285f, h * 0.815f), size = Size(w * 0.43f, h * 0.105f))
        drawOval(SandBeachLight.copy(alpha = 0.45f), topLeft = Offset(w * 0.32f, h * 0.805f), size = Size(w * 0.21f, h * 0.065f))

        // Palm trunk
        val trunk = Path()
        trunk.moveTo(w * 0.50f, h * 0.835f)
        trunk.quadraticBezierTo(w * 0.464f, h * 0.67f, w * 0.478f, h * 0.545f)
        trunk.quadraticBezierTo(w * 0.495f, h * 0.465f, w * 0.548f, h * 0.385f)
        drawPath(trunk, color = TrunkBrown, style = Stroke(width = 4f, cap = StrokeCap.Round))

        val ptx = w * 0.548f; val pty = h * 0.385f

        // Palm fronds — filled leaf shapes (wide in middle, tapers to tip)
        listOf(
            Triple(Offset(w * 0.09f, h * 0.585f), 6.5f, PalmDark),
            Triple(Offset(w * 0.92f, h * 0.585f), 6.5f, PalmDark),
            Triple(Offset(w * 0.17f, h * 0.185f), 5.5f, PalmMid),
            Triple(Offset(w * 0.84f, h * 0.185f), 5.5f, PalmMid),
            Triple(Offset(w * 0.52f, h * 0.095f), 5.0f, PalmDark),
        ).forEach { (end, hw, color) ->
            val dx = end.x - ptx; val dy = end.y - pty
            val len = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            val px = -dy / len; val py = dx / len
            val wx = ptx + dx * 0.42f; val wy = pty + dy * 0.42f

            val frond = Path()
            frond.moveTo(ptx + px * 1.5f, pty + py * 1.5f)
            frond.quadraticBezierTo(wx + px * hw, wy + py * hw, end.x, end.y)
            frond.quadraticBezierTo(wx - px * hw, wy - py * hw, ptx - px * 1.5f, pty - py * 1.5f)
            frond.close()
            drawPath(frond, color = color)
            drawLine(PalmDeep.copy(alpha = 0.35f), Offset(ptx, pty), end, strokeWidth = 1.2f)
        }
    }
}

@Composable
fun PlayingCard(
    rank: String,
    suit: String,
    faceUp: Boolean,
    selected: Boolean,
    accentColor: Color = Teal,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 56.dp,
    cardHeight: Dp = 80.dp,
) {
    val cardColor = suitColor(suit)
    val borderColor = if (selected) accentColor else CardBorderLight

    Box(
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .background(if (faceUp) CardFace else CardBack),
    ) {
        if (faceUp) {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
            ) {
                Text(
                    text = rank,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = cardColor,
                    lineHeight = 12.sp,
                )
                BeachSuitIcon(suit = suit, sizeDp = 10.dp)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    BeachSuitIcon(suit = suit, sizeDp = 24.dp)
                }
            }
        } else {
            CardBackScene(modifier = Modifier.fillMaxSize())
        }
    }
}

// ── CardFanRow ────────────────────────────────────────────────────────────────

@Composable
fun <T> CardFanRow(
    cards: List<T>,
    modifier: Modifier = Modifier,
    overlapFraction: Float = 0.35f,
    maxAngle: Float = 7f,
    content: @Composable (card: T, index: Int) -> Unit,
) {
    if (cards.isEmpty()) return
    val n = cards.size
    Layout(
        content = {
            cards.forEachIndexed { idx, card ->
                val frac = if (n > 1) (idx.toFloat() - (n - 1) / 2f) / ((n - 1) / 2f) else 0f
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = frac * maxAngle
                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                    }
                ) { content(card, idx) }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        if (placeables.isEmpty()) return@Layout layout(0, 0) {}
        val cardW = placeables.maxOf { it.width }
        val cardH = placeables.maxOf { it.height }
        val step = (cardW * (1f - overlapFraction)).toInt().coerceAtLeast(1)
        val totalW = cardW + step * (n - 1)
        layout(totalW, cardH) {
            placeables.forEachIndexed { idx, p ->
                p.place(x = idx * step, y = 0)
            }
        }
    }
}
