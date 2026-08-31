package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import com.bestfriends.beachbingo.feature.mahjong.MahjongTile
import com.bestfriends.beachbingo.feature.mahjong.TileGroup
import com.bestfriends.beachbingo.feature.mahjong.getTileType
import com.bestfriends.beachbingo.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

const val TILE_LAYER_DX = 5f
const val TILE_LAYER_DY = -5f

private const val EDGE_R_FRAC = 0.12f
private const val EDGE_B_FRAC = 0.10f

private fun Color.lighten(f: Float) = Color(
    red   = (red   + (1f - red)   * f).coerceIn(0f, 1f),
    green = (green + (1f - green) * f).coerceIn(0f, 1f),
    blue  = (blue  + (1f - blue)  * f).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.darken(f: Float) = Color(
    red   = (red   * (1f - f)).coerceIn(0f, 1f),
    green = (green * (1f - f)).coerceIn(0f, 1f),
    blue  = (blue  * (1f - f)).coerceIn(0f, 1f),
    alpha = alpha,
)

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
    val faceBg = when {
        removing   -> TileRemoveFlash
        selected   -> TileSelected
        hinted     -> TileHinted
        isFreeHint -> TileFreeHint
        free       -> TileFree
        else       -> TileBlocked
    }
    val borderColor = when {
        selected   -> OceanBlue
        hinted     -> Coral
        isFreeHint -> Success
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
            // Drop shadow — drawn first, below edges and face
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(3f, 4f),
                size = Size(innerW, innerH),
                cornerRadius = CornerRadius(cornerR),
            )
            // Right edge — gradient top-light to bottom-dark
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(TileSandLight.lighten(0.08f), TileSandLight.darken(0.12f)),
                    start = Offset(innerW, edgeH),
                    end   = Offset(innerW, edgeH + innerH),
                ),
                topLeft = Offset(innerW, edgeH),
                size = Size(edgeW, innerH),
                cornerRadius = CornerRadius(cornerR),
            )
            // Bottom edge — gradient left-light to right-dark
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(TileSandDark.lighten(0.06f), TileSandDark.darken(0.10f)),
                    start = Offset(edgeW, innerH),
                    end   = Offset(edgeW + innerW, innerH),
                ),
                topLeft = Offset(edgeW, innerH),
                size = Size(innerW, edgeH),
                cornerRadius = CornerRadius(cornerR),
            )
            // Face background with subtle gradient (light top-left → darker bottom-right)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(faceBg.lighten(0.07f), faceBg.darken(0.06f)),
                    start = Offset(0f, 0f),
                    end = Offset(innerW * 0.5f, innerH),
                ),
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

            // Pip pattern for suit tiles; single icon for all others
            if (isSuit) {
                drawSuitPips(tt.svgIcon, tt.rank, iconColor, innerW, innerH)
            } else {
                val iconYOffset = (innerH - iconSize) / 2f
                val iconXOffset = (innerW - iconSize) / 2f
                withTransform({
                    translate(left = iconXOffset, top = iconYOffset)
                    scale(scaleX = iconSize / 32f, scaleY = iconSize / 32f, pivot = Offset.Zero)
                }) {
                    drawTileIcon(tt.svgIcon, iconColor)
                }
            }
        }
    }
}

