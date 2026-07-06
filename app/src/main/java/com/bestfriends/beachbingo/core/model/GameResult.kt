package com.bestfriends.beachbingo.core.model

data class GameResult(
    val resultId: String = "",
    val gameId: String = "",
    val winnerName: String = "",
    val winnerId: String = "",
    val winnerAvatar: String = "",
    val playerCount: Int = 0,
    val drawnNumbersCount: Int = 0,
    val finishedAt: Long = System.currentTimeMillis(),
    val playerIds: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val playerAvatars: List<String> = emptyList()
)
