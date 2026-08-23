package com.bestfriends.beachbingo.feature.worm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.components.GameHudBar
import androidx.activity.compose.BackHandler
import com.bestfriends.beachbingo.ui.components.GameSaveQuitDialog
import com.bestfriends.beachbingo.ui.theme.*
import com.bestfriends.beachbingo.feature.raetsel.GameSave
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// ── Game constants ────────────────────────────────────────────────────────────

private const val COLS = 20
private const val ROWS = 20
private const val VIRT_CELL = 20f      // virtual cell size in px at base resolution
private const val VIRT_W    = COLS * VIRT_CELL  // 400f
private const val VIRT_H    = ROWS * VIRT_CELL  // 400f

private val GridColor = BorderColor.copy(alpha = 0.9f)

private data class FoodType(val emoji: String, val points: Int, val weight: Int)
private val FOOD_TYPES = listOf(
    FoodType("🦀", 10, 60),
    FoodType("🐚", 20, 30),
    FoodType("🐟", 30, 10),
)

private val STEP_MS = mapOf("ROOKIE" to 300L, "SNIPER" to 150L, "BOSS_LEVEL" to 75L)
private val WALLS_WRAP = mapOf("ROOKIE" to false, "SNIPER" to false, "BOSS_LEVEL" to false)

// ── Data classes ──────────────────────────────────────────────────────────────

private data class Vec2(val x: Int, val y: Int)
private data class Food(val x: Int, val y: Int, val emoji: String, val points: Int)

// ── Game state ────────────────────────────────────────────────────────────────

private class WormState(val difficultyKey: String) {
    val snake = mutableListOf(Vec2(12, 10), Vec2(11, 10), Vec2(10, 10))
    var dir     = Vec2(1, 0)
    var nextDir = Vec2(1, 0)
    var food    = spawnFood(snake)
    var dead    = false

    var score  by mutableIntStateOf(0)
    var length by mutableIntStateOf(3)

    val stepMs   = STEP_MS[difficultyKey]   ?: 150L
    val wrapWalls = WALLS_WRAP[difficultyKey] ?: false

    var accumMs = 0L

    fun step() {
        if (dead) return
        dir = nextDir

        var nx = snake[0].x + dir.x
        var ny = snake[0].y + dir.y

        if (wrapWalls) {
            nx = (nx + COLS) % COLS
            ny = (ny + ROWS) % ROWS
        } else {
            if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) { dead = true; return }
        }

        if (snake.any { it.x == nx && it.y == ny }) { dead = true; return }

        val newHead = Vec2(nx, ny)
        if (nx == food.x && ny == food.y) {
            val bonus = 1.0 + (((snake.size - 3) / 10) * 0.1)
            val pts = (food.points * bonus).toInt()
            score += pts
            snake.add(0, newHead)
            food = spawnFood(snake)
            length = snake.size
        } else {
            snake.add(0, newHead)
            snake.removeAt(snake.size - 1)
        }
    }

    fun trySetDir(dx: Int, dy: Int) {
        if (dx != 0 && dir.x == -dx) return
        if (dy != 0 && dir.y == -dy) return
        nextDir = Vec2(dx, dy)
    }
}

private fun pickFoodType(): FoodType {
    val r = Random.nextInt(100)
    var acc = 0
    for (f in FOOD_TYPES) { acc += f.weight; if (r < acc) return f }
    return FOOD_TYPES[0]
}

private fun spawnFood(snake: List<Vec2>): Food {
    val occupied = snake.map { "${it.x},${it.y}" }.toSet()
    var x: Int; var y: Int
    do { x = Random.nextInt(COLS); y = Random.nextInt(ROWS) } while ("$x,$y" in occupied)
    val t = pickFoodType()
    return Food(x, y, t.emoji, t.points)
}

// ── Serialization ─────────────────────────────────────────────────────────────

