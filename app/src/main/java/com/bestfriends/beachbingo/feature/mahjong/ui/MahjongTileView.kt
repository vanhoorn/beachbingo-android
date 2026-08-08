package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import com.bestfriends.beachbingo.feature.mahjong.MahjongTile
import com.bestfriends.beachbingo.feature.mahjong.TileGroup
import com.bestfriends.beachbingo.feature.mahjong.getTileType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

const val TILE_LAYER_DX = 3f
const val TILE_LAYER_DY = -3f

private const val EDGE_R_FRAC = 0.12f
private const val EDGE_B_FRAC = 0.10f

@Composable
fun MahjongTileCanvas(
    tile: MahjongTile,
    tileW: Float, tileH: Float,
    selected: Boolean, hinted: Boolean, free: Boolean,
    showFreeHighlight: Boolean,
    removing: Boolean = false,
) {
    val tt = getTileType(tile.typeId)
    val accentColor = Color(tt.color)

    val edgeW  = tileW * EDGE_R_FRAC
    val edgeH  = tileH * EDGE_B_FRAC
    val innerW = tileW - edgeW
    val innerH = tileH - edgeH
    val cornerR = max(2f, tileW * 0.08f)
    val borderW = max(1f, tileW * 0.04f)

    val isFreeHint = showFreeHighlight && free && !selected
    // All backgrounds fully opaque — never transparent (avoids seeing tiles below)
    val faceBg = when {
        removing   -> Color(0xFFFFF176)  // gold flash on removal
        selected   -> Color(0xFFBEE3F8)
        hinted     -> Color(0xFFFFD6B0)
        isFreeHint -> Color(0xFFDCF5E5)
        free       -> Color(0xFFFAF0DC)
        else       -> Color(0xFFEDD9B8)  // blocked: slightly dimmed sand
    }
    val borderColor = when {
        selected   -> Color(0xFF0EA5E9)
        hinted     -> Color(0xFFF97316)
        isFreeHint -> Color(0xFF22C55E)
        else       -> if (free) accentColor else accentColor.copy(alpha = 0.55f)
    }

    val density  = LocalDensity.current
    val tileWDp  = with(density) { tileW.toDp() }
    val tileHDp  = with(density) { tileH.toDp() }

    val isSuit   = tt.group == TileGroup.MUSCHELN || tt.group == TileGroup.WELLEN || tt.group == TileGroup.FISCHE
    val iconSize = max(8f, innerW * 0.52f)
    val iconColor = if (free) accentColor else accentColor.copy(alpha = 0.5f)

    Box(modifier = Modifier.size(tileWDp, tileHDp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Right edge
            drawRoundRect(
                color = Color(0xFFBB9C78),
                topLeft = Offset(innerW, edgeH),
                size = Size(edgeW, innerH),
                cornerRadius = CornerRadius(cornerR),
            )
            // Bottom edge
            drawRoundRect(
                color = Color(0xFFA88B65),
                topLeft = Offset(edgeW, innerH),
                size = Size(innerW, edgeH),
                cornerRadius = CornerRadius(cornerR),
            )
            // Face background
            drawRoundRect(
                color = faceBg,
                topLeft = Offset.Zero,
                size = Size(innerW, innerH),
                cornerRadius = CornerRadius(cornerR),
            )
            // Face border
            if (borderW > 0f) {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(borderW / 2f, borderW / 2f),
                    size = Size(innerW - borderW, innerH - borderW),
                    cornerRadius = CornerRadius(max(0f, cornerR - borderW / 2f)),
                    style = Stroke(width = borderW),
                )
            }

            // Icon (scale from 32x32 SVG space)
            val iconYOffset = if (isSuit) innerH * 0.12f else (innerH - iconSize) / 2f
            val iconXOffset = (innerW - iconSize) / 2f
            withTransform({
                translate(left = iconXOffset, top = iconYOffset)
                scale(iconSize / 32f, pivot = Offset.Zero)
            }) {
                drawTileIcon(tt.svgIcon, iconColor)
            }

            // Rank number for suit tiles
            if (isSuit && iconSize >= 10f) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        textSize    = max(6f, iconSize * 0.45f)
                        this.color  = iconColor.toArgb()
                        typeface    = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign   = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        tt.rank.toString(),
                        innerW / 2f,
                        innerH - innerH * 0.08f,
                        paint,
                    )
                }
            }
        }
    }
}

