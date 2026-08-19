package com.bestfriends.beachbingo.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import com.bestfriends.beachbingo.feature.home.ui.ActionGamesScreen
import com.bestfriends.beachbingo.feature.home.ui.AllGamesScreen
import com.bestfriends.beachbingo.feature.home.ui.CardGamesScreen
import com.bestfriends.beachbingo.feature.home.ui.CategoryScreen
import com.bestfriends.beachbingo.feature.home.ui.CouchGamesScreen
import com.bestfriends.beachbingo.feature.home.ui.HomeScreen
import com.bestfriends.beachbingo.feature.home.ui.RaetselScreen
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
import com.bestfriends.beachbingo.feature.strandraeuber.ui.StrandraeuberLobbyScreen
import com.bestfriends.beachbingo.feature.strandraeuber.ui.StrandraeuberGameScreen
import com.bestfriends.beachbingo.feature.strandraeuber.ui.StrandraeuberSettingsScreen
import com.bestfriends.beachbingo.feature.strandraeuber.ui.StrandraeuberResultsScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.DuenenschattenLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.DuenenschattenGameScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.InselbrueckeLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.InselbrueckeGameScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.StrandokuLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.StrandokuGameScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.WellensummeLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.WellensummeGameScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegGameScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegPlacementScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegBattleScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegOnlineLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegOnlinePlacementScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.KuestenkriegOnlineBattleScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.WortWelleLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.ui.WortWelleGameScreen
import com.bestfriends.beachbingo.feature.mahjong.ui.MahjongLobbyScreen
import com.bestfriends.beachbingo.feature.mahjong.ui.MahjongGameScreen
import com.bestfriends.beachbingo.feature.mahjong.ui.MahjongSettingsScreen
import com.bestfriends.beachbingo.feature.perlentaucher.ui.PerlentaucherLobbyScreen
import com.bestfriends.beachbingo.feature.perlentaucher.ui.PerlentaucherGameScreen
import com.bestfriends.beachbingo.feature.perlentaucher.ui.PerlentaucherResultsScreen
import com.bestfriends.beachbingo.feature.perlentaucher.ui.PerlentaucherSettingsScreen
import com.bestfriends.beachbingo.feature.perlentaucher.ui.createFreshPerlentaucherSave
import com.bestfriends.beachbingo.feature.sonnenrad.ui.SonnenradBonusScreen
import com.bestfriends.beachbingo.feature.sonnenrad.ui.SonnenradLobbyScreen
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSave

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
                onNavigateToStrandraeuberLobby = { navController.navigate(Screen.StrandraeuberLobby) },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
                onNavigateToJoin = { navController.navigate(Screen.JoinGame) },
                onNavigateToCategory = { playerCount -> navController.navigate(Screen.Category(playerCount)) },
                onNavigateToCardGames = { navController.navigate(Screen.CardGames) },
                onNavigateToActionGames = { navController.navigate(Screen.ActionGames) },
                onNavigateToCouchGames = { navController.navigate(Screen.CouchGames) },
                onNavigateToAllGames = { navController.navigate(Screen.AllGames) },
                onNavigateToRaetsel = { navController.navigate(Screen.Raetsel) },
                onNavigateToDuenenschattenLobby = { navController.navigate(Screen.DuenenschattenLobby) },
                onNavigateToInselbrueckeLobby = { navController.navigate(Screen.InselbrueckeLobby) },
                onNavigateToStrandokuLobby = { navController.navigate(Screen.StrandokuLobby) },
                onNavigateToWellensummeLobby = { navController.navigate(Screen.WellensummeLobby) },
                onNavigateToKuestenkriegLobby = { navController.navigate(Screen.KuestenkriegLobby) },
                onNavigateToWortWelleLobby = { navController.navigate(Screen.WortWelleLobby) },
                onNavigateToPerlentaucherLobby = { navController.navigate(Screen.PerlentaucherLobby) },
                onNavigateToRaetselGame = { save ->
                    when (save.gameType) {
                        "duenenschatten" -> navController.navigate(Screen.DuenenschattenLobby)
                        "inselbruecke"   -> navController.navigate(Screen.InselbrueckeLobby)
                        "strandoku"      -> navController.navigate(Screen.StrandokuLobby)
                        "wellensumme"    -> navController.navigate(Screen.WellensummeLobby)
                        "kuestenkrieg"   -> navController.navigate(Screen.KuestenkriegLobby)
                        "wortwelle"      -> navController.navigate(Screen.WortWelleLobby)
                        "mahjong"        -> navController.navigate(Screen.MahjongLobby)
                    }
                },
                onRejoinGame = { type, gameId ->
                    when (type) {
                        "strandraeuber" -> navController.navigate(Screen.StrandraeuberGame("ONLINE", gameId, 0, "SNIPER", 3))
                        "meermau"       -> navController.navigate(Screen.MeermauGame("online", gameId, 0, "SNIPER"))
                        "brandung"      -> navController.navigate(Screen.BrandungGame("online", gameId, 0, "SNIPER"))
                        "bingo"         -> navController.navigate(Screen.Game(gameId))
                    }
                },
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
                onNavigateToStrandraeuberLobby = { navController.navigate(Screen.StrandraeuberLobby) },
                onNavigateToDuenenschattenLobby = { navController.navigate(Screen.DuenenschattenLobby) },
                onNavigateToInselbrueckeLobby = { navController.navigate(Screen.InselbrueckeLobby) },
                onNavigateToStrandokuLobby = { navController.navigate(Screen.StrandokuLobby) },
                onNavigateToWellensummeLobby = { navController.navigate(Screen.WellensummeLobby) },
                onNavigateToKuestenkriegLobby = { navController.navigate(Screen.KuestenkriegLobby) },
                onNavigateToWortWelleLobby = { navController.navigate(Screen.WortWelleLobby) },
            )
        }

        composable<Screen.CardGames> {
            CardGamesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBrandungLobby = { navController.navigate(Screen.BrandungLobby) },
                onNavigateToMeermauLobby = { navController.navigate(Screen.MeermauLobby) },
                onNavigateToStrandraeuberLobby = { navController.navigate(Screen.StrandraeuberLobby) },
            )
        }

        composable<Screen.ActionGames> {
            ActionGamesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPongLobby = { navController.navigate(Screen.PongLobby) },
                onNavigateToPiratesLobby = { navController.navigate(Screen.PiratesLobby) },
                onNavigateToStrandturmLobby = { navController.navigate(Screen.StrandturmLobby) },
            )
        }

        composable<Screen.CouchGames> {
            CouchGamesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBingoLobby = { navController.navigate(Screen.Lobby) },
                onNavigateToVierLobby = { navController.navigate(Screen.VierLobby) },
                onNavigateToWormLobby = { navController.navigate(Screen.WormLobby) },
                onNavigateToMahjongLobby = { navController.navigate(Screen.MahjongLobby) },
                onNavigateToSonnenrad = { navController.navigate(Screen.SonnenradLobby) },
            )
        }

        composable<Screen.Raetsel> {
            RaetselScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDuenenschattenLobby = { navController.navigate(Screen.DuenenschattenLobby) },
                onNavigateToInselbrueckeLobby = { navController.navigate(Screen.InselbrueckeLobby) },
                onNavigateToStrandokuLobby = { navController.navigate(Screen.StrandokuLobby) },
                onNavigateToWellensummeLobby = { navController.navigate(Screen.WellensummeLobby) },
                onNavigateToKuestenkriegLobby = { navController.navigate(Screen.KuestenkriegLobby) },
                onNavigateToWortWelleLobby = { navController.navigate(Screen.WortWelleLobby) },
                onNavigateToPerlentaucherLobby = { navController.navigate(Screen.PerlentaucherLobby) },
            )
        }

        // ── Rätsel-Rubrik ──────────────────────────────────────────────────────
        composable<Screen.DuenenschattenLobby> {
            DuenenschattenLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { diff, seed, saveId ->
                    navController.navigate(Screen.DuenenschattenGame(diff, seed, saveId)) { popUpTo(Screen.DuenenschattenLobby) }
                }
            )
        }
        composable<Screen.DuenenschattenGame> { backStack ->
            val route: Screen.DuenenschattenGame = backStack.toRoute()
            DuenenschattenGameScreen(
                difficulty = route.difficulty, seed = route.seed, saveId = route.saveId,
                onNavigateBack = { navController.navigate(Screen.DuenenschattenLobby) { popUpTo(Screen.DuenenschattenLobby) { inclusive = true } } }
            )
        }
        composable<Screen.InselbrueckeLobby> {
            InselbrueckeLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { diff, seed, saveId ->
                    navController.navigate(Screen.InselbrueckeGame(diff, seed, saveId)) { popUpTo(Screen.InselbrueckeLobby) }
                }
            )
        }
        composable<Screen.InselbrueckeGame> { backStack ->
            val route: Screen.InselbrueckeGame = backStack.toRoute()
            InselbrueckeGameScreen(
                difficulty = route.difficulty, seed = route.seed, saveId = route.saveId,
                onNavigateBack = { navController.navigate(Screen.InselbrueckeLobby) { popUpTo(Screen.InselbrueckeLobby) { inclusive = true } } }
            )
        }
        composable<Screen.StrandokuLobby> {
            StrandokuLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { variant, diff, seed, saveId ->
                    navController.navigate(Screen.StrandokuGame(variant, diff, seed, saveId)) { popUpTo(Screen.StrandokuLobby) }
                }
            )
        }
        composable<Screen.StrandokuGame> { backStack ->
            val route: Screen.StrandokuGame = backStack.toRoute()
            StrandokuGameScreen(
                variant = route.variant, difficulty = route.difficulty, seed = route.seed, saveId = route.saveId,
                onNavigateBack = { navController.navigate(Screen.StrandokuLobby) { popUpTo(Screen.StrandokuLobby) { inclusive = true } } }
            )
        }
        composable<Screen.WellensummeLobby> {
            WellensummeLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { diff, seed, saveId ->
                    navController.navigate(Screen.WellensummeGame(diff, seed, saveId)) { popUpTo(Screen.WellensummeLobby) }
                }
            )
        }
        composable<Screen.WellensummeGame> { backStack ->
            val route: Screen.WellensummeGame = backStack.toRoute()
            WellensummeGameScreen(
                difficulty = route.difficulty, seed = route.seed, saveId = route.saveId,
                onNavigateBack = { navController.navigate(Screen.WellensummeLobby) { popUpTo(Screen.WellensummeLobby) { inclusive = true } } }
            )
        }
        composable<Screen.KuestenkriegLobby> {
            KuestenkriegLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { diff, seed, saveId ->
                    navController.navigate(Screen.KuestenkriegGame(diff, seed, saveId)) { popUpTo(Screen.KuestenkriegLobby) }
                },
                onNavigateToPlacement = { aiMode ->
                    navController.navigate(Screen.KuestenkriegPlacement(aiMode))
                },
                onNavigateToOnlineLobby = { code ->
                    navController.navigate(Screen.KuestenkriegOnlineLobby(code))
                },
                onNavigateToBattle = {
                    navController.navigate(Screen.KuestenkriegBattle)
                },
            )
        }
        composable<Screen.KuestenkriegOnlineLobby> { backStack ->
            val route: Screen.KuestenkriegOnlineLobby = backStack.toRoute()
            KuestenkriegOnlineLobbyScreen(
                gameCode = route.joinCode,
                onNavigateBack = { navController.navigate(Screen.KuestenkriegLobby) { popUpTo(Screen.KuestenkriegLobby) { inclusive = true } } },
                onNavigateToPlacement = { code ->
                    navController.navigate(Screen.KuestenkriegOnlinePlacement(code)) {
                        popUpTo(Screen.KuestenkriegOnlineLobby(code)) { inclusive = true }
                    }
                },
            )
        }
        composable<Screen.KuestenkriegOnlinePlacement> { backStack ->
            val route: Screen.KuestenkriegOnlinePlacement = backStack.toRoute()
            KuestenkriegOnlinePlacementScreen(
                gameCode = route.gameCode,
                onNavigateBack = { navController.navigate(Screen.KuestenkriegLobby) { popUpTo(Screen.KuestenkriegLobby) { inclusive = true } } },
                onNavigateToBattle = { code ->
                    navController.navigate(Screen.KuestenkriegOnlineBattle(code)) {
                        popUpTo(Screen.KuestenkriegOnlinePlacement(code)) { inclusive = true }
                    }
                },
            )
        }
        composable<Screen.KuestenkriegOnlineBattle> { backStack ->
            val route: Screen.KuestenkriegOnlineBattle = backStack.toRoute()
            KuestenkriegOnlineBattleScreen(
                gameCode = route.gameCode,
                onNavigateBack = { navController.navigate(Screen.KuestenkriegLobby) { popUpTo(Screen.KuestenkriegLobby) { inclusive = true } } },
            )
        }
        composable<Screen.KuestenkriegGame> { backStack ->
            val route: Screen.KuestenkriegGame = backStack.toRoute()
            KuestenkriegGameScreen(
                difficulty = route.difficulty, seed = route.seed, saveId = route.saveId,
                onNavigateBack = { navController.navigate(Screen.KuestenkriegLobby) { popUpTo(Screen.KuestenkriegLobby) { inclusive = true } } }
            )
        }
        composable<Screen.WortWelleLobby> {
            WortWelleLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { diff, isDaily, dailyWord, dateStr, saveId ->
                    navController.navigate(Screen.WortWelleGame(diff, isDaily, dailyWord, dateStr, saveId)) {
                        popUpTo(Screen.WortWelleLobby)
                    }
                }
            )
        }
        composable<Screen.WortWelleGame> { backStack ->
            val route: Screen.WortWelleGame = backStack.toRoute()
            WortWelleGameScreen(
                difficulty = route.difficulty,
                isDaily = route.isDaily,
                dailyWord = route.dailyWord.ifEmpty { null },
                dateStr = route.dateStr.ifEmpty { null },
                saveId = route.saveId,
                onNavigateBack = { navController.navigate(Screen.WortWelleLobby) { popUpTo(Screen.WortWelleLobby) { inclusive = true } } }
            )
        }
        // ── GezeitenSteine (Mahjong) ───────────────────────────────────────────
        composable<Screen.MahjongLobby> {
            MahjongLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.MahjongSettings) },
                onNavigateToGame = { layout, difficulty, seed, saveId ->
                    navController.navigate(Screen.MahjongGame(layout, difficulty, seed, saveId)) {
                        popUpTo(Screen.MahjongLobby)
                    }
                }
            )
        }
        composable<Screen.MahjongSettings> {
            MahjongSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.MahjongGame> { backStack ->
            val route: Screen.MahjongGame = backStack.toRoute()
            MahjongGameScreen(
                layout = route.layout,
                difficulty = route.difficulty,
                seed = route.seed,
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
                onNavigateBack = { navController.navigate(Screen.MahjongLobby) { popUpTo(Screen.MahjongLobby) { inclusive = true } } }
            )
        }
        // ── Perlentaucher ──────────────────────────────────────────────────────
        composable<Screen.PerlentaucherLobby> {
            PerlentaucherLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { level, saveId ->
                    navController.navigate(Screen.PerlentaucherGame(level, saveId)) {
                        popUpTo(Screen.PerlentaucherLobby)
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.PerlentaucherSettings) },
            )
        }
        composable<Screen.PerlentaucherSettings> {
            PerlentaucherSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.PerlentaucherGame> { backStack ->
            val route: Screen.PerlentaucherGame = backStack.toRoute()
            PerlentaucherGameScreen(
                level = route.level,
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
                onNavigateBack = {
                    navController.navigate(Screen.PerlentaucherLobby) {
                        popUpTo(Screen.PerlentaucherLobby) { inclusive = true }
                    }
                },
                onNavigateToGame = { nextLevel, nextSaveId ->
                    navController.navigate(Screen.PerlentaucherGame(nextLevel, nextSaveId)) {
                        popUpTo(Screen.PerlentaucherLobby)
                    }
                },
                onNavigateToResults = { level, score, movesLeft, bestScore, newBestScore ->
                    navController.navigate(Screen.PerlentaucherResults(level, score, movesLeft, bestScore, newBestScore)) {
                        popUpTo(Screen.PerlentaucherLobby)
                    }
                },
            )
        }
        composable<Screen.PerlentaucherResults> { backStack ->
            val route: Screen.PerlentaucherResults = backStack.toRoute()
            val context = LocalContext.current
            PerlentaucherResultsScreen(
                level = route.level,
                score = route.score,
                movesLeft = route.movesLeft,
                bestScore = route.bestScore,
                newBestScore = route.newBestScore,
                onNextLevel = {
                    navController.navigate(Screen.PerlentaucherGame(route.level + 1, null)) {
                        popUpTo(Screen.PerlentaucherLobby)
                    }
                },
                onSaveAndQuit = {
                    if (route.level < 150) createFreshPerlentaucherSave(context, route.level + 1)
                    navController.navigate(Screen.PerlentaucherLobby) {
                        popUpTo(Screen.PerlentaucherLobby) { inclusive = true }
                    }
                },
                onNavigateToLobby = {
                    navController.navigate(Screen.PerlentaucherLobby) {
                        popUpTo(Screen.PerlentaucherLobby) { inclusive = true }
                    }
                },
            )
        }

        // ── Sonnenrad ──────────────────────────────────────────────────────────
        composable<Screen.SonnenradLobby> {
            SonnenradLobbyScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { navController.navigate(Screen.Sonnenrad) },
            )
        }
        composable<Screen.Sonnenrad> {
            SonnenradBonusScreen(
                onNavigateBack = { navController.popBackStack() },
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
            )
        }

        composable<Screen.KuestenkriegPlacement> { backStack ->
            val route: Screen.KuestenkriegPlacement = backStack.toRoute()
            KuestenkriegPlacementScreen(
                aiMode = route.aiMode,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBattle = {
                    navController.navigate(Screen.KuestenkriegBattle) { popUpTo(Screen.KuestenkriegPlacement(route.aiMode)) }
                }
            )
        }
        composable<Screen.KuestenkriegBattle> {
            KuestenkriegBattleScreen(
                onNavigateBack = { navController.navigate(Screen.KuestenkriegLobby) { popUpTo(Screen.KuestenkriegLobby) { inclusive = true } } },
                onNavigateToPlacement = { aiMode ->
                    navController.navigate(Screen.KuestenkriegPlacement(aiMode)) { popUpTo(Screen.KuestenkriegLobby) }
                }
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
                onNavigateToStrandraeuberLobby = { navController.navigate(Screen.StrandraeuberLobby) },
                onNavigateToDuenenschattenLobby = { navController.navigate(Screen.DuenenschattenLobby) },
                onNavigateToInselbrueckeLobby = { navController.navigate(Screen.InselbrueckeLobby) },
                onNavigateToStrandokuLobby = { navController.navigate(Screen.StrandokuLobby) },
                onNavigateToWellensummeLobby = { navController.navigate(Screen.WellensummeLobby) },
                onNavigateToKuestenkriegLobby = { navController.navigate(Screen.KuestenkriegLobby) },
                onNavigateToWortWelleLobby = { navController.navigate(Screen.WortWelleLobby) },
                onNavigateToPerlentaucherLobby = { navController.navigate(Screen.PerlentaucherLobby) },
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
                onNavigateToStrandraeuber = { gameId ->
                    navController.navigate(Screen.StrandraeuberGame("ONLINE", gameId, 0, "SNIPER", 3)) { popUpTo(Screen.Home) }
                },
                onNavigateToKuestenkrieg = { code ->
                    navController.navigate(Screen.KuestenkriegOnlineLobby(code)) { popUpTo(Screen.Home) }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Game> { backStack ->
            val route: Screen.Game = backStack.toRoute()
            GameScreen(
                gameId = route.gameId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                onNavigateToGame = { mode, gameId, myDrinkId, aiDrinkId, aiDifficulty, saveId ->
                    navController.navigate(Screen.VierGame(mode, gameId, myDrinkId, aiDrinkId, aiDifficulty, saveId)) {
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
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                onNavigateToGame = { difficulty, fireRate, controlMode, saveId ->
                    navController.navigate(Screen.PiratesGame(difficulty, fireRate, controlMode, saveId)) {
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
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                onNavigateToGame = { difficulty, controlMode, saveId ->
                    navController.navigate(Screen.WormGame(difficulty, controlMode, saveId)) {
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
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                onNavigateToGame = { controlMode, startLevel, saveId ->
                    navController.navigate(Screen.StrandturmGame(controlMode, startLevel, saveId)) {
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
                saveId      = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                onNavigateToGame = { mode, gameId, aiCount, difficulty, saveId ->
                    navController.navigate(Screen.BrandungGame(mode, gameId, aiCount, difficulty, saveId)) {
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
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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
                onNavigateToGame = { mode, gameId, aiCount, difficulty, saveId ->
                    navController.navigate(Screen.MeermauGame(mode, gameId, aiCount, difficulty, saveId)) {
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
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
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

        // ── Strandräuber ───────────────────────────────────────────────────────
        composable<Screen.StrandraeuberLobby> {
            StrandraeuberLobbyScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onNavigateToGame = { mode, gameId, aiCount, difficulty, totalRounds, saveId ->
                    navController.navigate(Screen.StrandraeuberGame(mode, gameId, aiCount, difficulty, totalRounds, saveId)) {
                        popUpTo(Screen.StrandraeuberLobby)
                    }
                },
                onNavigateToResults = { navController.navigate(Screen.StrandraeuberResults) },
                onNavigateToSettings = { navController.navigate(Screen.StrandraeuberSettings) },
            )
        }

        composable<Screen.StrandraeuberGame> { backStack ->
            val route: Screen.StrandraeuberGame = backStack.toRoute()
            StrandraeuberGameScreen(
                mode = route.mode,
                gameId = route.gameId,
                aiCount = route.aiCount,
                difficulty = route.difficulty,
                totalRounds = route.totalRounds,
                saveId = route.saveId,
                soundEnabled = currentUser?.soundEnabled ?: true,
                musicEnabled = currentUser?.musicEnabled ?: true,
                onNavigateBack = {
                    navController.navigate(Screen.StrandraeuberLobby) {
                        popUpTo(Screen.StrandraeuberLobby) { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.StrandraeuberSettings> {
            StrandraeuberSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
            )
        }

        composable<Screen.StrandraeuberResults> {
            StrandraeuberResultsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
