package com.bestfriends.beachbingo.feature.perlentaucher.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.lerp
import com.bestfriends.beachbingo.feature.perlentaucher.BOARD_SIZE
import com.bestfriends.beachbingo.feature.perlentaucher.PerlentaucherPiece
import com.bestfriends.beachbingo.feature.perlentaucher.PieceType
import com.bestfriends.beachbingo.feature.perlentaucher.SpecialType
import kotlin.math.*

private val BgSea     = Color(0xFF012A47) // board background
private val BgCell    = Color(0xFF033C63) // cell background
private val BgCellSel = Color(0xFF0EA5E9) // selected cell
private val GridLine  = Color(0xFF0A4A73) // grid lines
private val ScoreBg   = Color(0xFF021D30) // progress strip background

private val PieceColors = mapOf(
    PieceType.PERLE    to Color(0xFFF5EFE0),
    PieceType.SEEGLAS  to Color(0xFF00BCD4),
    PieceType.MUSCHEL  to Color(0xFFE91E8C),
    PieceType.SEESTERN to Color(0xFFFF5722),
    PieceType.KORALLE  to Color(0xFFEF5350),
    PieceType.SEETANG  to Color(0xFF4CAF50),
)

// Pre-swap piece data for the swap interpolation animation
data class SwapAnimData(
    val cell1: Pair<Int, Int>,
    val cell2: Pair<Int, Int>,
    val piece1: PerlentaucherPiece?,   // original piece at cell1 → animates to cell2
    val piece2: PerlentaucherPiece?,   // original piece at cell2 → animates to cell1
)