private fun serializeWorm(gs: WormState): String {
    val obj = JSONObject()
    val snakeArr = JSONArray()
    gs.snake.forEach { v -> snakeArr.put(JSONObject().put("x", v.x).put("y", v.y)) }
    obj.put("snake", snakeArr)
    obj.put("dirX", gs.dir.x); obj.put("dirY", gs.dir.y)
    obj.put("foodX", gs.food.x); obj.put("foodY", gs.food.y)
    obj.put("foodEmoji", gs.food.emoji); obj.put("foodPoints", gs.food.points)
    obj.put("score", gs.score)
    return obj.toString()
}

private fun restoreWorm(gs: WormState, json: String) {
    try {
        val obj = JSONObject(json)
        gs.snake.clear()
        val arr = obj.getJSONArray("snake")
        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i); gs.snake.add(Vec2(s.getInt("x"), s.getInt("y")))
        }
        val dx = obj.getInt("dirX"); val dy = obj.getInt("dirY")
        gs.dir = Vec2(dx, dy); gs.nextDir = Vec2(dx, dy)
        gs.food = Food(obj.getInt("foodX"), obj.getInt("foodY"), obj.getString("foodEmoji"), obj.getInt("foodPoints"))
        gs.score = obj.getInt("score")
        gs.length = gs.snake.size
    } catch (_: Exception) {}
}

// ── Draw ──────────────────────────────────────────────────────────────────────

