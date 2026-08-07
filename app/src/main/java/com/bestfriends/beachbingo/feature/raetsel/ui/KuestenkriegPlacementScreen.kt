package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import kotlin.random.Random

private val KkAccent = Color(0xFFFB7185)
private val DragInvalidColor = Color(0xFFEF4444)

private data class DragState(val start: Pair<Int, Int>, val current: Pair<Int, Int>)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KuestenkriegPlacementScreen(
    aiMode: String,
    onNavigateBack: () -> Unit,
    onNavigateToBattle: () -> Unit,
) {
    var fleet by remember { mutableStateOf(listOf<PlacedShip>()) }
    var horiz by remember { mutableStateOf(true) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var gridWidthPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current

    val activeIdx = fleet.size
    val allPlaced = fleet.size == FLEET_DEFS.size

    fun buildOccupied(): Array<BooleanArray> {
        val g = Array(BATTLE_GRID) { BooleanArray(BATTLE_GRID) }
        fleet.forEach { placeOnGrid(g, it) }
        return g
    }

    fun removeLastShip() {
        if (fleet.isEmpty()) return
        fleet = fleet.dropLast(1)
    }

    fun randomizeAll() {
        val g = Array(BATTLE_GRID) { BooleanArray(BATTLE_GRID) }
        val newFleet = mutableListOf<PlacedShip>()
        FLEET_DEFS.forEachIndexed { id, def ->
            repeat(300) {
                val h = Random.nextBoolean()
                val r = Random.nextInt(BATTLE_GRID)
                val c = Random.nextInt(BATTLE_GRID)
                if (canPlaceShip(g, r, c, def.size, h)) {
                    val ship = PlacedShip(id, def.size, r, c, h)
                    placeOnGrid(g, ship)
                    newFleet.add(ship)
                    return@repeat
                }
            }
        }
        fleet = newFleet
    }

    fun startBattle() {
        KuestenkriegSession.playerFleet = fleet
        KuestenkriegSession.aiMode = when (aiMode) {
            "admiral"  -> AiMode.ADMIRAL
            "matrose"  -> AiMode.MATROSE
            else       -> AiMode.KAPITAEN
        }
        onNavigateToBattle()
    }

    val occupied = buildOccupied()
    val currentDef = if (!allPlaced) FLEET_DEFS[activeIdx] else null

    // Drag preview: cells the current ship would occupy if placed now
    val ds = dragState
    // Orientation is always controlled by the button (never inferred from drag direction)
    val dragIsH: Boolean = horiz

    val dragPreview: Set<Pair<Int, Int>> = if (ds != null && !allPlaced && currentDef != null) {
        val (cr, cc) = ds.current  // preview follows the current finger position
        (0 until currentDef.size).mapNotNull { i ->
            val r2 = if (dragIsH) cr else cr + i
            val c2 = if (dragIsH) cc + i else cc
            if (r2 in 0 until BATTLE_GRID && c2 in 0 until BATTLE_GRID) Pair(r2, c2) else null
        }.toSet()
    } else emptySet()

    val dragIsValid: Boolean = if (ds != null && dragPreview.isNotEmpty() && !allPlaced && currentDef != null)
        canPlaceShip(occupied, ds.current.first, ds.current.second, currentDef.size, dragIsH)
    else false

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding().navigationBarsPadding()) {
        val cellDp = ((maxWidth - 54.dp) / BATTLE_GRID).coerceIn(18.dp, 52.dp)
        val cellPxF = with(density) { cellDp.toPx() }
        val labelColPxF = with(density) { 20.dp.toPx() }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
            .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text("⚓", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("KÜSTENKRIEG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                    Text("Schiffe setzen", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
                Text("${fleet.size}/${FLEET_DEFS.size} gesetzt", fontSize = 13.sp, color = TextMuted)
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // Active ship info
            if (currentDef != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, KkAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(currentDef.emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentDef.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Länge ${currentDef.size}", fontSize = 12.sp, color = TextMuted)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = Surface2Dark,
                            modifier = Modifier
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { horiz = !horiz }
                        ) {
                            Text(
                                if (horiz) "↔ Horizontal" else "↕ Vertikal",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Grid — drag-to-place handles gestures at container level
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { gridWidthPx = it.width }
                    .pointerInput(fleet.size) {
                        if (fleet.size >= FLEET_DEFS.size) return@pointerInput
                        awaitEachGesture {
                            val gridTotalWidthF = labelColPxF + BATTLE_GRID * cellPxF
                            val gridLeftF = (gridWidthPx - gridTotalWidthF) / 2f

                            fun posToCell(x: Float, y: Float): Pair<Int, Int>? {
                                val c = ((x - gridLeftF - labelColPxF) / cellPxF).toInt()
                                val r = ((y - cellPxF) / cellPxF).toInt()
                                return if (r in 0 until BATTLE_GRID && c in 0 until BATTLE_GRID)
                                    Pair(r, c) else null
                            }

                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startCell = posToCell(down.position.x, down.position.y)
                                ?: return@awaitEachGesture
                            dragState = DragState(startCell, startCell)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                change.consume()
                                val cell = posToCell(change.position.x, change.position.y)
                                if (cell != null) dragState = DragState(startCell, cell)
                            }

                            val gesture = dragState
                            if (gesture != null && fleet.size < FLEET_DEFS.size) {
                                val currFleetSize = fleet.size
                                val (cr, cc) = gesture.current  // place where the finger lifted
                                val def = FLEET_DEFS[currFleetSize]
                                val g = buildOccupied()
                                if (canPlaceShip(g, cr, cc, def.size, horiz)) {
                                    fleet = fleet + PlacedShip(currFleetSize, def.size, cr, cc, horiz)
                                }
                            }
                            dragState = null
                        }
                    }
            ) {
                // Column labels
                Row(modifier = Modifier.padding(start = 22.dp)) {
                    repeat(BATTLE_GRID) { c ->
                        Box(modifier = Modifier.size(cellDp), contentAlignment = Alignment.Center) {
                            Text(('A' + c).toString(), fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                repeat(BATTLE_GRID) { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.CenterEnd) {
                            Text("${r + 1}", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 2.dp))
                        }
                        repeat(BATTLE_GRID) { c ->
                            val cellKey = Pair(r, c)
                            val isOccupied = occupied[r][c]
                            val isPreview = cellKey in dragPreview
                            val cellBg = when {
                                isPreview && dragIsValid  -> KkAccent.copy(alpha = 0.8f)
                                isPreview && !dragIsValid -> DragInvalidColor.copy(alpha = 0.5f)
                                isOccupied               -> KkAccent.copy(alpha = 0.55f)
                                else                     -> Color.White
                            }
                            Box(modifier = Modifier
                                .size(cellDp)
                                .background(cellBg)
                                .border(0.5.dp, BorderColor)
                            )
                        }
                    }
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = ::removeLastShip,
                    enabled = fleet.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                ) { Text("⌫ Rückgängig", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = ::randomizeAll,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                ) { Text("🎲 Zufällig", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }

            // Fleet checklist
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLEET_DEFS.forEachIndexed { i, def ->
                    val placed = i < fleet.size
                    val active = i == activeIdx && !allPlaced
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (placed) KkAccent.copy(alpha = 0.1f) else SurfaceDark,
                        modifier = Modifier.border(
                            1.dp,
                            if (active) KkAccent else if (placed) KkAccent.copy(alpha = 0.4f) else BorderColor,
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            "${def.emoji} ${def.name}",
                            fontSize = 12.sp,
                            color = if (placed) KkAccent else TextMuted,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            textDecoration = TextDecoration.None,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Start button
            Button(
                onClick = ::startBattle,
                enabled = allPlaced,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KkAccent,
                    disabledContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (allPlaced) "⚓ Auf ins Gefecht!"
                    else "Noch ${FLEET_DEFS.size - fleet.size} Schiff${if (FLEET_DEFS.size - fleet.size != 1) "e" else ""} platzieren",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (allPlaced) BgDark else TextMuted
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    } // BoxWithConstraints
}
