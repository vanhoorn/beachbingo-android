package com.bestfriends.beachbingo.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub

data class GameEntry(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val accentColor: Color,
    val available: Boolean,
)

private val GAMES = listOf(
    GameEntry(
        id = "bingo",
        emoji = "🎱",
        title = "BeachBingo",
        description = "Ziehe Zahlen, markiere deine Karte – BINGO!",
        accentColor = OceanBlue,
        available = true,
    ),
    GameEntry(
        id = "pong",
        emoji = "🏓",
        title = "BeachPong",
        description = "Klassisches Tischtennis am Strand – wer gewinnt die Runde?",
        accentColor = Coral,
        available = false,
    ),
    GameEntry(
        id = "vier",
        emoji = "🐻",
        title = "Vier4Bear",
        description = "Vier in einer Reihe mit Beach-Twist.",
        accentColor = SandGold,
        available = false,
    ),
)

@Composable
fun HomeScreen(
    onNavigateToBingoLobby: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(SurfaceDark, Surface2Dark))
                )
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Surface2Dark,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentUser?.avatarUrl ?: "🏖️",
                            fontSize = 32.sp
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WILLKOMMEN ZURÜCK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = currentUser?.displayName ?: "…",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                    )
                }

                // Profile button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Surface2Dark,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToProfile() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = TextSub,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Headline
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
            Text(
                text = "BeachBande",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Spiel auswählen",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
        }

        // Game cards
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GAMES.forEach { game ->
                GameCard(
                    game = game,
                    onClick = {
                        if (game.available) {
                            when (game.id) {
                                "bingo" -> onNavigateToBingoLobby()
                            }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GameCard(game: GameEntry, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (game.available) 1f else 0.55f)
            .border(
                width = 1.5.dp,
                color = if (game.available) game.accentColor.copy(alpha = 0.35f) else BorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .then(if (game.available) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (game.available) game.accentColor.copy(alpha = 0.15f) else Surface2Dark,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = game.emoji, fontSize = 32.sp)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = game.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    if (!game.available) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Surface2Dark,
                        ) {
                            Text(
                                text = "BALD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = game.description,
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp,
                )
            }

            if (game.available) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
