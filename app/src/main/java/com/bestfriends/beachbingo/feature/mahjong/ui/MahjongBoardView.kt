package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.bestfriends.beachbingo.feature.mahjong.MahjongTile
import com.bestfriends.beachbingo.feature.mahjong.isFree
import com.bestfriends.beachbingo.feature.raetsel.ui.ZoomableGrid
import com.bestfriends.beachbingo.ui.theme.ChipLabelTiny
import kotlin.math.max

private const val PAD = 16f

data class BoardMetrics(
    val tileW: Float,
    val tileH: Float,
    val boardW: Float,
    val boardH: Float,
    val offsetX: Float,
    val offsetY: Float,
    val initialZoom: Float = 1f,
)

fun computeMetrics(tiles: List<MahjongTile>, containerW: Float, containerH: Float): BoardMetrics {
    val active = tiles.filter { !it.removed }
    if (active.isEmpty()) return BoardMetrics(40f, 52f, containerW, containerH, 0f, 0f)

    val maxLayer = active.maxOf { it.layer }
    val minCol   = active.minOf { it.col }
    val maxCol   = active.maxOf { it.col }
    val minRow   = active.minOf { it.row }
    val maxRow   = active.maxOf { it.row }

    val colSpan = (maxCol - minCol) / 2 + 1
    val rowSpan = (maxRow - minRow) / 2 + 1

    val layerExtraW = maxLayer * TILE_LAYER_DX
    val layerExtraH = maxLayer * (-TILE_LAYER_DY)

    val availW = containerW - 2 * PAD
    val availH = containerH - 2 * PAD

    val tileWByW = (availW - layerExtraW) / colSpan
    val tileWByH = (availH - layerExtraH) / (rowSpan * 1.3f)
    val naturalTileW = minOf(tileWByW, tileWByH, 60f)
    val tileW        = max(26f, naturalTileW)
    val tileH        = tileW * 1.3f
    val initialZoom  = (naturalTileW / tileW).coerceIn(0.25f, 1f)

    val EDGE_R = tileW * 0.12f
    val EDGE_B = tileH * 0.10f

    val boardW = colSpan * tileW + layerExtraW + EDGE_R
    val boardH = rowSpan * tileH + layerExtraH + EDGE_B

    val offsetX = ((containerW - boardW) / 2f).coerceAtLeast(PAD)
    val offsetY = ((containerH - boardH) / 2f).coerceAtLeast(PAD)

    return BoardMetrics(tileW, tileH, boardW, boardH, offsetX, offsetY, initialZoom)
}

private fun tilePixelX(tile: MahjongTile, m: BoardMetrics, minCol: Int): Float =
    (tile.col - minCol) / 2f * m.tileW + tile.layer * TILE_LAYER_DX

private fun tilePixelY(tile: MahjongTile, m: BoardMetrics, minRow: Int): Float =
    (tile.row - minRow) / 2f * m.tileH + tile.layer * TILE_LAYER_DY

@Composable
fun MahjongBoardView(
    tiles: List<MahjongTile>,
    selectedId: Int?,
    hintIds: Set<Int>,
    flashIds: Set<Int> = emptySet(),
    showFreeHighlight: Boolean,
    onTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        val active = tiles.filter { !it.removed }

        val metrics = remember(tiles, containerW, containerH) {
            computeMetrics(tiles, containerW, containerH)
        }

        val initialPan = remember(metrics) {
            if (metrics.initialZoom >= 1f) Offset.Zero
            else Offset(
                x = containerW / 2f * (1f - metrics.initialZoom),
                y = containerH / 2f * (1f - metrics.initialZoom),
            )
        }

        val minCol = remember(tiles) { if (active.isEmpty()) 0 else active.minOf { it.col } }
        val minRow = remember(tiles) { if (active.isEmpty()) 0 else active.minOf { it.row } }

        val freeSet = remember(tiles) {
            tiles.filter { !it.removed }
                .filter { t -> isFree(t, tiles) }
                .map { it.id }
                .toSet()
        }

        val sorted = remember(tiles, flashIds) {
            tiles.filter { !it.removed || it.id in flashIds }
                .sortedWith(compareBy({ it.layer }, { it.row }, { it.col }))
        }

        ZoomableGrid(
            modifier = Modifier.fillMaxSize(),
            initialZoom = metrics.initialZoom,
            initialPan = initialPan,
            onTap = { gx, gy ->
                val hit = sorted.lastOrNull { tile ->
                    if (tile.removed) return@lastOrNull false
                    val px = tilePixelX(tile, metrics, minCol) + metrics.offsetX
                    val py = tilePixelY(tile, metrics, minRow) + metrics.offsetY
                    gx >= px && gx < px + metrics.tileW && gy >= py && gy < py + metrics.tileH
                }
                if (hit != null) onTileClick(hit.id)
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                sorted.forEach { tile ->
                    val px = tilePixelX(tile, metrics, minCol) + metrics.offsetX
                    val py = tilePixelY(tile, metrics, minRow) + metrics.offsetY
                    with(density) {
                        Box(
                            modifier = Modifier
                                .absoluteOffset(px.toDp(), py.toDp())
                                .size(metrics.tileW.toDp(), metrics.tileH.toDp()),
                        ) {
                            MahjongTileCanvas(
                                tile              = tile,
                                tileW             = metrics.tileW,
                                tileH             = metrics.tileH,
                                selected          = tile.id == selectedId,
                                hinted            = tile.id in hintIds,
                                free              = tile.id in freeSet,
                                showFreeHighlight = showFreeHighlight,
                                removing          = tile.id in flashIds,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Doppeltippen: Zoom zurücksetzen",
            color = Color.White.copy(alpha = 0.30f),
            fontSize = ChipLabelTiny,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(density) { 4f.toDp() }),
        )
    }
}
