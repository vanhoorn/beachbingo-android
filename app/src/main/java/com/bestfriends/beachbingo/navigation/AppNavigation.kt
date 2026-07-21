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
import com.bestfriends.beachbingo.feature.home.ui.AllGamesScreen
import com.bestfriends.beachbingo.feature.home.ui.CardGamesScreen
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
import com.bestfriends.beachbingo.feature.worm.ui.WormLobbyScreen
import com.bestfriends.beachbingo.feature.worm.ui.WormGameScreen
import com.bestfriends.beachbingo.feature.worm.ui.WormSettingsScreen
import com.bestfriends.beachbingo.feature.worm.ui.WormResultsScreen
import com.bestfriends.beachbingo.feature.worm.ui.WormHighscoreScreen
import com.bestfriends.beachbingo.feature.strandturm.ui.StrandturmLobbyScreen
import com.bestfriends.beachbingo.feature.strandturm.ui.StrandturmGameScreen
import com.bestfriends.beachbingo.feature.strandturm.ui.StrandturmSettingsScreen
import com.bestfriends.beachbingo.feature.strandturm.ui.StrandturmHighscoreScreen
import com.bestfriends.beachbingo.feature.strandturm.ui.StrandturmResultsScreen
import com.bestfriends.beachbingo.feature.brandung.ui.BrandungLobbyScreen
import com.bestfriends.beachbingo.feature.brandung.ui.BrandungGameScreen
import com.bestfriends.beachbingo.feature.brandung.ui.BrandungSettingsScreen
import com.bestfriends.beachbingo.feature.brandung.ui.BrandungResultsScreen
import com.bestfriends.beachbingo.feature.meermau.ui.MeermauLobbyScreen
import com.bestfriends.beachbingo.feature.meermau.ui.MeermauGameScreen
import com.bestfriends.beachbingo.feature.meermau.ui.MeermauSettingsScreen
import com.bestfriends.beachbingo.feature.meermau.ui.MeermauResultsScreen

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
            val route = navController.currentBackStackEntry?.destination?.route ?: ""
            val onAuthScreen = "Screen.Login" in route || "Screen.Register" in route
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
                onNavigateToWormLobby = { navController.navigate(Screen.WormLobby) },
                onNavigateToStrandturmLobby = { navController.navigate(Screen.StrandturmLobby) },
                onNavigateToBrandungLobby = { navController.navigate(Screen.BrandungLobby) },
                onNavigateToMeermauLobby = { navController.navigate(Screen.MeermauLobby) },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
                onNavigateToJoin = { navController.navigate(Screen.JoinGame) },
                onNavigateToCategory = { playerCount -> navController.navigate(Screen.Category(playerCount)) },
                onNavigateToCardGames = { navController.navigate(Screen.CardGames) },
                onNavigateToAllGames = { navController.navigate(Screen.AllGames) },
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
                onNavigateToWormLobby = { navController.navigate(Screen.WormLobby) },
                onNavigateToStrandturmLobby = { navController.navigate(Screen.StrandturmLobby) },
                onNavigateToBrandungLobby = { navController.navigate(Screen.BrandungLobby) },
                onNavigateToMeermauLobby = { navController.navigate(Screen.MeermauLobby) },
            )
        }

        composable<Screen.CardGames> {
            CardGamesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBrandungLobby = { navController.navigate(Screen.BrandungLobby) },
                onNavigateToMeermauLobby = { navController.navigate(Screen.MeermauLobby) },
            )
        }

        composable<Screen.AllGames> {
            AllGamesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBingoLobby = { navController.navigate(Screen.Lobby) },
                onNavigateToPongLobby = { navController.navigate(Screen.PongLobby) },
                onNavigateToVierLobby = { navController.navigate(Screen.VierLobby) },
                onNavigateToPiratesLobby = { navController.navigate(Screen.PiratesLobby) },
                onNavigateToWormLobby = { navController.navigate(Screen.WormLobby) },
                onNavigateToStrandturmLobby = { navController.navigate(Screen.StrandturmLobby) },
                onNavigateToBrandungLobby = { navController.navigate(Screen.BrandungLobby) },
                onNavigateToMeermauLobby = { navController.navigate(Screen.MeermauLobby) },
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
                    navController.navigate(Screen.Game(gameId)) { popUpTo(Screen.Home) }
                },
                onNavigateToPong = { gameId, totalPaddles, humanCount, difficulty, scoreLimit, isHost, mySide ->
                    navController.navigate(
                        Screen.PongGame(gameId, totalPaddles, humanCount, difficulty, scoreLimit, isHost, mySide)
                    ) { popUpTo(Screen.Home) }
                },
                onNavigateToVier = { gameId, myDrinkId ->
                    navController.navigate(Screen.VierGame("online", gameId, myDrinkId, null)) { popUpTo(Screen.Home) }
                },
                onNavigateToBrandung = { gameId ->
                    navController.navigate(Screen.BrandungGame("online", gameId, 0, "SNIPER")) { popUpTo(Screen.Home) }
                },
                onNavigateToMeermau = { gameId ->
                    navController.navigate(Screen.MeermauGame("online", gameId, 0, "SNIPER")) { popUpTo(Screen.Home) }
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
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
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
            PongSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
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
            VierSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
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
            PiratesSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
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

        // ── Wattwurm ───────────────────────────────────────────────────────────
        composable<Screen.WormLobby> {
            WormLobbyScreen(
                onNavigateToGame = { difficulty, controlMode ->
                    navController.navigate(Screen.WormGame(difficulty, controlMode)) {
                        popUpTo(Screen.WormLobby)
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.WormSettings) },
                onNavigateToHighscore = { navController.navigate(Screen.WormHighscore) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }

        composable<Screen.WormHighscore> {
            WormHighscoreScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.WormGame> { backStack ->
            val route: Screen.WormGame = backStack.toRoute()
            WormGameScreen(
                difficulty = route.difficulty,
                controlMode = route.controlMode,
                onNavigateToResults = { score, length, highScore, newHighScore ->
                    navController.navigate(
                        Screen.WormResults(score, length, route.difficulty, route.controlMode, highScore, newHighScore)
                    ) { popUpTo(Screen.WormLobby) }
                },
                onNavigateToLobby = {
                    navController.navigate(Screen.WormLobby) {
                        popUpTo(Screen.WormLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.WormSettings> {
            WormSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
        }

        composable<Screen.WormResults> { backStack ->
            val route: Screen.WormResults = backStack.toRoute()
            WormResultsScreen(
                score = route.score,
                length = route.length,
                difficulty = route.difficulty,
                highScore = route.highScore,
                newHighScore = route.newHighScore,
                onPlayAgain = {
                    navController.navigate(Screen.WormGame(route.difficulty, route.controlMode)) {
                        popUpTo(Screen.WormLobby)
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }

        // ── Strandturm ─────────────────────────────────────────────────────────
        composable<Screen.StrandturmLobby> {
            StrandturmLobbyScreen(
                onNavigateToGame = { controlMode, startLevel ->
                    navController.navigate(Screen.StrandturmGame(controlMode, startLevel)) {
                        popUpTo(Screen.StrandturmLobby)
                    }
                },
                onNavigateToSettings  = { navController.navigate(Screen.StrandturmSettings) },
                onNavigateToHighscore = { navController.navigate(Screen.StrandturmHighscore) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }

        composable<Screen.StrandturmHighscore> {
            StrandturmHighscoreScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.StrandturmGame> { backStack ->
            val route: Screen.StrandturmGame = backStack.toRoute()
            StrandturmGameScreen(
                controlMode = route.controlMode,
                startLevel  = route.startLevel,
                onNavigateToResults = { score, level, highScore, bestLevel, newHighScore, newBestLevel ->
                    navController.navigate(
                        Screen.StrandturmResults(score, level, highScore, bestLevel, newHighScore, newBestLevel)
                    ) { popUpTo(Screen.StrandturmLobby) }
                },
                onNavigateToLobby = {
                    navController.navigate(Screen.StrandturmLobby) {
                        popUpTo(Screen.StrandturmLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.StrandturmSettings> {
            StrandturmSettingsScreen(
                onNavigateBack      = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
        }

        composable<Screen.StrandturmResults> { backStack ->
            val route: Screen.StrandturmResults = backStack.toRoute()
            StrandturmResultsScreen(
                score        = route.score,
                level        = route.level,
                highScore    = route.highScore,
                bestLevel    = route.bestLevel,
                newHighScore = route.newHighScore,
                newBestLevel = route.newBestLevel,
                onPlayAgain = {
                    navController.navigate(Screen.StrandturmLobby) {
                        popUpTo(Screen.StrandturmLobby) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
            )
        }

        // ── Brandung ───────────────────────────────────────────────────────────
        composable<Screen.BrandungLobby> {
            BrandungLobbyScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToGame = { mode, gameId, aiCount, difficulty ->
                    navController.navigate(Screen.BrandungGame(mode, gameId, aiCount, difficulty)) {
                        popUpTo(Screen.BrandungLobby)
                    }
                },
                onNavigateToResults = { navController.navigate(Screen.BrandungResults) },
                onNavigateToSettings = { navController.navigate(Screen.BrandungSettings) },
            )
        }

        composable<Screen.BrandungGame> { backStack ->
            val route: Screen.BrandungGame = backStack.toRoute()
            BrandungGameScreen(
                mode = route.mode,
                gameId = route.gameId,
                aiCount = route.aiCount,
                difficulty = route.difficulty,
                onNavigateBack = {
                    navController.navigate(Screen.BrandungLobby) {
                        popUpTo(Screen.BrandungLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.BrandungSettings> {
            BrandungSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
        }

        composable<Screen.BrandungResults> {
            BrandungResultsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ── MeerMau ────────────────────────────────────────────────────────────
        composable<Screen.MeermauLobby> {
            MeermauLobbyScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToGame = { mode, gameId, aiCount, difficulty ->
                    navController.navigate(Screen.MeermauGame(mode, gameId, aiCount, difficulty)) {
                        popUpTo(Screen.MeermauLobby)
                    }
                },
                onNavigateToResults = { navController.navigate(Screen.MeermauResults) },
                onNavigateToSettings = { navController.navigate(Screen.MeermauSettings) },
            )
        }

        composable<Screen.MeermauGame> { backStack ->
            val route: Screen.MeermauGame = backStack.toRoute()
            MeermauGameScreen(
                mode = route.mode,
                gameId = route.gameId,
                aiCount = route.aiCount,
                difficulty = route.difficulty,
                onNavigateBack = {
                    navController.navigate(Screen.MeermauLobby) {
                        popUpTo(Screen.MeermauLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.MeermauSettings> {
            MeermauSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
        }

        composable<Screen.MeermauResults> {
            MeermauResultsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
