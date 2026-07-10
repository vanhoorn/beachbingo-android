package com.bestfriends.beachbingo.core.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val age: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val preferredGameMode: GameMode = GameMode.MANUAL_MARK,
    val preferredDrawStyle: DrawStyle = DrawStyle.INSTANT,
    val bossLevelEliminationInterval: Int = 5,
    val preferredPongDifficulty: PongDifficulty? = null,
    val preferredPongScoreLimit: Int? = null,
    val preferredPongPaddles: Int? = null,
    val preferredVierDrinkId: String? = null,
    val preferredVierDifficulty: VierDifficulty? = null,
    val preferredPiratesDifficulty: PiratesDifficulty? = null,
    val preferredPiratesFireRate: Int? = null,
    val preferredPiratesControlMode: String? = null,
    val piratesHighScores: Map<String, Long> = emptyMap(),
    // Strandturm
    val preferredStrandturmControlMode: String? = null,
    val strandturmHighScore: Int = 0,
    val strandturmBestLevel: Int = 0,
    // Audio (all games)
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
)
