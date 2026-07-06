package com.bestfriends.beachbingo.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bestfriends.beachbingo.feature.auth.ui.LoginScreen
import com.bestfriends.beachbingo.feature.auth.ui.ProfileScreen
import com.bestfriends.beachbingo.feature.auth.ui.RegisterScreen
import com.bestfriends.beachbingo.feature.auth.ui.SettingsScreen
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel
import com.bestfriends.beachbingo.feature.bingo.ui.GameScreen
import com.bestfriends.beachbingo.feature.bingo.ui.JoinGameScreen
import com.bestfriends.beachbingo.feature.bingo.ui.LobbyScreen
import com.bestfriends.beachbingo.feature.bingo.ui.ResultsScreen
import com.bestfriends.beachbingo.feature.home.ui.CategoryScreen
import com.bestfriends.beachbingo.feature.home.ui.HomeScreen
import com.bestfriends.beachbingo.feature.pong.ui.PongGameScreen
import com.bestfriends.beachbingo.feature.pong.ui.PongLobbyScreen
import com.bestfriends.beachbingo.feature.pong.ui.PongResultsScreen
import com.bestfriends.beachbingo.feature.pong.ui.PongSettingsScreen
import com.bestfriends.beachbingo.feature.vier.ui.VierGameScreen
import com.bestfriends.beachbingo.feature.vier.ui.VierLobbyScreen
import com.bestfriends.beachbingo.feature.vier.ui.VierResultsScreen
import com.bestfriends.beachbingo.feature.vier.ui.VierSettingsScreen
import com.bestfriends.beachbingo.feature.pirates.ui.PiratesLobbyScreen
import com.bestfriends.beachbingo.feature.pirates.ui.PiratesGameScreen
import com.bestfriends.beachbingo.feature.pirates.ui.PiratesHighscoreScreen
import com.bestfriends.beachbingo.feature.pirates.ui.PiratesResultsScreen
import com.bestfriends.beachbingo.feature.pirates.ui.PiratesSettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isCheckingAuth by authViewModel.isCheckingAuth.collectAsStateWithLifecycle()

    if (isCheckingAuth) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val onAuthScreen = navController.currentBackStackEntry
                ?.destination
                ?.run { hasRoute(Screen.Login::class) || hasRoute(Screen.Register::class) }
                ?: false
            if (onAuthScreen) {
                navController.navigate(Screen.Home) {
                    popUpTo(Screen.Login) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login
    ) {
        composable<Screen.Login> {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Home> {
            HomeScreen(
                onNavigateToBingoLobby = { navController.navigate(Screen.Lobby) },
                onNavigateToPongLobby = { navController.navigate(Screen.PongLobby) },
                onNavigateToVierLobby = { navController.navigate(Screen.VierLobby) },
                onNavigateToPiratesLobby = { navController.navigate(Screen.PiratesLobby) },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
                onNavigateToJoin = { navController.navigate(Screen.JoinGame) },
                onNavigateToCategory = { playerCount -> navController.navigate(Screen.Category(playerCount)) },
                viewModel = authViewModel
            )
        }

        composable<Screen.Category> { backStack ->
            val route: Screen.Category = backStack.toRoute()
            CategoryScreen(
                playerCountName = route.playerCount,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBingoLobby = { navController.navigate(Screen.Lobby) },
                onNavigateToPongLobby = { navController.navigate(Screen.PongLobby) },
                onNavigateToVierLobby = { navController.navigate(Screen.VierLobby) },
                onNavigateToPiratesLobby = { navController.navigate(Screen.PiratesLobby) },
            )
        }

        composable<Screen.Lobby> {
            LobbyScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToGame = { gameId -> navController.navigate(Screen.Game(gameId)) },
                onNavigateToResults = { navController.navigate(Screen.Results) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) }
            )
        }

        composable<Screen.JoinGame> {
            JoinGameScreen(
                onNavigateToBingo = { gameId ->
                    navController.navigate(Screen.Game(gameId)) {
                        popUpTo(Screen.Home)
                    }
                },
                onNavigateToPong = { gameId, totalPaddles, humanCount, difficulty, scoreLimit, isHost, mySide ->
                    navController.navigate(
                        Screen.PongGame(gameId, totalPaddles, humanCount, difficulty, scoreLimit, isHost, mySide)
                    ) { popUpTo(Screen.Home) }
                },
                onNavigateToVier = { gameId, myDrinkId ->
                    navController.navigate(
                        Screen.VierGame("online", gameId, myDrinkId, null)
                    ) { popUpTo(Screen.Home) }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Game> { backStack ->
            val route: Screen.Game = backStack.toRoute()
            GameScreen(
                gameId = route.gameId,
                onNavigateBack = {
                    navController.navigate(Screen.Lobby) {
                        popUpTo(Screen.Lobby) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Results> {
            ResultsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.Profile> {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = authViewModel
            )
        }

        // ── BeachPong ──────────────────────────────────────────────────────────
        composable<Screen.PongLobby> {
            PongLobbyScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToGame = { gameId, totalPaddles, humanCount, difficulty, scoreLimit, isHost, mySide ->
                    navController.navigate(
                        Screen.PongGame(gameId, totalPaddles, humanCount, difficulty, scoreLimit, isHost, mySide)
                    ) { popUpTo(Screen.PongLobby) }
                },
                onNavigateToResults = { navController.navigate(Screen.PongResults) },
                onNavigateToSettings = { navController.navigate(Screen.PongSettings) },
            )
        }

        composable<Screen.PongGame> { backStack ->
            val route: Screen.PongGame = backStack.toRoute()
            PongGameScreen(
                gameId = route.gameId,
                totalPaddles = route.totalPaddles,
                humanCount = route.humanCount,
                difficulty = route.difficulty,
                scoreLimit = route.scoreLimit,
                isHost = route.isHost,
                mySide = route.mySide,
                onNavigateToLobby = {
                    navController.navigate(Screen.PongLobby) {
                        popUpTo(Screen.PongLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.PongSettings> {
            PongSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.PongResults> {
            PongResultsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.VierLobby> {
            VierLobbyScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToGame = { mode, gameId, myDrinkId, aiDrinkId, aiDifficulty ->
                    navController.navigate(Screen.VierGame(mode, gameId, myDrinkId, aiDrinkId, aiDifficulty)) {
                        popUpTo(Screen.VierLobby)
                    }
                },
                onNavigateToResults = { navController.navigate(Screen.VierResults) },
                onNavigateToSettings = { navController.navigate(Screen.VierSettings) },
            )
        }

        composable<Screen.VierGame> { backStack ->
            val route: Screen.VierGame = backStack.toRoute()
            VierGameScreen(
                mode = route.mode,
                gameId = route.gameId,
                myDrinkId = route.myDrinkId,
                aiDrinkId = route.aiDrinkId,
                aiDifficulty = route.aiDifficulty,
                onNavigateBack = {
                    navController.navigate(Screen.VierLobby) {
                        popUpTo(Screen.VierLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.VierSettings> {
            VierSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.VierResults> {
            VierResultsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── BeachPirates ───────────────────────────────────────────────────────
        composable<Screen.PiratesLobby> {
            PiratesLobbyScreen(
                onNavigateToGame = { difficulty, fireRate, controlMode ->
                    navController.navigate(Screen.PiratesGame(difficulty, fireRate, controlMode)) {
                        popUpTo(Screen.PiratesLobby)
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.PiratesSettings) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToResults = { navController.navigate(Screen.PiratesHighscore) },
            )
        }

        composable<Screen.PiratesHighscore> {
            PiratesHighscoreScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.PiratesGame> { backStack ->
            val route: Screen.PiratesGame = backStack.toRoute()
            PiratesGameScreen(
                difficulty = route.difficulty,
                fireRate = route.fireRate,
                controlMode = route.controlMode,
                onNavigateToResults = { score, wave, highScore, newHighScore ->
                    navController.navigate(
                        Screen.PiratesResults(score, wave, route.difficulty, highScore, newHighScore)
                    ) { popUpTo(Screen.PiratesLobby) }
                },
                onNavigateToLobby = {
                    navController.navigate(Screen.PiratesLobby) {
                        popUpTo(Screen.PiratesLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.PiratesSettings> {
            PiratesSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.PiratesResults> { backStack ->
            val route: Screen.PiratesResults = backStack.toRoute()
            PiratesResultsScreen(
                score = route.score,
                wave = route.wave,
                difficulty = route.difficulty,
                highScore = route.highScore,
                newHighScore = route.newHighScore,
                onPlayAgain = {
                    navController.navigate(Screen.PiratesLobby) {
                        popUpTo(Screen.PiratesLobby) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }
    }
}