@Composable
fun PerlentaucherBoardView(
    board: Array<Array<PerlentaucherPiece?>>,
    fallenCols: Map<Int, Int>,          // col → max rows fallen (was Set<Int>)
    targetScore: Int,
    currentScore: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSwap: (r1: Int, c1: Int, r2: Int, c2: Int) -> Unit,
    onPieceSelected: () -> Unit = {},
    swapAnimData: SwapAnimData? = null,
    explosionCells: List<Pair<Int, Int>> = emptyList(),
) {
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragStart    by remember { mutableStateOf(Offset.Zero) }
    var totalDrag    by remember { mutableStateOf(Offset.Zero) }

    // ── Swap animation: 0 = pieces at original positions, 1 = pieces at new positions ──
    val swapAnim = remember { Animatable(1f) }
    LaunchedEffect(swapAnimData) {
        if (swapAnimData == null) return@LaunchedEffect
        swapAnim.snapTo(0f)
        swapAnim.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
    }
    val swapProg = swapAnim.value

    // ── Fall animation: duration proportional to max fall distance ──────────────
    val maxFallRows = if (fallenCols.isEmpty()) 1 else (fallenCols.values.maxOrNull() ?: 1)
    val fallDuration = (80 + maxFallRows * 60).coerceIn(140, 440)
    val fallAnim = remember { Animatable(0f) }
    LaunchedEffect(fallenCols) {
        if (fallenCols.isEmpty()) return@LaunchedEffect
        fallAnim.snapTo(1f)
        fallAnim.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = fallDuration
                0.06f at (fallDuration * 0.75f).toInt()
                -0.07f at (fallDuration * 0.84f).toInt()
                0.03f at (fallDuration * 0.91f).toInt()
                0f at fallDuration
            },
        )
    }
    val fallOffset = fallAnim.value

    // ── Explosion animation: expanding + fading circles on cleared cells ─────────
    val explAnim = remember { Animatable(0f) }
    LaunchedEffect(explosionCells) {
        if (explosionCells.isEmpty()) return@LaunchedEffect
        explAnim.snapTo(0f)
        explAnim.animateTo(1f, tween(450, easing = LinearEasing))
    }
    val explProg = explAnim.value

    Canvas(
        modifier = modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { offset ->
                    dragStart = offset
                    totalDrag = Offset.Zero
                    // Highlight + haptic immediately when finger touches a valid cell
                    val wPx = size.width.toFloat()
                    val hPx = size.height.toFloat()
                    val boardSize = minOf(wPx, hPx)
                    val boardLeft = (wPx - boardSize) / 2f
                    val boardTop  = (hPx - boardSize) / 2f + 5f
                    val cellPx    = boardSize / BOARD_SIZE.toFloat()
                    val r = ((offset.y - boardTop)  / cellPx).toInt()
                    val c = ((offset.x - boardLeft) / cellPx).toInt()
                    if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                        selectedCell = Pair(r, c)
                        onPieceSelected()
                    }
                },
                onDrag = { change, amount ->
                    change.consume()
                    totalDrag += amount
                },
                onDragEnd = {
                    val wPx = size.width.toFloat()
                    val hPx = size.height.toFloat()
                    val boardSize = minOf(wPx, hPx)
                    val boardLeft = (wPx - boardSize) / 2f
                    val boardTop  = (hPx - boardSize) / 2f + 5f
                    val cellPx = boardSize / BOARD_SIZE.toFloat()
                    val threshold = cellPx * 0.35f

                    val startR = ((dragStart.y - boardTop) / cellPx).toInt()
                    val startC = ((dragStart.x - boardLeft) / cellPx).toInt()
                    if (startR !in 0 until BOARD_SIZE || startC !in 0 until BOARD_SIZE) {
                        totalDrag = Offset.Zero
                        return@detectDragGestures
                    }

                    val dx = totalDrag.x
                    val dy = totalDrag.y

                    if (abs(dx) < threshold && abs(dy) < threshold) {
                        // Tap
                        val tappedCell = Pair(startR, startC)
                        val sel = selectedCell
                        when {
                            sel == null                           -> { selectedCell = tappedCell; onPieceSelected() }
                            sel == tappedCell                     -> selectedCell = null
                            abs(sel.first - startR) + abs(sel.second - startC) == 1 -> {
                                onSwap(sel.first, sel.second, startR, startC)
                                selectedCell = null
                            }
                            else                                  -> selectedCell = tappedCell
                        }
                    } else {
                        // Swipe
                        val (dr, dc) = if (abs(dx) > abs(dy))
                            Pair(0, if (dx > 0) 1 else -1)
                        else
                            Pair(if (dy > 0) 1 else -1, 0)
                        val targetR = startR + dr
                        val targetC = startC + dc
                        if (targetR in 0 until BOARD_SIZE && targetC in 0 until BOARD_SIZE) {
                            onSwap(startR, startC, targetR, targetC)
                        }
                        selectedCell = null
                    }
                    totalDrag = Offset.Zero
                },
            )
        },
    ) {
        val boardPx = minOf(size.width, size.height)
        val offsetX = (size.width  - boardPx) / 2f
        val offsetY = (size.height - boardPx) / 2f
        val cellPx  = boardPx / BOARD_SIZE
        val pieceR  = cellPx * 0.36f

        // ── Board background ─────────────────────────────────────────────────────
        drawRect(BgSea, topLeft = Offset(offsetX, offsetY), size = Size(boardPx, boardPx))

        // Score progress strip (top 5px of board)
        val progressW = (currentScore.toFloat() / targetScore.coerceAtLeast(1)).coerceIn(0f, 1f) * boardPx
        drawRect(ScoreBg, topLeft = Offset(offsetX, offsetY), size = Size(boardPx, 5f))
        drawRect(Color(0xFF0EA5E9), topLeft = Offset(offsetX, offsetY), size = Size(progressW, 5f))

        // ── Cell backgrounds + grid ──────────────────────────────────────────────
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val cx = offsetX + col * cellPx
                val cy = offsetY + row * cellPx + 5f
                val isSelected = selectedCell == Pair(row, col)

                drawRect(
                    color = if (isSelected) BgCellSel.copy(alpha = 0.3f) else BgCell.copy(alpha = 0.5f),
                    topLeft = Offset(cx + 1f, cy + 1f),
                    size = Size(cellPx - 2f, cellPx - 2f),
                )
                if (isSelected) {
                    drawRect(
                        color = BgCellSel.copy(alpha = 0.7f),
                        topLeft = Offset(cx + 1f, cy + 1f),
                        size = Size(cellPx - 2f, cellPx - 2f),
                        style = Stroke(width = 2.5f),
                    )
                }
            }
        }

        // Grid lines
        for (i in 0..BOARD_SIZE) {
            drawLine(GridLine, Offset(offsetX + i * cellPx, offsetY), Offset(offsetX + i * cellPx, offsetY + boardPx), 1f)
            drawLine(GridLine, Offset(offsetX, offsetY + i * cellPx), Offset(offsetX + boardPx, offsetY + i * cellPx), 1f)
        }

        // ── Normal pieces (skip swap-animation cells while they are in flight) ───
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val piece = board[row][col] ?: continue
                // The swap ghosts handle these cells while the animation is active
                if (swapAnimData != null && swapProg < 1f) {
                    if (Pair(row, col) == swapAnimData.cell1 || Pair(row, col) == swapAnimData.cell2) continue
                }
                val cx    = offsetX + col * cellPx + cellPx / 2f
                val rawCy = offsetY + row * cellPx + cellPx / 2f + 5f
                val animCy = if (col in fallenCols) rawCy - fallOffset * cellPx else rawCy

                withTransform({ translate(0f, animCy - rawCy) }) {
                    drawPiece(piece, cx, rawCy, pieceR, PieceColors[piece.type] ?: Color.White)
                }
            }
        }

        // ── Swap ghost pieces: animate from their original positions ─────────────
        if (swapAnimData != null && swapProg < 1f) {
            fun drawSwapGhost(piece: PerlentaucherPiece?, fromCell: Pair<Int, Int>, toCell: Pair<Int, Int>) {
                piece ?: return
                val fromCx = offsetX + fromCell.second * cellPx + cellPx / 2f
                val fromCy = offsetY + fromCell.first  * cellPx + cellPx / 2f + 5f
                val toCx   = offsetX + toCell.second   * cellPx + cellPx / 2f
                val toCy   = offsetY + toCell.first    * cellPx + cellPx / 2f + 5f
                val cx = lerp(fromCx, toCx, swapProg)
                val cy = lerp(fromCy, toCy, swapProg)
                drawPiece(piece, cx, cy, pieceR, PieceColors[piece.type] ?: Color.White)
            }
            drawSwapGhost(swapAnimData.piece1, swapAnimData.cell1, swapAnimData.cell2)
            drawSwapGhost(swapAnimData.piece2, swapAnimData.cell2, swapAnimData.cell1)
        }

        // ── Explosions: expanding rings over cleared cells ────────────────────────
        if (explosionCells.isNotEmpty() && explProg < 1f) {
            val maxRadius = cellPx * 0.78f
            for ((er, ec) in explosionCells) {
                val ecx = offsetX + ec * cellPx + cellPx / 2f
                val ecy = offsetY + er * cellPx + cellPx / 2f + 5f
                val ringR = explProg * maxRadius
                val fade  = (1f - explProg).coerceIn(0f, 1f)
                // Outer white ring
                drawCircle(Color.White.copy(alpha = fade * 0.90f), ringR, Offset(ecx, ecy), style = Stroke(width = cellPx * 0.09f))
                // Inner gold ring
                drawCircle(Color(0xFFFFD700).copy(alpha = fade * 0.75f), ringR * 0.60f, Offset(ecx, ecy), style = Stroke(width = cellPx * 0.07f))
                // Center flash in first 40% of animation
                if (explProg < 0.4f) {
                    val flashFade = 1f - explProg / 0.4f
                    drawCircle(Color.White.copy(alpha = flashFade * 0.85f), cellPx * 0.30f * flashFade + cellPx * 0.05f, Offset(ecx, ecy))
                }
            }
        }
    }
}

