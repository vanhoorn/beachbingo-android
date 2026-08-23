package com.bestfriends.beachbingo.feature.mahjong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.bestfriends.beachbingo.feature.mahjong.*
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSave
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MahjongGameScreen(
    layout: String,
    difficulty: String,
    seed: Long,
    saveId: String?,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val auth    = FirebaseAuth.getInstance()
    val db      = FirebaseFirestore.getInstance()
    val uid     = auth.currentUser?.uid ?: ""
    val scope   = rememberCoroutineScope()

    val audio = remember { MahjongAudioManager(context) }
    DisposableEffect(Unit) { onDispose { audio.release() } }


    val layoutId   = remember { runCatching { LayoutId.valueOf(layout) }.getOrElse { LayoutId.SCHILDKROETE } }
    val diff       = remember { runCatching { MahjongDifficulty.valueOf(difficulty) }.getOrElse { MahjongDifficulty.ROOKIE } }
    val localSeed  = remember { (seed % Int.MAX_VALUE).toInt().coerceAtLeast(1) }
    val saveIdRef  = remember { saveId ?: SoloGameSaveManager.generateId() }

    // ── Initialzustand ───────────────────────────────────────────────────────
    var state by remember {
        mutableStateOf(
            if (saveId != null) {
                val save = SoloGameSaveManager.getSaves(context).find { it.id == saveId }
                if (save != null) deserializeMahjong(save.puzzleState)
                else createMahjongState(layoutId, diff, localSeed)
            } else {
                createMahjongState(layoutId, diff, localSeed)
            }
        )
    }

    var elapsed  by remember {
        val savedSec = if (saveId != null)
            SoloGameSaveManager.getSaves(context).find { it.id == saveId }?.elapsedSeconds ?: 0
        else 0
        mutableIntStateOf(savedSec)
    }
    var paused      by remember { mutableStateOf(false) }
    var showQuit    by remember { mutableStateOf(false) }
    var showRules   by remember { mutableStateOf(false) }
    var hintIds     by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var flashIds    by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val hintLimit   = HINT_LIMIT[diff]!!
    val shuffleLimit = SHUFFLE_LIMIT[diff]!!
    val showFreeHL  = SHOW_FREE_HIGHLIGHT[diff]!!
    val useTimer    = diff != MahjongDifficulty.ROOKIE
    val bestWin     = remember(state.won) {
        if (state.won && diff == MahjongDifficulty.BOSS)
            SoloGameSaveManager.getBestTime(context, "mahjong", layoutId.name, diff.name) else null
    }

    BackHandler { paused = true; showQuit = true }

    // ── Timer ────────────────────────────────────────────────────────────────
    LaunchedEffect(paused, state.won, state.gameOver) {
        if (!useTimer || paused || state.won || state.gameOver) return@LaunchedEffect
        while (isActive) {
            delay(1000)
            elapsed++
        }
    }

    // ── Settings + Musik ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        audio.startMusic(soundEnabled, musicEnabled)
    }

    // ── Auto-Save ────────────────────────────────────────────────────────────
    LaunchedEffect(state) {
        if (state.won || state.gameOver) return@LaunchedEffect
        SoloGameSaveManager.savePuzzle(
            context,
            PuzzleSave(
                id             = saveIdRef,
                gameType       = "mahjong",
                variant        = layoutId.name,
                difficulty     = diff.name,
                seed           = seed,
                puzzleState    = serializeMahjong(state),
                startedAt      = System.currentTimeMillis(),
                elapsedSeconds = elapsed,
            )
        )
    }

    // ── Win-Handling ─────────────────────────────────────────────────────────
    LaunchedEffect(state.won) {
        if (!state.won) return@LaunchedEffect
        SoloGameSaveManager.deleteSave(context, saveIdRef)
        if (diff == MahjongDifficulty.BOSS) {
            val prevBest = SoloGameSaveManager.getBestTime(context, "mahjong", layoutId.name, diff.name)
            SoloGameSaveManager.recordBestTime(context, "mahjong", layoutId.name, diff.name, elapsed)
            if ((prevBest == null || elapsed < prevBest) && uid.isNotEmpty()) {
                try {
                    db.collection("users").document(uid)
                        .update(mapOf("mahjongBestTimes.${layoutId.name}_BOSS" to elapsed.toLong()))
                        .await()
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            GameHudBar(
                paused = paused,
                onPauseToggle = { paused = !paused },
                onQuit = { paused = true; showQuit = true },
                onShowRules = { showRules = true },
            ) {
                Text(layoutId.emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                Column {
                    Text("GEZEITENSTEINE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val remaining = state.tiles.count { !it.removed }
                        Text(
                            "$remaining Steine",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (useTimer) {
                            Text(
                                "⏱ ${SoloGameSaveManager.formatElapsed(elapsed)}",
                                fontSize = ChipLabel,
                                color = MahjongGold,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        containerColor = BgDark,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Spielfeld ────────────────────────────────────────────────────
            MahjongBoardView(
                tiles             = state.tiles,
                selectedId        = state.selectedId,
                hintIds           = hintIds,
                flashIds          = flashIds,
                showFreeHighlight = showFreeHL,
                onTileClick       = { id ->
                    if (!paused && !state.won && !state.gameOver) {
                        hintIds = emptySet()
                        val prevRemoved = state.tiles.filter { it.removed }.map { it.id }.toSet()
                        val nextState = handleTileClick(state, id)
                        val newlyRemoved = nextState.tiles
                            .filter { it.removed && it.id !in prevRemoved }
                            .map { it.id }
                            .toSet()
                        if (newlyRemoved.size == 2) {
                            flashIds = newlyRemoved
                            scope.launch {
                                delay(350)
                                flashIds = emptySet()
                            }
                        }
                        state = nextState
                    }
                },
                modifier          = Modifier.weight(1f).fillMaxWidth(),
            )

            // ── Control-Bar ──────────────────────────────────────────────────
            ControlBar(
                diff          = diff,
                state         = state,
                hintLimit     = hintLimit,
                shuffleLimit  = shuffleLimit,
                onHint        = {
                    if (state.hintsUsed < hintLimit) {
                        val pair = getHint(state.tiles)
                        if (pair != null) {
                            hintIds = setOf(pair.first.id, pair.second.id)
                            state = state.copy(hintsUsed = state.hintsUsed + 1)
                        }
                    }
                },
                onShuffle     = {
                    if (state.shufflesUsed < shuffleLimit) {
                        state = state.copy(
                            tiles         = shuffleTiles(state.tiles, System.currentTimeMillis().toInt()),
                            shufflesUsed  = state.shufflesUsed + 1,
                            selectedId    = null,
                        )
                        hintIds = emptySet()
                    }
                },
                onUndo        = {
                    state = undoLast(state)
                    hintIds = emptySet()
                },
            )
        }

        // ── Pause-Overlay ────────────────────────────────────────────────────
        if (paused && !showQuit) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Pausiert", fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { paused = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MahjongGold),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Weiterspielen", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // ── GameOver-Overlay ──────────────────────────────────────────────────
        if (state.gameOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("😔", fontSize = DrawNumberTablet)
                        Spacer(Modifier.height(8.dp))
                        Text("Kein Zug mehr möglich", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        val remaining = state.tiles.count { !it.removed }
                        Text("Noch $remaining Steine übrig.", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        if (state.shufflesUsed < shuffleLimit) {
                            Button(
                                onClick = {
                                    state = state.copy(
                                        tiles        = shuffleTiles(state.tiles, System.currentTimeMillis().toInt()),
                                        shufflesUsed = state.shufflesUsed + 1,
                                        gameOver     = false,
                                        selectedId   = null,
                                    )
                                    hintIds = emptySet()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MahjongGold),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("🔀  Steine mischen", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextSub.copy(0.4f)),
                        ) {
                            Text("Zurück zur Lobby", color = TextSub)
                        }
                    }
                }
            }
        }

        // ── Won-Overlay ───────────────────────────────────────────────────────
        if (state.won) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = SurfaceDark) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🎉", fontSize = EmojiXLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Alle Steine entfernt!", fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        if (useTimer) {
                            Text(
                                SoloGameSaveManager.formatElapsed(elapsed),
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = MahjongGold,
                            )
                        }
                        if (diff == MahjongDifficulty.BOSS && bestWin != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Bestzeit: ${SoloGameSaveManager.formatElapsed(bestWin)}",
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                color = TextMuted,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${layoutId.emoji} ${layoutId.label} · ${diff.name}",
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            color = TextMuted,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                state = createMahjongState(layoutId, diff, System.currentTimeMillis().toInt())
                                elapsed = 0
                                hintIds = emptySet()
                                paused = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MahjongGold),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Nochmal spielen", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TextSub.copy(0.4f)),
                        ) {
                            Text("Zur Lobby", color = TextSub)
                        }
                    }
                }
            }
        }

        // ── Quit-Dialog ───────────────────────────────────────────────────────
        if (showQuit) {
            GameSaveQuitDialog(
                emoji    = "🀄",
                message  = "${layoutId.emoji} ${layoutId.label} · ${diff.name}",
                hideSave = state.won || state.gameOver,
                onContinue = {
                    showQuit = false
                    paused   = false
                },
                onSaveAndQuit = {
                    SoloGameSaveManager.savePuzzle(
                        context,
                        PuzzleSave(
                            id             = saveIdRef,
                            gameType       = "mahjong",
                            variant        = layoutId.name,
                            difficulty     = diff.name,
                            seed           = seed,
                            puzzleState    = serializeMahjong(state),
                            startedAt      = System.currentTimeMillis(),
                            elapsedSeconds = elapsed,
                        )
                    )
                    onNavigateBack()
                },
                onQuitWithoutSave = {
                    SoloGameSaveManager.deleteSave(context, saveIdRef)
                    onNavigateBack()
                },
            )
        }

        // ── Spielregeln ───────────────────────────────────────────────────────
        if (showRules) {
            val rule = ALL_GAME_RULES["mahjong"]
            if (rule != null) {
                GameRulesBottomSheet(rule = rule, onDismiss = { showRules = false })
            }
        }
    }
}

@Composable
private fun ControlBar(
    diff: MahjongDifficulty,
    state: MahjongState,
    hintLimit: Int,
    shuffleLimit: Int,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onUndo: () -> Unit,
) {
    val canHint    = state.hintsUsed < hintLimit
    val canShuffle = state.shufflesUsed < shuffleLimit
    val canUndo    = state.history.isNotEmpty()

    Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Hint
            if (hintLimit > 0) {
                val countLabel = if (hintLimit == Int.MAX_VALUE) "💡" else "💡 ${hintLimit - state.hintsUsed}"
                CtrlBtn(
                    label = countLabel,
                    active = false,
                    accent = MahjongGold,
                    enabled = canHint,
                    onClick = onHint,
                )
            }

            // Shuffle
            if (shuffleLimit > 0) {
                val countLabel = if (shuffleLimit == Int.MAX_VALUE) "🔀" else "🔀 ${shuffleLimit - state.shufflesUsed}"
                CtrlBtn(
                    label = countLabel,
                    active = false,
                    accent = MahjongGold,
                    enabled = canShuffle,
                    onClick = onShuffle,
                )
            }

            // Undo
            CtrlBtn(
                label = "↩",
                active = false,
                accent = MahjongGold,
                enabled = canUndo,
                onClick = onUndo,
            )
        }
        } // Box
    }
}

@Composable
private fun CtrlBtn(
    label: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg     = if (active) accent.copy(0.2f) else Surface2Dark
    val border = if (active) accent else BorderColor
    val alpha  = if (enabled) 1f else 0.3f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border.copy(alpha = alpha), RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
    ) {
        Text(
            text = label,
            fontSize = CellNumber,
            color = if (active) accent else TextSub.copy(alpha = alpha),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}
