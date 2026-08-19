package com.bestfriends.beachbingo.feature.sonnenrad.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.feature.sonnenrad.SonnenradSymbol
import com.bestfriends.beachbingo.ui.components.CardBackScene
import com.bestfriends.beachbingo.ui.components.drawPalm
import com.bestfriends.beachbingo.ui.components.drawShell
import com.bestfriends.beachbingo.ui.components.drawSun
import com.bestfriends.beachbingo.ui.components.drawWave
import com.bestfriends.beachbingo.ui.theme.CardFace
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.PiratesPurple
import com.bestfriends.beachbingo.ui.theme.PurpleDeep
import com.bestfriends.beachbingo.ui.theme.PurpleLight
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.Teal
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SonnenradSymbolCard(
    symbol: SonnenradSymbol?,
    faceUp: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 100.dp,
    glowBorderColor: Color = Color.Transparent,
) {
    val flip by animateFloatAsState(
        targetValue = if (faceUp) 180f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "card_flip",
    )
    val showFront = flip > 90f

    Box(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer {
                rotationY = flip
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(12.dp)),
    ) {
        if (!showFront) {
            CardBackScene(modifier = Modifier.fillMaxSize())
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = -1f },
            ) {
                drawRect(color = CardFace)
                val r = minOf(size.width, size.height) * 0.36f
                val cx = size.width / 2f
                val cy = size.height / 2f
                when (symbol) {
                    SonnenradSymbol.SONNE        -> drawSun(cx, cy, r, SandGold)
                    SonnenradSymbol.WELLE        -> drawWave(cx, cy, r, Teal)
                    SonnenradSymbol.PALME        -> drawPalm(cx, cy, r, Success)
                    SonnenradSymbol.MUSCHEL      -> drawShell(cx, cy, r, Coral)
                    SonnenradSymbol.SONNENSCHIRM -> drawParasol(cx, cy, r)
                    null                         -> Unit
                }
            }
        }
    }
}

internal fun DrawScope.drawParasol(cx: Float, cy: Float, r: Float) {
    val cTop = cy - r * 0.15f
    val cRad = r * 0.76f

    for (i in 0 until 6) {
        val startAngle = 180f + i * 30f
        val color = if (i % 2 == 0) PiratesPurple else PurpleLight
        drawArc(
            color      = color,
            startAngle = startAngle,
            sweepAngle = 30f,
            useCenter  = true,
            topLeft    = Offset(cx - cRad, cTop - cRad),
            size       = Size(cRad * 2f, cRad * 2f),
        )
    }

    for (i in 0 until 6) {
        val angleMid = Math.toRadians(180.0 + i * 30.0 + 15.0)
        val bx = (cx + cos(angleMid) * cRad).toFloat()
        val by = (cTop + sin(angleMid) * cRad).toFloat()
        drawCircle(
            color  = PurpleDeep.copy(alpha = 0.55f),
            radius = cRad * 0.12f,
            center = Offset(bx, by),
        )
    }

    drawArc(
        color      = PurpleDeep,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter  = false,
        topLeft    = Offset(cx - cRad, cTop - cRad),
        size       = Size(cRad * 2f, cRad * 2f),
        style      = Stroke(width = r * 0.055f),
    )

    val poleBotX = cx + r * 0.06f
    val poleBotY = cy + r * 0.64f
    drawLine(
        color       = PurpleDeep,
        start       = Offset(cx, cTop),
        end         = Offset(poleBotX, poleBotY),
        strokeWidth = r * 0.09f,
        cap         = StrokeCap.Round,
    )

    val gPath = Path()
    gPath.moveTo(poleBotX - r * 0.22f, poleBotY + r * 0.04f)
    gPath.quadraticBezierTo(
        poleBotX, poleBotY + r * 0.18f,
        poleBotX + r * 0.22f, poleBotY + r * 0.04f,
    )
    drawPath(
        path  = gPath,
        color = SandGold.copy(alpha = 0.6f),
        style = Stroke(width = r * 0.08f, cap = StrokeCap.Round),
    )
}