// ── Piece drawing dispatcher ───────────────────────────────────────────────────

private fun DrawScope.drawPiece(piece: PerlentaucherPiece, cx: Float, cy: Float, r: Float, color: Color) {
    when (piece.type) {
        PieceType.PERLE    -> drawPearl(cx, cy, r, color)
        PieceType.SEEGLAS  -> drawSeaGlass(cx, cy, r, color)
        PieceType.MUSCHEL  -> drawShell(cx, cy, r, color)
        PieceType.SEESTERN -> drawStarfish(cx, cy, r, color)
        PieceType.KORALLE  -> drawCoral(cx, cy, r, color)
        PieceType.SEETANG  -> drawSeaWeed(cx, cy, r, color)
    }
    when (piece.special) {
        SpecialType.GESTREIFT_H  -> drawSpecialStripesH(cx, cy, r, color)
        SpecialType.GESTREIFT_V  -> drawSpecialStripesV(cx, cy, r, color)
        SpecialType.EINGEPACKT   -> drawSpecialWrapped(cx, cy, r)
        SpecialType.PERLENKETTE  -> drawSpecialStarburst(cx, cy, r)
        SpecialType.NONE         -> {}
    }
}

// ── Color helpers ─────────────────────────────────────────────────────────────

private fun lightened(c: Color, by: Float = 0.42f) = Color(
    red   = (c.red   + (1f - c.red)   * by).coerceIn(0f, 1f),
    green = (c.green + (1f - c.green) * by).coerceIn(0f, 1f),
    blue  = (c.blue  + (1f - c.blue)  * by).coerceIn(0f, 1f),
    alpha = c.alpha,
)

