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
import com.bestfriends.beachbingo.feature.home.ui.HomeScreen

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
            navController.navigate(Screen.Home) {
                popUpTo(Screen.Login) { inclusive = true }
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
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
                viewModel = authViewModel
            )
        }

        composable<Screen.Lobby> {
            LobbyScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToJoinGame = { navController.navigate(Screen.JoinGame) },
                onNavigateToGame = { gameId -> navController.navigate(Screen.Game(gameId)) },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
                onNavigateToResults = { navController.navigate(Screen.Results) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) }
            )
        }

        composable<Screen.JoinGame> {
            JoinGameScreen(
                onNavigateToGame = { gameId ->
                    navController.navigate(Screen.Game(gameId)) {
                        popUpTo(Screen.Lobby)
                    }
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
    }
}
