package com.bestfriends.beachbingo.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.ALL_GAMES
import com.bestfriends.beachbingo.core.model.GameMetadata
import com.bestfriends.beachbingo.core.model.PlayerCount
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class PlayerCountEntry(
    val key: PlayerCount,
    val label: String,
    val emoji: String,
)

private val PLAYER_COUNT_LIST = listOf(
    PlayerCountEntry(PlayerCount.ONE,       "1 Spieler",   "👤"),
    PlayerCountEntry(PlayerCount.ONE_TWO,   "1-2 Spieler", "🤝"),
    PlayerCountEntry(PlayerCount.TWO_FOUR,  "2-4 Spieler", "👥"),
    PlayerCountEntry(PlayerCount.FOUR_PLUS, "4+ Spieler",  "🎉"),
)

@Composable
fun HomeScreen(
    onNavigateToBingoLobby: () -> Unit,
    onNavigateToPongLobby: () -> Unit,
    onNavigateToVierLobby: () -> Unit,
    onNavigateToPiratesLobby: () -> Unit,
    onNavigateToWormLobby: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var favoriteIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = firestore.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            favoriteIds = (snap.get("favoriteGames") as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            recentIds = (snap.get("recentGames") as? List<String>) ?: emptyList()
        } catch (_: Exception) {}
    }

    fun handleGameClick(gameId: String, navigate: () -> Unit) {
        if (uid != null) {
            val updated = (listOf(gameId) + recentIds.filter { it != gameId }).take(10)
            recentIds = updated
            scope.launch {
                try {
                    firestore.collection("users").document(uid).update("recentGames", updated)
                } catch (_: Exception) {}
            }
        }
        navigate()
    }

    val favoriteGames = ALL_GAMES
        .filter { it.id in favoriteIds }
        .sortedBy { it.title }

    val recentGames = recentIds.take(3)
        .mapNotNull { id -> ALL_GAMES.find { it.id == id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero ─────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Surface2Dark,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = currentUser?.avatarUrl ?: "🏖️", fontSize = 32.sp)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WILLKOMMEN ZURÜCK",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = currentUser?.displayName ?: "…",
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp), color = Surface2Dark,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToJoin() }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("🔗", fontSize = 22.sp) }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp), color = Surface2Dark,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToProfile() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = TextSub, modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ── Favoriten ─────────────────────────────────────────────────────────────
        if (favoriteGames.isNotEmpty()) {
            SectionHeader(title = "FAVORITEN", emoji = "★", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                favoriteGames.forEach { game ->
                    MiniGameCard(game = game, onClick = {
                        handleGameClick(game.id) {
                            when (game.id) {
                                "bingo"   -> onNavigateToBingoLobby()
                                "pong"    -> onNavigateToPongLobby()
                                "vier"    -> onNavigateToVierLobby()
                                "pirates" -> onNavigateToPiratesLobby()
                                "worm"    -> onNavigateToWormLobby()
                            }
                        }
                    })
                }
            }
        }

        // ── Zuletzt gespielt ──────────────────────────────────────────────────────
        if (recentGames.isNotEmpty()) {
            SectionHeader(title = "ZULETZT GESPIELT", emoji = "🕐", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentGames.forEach { game ->
                    MiniGameCard(game = game, onClick = {
                        handleGameClick(game.id) {
                            when (game.id) {
                                "bingo"   -> onNavigateToBingoLobby()
                                "pong"    -> onNavigateToPongLobby()
                                "vier"    -> onNavigateToVierLobby()
                                "pirates" -> onNavigateToPiratesLobby()
                                "worm"    -> onNavigateToWormLobby()
                            }
                        }
                    })
                }
            }
        }

        // ── Spieleranzahl ─────────────────────────────────────────────────────────
        SectionHeader(title = "SPIELERANZAHL", emoji = "🎮", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp))

        // 3-column grid — first 3 in a row, last one below
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PLAYER_COUNT_LIST.take(3).forEach { entry ->
                    val gameCount = ALL_GAMES.count { entry.key in it.playerCounts }
                    CategoryTile(
                        entry = entry,
                        gameCount = gameCount,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToCategory(entry.key.name) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val entry = PLAYER_COUNT_LIST[3]
                val gameCount = ALL_GAMES.count { entry.key in it.playerCounts }
                CategoryTile(
                    entry = entry,
                    gameCount = gameCount,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToCategory(entry.key.name) }
                )
                // Empty spacers to keep tile same width as grid columns
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String, emoji: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text = title,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = TextMuted, letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun MiniGameCard(game: GameMetadata, onClick: () -> Unit) {
    val accentColor = Color(game.color)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        modifier = Modifier
            .width(90.dp)
            .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = game.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = game.title,
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = TextPrimary, lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun CategoryTile(
    entry: PlayerCountEntry,
    gameCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        modifier = modifier
            .border(1.5.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = entry.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.label,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = TextPrimary, lineHeight = 15.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$gameCount ${if (gameCount == 1) "Spiel" else "Spiele"}",
                fontSize = 10.sp, color = TextMuted,
            )
        }
    }
}
