package com.bestfriends.beachbingo.core.model

data class BingoGame(
    val gameId: String = "",
    val adminId: String = "",
    val status: GameStatus = GameStatus.LOBBY,
    val gameMode: GameMode = GameMode.MANUAL_MARK,
    val drawStyle: DrawStyle = DrawStyle.INSTANT,
    val drawAnimationActive: Boolean = false,
    val players: Map<String, BingoPlayer> = emptyMap(),
    val drawnNumbers: List<Int> = emptyList(),
    val currentNumber: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalDrawCount: Int = 0,
    val eliminationInterval: Int = 5,
    val eliminationPendingPlayerId: String? = null,
    val eliminationAnimationActive: Boolean = false,
    val eliminationPlayerName: String = "",
    val eliminationPlayerAvatar: String = "",
    val eliminationNumber: Int = 0
)

data class BingoPlayer(
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val card: BingoCard = BingoCard(),
    val hasBingo: Boolean = false
)