private fun DrawScope.drawGame(gs: WormState, scale: Float) {
    // Background
    drawRect(BgDark, size = Size(VIRT_W * scale, VIRT_H * scale))

    // Grid
    val cell = VIRT_CELL * scale
    for (i in 0..COLS) {
        drawLine(GridColor, Offset(i * cell, 0f), Offset(i * cell, VIRT_H * scale), strokeWidth = 0.5f)
    }
    for (j in 0..ROWS) {
        drawLine(GridColor, Offset(0f, j * cell), Offset(VIRT_W * scale, j * cell), strokeWidth = 0.5f)
    }

    // Outer border – marks the deadly wall
    drawRect(
        color = WormDeathFlash,
        topLeft = Offset(1.5f, 1.5f),
        size = Size(VIRT_W * scale - 3f, VIRT_H * scale - 3f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
    )

    // Snake segments
    gs.snake.forEachIndexed { i, seg ->
        val color = if (i == 0) SuccessDark else Success
        drawRoundRect(
            color = color,
            topLeft = Offset(seg.x * cell + scale, seg.y * cell + scale),
            size = Size(cell - 2 * scale, cell - 2 * scale),
            cornerRadius = CornerRadius(if (i == 0) 6f * scale else 4f * scale),
        )
    }

    // Eyes on head
    if (gs.snake.isNotEmpty()) {
        val head = gs.snake[0]
        val d = gs.dir
        val cx = (head.x * cell) + cell / 2f
        val cy = (head.y * cell) + cell / 2f
        val eyeOff = 3.5f * scale
        val eyeR   = 2f * scale
        if (d.x != 0) {
            drawCircle(Color.White, eyeR, Offset(cx + d.x * 2 * scale, cy - eyeOff))
            drawCircle(Color.White, eyeR, Offset(cx + d.x * 2 * scale, cy + eyeOff))
        } else {
            drawCircle(Color.White, eyeR, Offset(cx - eyeOff, cy + d.y * 2 * scale))
            drawCircle(Color.White, eyeR, Offset(cx + eyeOff, cy + d.y * 2 * scale))
        }
    }

    // Food emoji
    val food = gs.food
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize  = (VIRT_CELL - 2) * scale
        }
        canvas.nativeCanvas.drawText(
            food.emoji,
            food.x * cell + cell / 2f,
            food.y * cell + cell / 2f + paint.textSize * 0.35f,
            paint,
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WormGameScreen(
    difficulty: String,
    controlMode: String,
    saveId: String? = null,
    soundEnabled: Boolean = true,
    musicEnabled: Boolean = true,
    onNavigateToResults: (score: Int, length: Int, highScore: Int, newHighScore: Boolean) -> Unit,
    onNavigateToLobby: () -> Unit,
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid       = auth.currentUser?.uid
    val context   = androidx.compose.ui.platform.LocalContext.current

    val gs    = remember { WormState(difficulty) }
    val audio = remember { WormAudioManager(context) }

    var renderTick     by remember { mutableLongStateOf(0L) }
    var paused         by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var showRules      by remember { mutableStateOf(false) }
    var resultHandled  by remember { mutableStateOf(false) }

    BackHandler { paused = true; showQuitDialog = true }
    var showGameOver   by remember { mutableStateOf(false) }
    var savedHighScore by remember { mutableIntStateOf(0) }
    var isNewRecord    by remember { mutableStateOf(false) }
    var musicStarted   by remember { mutableStateOf(false) }

    // Restore saved game state
    LaunchedEffect(saveId) {
        if (saveId != null) {
            val save = SoloGameSaveManager.getGameSave(context, "worm")
            if (save != null) restoreWorm(gs, save.gameState)
        }
    }

    // Load audio prefs and start music
    LaunchedEffect(Unit) {
        audio.soundEnabled = soundEnabled
        audio.musicEnabled = musicEnabled
        audio.startMusic()
        musicStarted = true
    }
    DisposableEffect(Unit) { onDispose { audio.release() } }
    LaunchedEffect(paused) {
        if (!musicStarted) return@LaunchedEffect
        if (paused) audio.stopMusic() else audio.startMusic()
    }

    // Game loop
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                val deltaMs = if (lastNanos == 0L) 16L
                              else ((frameNanos - lastNanos) / 1_000_000L).coerceIn(1L, 50L)
                lastNanos = frameNanos

                if (!paused && !gs.dead) {
                    gs.accumMs += deltaMs
                    if (gs.accumMs >= gs.stepMs) {
                        val prevLen = gs.length
                        val prevPts = gs.food.points
                        gs.step()
                        gs.accumMs = 0L
                        if (gs.length > prevLen) {
                            audio.playSound(if (prevPts >= 20) "eat_rare" else "eat")
                        }
                    }
                }
                renderTick++
            }

            if (gs.dead && !resultHandled) {
                audio.stopMusic()
                audio.playSound("die")
                resultHandled = true
                val finalScore = gs.score
                val prev = if (uid != null) try {
                    val snap = firestore.collection("users").document(uid).get().await()
                    @Suppress("UNCHECKED_CAST")
                    (snap.get("wormHighScores") as? Map<String, Long>)?.get(difficulty)?.toInt() ?: 0
                } catch (_: Exception) { 0 } else 0
                val isNew = finalScore > prev
                val hs    = max(prev, finalScore)
                if (isNew && uid != null) try {
                    firestore.collection("users").document(uid)
                        .update("wormHighScores.$difficulty", hs.toLong()).await()
                } catch (_: Exception) {}
                savedHighScore = hs
                isNewRecord    = isNew
                showGameOver   = true
                break
            }
        }
    }

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // HUD
        GameHudBar(
            paused        = paused,
            onPauseToggle = { paused = !paused },
            onQuit        = { paused = true; showQuitDialog = true },
            onShowRules   = { showRules = true },
        ) {
            Text("${gs.score}", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = Success, modifier = Modifier.padding(end = 2.dp))
            Text("Pts", fontSize = ChipLabelTiny, color = TextMuted)
            Spacer(Modifier.width(12.dp))
            Text("${gs.length}", fontSize = MaterialTheme.typography.titleSmall.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary, modifier = Modifier.padding(end = 2.dp))
            Text("Länge", fontSize = ChipLabelTiny, color = TextMuted)
        }

        Spacer(Modifier.height(8.dp))

        // Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val pxW   = with(density) { maxWidth.toPx() }
                val pxH   = with(density) { maxHeight.toPx() }
                val scale = min(pxW / VIRT_W, pxH / VIRT_H)

                val swipeMod = if (controlMode == "SWIPE") {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                if (dragAmount.x > 8f) gs.trySetDir(1, 0)
                                else if (dragAmount.x < -8f) gs.trySetDir(-1, 0)
                            } else {
                                if (dragAmount.y > 8f) gs.trySetDir(0, 1)
                                else if (dragAmount.y < -8f) gs.trySetDir(0, -1)
                            }
                        }
                    }
                } else Modifier

                Canvas(
                    modifier = Modifier
                        .size(
                            width  = with(density) { (VIRT_W * scale).toDp() },
                            height = with(density) { (VIRT_H * scale).toDp() },
                        )
                        .then(swipeMod),
                ) {
                    @Suppress("UNUSED_EXPRESSION")
                    renderTick
                    drawGame(gs, scale)
                }
            }

            // Pause overlay
            if (paused && !gs.dead) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(color = SurfaceDark, shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(48.dp))
                            Text("Pause", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text("Tippe zum Weiterspielen", fontSize = MaterialTheme.typography.labelMedium.fontSize, color = TextMuted)
                        }
                    }
                }
            }

            // Game Over overlay
            if (showGameOver) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(260.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("🪱", fontSize = EmojiLarge)
                            Text("Game Over!", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            if (isNewRecord) {
                                Text("🏆 Neuer Rekord!", fontSize = CellNumber, fontWeight = FontWeight.Bold, color = SandGold)
                            }
                            Text("${gs.score}", fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.ExtraBold, color = Success)
                            Text("Punkte · Länge: ${gs.length}", fontSize = ChipLabel, color = TextMuted)
                            Button(
                                onClick = { onNavigateToResults(gs.score, gs.length, savedHighScore, isNewRecord) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Success),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Weiter →", fontSize = MaterialTheme.typography.labelLarge.fontSize, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // D-Pad buttons (BUTTONS mode)
        if (controlMode == "BUTTONS") {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                val isTablet = maxWidth > 600.dp
                val btnSize  = if (isTablet) 88.dp else 56.dp
                val btnMod = Modifier
                    .size(btnSize)
                    .background(SurfaceDark, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 8.dp else 4.dp),
                ) {
                    DPadButton(label = "▲", modifier = btnMod) { gs.trySetDir(0, -1) }
                    Row(horizontalArrangement = Arrangement.spacedBy(if (isTablet) 8.dp else 4.dp)) {
                        DPadButton(label = "◄", modifier = btnMod) { gs.trySetDir(-1, 0) }
                        Spacer(Modifier.size(btnSize))
                        DPadButton(label = "►", modifier = btnMod) { gs.trySetDir(1, 0) }
                    }
                    DPadButton(label = "▼", modifier = btnMod) { gs.trySetDir(0, 1) }
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showRules) {
        ALL_GAME_RULES["worm"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }

    if (showQuitDialog) {
        GameSaveQuitDialog(
            emoji = "🪱",
            message = "Score: ${gs.score} Pts · Länge: ${gs.length}",
            onContinue = { showQuitDialog = false; paused = false },
            onSaveAndQuit = {
                SoloGameSaveManager.saveGame(context, GameSave(
                    id = SoloGameSaveManager.generateId(),
                    gameType = "worm",
                    difficulty = difficulty,
                    gameState = serializeWorm(gs),
                    displayLabel = "Score: ${gs.score} · Länge: ${gs.length}",
                    savedAt = System.currentTimeMillis(),
                ))
                onNavigateToLobby()
            },
            onQuitWithoutSave = {
                SoloGameSaveManager.deleteGameSave(context, "worm")
                onNavigateToLobby()
            },
        )
    }
}

@Composable
private fun DPadButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.then(
            Modifier.padding(0.dp)
        ),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(label, fontSize = BingoCallSize, color = TextPrimary)
        }
    }
}
