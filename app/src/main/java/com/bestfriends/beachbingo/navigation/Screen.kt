package com.bestfriends.beachbingo.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable object Login : Screen
    @Serializable object Register : Screen
    @Serializable object Home : Screen
    @Serializable object Lobby : Screen
    @Serializable object JoinGame : Screen
    @Serializable data class Game(val gameId: String) : Screen
    @Serializable object Profile : Screen
    @Serializable object Results : Screen
    @Serializable object Settings : Screen
    // BeachPong
    @Serializable object PongLobby : Screen
    @Serializable data class PongGame(
        val gameId: String?,
        val totalPaddles: Int,
        val humanCount: Int,
        val difficulty: String,
        val scoreLimit: Int,
        val isHost: Boolean,
        val mySide: String,
    ) : Screen
    @Serializable object PongSettings : Screen
    @Serializable object PongResults : Screen
    // Vier4Bier
    @Serializable object VierLobby : Screen
    @Serializable data class VierGame(
        val mode: String,
        val gameId: String?,
        val myDrinkId: String,
        val aiDrinkId: String?,
        val aiDifficulty: String = "SNIPER",
    ) : Screen
    @Serializable object VierSettings : Screen
    @Serializable object VierResults : Screen
    // BeachPirates
    @Serializable object PiratesLobby : Screen
    @Serializable data class PiratesGame(
        val difficulty: String,
        val fireRate: Int,
        val controlMode: String,
    ) : Screen
    @Serializable object PiratesSettings : Screen
    @Serializable object PiratesHighscore : Screen
    @Serializable data class PiratesResults(
        val score: Int,
        val wave: Int,
        val difficulty: String,
        val highScore: Int,
        val newHighScore: Boolean,
    ) : Screen
}
