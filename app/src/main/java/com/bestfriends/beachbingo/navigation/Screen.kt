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
}