/** Draws N pips (mini copies of the suit icon) in a playing-card arrangement. */
private fun DrawScope.drawSuitPips(
    svgIcon: String,
    rank: Int,
    color: Color,
    faceW: Float,
    faceH: Float,
) {
    val pad    = faceW * 0.08f
    val availW = faceW - 2f * pad
    val availH = faceH - 2f * pad
    val startX = pad
    val startY = pad

    val pipScale = when {
        rank == 1 -> 0.52f
        rank <= 3 -> 0.36f
        rank <= 5 -> 0.30f
        rank <= 8 -> 0.25f
        else      -> 0.22f
    }
    val pipSize = min(availW, availH) * pipScale

    val positions: List<Pair<Float, Float>> = when (rank) {
        1 -> listOf(0.5f to 0.5f)
        2 -> listOf(0.5f to 0.25f, 0.5f to 0.75f)
        3 -> listOf(0.5f to 0.2f, 0.5f to 0.5f, 0.5f to 0.8f)
        4 -> listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.25f to 0.75f, 0.75f to 0.75f)
        5 -> listOf(0.25f to 0.25f, 0.75f to 0.25f, 0.5f to 0.5f, 0.25f to 0.75f, 0.75f to 0.75f)
        6 -> listOf(0.25f to 0.2f, 0.75f to 0.2f, 0.25f to 0.5f, 0.75f to 0.5f, 0.25f to 0.8f, 0.75f to 0.8f)
        7 -> listOf(0.25f to 0.2f, 0.75f to 0.2f, 0.5f to 0.35f, 0.25f to 0.5f, 0.75f to 0.5f, 0.25f to 0.8f, 0.75f to 0.8f)
        8 -> listOf(0.25f to 0.2f, 0.75f to 0.2f, 0.5f to 0.3f, 0.25f to 0.5f, 0.75f to 0.5f, 0.5f to 0.7f, 0.25f to 0.8f, 0.75f to 0.8f)
        else -> listOf(
            0.2f to 0.2f, 0.5f to 0.2f, 0.8f to 0.2f,
            0.2f to 0.5f, 0.5f to 0.5f, 0.8f to 0.5f,
            0.2f to 0.8f, 0.5f to 0.8f, 0.8f to 0.8f,
        )
    }

    positions.forEach { (xf, yf) ->
        val cx = startX + availW * xf
        val cy = startY + availH * yf
        withTransform({
            translate(cx - pipSize / 2f, cy - pipSize / 2f)
            scale(scaleX = pipSize / 32f, scaleY = pipSize / 32f, pivot = Offset.Zero)
        }) {
            drawTileIcon(svgIcon, color)
        }
    }
}

