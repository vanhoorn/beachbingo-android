package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.theme.*
import kotlinx.coroutines.delay


private enum class CellView { UNKNOWN, MISS, HIT, SUNK, MYSHIP }

private fun cellBg(v: CellView): Color = when (v) {
    CellView.MISS    -> BorderColor
    CellView.HIT     -> Danger.copy(alpha = 0.53f)
    CellView.SUNK    -> Danger.copy(alpha = 0.80f)
    CellView.MYSHIP  -> RoseRed.copy(alpha = 0.53f)
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
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val aiModeEnum = KuestenkriegSession.aiMode
    val saveIdRef = remember { KuestenkriegSession.resumedSaveId ?: SoloGameSaveManager.generateId() }
    val startedAtRef = remember { System.currentTimeMillis() }
    var state by remember { mutableStateOf(KuestenkriegSession.resumedState ?: createBattleState(KuestenkriegSession.playerFleet)) }
    var aiMsg by remember { mutableStateOf<String?>(null) }
    var showQuit by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var aiFireCount by remember { mutableIntStateOf(0) }
    val audio = remember { KuestenkriegAudioManager() }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.startMusic(soundEnabled, musicEnabled) }

    LaunchedEffect(Unit) {
        KuestenkriegSession.resumedState = null
        KuestenkriegSession.resumedSaveId = null
    }

    LaunchedEffect(state.gameOver) {
        if (state.gameOver) {
            audio.playSound(if (state.winner == BattleTurn.PLAYER) "win" else "lose")
        }
    }

    LaunchedEffect(state) {
        if (state.gameOver) {
            SoloGameSaveManager.deleteSave(context, saveIdRef)
        } else {
            SoloGameSaveManager.savePuzzle(context, PuzzleSave(
                id = saveIdRef, gameType = "kuestenkrieg_ki",
                variant = aiModeEnum.name.lowercase(), difficulty = "ki", seed = 0L,
                puzzleState = serializeBattleState(state),
                startedAt = startedAtRef, elapsedSeconds = 0,
            ))
        }
    }

    BackHandler {
        if (paused) paused = false else showQuit = true
    }

    // AI turn handler — aiFireCount ensures re-trigger when AI hits (turn stays AI)
    LaunchedEffect(state.turn, state.gameOver, aiFireCount, paused) {
        if (state.turn == BattleTurn.AI && !state.gameOver && !paused) {
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
                        audio.playSound(when (next.playerGrid[r][c]) {
                            ShotResult.SUNK -> "sunk"
                            ShotResult.HIT  -> "own_hit"
                            else            -> "miss"
                        })
                        delay(2000L)
                        aiMsg = null
                    }
                }
            }
            // Re-trigger if AI hit and must shoot again
            if (next.turn == BattleTurn.AI && !next.gameOver) {
                aiFireCount++
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

    Column(modifier = Modifier.fillMaxSize().background(BgDark).navigationBarsPadding()) {
        GameHudBar(
            paused = paused,
            onPauseToggle = { paused = !paused },
            onQuit = { showQuit = true },
            onShowRules = { showRules = true },
        ) {
            Text("⚓", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("KI: $aiModeLabel", fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                Text(
                    when {
                        state.gameOver && state.winner == BattleTurn.PLAYER -> "Sieg!"
                        state.gameOver -> "Niederlage!"
                        aiThinking -> "KI denkt..."
                        isMyTurn -> "Dein Schuss"
                        else -> "KI ist dran"
                    },
                    fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary,
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                Surface(shape = RoundedCornerShape(10.dp), color = RoseRed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().border(1.dp, RoseRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Text("KI: $aiMsg", fontSize = CellNumber, fontWeight = FontWeight.Bold, color = RoseRed,
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
                        audio.playSound(when (state.aiGrid[r][c]) {
                            ShotResult.SUNK -> "sunk"
                            ShotResult.HIT  -> "hit"
                            else            -> "miss"
                        })
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
                        color = if (ship.sunk) Danger.copy(alpha = 0.1f) else RoseRed.copy(alpha = 0.06f),
                        modifier = Modifier.border(1.dp, if (ship.sunk) Danger.copy(alpha = 0.4f) else RoseRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            "${def?.emoji ?: "🚢"} ${def?.name ?: "Schiff"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ship.sunk) Danger else RoseRed,
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
                        Text(if (won) "🏆" else "💀", fontSize = EmojiLarge)
                        Text(if (won) "Du hast gewonnen!" else "KI hat gewonnen!",
                            fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text(if (won) "Alle feindlichen Schiffe versenkt!" else "Deine Flotte wurde vernichtet!",
                            style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Lobby", fontSize = CellNumber, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { onNavigateToPlacement(KuestenkriegSession.aiMode.name.lowercase()) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Nochmal!", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = BgDark) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
        } // BoxWithConstraints

        if (paused && !state.gameOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Pause, null, tint = TextPrimary, modifier = Modifier.size(48.dp))
                    Text(
                        "Spiel pausiert",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    OutlinedButton(onClick = { paused = false }) { Text("Weiterspielen", color = TextSub) }
                }
            }
        }
        } // Box weight(1f)
    }

    if (showQuit) {
        GameSaveQuitDialog(
            emoji = "⚓",
            onContinue = { showQuit = false },
            onSaveAndQuit = onNavigateBack,
            onQuitWithoutSave = { SoloGameSaveManager.deleteSave(context, saveIdRef); onNavigateBack() },
        )
    }

    if (showRules) {
        KuestenkriegRulesDialog(onDismiss = { showRules = false })
    }
}

@Composable
fun KuestenkriegRulesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("⚓", fontSize = EmojiLarge)
                Text(
                    "Küstenkrieg – Regeln",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Platziere deine Flotte und versenke alle Schiffe des Gegners. Tippe auf ein Feld im gegnerischen Gewässer, um dort zu schießen.",
                        color = TextSub,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                    Text(
                        "• Wasser (•): daneben\n• Treffer (●): Schiff getroffen\n• Versenkt (✕): alle Felder eines Schiffs getroffen\n\nWer zuerst alle gegnerischen Schiffe versenkt, gewinnt.",
                        color = TextSub,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                ) {
                    Text("Verstanden", color = BgDark, fontWeight = FontWeight.Bold)
                }
            }
        }
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
                Text(title, fontSize = ChipLabel, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                if (subtitle != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(subtitle, fontSize = ChipLabel, color = RoseRed)
                }
            }
            Row(modifier = Modifier.padding(start = 22.dp)) {
                repeat(BATTLE_GRID) { c ->
                    Box(modifier = Modifier.size(cellDp), contentAlignment = Alignment.Center) {
                        Text(('A' + c).toString(), fontSize = LabelMicro, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            repeat(BATTLE_GRID) { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.CenterEnd) {
                        Text("${r + 1}", fontSize = LabelMicro, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 2.dp))
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
                                Text(lbl, fontSize = StatusTiny, fontWeight = FontWeight.ExtraBold,
                                    color = if (v == CellView.MISS) TextMuted else Color.White)
                            }
                        }
                    }
                }
            }
        }
}