private fun darkened(c: Color, by: Float = 0.35f) = Color(
    red   = (c.red   * (1f - by)).coerceIn(0f, 1f),
    green = (c.green * (1f - by)).coerceIn(0f, 1f),
    blue  = (c.blue  * (1f - by)).coerceIn(0f, 1f),
    alpha = c.alpha,
)

// ── Piece drawing functions ────────────────────────────────────────────────────

private fun DrawScope.drawPearl(cx: Float, cy: Float, r: Float, color: Color) {
    val lit = lightened(color)
    val drk = darkened(color)
    drawCircle(Color.Black.copy(alpha = 0.18f), r * 0.96f, Offset(cx + r * 0.08f, cy + r * 0.12f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(lit, color, drk),
            center = Offset(cx - r * 0.18f, cy - r * 0.18f),
            radius = r * 1.35f,
        ),
        radius = r,
        center = Offset(cx, cy),
    )
    drawCircle(Color.White.copy(alpha = 0.72f), r * 0.36f, Offset(cx - r * 0.28f, cy - r * 0.28f))
    drawCircle(Color.White.copy(alpha = 0.28f), r * 0.14f, Offset(cx - r * 0.08f, cy - r * 0.42f))
    drawCircle(lit.copy(alpha = 0.22f), r * 0.38f, Offset(cx + r * 0.14f, cy + r * 0.20f))
}

private fun DrawScope.drawSeaGlass(cx: Float, cy: Float, r: Float, color: Color) {
    val lit = lightened(color, 0.50f)
    val drk = darkened(color, 0.28f)
    val rw = r * 0.90f; val rh = r * 0.78f
    val cr = CornerRadius(r * 0.44f)
    drawRoundRect(Color.Black.copy(alpha = 0.16f), Offset(cx - rw + r * 0.06f, cy - rh + r * 0.10f), Size(rw * 2f, rh * 2f), cr)
    drawRoundRect(drk,   Offset(cx - rw,          cy - rh),          Size(rw * 2f,    rh * 2f),    cr)
    drawRoundRect(color, Offset(cx - rw * 0.72f,  cy - rh * 0.72f),  Size(rw * 1.44f, rh * 1.44f), CornerRadius(r * 0.30f))
    drawRoundRect(lit.copy(alpha = 0.55f), Offset(cx - rw * 0.45f, cy - rh * 0.58f), Size(rw * 0.90f, rh * 0.55f), CornerRadius(r * 0.18f))
    drawRoundRect(Color.White.copy(alpha = 0.20f), Offset(cx - rw, cy - rh), Size(rw * 2f, rh * 2f), cr, style = Stroke(r * 0.09f))
}

private fun DrawScope.drawShell(cx: Float, cy: Float, r: Float, color: Color) {
    val lit = lightened(color, 0.40f)
    val drk = darkened(color, 0.32f)
    val hinge = Offset(cx, cy + r * 0.22f)
    val bodyPath = Path().apply {
        moveTo(hinge.x, hinge.y)
        addArc(Rect(cx - r, cy - r * 0.72f, cx + r, cy + r), 10f, 160f)
        close()
    }
    drawPath(bodyPath, lit.copy(alpha = 0.92f))
    for (i in 0..5) {
        val a = (i * 27f - 68f).toDouble() * PI / 180.0
        drawLine(drk, hinge,
            Offset(cx + (r * 0.94f * cos(a)).toFloat(), cy - (r * 0.86f * sin(a)).toFloat() + r * 0.08f),
            r * 0.10f, StrokeCap.Round)
    }
    drawPath(bodyPath, color, style = Stroke(r * 0.11f, cap = StrokeCap.Round))
    drawCircle(drk, r * 0.18f, hinge)
    drawCircle(lit.copy(alpha = 0.80f), r * 0.08f, hinge)
}

