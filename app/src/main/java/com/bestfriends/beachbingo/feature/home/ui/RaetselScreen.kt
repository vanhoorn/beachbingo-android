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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.core.model.RIDDLE_GAMES
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub

@Composable
fun RaetselScreen(
    onNavigateBack: () -> Unit,
) {
    val games = RIDDLE_GAMES.sortedBy { it.title }
    var rulesGameId by remember { mutableStateOf<String?>(null) }
    val activeRule = rulesGameId?.let { ALL_GAME_RULES[it] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Surface2Dark,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = TextSub,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(text = "🧩", fontSize = 32.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "KATEGORIE",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = "Rätsel",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                    )
                }
            }
        }

        // ── Intro-Banner ──────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "🧠 Logik-Rätsel für Solo-Spieler — von Sudoku bis Schiffe Versenken. " +
                        "Tippe auf ℹ für die Anleitung. Die Spiele werden in den nächsten Updates freigeschaltet.",
                fontSize = 13.sp, color = TextMuted, lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp),
            )
        }

        // ── Spiele-Liste ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            games.forEach { game ->
                val accentColor = Color(game.color)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = accentColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji icon
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = game.emoji, fontSize = 30.sp)
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
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                                )
                                // "Bald verfügbar" badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.15f),
                                    modifier = Modifier.border(
                                        1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp)
                                    )
                                ) {
                                    Text(
                                        text = "BALD",
                                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                        color = accentColor, letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = game.description,
                                fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Info button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable { rulesGameId = game.id }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Anleitung",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (activeRule != null) {
        GameRulesBottomSheet(
            rule = activeRule,
            onDismiss = { rulesGameId = null },
        )
    }
}
