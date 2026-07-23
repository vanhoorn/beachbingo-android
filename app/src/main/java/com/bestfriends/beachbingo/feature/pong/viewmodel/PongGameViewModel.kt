package com.bestfriends.beachbingo.feature.pong.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestfriends.beachbingo.core.data.repository.AuthRepository
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.core.model.PongPlayer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

// ── Canvas dimensions ─────────────────────────────────────────────────────────
const val W2 = 400f
const val H2 = 700f
const val SQ = 500f

const val PADDLE_THICK = 14f
const val PADDLE_LEN = 90f
const val MARGIN = 20f
const val BALL_R = 9f
const val BASE_SPEED = 5.0
const val MAX_SPEED = 13.0
const val CORNER_SIZE = 38f

data class PongGS(
    var bx: Double = W2 / 2.0,
    var by: Double = H2 / 2.0,
    var bvx: Double = BASE_SPEED,
    var bvy: Double = 0.0,
    var speed: Double = BASE_SPEED,
    // Paddle positions (center of paddle along the axis)
    var paddleLeft: Double = (H2 / 2).toDouble(),
    var paddleRight: Double = (H2 / 2).toDouble(),
    var paddleTop: Double = (SQ / 2).toDouble(),
    var paddleBottom: Double = (SQ / 2).toDouble(),
    var scoreLeft: Int = 0,
    var scoreRight: Int = 0,
    var scoreTop: Int = 0,
    var scoreBottom: Int = 0,
    var paused: Boolean = true,
    var pauseTimer: Int = 90,
    var wallSide: String? = null,
)

data class RemoteState(
    val bx: Double, val by: Double, val bvx: Double, val bvy: Double,
    val paddleLeft: Double, val paddleRight: Double,
    val paddleTop: Double, val paddleBottom: Double,
    val scoreLeft: Int, val scoreRight: Int,
    val scoreTop: Int, val scoreBottom: Int,
    val paused: Boolean, val pauseTimer: Int,
)

