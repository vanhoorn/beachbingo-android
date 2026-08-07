package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.delay

private val KkAccent   = Color(0xFFFB7185)
private val MissColor  = Color(0xFF1E3050)
private val RedBase    = Color(0xFFEF4444)

private enum class CellView { UNKNOWN, MISS, HIT, SUNK, MYSHIP }

private fun cellBg(v: CellView): Color = when (v) {
    CellView.MISS    -> MissColor
    CellView.HIT     -> RedBase.copy(alpha = 0.53f)
    CellView.SUNK    -> RedBase.copy(alpha = 0.80f)
    CellView.MYSHIP  -> KkAccent.copy(alpha = 0.53f)
    CellView.UNKNOWN -> Color.White
}

private fun cellLabel(v: CellView): String = when (v) {
    CellView.MISS -> "•"
    CellView.HIT  -> "●"
    CellView.SUNK -> "✕"
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KuestenkriegBattleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlacement: (aiMode: String) -> Unit,
) {
    val context = LocalContext.current
    val aiModeEnum = KuestenkriegSession.aiMode
    val saveIdRef = remember { KuestenkriegSession.resumedSaveId ?: PuzzleSaveManager.generateId() }
    val startedAtRef = remember { System.currentTimeMillis() }
    var state by remember { mutableStateOf(KuestenkriegSession.resumedState ?: createBattleState(KuestenkriegSession.playerFleet)) }
    var aiMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        KuestenkriegSession.resumedState = null
        KuestenkriegSession.resumedSaveId = null
    }

    LaunchedEffect(state) {
        if (state.gameOver) {
            PuzzleSaveManager.deleteSave(context, saveIdRef)
        } else {
            PuzzleSaveManager.savePuzzle(context, PuzzleSave(
                id = saveIdRef, gameType = "kuestenkrieg_ki",
                variant = aiModeEnum.name.lowercase(), difficulty = "ki", seed = 0L,
                puzzleState = serializeBattleState(state),
                startedAt = startedAtRef, elapsedSeconds = 0,
            ))
        }
    }

    // AI turn handler
    LaunchedEffect(state.turn, state.gameOver) {
        if (state.turn == BattleTurn.AI && !state.gameOver) {
            val delayMs = when (aiModeEnum) {
                AiMode.ADMIRAL   -> 900L
                AiMode.KAPITAEN  -> 700L
                AiMode.MATROSE   -> 500L
            }
            delay(delayMs)
            val prev = state
            val next = aiShoot(state, aiModeEnum)
            state = next
            // Show which cell AI shot
            for (r in 0 until BATTLE_GRID) {
                for (c in 0 until BATTLE_GRID) {
                    if (next.playerGrid[r][c] != prev.playerGrid[r][c]) {
                        val col = ('A' + c).toString()
                        val row = r + 1
                        aiMsg = when (next.playerGrid[r][c]) {
                            ShotResult.HIT, ShotResult.SUNK -> "$col$row — Treffer!"
                            else -> "$col$row — Wasser!"
                        }
                        delay(2000L)
                        aiMsg = null
                    }
                }
            }
        }
    }

    val aiModeLabel = when (aiModeEnum) {
        AiMode.MATROSE  -> "Matrose"
        AiMode.KAPITAEN -> "Kapitän"
        AiMode.ADMIRAL  -> "Admiral"
    }

    // Build grid views
    val myView = Array(BATTLE_GRID) { r ->
        Array(BATTLE_GRID) { c ->
            when (state.playerGrid[r][c]) {
                ShotResult.HIT  -> CellView.HIT
                ShotResult.SUNK -> CellView.SUNK
                ShotResult.MISS -> CellView.MISS
                else -> {
                    val onShip = state.playerFleet.any { s -> !s.sunk && shipCells(s).any { (sr, sc) -> sr == r && sc == c } }
                    if (onShip) CellView.MYSHIP else CellView.UNKNOWN
                }
            }
        }
    }
    val enemyView = Array(BATTLE_GRID) { r ->
        Array(BATTLE_GRID) { c ->
            when (state.aiGrid[r][c]) {
                ShotResult.HIT  -> CellView.HIT
                ShotResult.SUNK -> CellView.SUNK
                ShotResult.MISS -> CellView.MISS
                else            -> CellView.UNKNOWN
            }
        }
    }

    val myRemaining  = countRemainingCells(state.playerFleet)
    val aiRemaining  = countRemainingCells(state.aiFleet)
    val isMyTurn     = state.turn == BattleTurn.PLAYER && !state.gameOver
    val aiThinking   = state.turn == BattleTurn.AI && !state.gameOver

    Column(modifier = Modifier.fillMaxSize().background(BgDark).statusBarsPadding().navigationBarsPadding()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark))).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = Surface2Dark,
                    modifier = Modifier.size(36.dp).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).clickable { onNavigateBack() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(18.dp)) } }
                Spacer(Modifier.width(10.dp))
                Text("⚓", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("KÜSTENKRIEG · $aiModeLabel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Text(
                        when {
                            state.gameOver && state.winner == BattleTurn.PLAYER -> "🏆 Sieg!"
                            state.gameOver                                       -> "💀 Niederlage!"
                            aiThinking                                          -> "KI denkt nach…"
                            isMyTurn                                            -> "Dein Schuss 🎯"
                            else                                                -> "KI ist am Zug…"
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Du: $myRemaining ❤️", fontSize = 11.sp, color = TextMuted)
                    Text("KI: $aiRemaining 💀", fontSize = 11.sp, color = TextMuted)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Reserved: padding(20) + 2 gaps(24) + 2 title rows(48) + fleet row(28) + bottom spacer(24)
            val reservedH = 144.dp
            val availPerGrid = (maxHeight - reservedH) / 2
            val cellDp = minOf(
                availPerGrid / (BATTLE_GRID + 1),
                (maxWidth - 42.dp) / BATTLE_GRID,
            ).coerceIn(18.dp, 52.dp)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // AI notification toast
            if (aiMsg != null) {
                Surface(shape = RoundedCornerShape(10.dp), color = KkAccent.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().border(1.dp, KkAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Text("KI: $aiMsg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KkAccent,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth())
                }
            }

            // Enemy grid
            BattleGridSection(
                title = "Gegnerisches Gewässer",
                subtitle = if (isMyTurn) "← Tippen zum Schießen" else null,
                grid = enemyView,
                cellDp = cellDp,
                onCellTap = { r, c ->
                    if (isMyTurn && state.aiGrid[r][c] == ShotResult.UNKNOWN) {
                        state = playerShoot(state, r, c)
                    }
                }
            )

            // My grid
            BattleGridSection(title = "Dein Gewässer", subtitle = null, grid = myView, cellDp = cellDp, onCellTap = { _, _ -> })

            // Fleet status (AI fleet)
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.aiFleet.forEachIndexed { i, ship ->
                    val def = FLEET_DEFS.getOrNull(ship.id)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (ship.sunk) RedBase.copy(alpha = 0.1f) else KkAccent.copy(alpha = 0.06f),
                        modifier = Modifier.border(1.dp, if (ship.sunk) RedBase.copy(alpha = 0.4f) else KkAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            "${def?.emoji ?: "🚢"} ${def?.name ?: "Schiff"}",
                            fontSize = 11.sp,
                            color = if (ship.sunk) RedBase else KkAccent,
                            textDecoration = if (ship.sunk) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            } // end centering Column

            // Game over panel
            if (state.gameOver) {
                val won = state.winner == BattleTurn.PLAYER
                Surface(shape = RoundedCornerShape(16.dp), color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (won) "🏆" else "💀", fontSize = 40.sp)
                        Text(if (won) "Du hast gewonnen!" else "KI hat gewonnen!",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text(if (won) "Alle feindlichen Schiffe versenkt!" else "Deine Flotte wurde vernichtet!",
                            fontSize = 13.sp, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Lobby", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { onNavigateToPlacement(KuestenkriegSession.aiMode.name.lowercase()) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KkAccent),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Nochmal!", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = BgDark) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
        } // BoxWithConstraints
    }
}

@Composable
private fun BattleGridSection(
    title: String,
    subtitle: String?,
    grid: Array<Array<CellView>>,
    cellDp: Dp,
    onCellTap: (Int, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                if (subtitle != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(subtitle, fontSize = 12.sp, color = KkAccent)
                }
            }
            Row(modifier = Modifier.padding(start = 22.dp)) {
                repeat(BATTLE_GRID) { c ->
                    Box(modifier = Modifier.size(cellDp), contentAlignment = Alignment.Center) {
                        Text(('A' + c).toString(), fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            repeat(BATTLE_GRID) { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.CenterEnd) {
                        Text("${r + 1}", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 2.dp))
                    }
                    repeat(BATTLE_GRID) { c ->
                        val v = grid[r][c]
                        Box(
                            modifier = Modifier
                                .size(cellDp)
                                .background(cellBg(v))
                                .border(0.5.dp, BorderColor)
                                .clickable { onCellTap(r, c) },
                            contentAlignment = Alignment.Center
                        ) {
                            val lbl = cellLabel(v)
                            if (lbl.isNotEmpty()) {
                                Text(lbl, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (v == CellView.MISS) TextMuted else Color.White)
                            }
                        }
                    }
                }
            }
        }
}