private fun DrawScope.drawStarfish(cx: Float, cy: Float, r: Float, color: Color) {
    val lit = lightened(color, 0.40f)
    val drk = darkened(color, 0.30f)
    val outerR = r; val innerR = r * 0.40f

    fun starPath(ox: Float = 0f, oy: Float = 0f) = Path().apply {
        for (i in 0 until 10) {
            val a = (i * 36f - 90f).toDouble() * PI / 180.0
            val rad = if (i % 2 == 0) outerR else innerR
            val x = cx + ox + (rad * cos(a)).toFloat(); val y = cy + oy + (rad * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }; close()
    }
    drawPath(starPath(r * 0.06f, r * 0.07f), Color.Black.copy(alpha = 0.18f))
    drawPath(starPath(), color)
    for (i in 0 until 5) {
        val a = (i * 72f - 90f).toDouble() * PI / 180.0
        drawLine(drk.copy(alpha = 0.50f), Offset(cx, cy),
            Offset(cx + (outerR * 0.88f * cos(a)).toFloat(), cy + (outerR * 0.88f * sin(a)).toFloat()),
            r * 0.12f, StrokeCap.Round)
        for (dist in listOf(0.42f, 0.72f)) {
            val dx = cx + (outerR * dist * cos(a)).toFloat(); val dy = cy + (outerR * dist * sin(a)).toFloat()
            drawCircle(drk.copy(alpha = 0.65f), r * 0.075f, Offset(dx, dy))
            drawCircle(lit.copy(alpha = 0.45f), r * 0.035f, Offset(dx, dy))
        }
    }
    drawCircle(drk, r * 0.24f, Offset(cx, cy))
    drawCircle(color, r * 0.16f, Offset(cx, cy))
    drawCircle(lit.copy(alpha = 0.55f), r * 0.07f, Offset(cx - r * 0.05f, cy - r * 0.05f))
}

private fun DrawScope.drawCoral(cx: Float, cy: Float, r: Float, color: Color) {
    val lit = lightened(color, 0.45f)
    val drk = darkened(color, 0.28f)
    val sw  = r * 0.36f  // thicker trunk

    val bottom = Offset(cx,             cy + r * 0.78f)
    val mid    = Offset(cx,             cy - r * 0.02f)
    val top    = Offset(cx,             cy - r * 0.44f)
    val leftB  = Offset(cx - r * 0.68f, cy - r * 0.62f)
    val rightB = Offset(cx + r * 0.66f, cy - r * 0.52f)
    val leftT  = Offset(cx - r * 0.36f, cy - r * 0.88f)
    val rightT = Offset(cx + r * 0.32f, cy - r * 0.92f)

    drawLine(color, bottom, top, sw, StrokeCap.Round)
    drawLine(color, mid, leftB, sw * 0.76f, StrokeCap.Round)
    drawLine(color, Offset(cx, cy - r * 0.12f), rightB, sw * 0.76f, StrokeCap.Round)
    drawLine(lit, leftB,  leftT,  sw * 0.54f, StrokeCap.Round)
    drawLine(lit, rightB, rightT, sw * 0.54f, StrokeCap.Round)
    val tipR = sw * 0.88f
    for (tip in listOf(top, leftB, rightB, leftT, rightT)) {
        drawCircle(drk, tipR, tip)
        drawCircle(lit, tipR * 0.52f, tip)
    }
}

private fun DrawScope.drawSeaWeed(cx: Float, cy: Float, r: Float, color: Color) {
    val lit = lightened(color, 0.42f)
    val drk = darkened(color, 0.28f)
    val sw  = r * 0.22f

    val stemPath = Path().apply {
        moveTo(cx, cy + r * 0.90f)
        cubicTo(cx - r * 0.55f, cy + r * 0.40f, cx + r * 0.55f, cy - r * 0.15f, cx - r * 0.15f, cy - r * 0.65f)
    }
    drawPath(stemPath, drk,   style = Stroke(sw * 1.55f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(stemPath, color, style = Stroke(sw,          cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Leaf specs: x, y, rotation angle, scale
    val leafSpecs = listOf(
        floatArrayOf(cx - r * 0.32f, cy + r * 0.42f, -38f, 1.00f),
        floatArrayOf(cx + r * 0.26f, cy + r * 0.04f,  32f, 0.88f),
        floatArrayOf(cx - r * 0.20f, cy - r * 0.36f, -48f, 0.74f),
        floatArrayOf(cx + r * 0.08f, cy - r * 0.60f,  42f, 0.62f),
    )
    for (spec in leafSpecs) {
        val lx = spec[0]; val ly = spec[1]; val angle = spec[2]; val scale = spec[3]
        val lw = r * 0.44f * scale; val lh = r * 0.21f * scale
        withTransform({ rotate(angle, Offset(lx, ly)) }) {
            drawOval(lit.copy(alpha = 0.90f), Offset(lx - lw, ly - lh), Size(lw * 2f, lh * 2f))
            drawLine(color.copy(alpha = 0.65f), Offset(lx - lw * 0.68f, ly), Offset(lx + lw * 0.68f, ly), r * 0.035f, StrokeCap.Round)
        }
    }
}

// ── Special overlays ──────────────────────────────────────────────────────────

private fun DrawScope.drawSpecialStripesH(cx: Float, cy: Float, r: Float, baseColor: Color) {
    val lighter = baseColor.copy(alpha = 0.6f, red = minOf(1f, baseColor.red * 1.4f), green = minOf(1f, baseColor.green * 1.4f), blue = minOf(1f, baseColor.blue * 1.4f))
    for (i in -1..1) {
        drawLine(
            lighter,
            Offset(cx - r, cy + i * r * 0.38f),
            Offset(cx + r, cy + i * r * 0.38f),
            r * 0.16f,
        )
    }
}

private fun DrawScope.drawSpecialStripesV(cx: Float, cy: Float, r: Float, baseColor: Color) {
    val lighter = baseColor.copy(alpha = 0.6f, red = minOf(1f, baseColor.red * 1.4f), green = minOf(1f, baseColor.green * 1.4f), blue = minOf(1f, baseColor.blue * 1.4f))
    for (i in -1..1) {
        drawLine(
            lighter,
            Offset(cx + i * r * 0.38f, cy - r),
            Offset(cx + i * r * 0.38f, cy + r),
            r * 0.16f,
        )
    }
}

private fun DrawScope.drawSpecialWrapped(cx: Float, cy: Float, r: Float) {
    // Glowing rounded frame
    drawRect(
        color = Color(0xCCFFD700),
        topLeft = Offset(cx - r * 0.9f, cy - r * 0.9f),
        size = Size(r * 1.8f, r * 1.8f),
        style = Stroke(width = r * 0.18f, pathEffect = PathEffect.cornerPathEffect(r * 0.3f)),
    )
    // Corner dots
    for ((dx, dy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) {
        drawCircle(Color(0xCCFFD700), r * 0.12f, Offset(cx + dx * r * 0.8f, cy + dy * r * 0.8f))
    }
}

private fun DrawScope.drawSpecialStarburst(cx: Float, cy: Float, r: Float) {
    // Radiating rays
    for (i in 0 until 8) {
        val angle = (i * 45f).toDouble() * PI / 180.0
        drawLine(
            Color(0xCCFFFFFF),
            Offset(cx + (r * 0.45f * cos(angle)).toFloat(), cy + (r * 0.45f * sin(angle)).toFloat()),
            Offset(cx + (r * 0.95f * cos(angle)).toFloat(), cy + (r * 0.95f * sin(angle)).toFloat()),
            r * 0.14f,
            cap = StrokeCap.Round,
        )
    }
    // Center ring
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.3f, Offset(cx, cy), style = Stroke(r * 0.12f))
}