@Suppress("NestedLambdaShadowedImplicitParameter")
private fun DrawScope.drawTileIcon(svgIcon: String, color: Color) {
    val sw      = 3.5f
    val stroke  = Stroke(sw,         cap = StrokeCap.Round, join = StrokeJoin.Round)
    val stroke1 = Stroke(sw - 0.5f,  cap = StrokeCap.Round, join = StrokeJoin.Round)

    val outline = Color.Black.copy(alpha = 0.25f)
    val oStroke = Stroke(0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Local helpers: fill a closed shape then add thin dark outline for depth
    fun filled(path: Path) {
        drawPath(path, color)
        drawPath(path, outline, style = oStroke)
    }
    fun filledOval(tl: Offset, sz: Size) {
        drawOval(color, topLeft = tl, size = sz)
        drawOval(outline, topLeft = tl, size = sz, style = Stroke(0.8f))
    }
    fun filledCircle(r: Float, c: Offset) {
        drawCircle(color, r, c)
        drawCircle(outline, r, c, style = Stroke(0.8f))
    }
    fun filledRect(tl: Offset, sz: Size) {
        drawRect(color, topLeft = tl, size = sz)
        drawRect(outline, topLeft = tl, size = sz, style = Stroke(0.8f))
    }

    val detail = Color.Black.copy(alpha = 0.35f)
    val eye    = Color.Black.copy(alpha = 0.7f)

    when {
        // ── Muscheln ──────────────────────────────────────────────────────────
        svgIcon.startsWith("muscheln") -> {
            filledOval(Offset(5f, 13f), Size(22f, 14f))
            val shell = Path().apply {
                moveTo(16f, 13f)
                cubicTo(10f, 8f, 6f, 14f, 16f, 20f)
                cubicTo(26f, 14f, 22f, 8f, 16f, 13f)
                close()
            }
            filled(shell)
            drawLine(detail, Offset(16f, 13f), Offset(16f, 20f), sw - 1f)
            drawLine(detail, Offset(10f, 15f), Offset(16f, 20f), sw - 1.5f)
            drawLine(detail, Offset(22f, 15f), Offset(16f, 20f), sw - 1.5f)
        }

        // ── Wellen ────────────────────────────────────────────────────────────
        svgIcon.startsWith("wellen") -> {
            fun wave(y: Float) = Path().apply {
                moveTo(4f, y)
                quadraticTo(8f, y - 4f, 12f, y)
                quadraticTo(16f, y + 4f, 20f, y)
                quadraticTo(24f, y - 4f, 28f, y)
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
            filled(body)
            val tail = Path().apply {
                moveTo(24f, 16f); lineTo(30f, 10f); lineTo(30f, 22f); close()
            }
            filled(tail)
            drawCircle(eye, 1.5f, Offset(11f, 14f))
        }

        // ── Sonne (Sonnenaufgang) ─────────────────────────────────────────────
        svgIcon == "wind_ost" -> {
            filledCircle(6f, Offset(16f, 16f))
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
            val trunk = Path().apply {
                moveTo(16f, 30f); cubicTo(18f, 24f, 17f, 18f, 16f, 14f)
            }
            drawPath(trunk, color, style = stroke)
            val l1 = Path().apply {
                moveTo(16f, 14f); cubicTo(10f, 8f, 2f, 6f, 0f, 10f)
                cubicTo(5f, 10f, 11f, 13f, 16f, 14f); close()
            }
            val l2 = Path().apply {
                moveTo(16f, 14f); cubicTo(22f, 8f, 30f, 6f, 32f, 10f)
                cubicTo(27f, 10f, 21f, 13f, 16f, 14f); close()
            }
            val l3 = Path().apply {
                moveTo(16f, 14f); cubicTo(12f, 6f, 6f, 2f, 4f, 4f)
                cubicTo(7f, 7f, 12f, 11f, 16f, 14f); close()
            }
            val l4 = Path().apply {
                moveTo(16f, 14f); cubicTo(20f, 6f, 26f, 2f, 28f, 4f)
                cubicTo(25f, 7f, 20f, 11f, 16f, 14f); close()
            }
            val l5 = Path().apply {
                moveTo(16f, 14f); cubicTo(14f, 8f, 14f, 2f, 16f, 2f)
                cubicTo(18f, 2f, 18f, 8f, 16f, 14f); close()
            }
            filled(l1); filled(l2); filled(l3); filled(l4); filled(l5)
        }

        // ── Sonnenuntergang ───────────────────────────────────────────────────
        svgIcon == "wind_west" -> {
            val arch = Path().apply {
                moveTo(4f, 20f); quadraticTo(16f, 8f, 28f, 20f)
            }
            drawPath(arch, color, style = stroke)
            drawLine(color, Offset(4f,  24f), Offset(28f, 24f), sw)
            drawLine(color, Offset(16f,  6f), Offset(16f, 10f), sw, StrokeCap.Round)
            drawLine(color, Offset(6f,  10f), Offset(9f,  12f), sw, StrokeCap.Round)
            drawLine(color, Offset(26f, 10f), Offset(23f, 12f), sw, StrokeCap.Round)
        }

        // ── Leuchtturm ────────────────────────────────────────────────────────
        svgIcon == "wind_nord" -> {
            filledRect(Offset(12f, 14f), Size(8f, 14f))
            val top = Path().apply {
                moveTo(10f, 14f); lineTo(22f, 14f); lineTo(19f, 6f); lineTo(13f, 6f); close()
            }
            filled(top)
            filledRect(Offset(13f, 3f), Size(6f, 4f))
            // Balcony rail at gallery level
            drawLine(color, Offset(8f, 14f), Offset(24f, 14f), sw - 0.5f, StrokeCap.Round)
            // Stripe, window, door
            drawLine(detail, Offset(12f, 20f), Offset(20f, 20f), sw - 0.5f)
            drawLine(detail, Offset(14f, 10f), Offset(18f, 10f), sw - 1.5f)
            drawLine(detail, Offset(14f, 28f), Offset(14f, 24f), sw - 1.5f)
            drawLine(detail, Offset(18f, 28f), Offset(18f, 24f), sw - 1.5f)
            // Light rays from lantern
            drawLine(color, Offset(16f, 4f), Offset(4f, 10f), sw - 2f, StrokeCap.Round)
            drawLine(color, Offset(16f, 4f), Offset(28f, 10f), sw - 2f, StrokeCap.Round)
            drawLine(color, Offset(16f, 3f), Offset(16f, 0f), sw - 2f, StrokeCap.Round)
        }

        // ── Hai ───────────────────────────────────────────────────────────────
        svgIcon == "drache_rot" -> {
            val body = Path().apply {
                moveTo(4f, 22f)
                quadraticTo(10f, 10f, 20f, 14f)
                quadraticTo(28f, 18f, 28f, 22f)
                quadraticTo(20f, 28f, 12f, 26f)
                quadraticTo(6f, 24f, 4f, 22f)
                close()
            }
            filled(body)
            val fin = Path().apply {
                moveTo(16f, 14f); lineTo(18f, 6f); lineTo(22f, 14f); close()
            }
            filled(fin)
            drawOval(eye, topLeft = Offset(20f, 18.5f), size = Size(4f, 3f))
            drawLine(detail, Offset(8f,  24f), Offset(6f,  28f), sw - 1f)
            drawLine(detail, Offset(12f, 26f), Offset(11f, 30f), sw - 1f)
        }

        // ── Delfin ────────────────────────────────────────────────────────────
        svgIcon == "drache_gruen" -> {
            val body = Path().apply {
                moveTo(4f, 18f)
                quadraticTo(10f, 8f, 20f, 12f)
                quadraticTo(28f, 16f, 26f, 22f)
                quadraticTo(20f, 28f, 10f, 24f)
                quadraticTo(4f, 20f, 4f, 18f)
                close()
            }
            filled(body)
            val tail = Path().apply {
                moveTo(26f, 16f); lineTo(30f, 10f); lineTo(28f, 18f); close()
            }
            filled(tail)
            val dorsal = Path().apply {
                moveTo(16f, 8f); quadraticTo(19f, 4f, 22f, 8f); close()
            }
            filled(dorsal)
            drawCircle(eye, 1.5f, Offset(12f, 16f))
        }

        // ── Oktopus ───────────────────────────────────────────────────────────
        svgIcon == "drache_weiss" -> {
            filledOval(Offset(8f, 7f), Size(16f, 14f))
            val fromXs = listOf(8f, 11f, 14f, 17f, 20f, 23f)
            val toXs   = listOf(8f, 12f, 15f, 17f, 20f, 24f)
            val toYs   = listOf(28f, 29f, 30f, 30f, 29f, 28f)
            fromXs.forEachIndexed { i, fx ->
                val tx = toXs[i]; val ty = toYs[i]
                val p = Path().apply {
                    moveTo(fx, 18f)
                    quadraticTo((fx + tx) / 2f - 1f, (18f + ty) / 2f, tx, ty)
                }
                drawPath(p, color, style = stroke1)
            }
            drawCircle(eye, 1.5f, Offset(12f, 12f))
            drawCircle(eye, 1.5f, Offset(20f, 12f))
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
            filled(leaf1); filled(leaf2)
        }

        // ── Sommer (Sonne mit Strahlen) ────────────────────────────────────────
        svgIcon == "jahreszeit_sommer" -> {
            filledCircle(7f, Offset(16f, 16f))
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
                quadraticTo(20f, 12f, 18f, 18f)
                quadraticTo(22f, 14f, 26f, 16f)
                quadraticTo(22f, 22f, 16f, 24f)
                quadraticTo(10f, 22f, 6f, 16f)
                quadraticTo(10f, 14f, 14f, 18f)
                quadraticTo(12f, 12f, 16f, 8f)
                close()
            }
            filled(leaf)
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
                    filledOval(Offset(cx - 5f, cy - 3f), Size(10f, 6f))
                }
            }
            filledCircle(3f, Offset(16f, 16f))
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
            filledCircle(4f, Offset(16f, 16f))
        }

        // ── Seerose (8 Blütenblätter) ─────────────────────────────────────────
        svgIcon == "blume_seerose" -> {
            listOf(0, 90, 180, 270).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                val cx = 16f + 7f * cos(rad); val cy = 16f + 7f * sin(rad)
                withTransform({ rotate(a.toFloat(), Offset(cx, cy)) }) {
                    filledOval(Offset(cx - 6f, cy - 4f), Size(12f, 8f))
                }
            }
            listOf(45, 135, 225, 315).forEach { a ->
                val rad = a * PI.toFloat() / 180f
                val cx = 16f + 7f * cos(rad); val cy = 16f + 7f * sin(rad)
                withTransform({ rotate(a.toFloat(), Offset(cx, cy)) }) {
                    filledOval(Offset(cx - 5f, cy - 3f), Size(10f, 6f))
                }
            }
            filledCircle(3f, Offset(16f, 16f))
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
            filledCircle(4f, Offset(16f, 16f))
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
