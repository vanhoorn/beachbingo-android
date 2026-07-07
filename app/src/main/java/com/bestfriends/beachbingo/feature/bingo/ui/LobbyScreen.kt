package com.bestfriends.beachbingo.feature.bingo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.core.model.BingoGame
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.core.model.GameStatus
import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.feature.bingo.viewmodel.LobbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToGame: (String) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userGames by viewModel.userGames.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var gameToDelete by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    val lobbyAuth = FirebaseAuth.getInstance()
    val lobbyFirestore = FirebaseFirestore.getInstance()
    val lobbyUid = lobbyAuth.currentUser?.uid

    LaunchedEffect(lobbyUid) {
        if (lobbyUid == null) return@LaunchedEffect
        val snap = try { lobbyFirestore.collection("users").document(lobbyUid).get().await() } catch (_: Exception) { return@LaunchedEffect }
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("bingo") == true
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("bingo") else FieldValue.arrayRemove("bingo")
        if (lobbyUid != null) lobbyFirestore.collection("users").document(lobbyUid).update("favoriteGames", update)
    }

    LaunchedEffect(uiState.navigateToGameId) {
        uiState.navigateToGameId?.let { gameId ->
            viewModel.clearNavigate()
            onNavigateToGame(gameId)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNavigate()
        }
    }

    if (gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = { Text("Spiel löschen?") },
            text = { Text("Das Spiel wird für alle Spieler unwiderruflich gelöscht.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteGame(gameToDelete!!); gameToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { gameToDelete = null }) { Text("Abbrechen") }
            }
        )
    }

    if (showCreateDialog) {
        val user = currentUser
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Spiel erstellen", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user != null) {
                        Text(
                            "Deine aktuellen Einstellungen:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Level: ${when (user.preferredGameMode) {
                                        GameMode.AUTO_MARK -> "🌊 Rookie"
                                        GameMode.MANUAL_MARK -> "🎯 Sniper"
                                        GameMode.MINI_BOSS_LEVEL -> "🔵 Mini Boss Level"
                                        GameMode.BOSS_LEVEL -> "💪 Boss Level"
                                    }}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Ziehung: ${if (user.preferredDrawStyle == DrawStyle.INSTANT) "⚡ Sofort" else "🥁 Lostrommel"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Einstellungen ändern? Tippe oben auf ⚙️.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCreateDialog = false; viewModel.createGame() },
                    modifier = Modifier.height(48.dp)
                ) { Text("Los geht's! 🏖️") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BEACHBINGO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🎯 Lobby",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToResults) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Ergebnisse", tint = SandGold, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { toggleFavorite() }) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = 22.sp,
                            color = if (isFavorite) SandGold else com.bestfriends.beachbingo.ui.theme.TextMuted,
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen", modifier = Modifier.size(28.dp))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                },
                text = { Text("Spiel erstellen", style = MaterialTheme.typography.labelLarge) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (userGames.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌊", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Noch keine Spiele.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Erstelle ein neues oder tritt einem bei!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "Deine Spiele",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                items(userGames, key = { it.gameId }) { game ->
                    GameCard(
                        game = game,
                        isAdmin = game.adminId == currentUser?.uid,
                        onClick = { onNavigateToGame(game.gameId) },
                        onDelete = { gameToDelete = game.gameId }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun GameCard(game: BingoGame, isAdmin: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Spiel #${game.gameId.take(8).uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${game.players.size} Spieler · ${
                        when (game.gameMode) {
                            GameMode.AUTO_MARK -> "🌊 Rookie"
                            GameMode.MANUAL_MARK -> "🎯 Sniper"
                            GameMode.MINI_BOSS_LEVEL -> "🔵 Mini Boss Level"
                            GameMode.BOSS_LEVEL -> "💪 Boss Level"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            val (statusLabel, statusColor) = when (game.status) {
                GameStatus.LOBBY -> "Wartet" to MaterialTheme.colorScheme.tertiary
                GameStatus.RUNNING -> "Läuft 🔥" to MaterialTheme.colorScheme.primary
                GameStatus.FINISHED -> "Beendet" to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            if (isAdmin) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
