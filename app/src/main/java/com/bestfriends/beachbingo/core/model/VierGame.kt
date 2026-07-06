package com.bestfriends.beachbingo.core.model

data class VierGame(
    val gameId: String = "",
    val adminId: String = "",
    val status: VierStatus = VierStatus.LOBBY,
    val humanCount: Int = 2,
    val players: List<VierPlayer> = emptyList(),
    val playerIds: List<String> = emptyList(),
    val board: List<Int> = List(42) { 0 },
    val currentTurn: String = "",
    val winnerId: String? = null,
    val isDraw: Boolean = false,
    val createdAt: Long = 0L,
)

data class VierPlayer(
    val userId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val drinkId: String = "lager",
)

enum class VierStatus { LOBBY, RUNNING, FINISHED }

enum class VierDifficulty { ROOKIE, SNIPER, BOSS_LEVEL }
