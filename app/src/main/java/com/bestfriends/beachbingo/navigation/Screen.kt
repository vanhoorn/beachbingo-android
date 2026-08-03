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
        val saveId: String? = null,
    ) : Screen
    @Serializable object PiratesSettings : Screen
    @Serializable object PiratesHighscore : Screen
    // Wattwurm
    @Serializable object WormLobby : Screen
    @Serializable data class WormGame(
        val difficulty: String,
        val controlMode: String,
        val saveId: String? = null,
    ) : Screen
    @Serializable object WormSettings : Screen
    @Serializable object WormHighscore : Screen
    @Serializable data class WormResults(
        val score: Int,
        val length: Int,
        val difficulty: String,
        val controlMode: String,
        val highScore: Int,
        val newHighScore: Boolean,
    ) : Screen
    // Strandturm
    @Serializable object StrandturmLobby : Screen
    @Serializable data class StrandturmGame(
        val controlMode: String,
        val startLevel: Int = 1,
        val saveId: String? = null,
    ) : Screen
    @Serializable object StrandturmSettings : Screen
    @Serializable object StrandturmHighscore : Screen
    @Serializable data class StrandturmResults(
        val score: Int,
        val level: Int,
        val highScore: Int,
        val bestLevel: Int,
        val newHighScore: Boolean,
        val newBestLevel: Boolean,
    ) : Screen
    @Serializable data class Category(val playerCount: String) : Screen
    @Serializable object AllGames : Screen
    @Serializable object CardGames : Screen
    @Serializable object ActionGames : Screen
    @Serializable object CouchGames : Screen
    @Serializable object Raetsel : Screen
    // Brandung
    @Serializable object BrandungLobby : Screen
    @Serializable data class BrandungGame(
        val mode: String,
        val gameId: String? = null,
        val aiCount: Int = 2,
        val difficulty: String = "SNIPER",
    ) : Screen
    @Serializable object BrandungSettings : Screen
    @Serializable object BrandungResults : Screen
    // MeerMau
    @Serializable object MeermauLobby : Screen
    @Serializable data class MeermauGame(
        val mode: String,
        val gameId: String? = null,
        val aiCount: Int = 1,
        val difficulty: String = "SNIPER",
    ) : Screen
    @Serializable object MeermauSettings : Screen
    @Serializable object MeermauResults : Screen
    @Serializable data class PiratesResults(
        val score: Int,
        val wave: Int,
        val difficulty: String,
        val highScore: Int,
        val newHighScore: Boolean,
    ) : Screen
    // Strandräuber
    @Serializable object StrandraeuberLobby : Screen
    @Serializable data class StrandraeuberGame(
        val mode: String,
        val gameId: String? = null,
        val aiCount: Int = 2,
        val difficulty: String = "SNIPER",
        val totalRounds: Int = 3,
    ) : Screen
    @Serializable object StrandraeuberSettings : Screen
    @Serializable object StrandraeuberResults : Screen
    // Rätsel-Rubrik
    @Serializable object DuenenschattenLobby : Screen
    @Serializable data class DuenenschattenGame(
        val difficulty: String,
        val seed: Long,
        val saveId: String? = null,
    ) : Screen
    @Serializable object InselbrueckeLobby : Screen
    @Serializable data class InselbrueckeGame(
        val difficulty: String,
        val seed: Long,
        val saveId: String? = null,
    ) : Screen
    @Serializable object StrandokuLobby : Screen
    @Serializable data class StrandokuGame(
        val variant: String,
        val difficulty: String,
        val seed: Long,
        val saveId: String? = null,
    ) : Screen
    @Serializable object WellensummeLobby : Screen
    @Serializable data class WellensummeGame(
        val difficulty: String,
        val seed: Long,
        val saveId: String? = null,
    ) : Screen
    @Serializable object KuestenkriegLobby : Screen
    @Serializable data class KuestenkriegGame(
        val difficulty: String,
        val seed: Long,
        val saveId: String? = null,
    ) : Screen
    @Serializable data class KuestenkriegPlacement(val aiMode: String, val onlineCode: String = "") : Screen
    @Serializable object KuestenkriegBattle : Screen
    @Serializable data class KuestenkriegOnlineLobby(val joinCode: String = "") : Screen
    @Serializable data class KuestenkriegOnlinePlacement(val gameCode: String) : Screen
    @Serializable data class KuestenkriegOnlineBattle(val gameCode: String) : Screen
    // WortWelle
    @Serializable object WortWelleLobby : Screen
    @Serializable data class WortWelleGame(
        val difficulty: String,
        val isDaily: Boolean = false,
        val dailyWord: String = "",
        val dateStr: String = "",
        val saveId: String? = null,
    ) : Screen
}