@Suppress("NestedLambdaShadowedImplicitParameter")
private fun DrawScope.drawTileIcon(svgIcon: String, color: Color) {
    val sw      = 3.5f
    val stroke  = Stroke(sw,         cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke1 = Stroke(sw - 0.5f,  cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke2 = Stroke(sw - 1f,    cap = StrokeCap.Round, join = StrokeJoin.Round)

    when {
        // ── Muscheln ──────────────────────────────────────────────────────────
        svgIcon.startsWith("muscheln") -> {
            drawOval(color, topLeft = Offset(5f, 13f), size = Size(22f, 14f), style = stroke)
            val shell = Path().apply {
                moveTo(16f, 13f)
                cubicTo(10f, 8f, 6f, 14f, 16f, 20f)
                cubicTo(26f, 14f, 22f, 8f, 16f, 13f)
                close()
            }
            drawPath(shell, color, style = stroke1)
            drawLine(color, Offset(16f, 13f), Offset(16f, 20f), sw - 1f)
            drawLine(color, Offset(10f, 15f), Offset(16f, 20f), sw - 1.5f)
            drawLine(color, Offset(22f, 15f), Offset(16f, 20f), sw - 1.5f)
        }

        // ── Wellen ────────────────────────────────────────────────────────────
        svgIcon.startsWith("wellen") -> {
            fun wave(y: Float) = Path().apply {
                moveTo(4f, y)
                quadraticBezierTo(8f, y - 4f, 12f, y)
                quadraticBezierTo(16f, y + 4f, 20f, y)
                quadraticBezierTo(24f, y - 4f, 28f, y)
            }
            drawPath(wave(10f), color, style = stroke)
            drawPath(wave(17f), color, style = stroke)
            drawPath(wave(24f), color, style = stroke)
        }

        // ── Fische ────────────────────────────────────────────────────────────
        svgIcon.startsWith("fische") -> {
            val body = Path().apply {
                moveTo(6f, 16f)
                cubicTo(10f, 8f, 22f, 8f, 24f, 16f)
                cubicTo(22f, 24f, 10f, 24f, 6f, 16f)
                close()
            }
            drawPath(body, color, style = stroke)
            val tail = Path().apply {
                moveTo(24f, 16f); lineTo(30f, 10f); lineTo(30f, 22f); close()
            }
            drawPath(tail, color, style = stroke1)
            drawCircle(color, 1.5f, Offset(11f, 14f))
        }

        // ── Sonne (Sonnenaufgang) ─────────────────────────────────────────────
        svgIcon == "wind_ost" -> {
            drawCircle(color, 6f, Offset(16f, 16f), style = stroke)
            listOf(0f to 90f, 90f to 0f).forEach { (a, _) -> } // silence unused warning
            drawLine(color, Offset(16f, 4f),  Offset(16f, 8f),  sw)
            drawLine(color, Offset(16f, 24f), Offset(16f, 28f), sw)
            drawLine(color, Offset(4f,  16f), Offset(8f,  16f), sw)
            drawLine(color, Offset(24f, 16f), Offset(28f, 16f), sw)
            drawLine(color, Offset(8f,  8f),  Offset(11f, 11f), sw)
            drawLine(color, Offset(21f, 21f), Offset(24f, 24f), sw)
            drawLine(color, Offset(24f, 8f),  Offset(21f, 11f), sw)
            drawLine(color, Offset(8f,  24f), Offset(11f, 21f), sw)
        }

        // ── Palme ─────────────────────────────────────────────────────────────
        svgIcon == "wind_sued" -> {
            drawLine(color, Offset(16f, 30f), Offset(16f, 16f), sw, StrokeCap.Round)
            val l1 = Path().apply {
                moveTo(16f, 16f); cubicTo(12f, 8f, 4f, 6f, 2f, 10f)
                cubicTo(6f, 10f, 10f, 14f, 16f, 16f); close()
            }
            val l2 = Path().apply {
                moveTo(16f, 16f); cubicTo(20f, 8f, 28f, 6f, 30f, 10f)
                cubicTo(26f, 10f, 22f, 14f, 16f, 16f); close()
            }
            val l3 = Path().apply {
                moveTo(16f, 16f); cubicTo(14f, 6f, 8f, 2f, 6f, 4f)
                cubicTo(8f, 8f, 12f, 12f, 16f, 16f); close()
            }
            drawPath(l1, color, style = stroke1)
            drawPath(l2, color, style = stroke1)
            drawPath(l3, color, style = stroke1)
        }

        // ── Sonnenuntergang ───────────────────────────────────────────────────
        svgIcon == "wind_west" -> {
            val arch = Path().apply {
                moveTo(4f, 20f); quadraticBezierTo(16f, 8f, 28f, 20f)
            }
            drawPath(arch, color, style = stroke)
            drawLine(color, Offset(4f,  24f), Offset(28f, 24f), sw)
            drawLine(color, Offset(16f,  6f), Offset(16f, 10f), sw, StrokeCap.Round)
            drawLine(color, Offset(6f,  10f), Offset(9f,  12f), sw, StrokeCap.Round)
            drawLine(color, Offset(26f, 10f), Offset(23f, 12f), sw, StrokeCap.Round)
        }

        // ── Leuchtturm ────────────────────────────────────────────────────────
        svgIcon == "wind_nord" -> {
            drawRect(color, topLeft = Offset(12f, 14f), size = Size(8f, 14f), style = stroke1)
            val top = Path().apply {
                moveTo(10f, 14f); lineTo(22f, 14f); lineTo(19f, 6f); lineTo(13f, 6f); close()
            }
            drawPath(top, color, style = stroke1)
            drawRect(color, topLeft = Offset(14f, 4f), size = Size(4f, 3f), style = stroke2)
            drawLine(color, Offset(8f,  18f), Offset(12f, 18f), sw - 1f)
            drawLine(color, Offset(20f, 18f), Offset(24f, 18f), sw - 1f)
            drawLine(color, Offset(12f, 23f), Offset(20f, 23f), sw - 1f)
            drawLine(color, Offset(14f, 28f), Offset(18f, 28f), sw - 1f)
        }

        // ── Hai ───────────────────────────────────────────────────────────────
        svgIcon == "drache_rot" -> {
            val body = Path().apply {
                moveTo(4f, 22f)
                quadraticBezierTo(10f, 10f, 20f, 14f)
                quadraticBezierTo(28f, 18f, 28f, 22f)
            }
            drawPath(body, color, style = stroke)
            val fin = Path().apply {
                moveTo(16f, 14f); lineTo(18f, 6f); lineTo(22f, 14f)
            }
            drawPath(fin, color, style = stroke1)
            val belly = Path().apply {
                moveTo(28f, 22f)
                quadraticBezierTo(20f, 28f, 12f, 26f)
                quadraticBezierTo(6f, 24f, 4f, 22f)
            }
            drawPath(belly, color, style = stroke)
            drawOval(color, topLeft = Offset(20f, 18.5f), size = Size(4f, 3f))
            drawLine(color, Offset(8f,  24f), Offset(6f,  28f), sw - 1f)
            drawLine(color, Offset(12f, 26f), Offset(11f, 30f), sw - 1f)
        }

        // ── Delfin ────────────────────────────────────────────────────────────
        svgIcon == "drache_gruen" -> {
            val body = Path().apply {
                moveTo(4f, 18f)
                quadraticBezierTo(10f, 8f, 20f, 12f)
                quadraticBezierTo(28f, 16f, 26f, 22f)
                quadraticBezierTo(20f, 28f, 10f, 24f)
                quadraticBezierTo(4f, 20f, 4f, 18f)
                close()
            }
            drawPath(body, color, style = stroke)
            val tail = Path().apply {
                moveTo(26f, 16f); lineTo(30f, 10f); lineTo(28f, 18f); close()
            }
            drawPath(tail, color, style = stroke1)
            drawCircle(color, 1.5f, Offset(12f, 16f))
            val dorsal = Path().apply {
                moveTo(16f, 8f); quadraticBezierTo(19f, 4f, 22f, 8f)
            }
            drawPath(dorsal, color, style = stroke1)
        }

        // ── Oktopus ───────────────────────────────────────────────────────────
        svgIcon == "drache_weiss" -> {
            drawOval(color, topLeft = Offset(8f, 7f), size = Size(16f, 14f), style = stroke)
            val fromXs = listOf(8f, 11f, 14f, 17f, 20f, 23f)
            val toXs   = listOf(8f, 12f, 15f, 17f, 20f, 24f)
            val toYs   = listOf(28f, 29f, 30f, 30f, 29f, 28f)
            fromXs.forEachIndexed { i, fx ->
                val tx = toXs[i]; val ty = toYs[i]
                val p = Path().apply {
                    moveTo(fx, 18f)
                    quadraticBezierTo((fx + tx) / 2f - 1f, (18f + ty) / 2f, tx, ty)
                }
                drawPath(p, color, style = stroke1)
            }
            drawCircle(color, 1.5f, Offset(12f, 12f))
            drawCircle(color, 1.5f, Offset(20f, 12f))
        }

        // ── Fruehling (Pflanze) ────────────────────────────────────────────────
        svgIcon == "jahreszeit_fruehling" -> {
            drawLine(color, Offset(16f, 28f), Offset(16f, 16f), sw, StrokeCap.Round)
            val leaf1 = Path().apply {
                moveTo(16f, 16f); cubicTo(10f, 12f, 6f, 6f, 10f, 4f)
                cubicTo(14f, 4f, 16f, 10f, 16f, 16f); close()
            }
            val leaf2 = Path().apply {
                moveTo(16f, 20f); cubicTo(20f, 16f, 26f, 12f, 28f, 8f)
                cubicTo(24f, 6f, 18f, 14f, 16f, 20f); close()
            }
            drawPath(leaf1, color, style = stroke1)
            drawPath(leaf2, color, style = stroke1)
        }

        // ── Sommer (Sonne mit Strahlen) ────────────────────────────────────────
        svgIcon == "jahreszeit_sommer" -> {
            drawCircle(color, 7f, Offset(16f, 16f), style = stroke)
            listOf(0, 45, 90, 135, 180, 225, 270, 315).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                drawLine(
                    color,
                    Offset(16f + 9f * cos(rad), 16f + 9f * sin(rad)),
                    Offset(16f + 13f * cos(rad), 16f + 13f * sin(rad)),
                    sw - 0.5f,
                )
            }
        }

        // ── Herbst (Blatt) ────────────────────────────────────────────────────
        svgIcon == "jahreszeit_herbst" -> {
            val leaf = Path().apply {
                moveTo(16f, 8f)
                quadraticBezierTo(20f, 12f, 18f, 18f)
                quadraticBezierTo(22f, 14f, 26f, 16f)
                quadraticBezierTo(22f, 22f, 16f, 24f)
                quadraticBezierTo(10f, 22f, 6f, 16f)
                quadraticBezierTo(10f, 14f, 14f, 18f)
                quadraticBezierTo(12f, 12f, 16f, 8f)
                close()
            }
            drawPath(leaf, color, style = stroke1)
            drawLine(color, Offset(16f, 24f), Offset(16f, 30f), sw, StrokeCap.Round)
        }

        // ── Winter (Schneeflocke) ─────────────────────────────────────────────
        svgIcon == "jahreszeit_winter" -> {
            drawLine(color, Offset(16f, 4f),  Offset(16f, 28f), sw)
            drawLine(color, Offset(4f,  16f), Offset(28f, 16f), sw)
            drawLine(color, Offset(8f,  8f),  Offset(24f, 24f), sw)
            drawLine(color, Offset(24f, 8f),  Offset(8f,  24f), sw)
            drawCircle(color, 2f, Offset(16f, 16f))
        }

        // ── Hibiskus (5 Blüten) ───────────────────────────────────────────────
        svgIcon == "blume_hibiskus" -> {
            listOf(0, 72, 144, 216, 288).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                val cx = 16f + 8f * cos(rad); val cy = 16f + 8f * sin(rad)
                withTransform({ rotate(a.toFloat(), Offset(cx, cy)) }) {
                    drawOval(color, topLeft = Offset(cx - 5f, cy - 3f), size = Size(10f, 6f), style = Stroke(sw - 1f))
                }
            }
            drawCircle(color, 3f, Offset(16f, 16f), style = Stroke(sw - 0.5f))
        }

        // ── Seeanemone (6 Strahlen) ───────────────────────────────────────────
        svgIcon == "blume_anemone" -> {
            listOf(0, 60, 120, 180, 240, 300).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                drawLine(
                    color,
                    Offset(16f + 4f * cos(rad), 16f + 4f * sin(rad)),
                    Offset(16f + 12f * cos(rad), 16f + 12f * sin(rad)),
                    sw - 0.5f,
                )
            }
            drawCircle(color, 4f, Offset(16f, 16f), style = stroke)
        }

        // ── Seerose (8 Blütenblätter) ─────────────────────────────────────────
        svgIcon == "blume_seerose" -> {
            listOf(0, 90, 180, 270).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                val cx = 16f + 7f * cos(rad); val cy = 16f + 7f * sin(rad)
                withTransform({ rotate(a.toFloat(), Offset(cx, cy)) }) {
                    drawOval(color, topLeft = Offset(cx - 6f, cy - 4f), size = Size(12f, 8f), style = Stroke(sw - 1f))
                }
            }
            listOf(45, 135, 225, 315).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                val cx = 16f + 7f * cos(rad); val cy = 16f + 7f * sin(rad)
                withTransform({ rotate(a.toFloat(), Offset(cx, cy)) }) {
                    drawOval(color, topLeft = Offset(cx - 5f, cy - 3f), size = Size(10f, 6f), style = Stroke(sw - 1f))
                }
            }
            drawCircle(color, 3f, Offset(16f, 16f))
        }

        // ── Stranddistel (12 Strahlen) ────────────────────────────────────────
        svgIcon == "blume_stranddistel" -> {
            (0..330 step 30).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                drawLine(
                    color,
                    Offset(16f, 16f),
                    Offset(16f + 11f * cos(rad), 16f + 11f * sin(rad)),
                    sw - 1.5f,
                    StrokeCap.Round,
                )
            }
            drawCircle(color, 4f, Offset(16f, 16f), style = Stroke(sw - 0.5f))
        }

        // ── Fallback ──────────────────────────────────────────────────────────
        else -> {
            drawIntoCanvas { canvas ->
                val p = android.graphics.Paint().apply {
                    textSize    = 14f
                    this.color  = color.toArgb()
                    isAntiAlias = true
                    textAlign   = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(svgIcon.take(2), 16f, 20f, p)
            }
        }
    }
}