@HiltViewModel
class PongGameViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore

    private val _gs = MutableStateFlow(PongGS())
    val gs: StateFlow<PongGS> = _gs.asStateFlow()

    private val _opponentNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val opponentNames: StateFlow<Map<String, String>> = _opponentNames.asStateFlow()

    private val _loserSide = MutableStateFlow<String?>(null)
    val loserSide: StateFlow<String?> = _loserSide.asStateFlow()

    private val _winnerId = MutableStateFlow<String?>(null)
    val winnerId: StateFlow<String?> = _winnerId.asStateFlow()

    // Remote state for non-host interpolation
    var remoteState: RemoteState? = null
        private set

    private var currentUid: String = ""
    private var currentDisplayName: String = ""
    private var currentAvatarUrl: String = ""
    private var gameId: String? = null
    private var isHost: Boolean = false
    private var mySide: String = "left"
    private var guestSides: List<String> = emptyList()
    private var totalPaddles: Int = 2
    private var humanCount: Int = 1
    private var scoreLimit: Int = 7
    private var difficulty: PongDifficulty = PongDifficulty.ROOKIE
    private var firestoreListenerRemover: (() -> Unit)? = null
    private var aiResultWritten = false

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    val aiSpeed get() = when (difficulty) {
        PongDifficulty.ROOKIE -> 2.8
        PongDifficulty.SNIPER -> 5.0
        PongDifficulty.BOSS_LEVEL -> 9.0
    }

    val aiError get() = when (difficulty) {
        PongDifficulty.ROOKIE -> 45.0
        PongDifficulty.SNIPER -> 12.0
        PongDifficulty.BOSS_LEVEL -> 0.0
    }

    fun init(
        gameId: String?,
        totalPaddles: Int,
        humanCount: Int,
        difficulty: PongDifficulty,
        scoreLimit: Int,
        isHost: Boolean,
        mySide: String,
    ) {
        this.gameId = gameId
        this.totalPaddles = totalPaddles
        this.humanCount = humanCount
        this.difficulty = difficulty
        this.scoreLimit = scoreLimit
        this.isHost = isHost
        this.mySide = mySide

        val is2P = totalPaddles == 2
        val cw = if (is2P) W2.toDouble() else SQ.toDouble()
        val ch = if (is2P) H2.toDouble() else SQ.toDouble()
        // Wall must not be a human-assigned side (left/right are always the first two human sides)
        val humanSides = sidesForPaddles(totalPaddles, null).take(humanCount)
        val wall = if (totalPaddles == 3) {
            listOf("left", "right", "top", "bottom").filter { it !in humanSides }.random()
        } else null
        val angle = Math.random() * Math.PI * 2
        _gs.value = PongGS(
            bx = cw / 2, by = ch / 2,
            bvx = BASE_SPEED * cos(angle), bvy = BASE_SPEED * sin(angle),
            speed = BASE_SPEED,
            paddleLeft = ch / 2, paddleRight = ch / 2,
            paddleTop = cw / 2, paddleBottom = cw / 2,
            wallSide = wall,
        )
        // Guests start inactive; they wait until host writes IN_PROGRESS
        _isGameActive.value = isHost || gameId == null

        aiResultWritten = false
        viewModelScope.launch {
            val user = authRepository.currentUser.first { it != null } ?: return@launch
            currentUid = user.uid
            currentDisplayName = user.displayName
            currentAvatarUrl = user.avatarUrl
        }

        if (gameId != null) observeGame(gameId)

        // Host immediately marks the game as running
        if (isHost && gameId != null) {
            viewModelScope.launch {
                try {
                    db.collection("pongGames").document(gameId).update(
                        "status", "IN_PROGRESS",
                        "wallSide", wall,
                    ).await()
                } catch (_: Exception) {}
            }
        }
    }

    private fun observeGame(gameId: String) {
        firestoreListenerRemover?.invoke()
        val reg = db.collection("pongGames").document(gameId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val data = snap.data ?: return@addSnapshotListener

                // Build opponent names and track guest sides for AI logic
                val players = (data["players"] as? List<*>)?.mapNotNull { p ->
                    (p as? Map<*, *>)?.let { m ->
                        PongPlayer(
                            userId = m["userId"] as? String ?: "",
                            displayName = m["displayName"] as? String ?: "",
                            avatarUrl = m["avatarUrl"] as? String ?: "",
                            side = m["side"] as? String ?: "left",
                        )
                    }
                } ?: emptyList()
                val names = players.filter { it.userId != currentUid }
                    .associate { it.side to it.displayName }
                _opponentNames.value = names
                guestSides = players.filter { it.userId != currentUid }.map { it.side }

                // Guest activates once host sets IN_PROGRESS
                if (!isHost) {
                    val status = data["status"] as? String
                    if (status == "IN_PROGRESS") _isGameActive.value = true
                    // Sync wallSide from host
                    val remoteWall = data["wallSide"] as? String
                    if (remoteWall != null && _gs.value.wallSide != remoteWall) {
                        _gs.update { it.copy(wallSide = remoteWall) }
                    }
                }

                val winnerIdVal = data["winnerId"] as? String
                if (winnerIdVal != null) {
                    _winnerId.value = winnerIdVal
                    val g = _gs.value
                    val loser = when {
                        g.scoreLeft >= scoreLimit -> "left"
                        g.scoreRight >= scoreLimit -> "right"
                        g.scoreTop >= scoreLimit -> "top"
                        g.scoreBottom >= scoreLimit -> "bottom"
                        else -> null
                    }
                    _loserSide.value = loser
                }

                if (!isHost) {
                    remoteState = RemoteState(
                        bx = (data["ballX"] as? Double) ?: 200.0,
                        by = (data["ballY"] as? Double) ?: 200.0,
                        bvx = (data["ballVX"] as? Double) ?: 0.0,
                        bvy = (data["ballVY"] as? Double) ?: 0.0,
                        paddleLeft = (data["paddleLeft"] as? Double) ?: 250.0,
                        paddleRight = (data["paddleRight"] as? Double) ?: 250.0,
                        paddleTop = (data["paddleTop"] as? Double) ?: 250.0,
                        paddleBottom = (data["paddleBottom"] as? Double) ?: 250.0,
                        scoreLeft = (data["scoreLeft"] as? Long)?.toInt() ?: 0,
                        scoreRight = (data["scoreRight"] as? Long)?.toInt() ?: 0,
                        scoreTop = (data["scoreTop"] as? Long)?.toInt() ?: 0,
                        scoreBottom = (data["scoreBottom"] as? Long)?.toInt() ?: 0,
                        paused = (data["paused"] as? Boolean) ?: true,
                        pauseTimer = (data["pauseTimer"] as? Long)?.toInt() ?: 90,
                    )
                    _gs.update { it.copy(
                        scoreLeft = remoteState!!.scoreLeft,
                        scoreRight = remoteState!!.scoreRight,
                        scoreTop = remoteState!!.scoreTop,
                        scoreBottom = remoteState!!.scoreBottom,
                    ) }
                } else {
                    // Host reads guest paddle positions
                    players.filter { it.userId != currentUid }.forEach { p ->
                        val paddleVal = (data["paddle${p.side.replaceFirstChar { c -> c.uppercase() }}"] as? Double)
                            ?: return@forEach
                        _gs.update { gs ->
                            when (p.side) {
                                "left" -> gs.copy(paddleLeft = paddleVal)
                                "right" -> gs.copy(paddleRight = paddleVal)
                                "top" -> gs.copy(paddleTop = paddleVal)
                                "bottom" -> gs.copy(paddleBottom = paddleVal)
                                else -> gs
                            }
                        }
                    }
                }
            }
        firestoreListenerRemover = { reg.remove() }
    }

    // ── Physics tick — called from the game loop in the composable ────────────
    fun tick(frameCount: Int): String? {
        if (_loserSide.value != null) return null
        val is2P = totalPaddles == 2
        val cw = if (is2P) W2.toDouble() else SQ.toDouble()
        val ch = if (is2P) H2.toDouble() else SQ.toDouble()

        val g = _gs.value.copy() // work on a copy, then update atomically

        // Pause countdown
        if (g.paused) {
            val newTimer = g.pauseTimer - 1
            _gs.value = g.copy(pauseTimer = newTimer, paused = newTimer > 0)
            return null
        }

        // Move AI paddles — skip own side and actual human guest sides
        val activeSides = sidesForPaddles(totalPaddles, g.wallSide)
        activeSides.forEach { side ->
            val isMyHumanSide = side == mySide
            val isGuestSide = guestSides.contains(side)
            if (!isMyHumanSide && !isGuestSide) {
                val target = if (side == "left" || side == "right") g.by else g.bx
                val size = if (side == "left" || side == "right") ch else cw
                val current = paddleOf(g, side)
                val moved = moveAI(current, target, aiSpeed, aiError, size)
                setPaddle(g, side, moved)
            }
        }

        // Ball move
        val newBx = g.bx + g.bvx
        val newBy = g.by + g.bvy
        val movedG = g.copy(bx = newBx, by = newBy)

        // Physics
        val lostSide = if (is2P) physics2P(movedG, cw, ch) else physicsMulti(movedG, totalPaddles, cw)

        _gs.value = movedG

        if (lostSide != null) {
            val newGs = _gs.value.let { gs ->
                when (lostSide) {
                    "left" -> gs.copy(scoreLeft = gs.scoreLeft + 1)
                    "right" -> gs.copy(scoreRight = gs.scoreRight + 1)
                    "top" -> gs.copy(scoreTop = gs.scoreTop + 1)
                    "bottom" -> gs.copy(scoreBottom = gs.scoreBottom + 1)
                    else -> gs
                }
            }
            val score = scoreOf(newGs, lostSide)
            if (score >= scoreLimit) {
                _gs.value = newGs
                _loserSide.value = lostSide
                // Write finish to Firestore
                val humanWon = lostSide != mySide
                if (gameId != null) {
                    val gid = gameId!!
                    viewModelScope.launch {
                        try {
                            db.collection("pongGames").document(gid).update(mapOf(
                                "winnerId" to if (humanWon) currentUid else null,
                                "status" to "FINISHED",
                                "scoreLeft" to newGs.scoreLeft,
                                "scoreRight" to newGs.scoreRight,
                                "scoreTop" to newGs.scoreTop,
                                "scoreBottom" to newGs.scoreBottom,
                            )).await()
                        } catch (_: Exception) {}
                    }
                } else {
                    saveAiResult(humanWon, newGs)
                }
                return lostSide
            } else {
                // Reset ball after point
                val angle = Math.random() * Math.PI * 2
                _gs.value = newGs.copy(
                    bx = cw / 2, by = ch / 2,
                    bvx = BASE_SPEED * cos(angle), bvy = BASE_SPEED * sin(angle),
                    speed = BASE_SPEED,
                    paused = true, pauseTimer = 90,
                )
            }
        }

        // Throttled Firestore write (every 3 frames)
        if (gameId != null && frameCount % 3 == 0) {
            pushToFirestore()
        }

        return null
    }

    fun applyRemoteInterpolation(frameCount: Int) {
        val r = remoteState ?: return
        _gs.update { gs ->
            gs.copy(
                bx = lerp(gs.bx, r.bx, 0.3),
                by = lerp(gs.by, r.by, 0.3),
                // Never lerp own paddle — user input must win
                paddleLeft   = if (mySide != "left")   lerp(gs.paddleLeft,   r.paddleLeft,   0.4) else gs.paddleLeft,
                paddleRight  = if (mySide != "right")  lerp(gs.paddleRight,  r.paddleRight,  0.4) else gs.paddleRight,
                paddleTop    = if (mySide != "top")    lerp(gs.paddleTop,    r.paddleTop,    0.4) else gs.paddleTop,
                paddleBottom = if (mySide != "bottom") lerp(gs.paddleBottom, r.paddleBottom, 0.4) else gs.paddleBottom,
                paused = r.paused,
                pauseTimer = r.pauseTimer,
            )
        }
        // Throttle Firestore writes to every 4 frames (same as host)
        if (gameId != null && frameCount % 4 == 0) {
            val gs = _gs.value
            val paddleVal = paddleOf(gs, mySide)
            viewModelScope.launch {
                try {
                    db.collection("pongGames").document(gameId!!).update(
                        "paddle${mySide.replaceFirstChar { it.uppercase() }}", paddleVal
                    ).await()
                } catch (_: Exception) {}
            }
        }
    }

    fun updateMyPaddle(position: Double) {
        val is2P = totalPaddles == 2
        val ch = if (is2P) H2.toDouble() else SQ.toDouble()
        val cw = if (is2P) W2.toDouble() else SQ.toDouble()
        val size = if (mySide == "left" || mySide == "right") ch else cw
        val clamped = position.coerceIn(PADDLE_LEN / 2.0, size - PADDLE_LEN / 2.0)
        _gs.update { gs ->
            when (mySide) {
                "left" -> gs.copy(paddleLeft = clamped)
                "right" -> gs.copy(paddleRight = clamped)
                "top" -> gs.copy(paddleTop = clamped)
                "bottom" -> gs.copy(paddleBottom = clamped)
                else -> gs
            }
        }
    }

    fun resetGame() {
        val is2P = totalPaddles == 2
        val cw = if (is2P) W2.toDouble() else SQ.toDouble()
        val ch = if (is2P) H2.toDouble() else SQ.toDouble()
        val wall = if (totalPaddles == 3) listOf("left", "right", "top", "bottom").random() else null
        val angle = Math.random() * Math.PI * 2
        _gs.value = PongGS(
            bx = cw / 2, by = ch / 2,
            bvx = BASE_SPEED * cos(angle), bvy = BASE_SPEED * sin(angle),
            speed = BASE_SPEED,
            paddleLeft = ch / 2, paddleRight = ch / 2,
            paddleTop = cw / 2, paddleBottom = cw / 2,
            wallSide = wall,
        )
        _loserSide.value = null
        _winnerId.value = null
    }

    private fun pushToFirestore() {
        val gid = gameId ?: return
        val gs = _gs.value
        viewModelScope.launch {
            try {
                db.collection("pongGames").document(gid).update(mapOf(
                    "ballX" to gs.bx,
                    "ballY" to gs.by,
                    "ballVX" to gs.bvx,
                    "ballVY" to gs.bvy,
                    "speed" to gs.speed,
                    "paddleLeft" to gs.paddleLeft,
                    "paddleRight" to gs.paddleRight,
                    "paddleTop" to gs.paddleTop,
                    "paddleBottom" to gs.paddleBottom,
                    "scoreLeft" to gs.scoreLeft,
                    "scoreRight" to gs.scoreRight,
                    "scoreTop" to gs.scoreTop,
                    "scoreBottom" to gs.scoreBottom,
                    "paused" to gs.paused,
                    "pauseTimer" to gs.pauseTimer,
                )).await()
            } catch (_: Exception) {}
        }
    }

    // ── Physics ───────────────────────────────────────────────────────────────
    // All Float constants (BALL_R, PADDLE_LEN, etc.) cast to Double for arithmetic with g fields

    private fun physics2P(g: PongGS, cw: Double, ch: Double): String? {
        val br = BALL_R.toDouble()
        val pl = PADDLE_LEN.toDouble()
        val pt = PADDLE_THICK.toDouble()
        val mg = MARGIN.toDouble()

        if (g.by - br < 0) { g.by = br; g.bvy = abs(g.bvy) }
        if (g.by + br > ch) { g.by = ch - br; g.bvy = -abs(g.bvy) }

        val lpx = mg + pt
        if (g.bvx < 0 && g.bx - br < lpx && g.bx - br > mg - 2 &&
            inRange(g.by, g.paddleLeft - pl / 2 - br, g.paddleLeft + pl / 2 + br)) {
            val rel = (g.by - g.paddleLeft) / (pl / 2)
            g.speed = min(g.speed + 0.35, MAX_SPEED)
            g.bvx = g.speed * cos(rel * 0.75)
            g.bvy = g.speed * sin(rel * 0.75)
            g.bx = lpx + br + 1
        }

        val rpx = cw - mg - pt
        if (g.bvx > 0 && g.bx + br > rpx && g.bx + br < cw - mg + 2 &&
            inRange(g.by, g.paddleRight - pl / 2 - br, g.paddleRight + pl / 2 + br)) {
            val rel = (g.by - g.paddleRight) / (pl / 2)
            g.speed = min(g.speed + 0.35, MAX_SPEED)
            g.bvx = -g.speed * cos(rel * 0.75)
            g.bvy = g.speed * sin(rel * 0.75)
            g.bx = rpx - br - 1
        }

        if (g.bx + br < 0) return "left"
        if (g.bx - br > cw) return "right"
        return null
    }

    private fun physicsMulti(g: PongGS, total: Int, size: Double): String? {
        val br = BALL_R.toDouble()
        val pl = PADDLE_LEN.toDouble()
        val pt = PADDLE_THICK.toDouble()
        val mg = MARGIN.toDouble()
        val wall = g.wallSide
        val padSide = sidesForPaddles(total, wall)

        if (total == 4) {
            val cs = CORNER_SIZE.toDouble()
            if (g.bx < cs && g.by < cs) { g.bvx = abs(g.bvx); g.bvy = abs(g.bvy) }
            if (g.bx > size - cs && g.by < cs) { g.bvx = -abs(g.bvx); g.bvy = abs(g.bvy) }
            if (g.bx < cs && g.by > size - cs) { g.bvx = abs(g.bvx); g.bvy = -abs(g.bvy) }
            if (g.bx > size - cs && g.by > size - cs) { g.bvx = -abs(g.bvx); g.bvy = -abs(g.bvy) }
        }

        val lx = mg + pt
        if (g.bvx < 0 && g.bx - br < lx) {
            if (wall == "left") { g.bvx = abs(g.bvx); g.bx = lx + br }
            else if (inRange(g.bx - br, mg - 2, lx) &&
                inRange(g.by, g.paddleLeft - pl / 2 - br, g.paddleLeft + pl / 2 + br)) {
                val rel = (g.by - g.paddleLeft) / (pl / 2)
                g.speed = min(g.speed + 0.3, MAX_SPEED)
                g.bvx = g.speed * cos(rel * 0.7)
                g.bvy = g.speed * sin(rel * 0.7)
                g.bx = lx + br + 1
            } else if (g.bx + br < 0 && "left" in padSide) return "left"
        }

        val rx = size - mg - pt
        if (g.bvx > 0 && g.bx + br > rx) {
            if (wall == "right") { g.bvx = -abs(g.bvx); g.bx = rx - br }
            else if (inRange(g.bx + br, rx, size - mg + 2) &&
                inRange(g.by, g.paddleRight - pl / 2 - br, g.paddleRight + pl / 2 + br)) {
                val rel = (g.by - g.paddleRight) / (pl / 2)
                g.speed = min(g.speed + 0.3, MAX_SPEED)
                g.bvx = -g.speed * cos(rel * 0.7)
                g.bvy = g.speed * sin(rel * 0.7)
                g.bx = rx - br - 1
            } else if (g.bx - br > size && "right" in padSide) return "right"
        }

        val ty = mg + pt
        if (g.bvy < 0 && g.by - br < ty) {
            if (wall == "top") { g.bvy = abs(g.bvy); g.by = ty + br }
            else if (inRange(g.by - br, mg - 2, ty) &&
                inRange(g.bx, g.paddleTop - pl / 2 - br, g.paddleTop + pl / 2 + br)) {
                val rel = (g.bx - g.paddleTop) / (pl / 2)
                g.speed = min(g.speed + 0.3, MAX_SPEED)
                g.bvy = g.speed * cos(rel * 0.7)
                g.bvx = g.speed * sin(rel * 0.7)
                g.by = ty + br + 1
            } else if (g.by + br < 0 && "top" in padSide) return "top"
        }

        val by_ = size - mg - pt
        if (g.bvy > 0 && g.by + br > by_) {
            if (wall == "bottom") { g.bvy = -abs(g.bvy); g.by = by_ - br }
            else if (inRange(g.by + br, by_, size - mg + 2) &&
                inRange(g.bx, g.paddleBottom - pl / 2 - br, g.paddleBottom + pl / 2 + br)) {
                val rel = (g.bx - g.paddleBottom) / (pl / 2)
                g.speed = min(g.speed + 0.3, MAX_SPEED)
                g.bvy = -g.speed * cos(rel * 0.7)
                g.bvx = g.speed * sin(rel * 0.7)
                g.by = by_ - br - 1
            } else if (g.by - br > size && "bottom" in padSide) return "bottom"
        }

        return null
    }

    private fun saveAiResult(humanWon: Boolean, gs: PongGS) {
        if (aiResultWritten || currentUid.isEmpty()) return
        aiResultWritten = true
        viewModelScope.launch {
            try {
                db.collection("pongGames").add(
                    mapOf(
                        "adminId" to currentUid,
                        "status" to "FINISHED",
                        "totalPaddles" to totalPaddles,
                        "humanCount" to humanCount,
                        "difficulty" to difficulty.name,
                        "scoreLimit" to scoreLimit,
                        "players" to listOf(
                            mapOf(
                                "userId" to currentUid,
                                "displayName" to currentDisplayName,
                                "avatarUrl" to currentAvatarUrl,
                                "side" to mySide,
                            )
                        ),
                        "playerIds" to listOf(currentUid),
                        "winnerId" to if (humanWon) currentUid else null,
                        "scoreLeft" to gs.scoreLeft,
                        "scoreRight" to gs.scoreRight,
                        "scoreTop" to gs.scoreTop,
                        "scoreBottom" to gs.scoreBottom,
                        "createdAt" to System.currentTimeMillis(),
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListenerRemover?.invoke()
    }

    companion object {
        fun sidesForPaddles(total: Int, wall: String?): List<String> {
            val all = listOf("left", "right", "top", "bottom")
            return when (total) {
                2 -> listOf("left", "right")
                3 -> all.filter { it != wall }
                else -> all
            }
        }

        fun moveAI(current: Double, target: Double, speed: Double, error: Double, size: Double): Double {
            val t = target + (Math.random() - 0.5) * error * 2
            val diff = t - current
            val next = current + sign(diff) * min(abs(diff), speed)
            return next.coerceIn(PADDLE_LEN / 2.0, size - PADDLE_LEN / 2.0)
        }

        fun paddleOf(gs: PongGS, side: String) = when (side) {
            "left" -> gs.paddleLeft
            "right" -> gs.paddleRight
            "top" -> gs.paddleTop
            "bottom" -> gs.paddleBottom
            else -> gs.paddleLeft
        }

        fun setPaddle(gs: PongGS, side: String, value: Double) = when (side) {
            "left" -> gs.paddleLeft = value
            "right" -> gs.paddleRight = value
            "top" -> gs.paddleTop = value
            "bottom" -> gs.paddleBottom = value
            else -> {}
        }

        fun scoreOf(gs: PongGS, side: String) = when (side) {
            "left" -> gs.scoreLeft
            "right" -> gs.scoreRight
            "top" -> gs.scoreTop
            "bottom" -> gs.scoreBottom
            else -> 0
        }

        fun inRange(v: Double, min: Double, max: Double) = v >= min && v <= max
        fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    }
}
