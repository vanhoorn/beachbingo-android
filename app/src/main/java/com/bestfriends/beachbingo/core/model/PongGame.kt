package com.bestfriends.beachbingo.core.model

data class PongGame(
    val gameId: String = "",
    val adminId: String = "",
    val status: PongStatus = PongStatus.LOBBY,
    val totalPaddles: Int = 2,
    val humanCount: Int = 1,
    val difficulty: PongDifficulty = PongDifficulty.ROOKIE,
    val scoreLimit: Int = 7,
    val players: List<PongPlayer> = emptyList(),
    val playerIds: List<String> = emptyList(),
    val wallSide: String? = null,
    val ballX: Double = 200.0,
    val ballY: Double = 200.0,
    val ballVX: Double = 0.0,
    val ballVY: Double = 0.0,
    val speed: Double = 5.0,
    val paddleLeft: Double = 250.0,
    val paddleRight: Double = 250.0,
    val paddleTop: Double = 250.0,
    val paddleBottom: Double = 250.0,
    val scoreLeft: Int = 0,
    val scoreRight: Int = 0,
    val scoreTop: Int = 0,
    val scoreBottom: Int = 0,
    val paused: Boolean = true,
    val pauseTimer: Int = 90,
    val winnerId: String? = null,
    val createdAt: Long = 0L,
)

data class PongPlayer(
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val side: String = "left",
)

enum class PongStatus { LOBBY, RUNNING, FINISHED }

enum class PongDifficulty { ROOKIE, SNIPER, BOSS_LEVEL }
