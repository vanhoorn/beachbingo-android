package com.bestfriends.beachbingo.feature.bingo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.GameResult
import com.bestfriends.beachbingo.feature.bingo.viewmodel.LobbyViewModel
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.DrawNumberPhone
import com.bestfriends.beachbingo.ui.theme.EmojiMedium
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TEAM_NAMES = listOf(
    "Die Strandpiraten", "Die Wellenreiter", "Die Muschelsammler",
    "Die Korallenritter", "Die Neptun-Crew", "Die Gezeitenbande",
    "Die Krakenbrüder", "Das Sturmsegel-Kollektiv", "Die Palmenwächter",
    "Die Tintenfisch-Allianz", "Die Goldflossen-Gang", "Die Meeresgötter",
    "Die Sandsturm-Fraktion", "Die Delphin-Division", "Die Seestern-Society",
    "Die Brandungshelden", "Die Lagune-Legenden", "Die Tiefseepiraten",
    "Die Mondgezeitengang", "Die Schatzinsel-Bande", "Die Seemanns-Gilde",
    "Die Haiflosse-Fraktion", "Die Perlensucher", "Die Riffwächter",
    "Die Salzwasser-Söldner",
)

private fun teamName(key: String): String {
    var hash = 0
    for (c in key) hash = (hash * 31 + c.code) and 0x7fffffff
    return TEAM_NAMES[hash % TEAM_NAMES.size]
}

private fun rankEmoji(rank: Int, isLast: Boolean, total: Int): String = when {
    rank == 0 -> "🥇"
    rank == 1 && total > 2 -> "🥈"
    rank == 2 && total > 3 -> "🥉"
    isLast && total > 2 -> "🦀"
    else -> "${rank + 1}."
}

private data class BingoPlayerStat(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val wins: Int,
    val played: Int,
)

private data class BingoTeam(
    val key: String,
    val name: String,
    val playerStats: List<BingoPlayerStat>,
    val results: List<GameResult>,
)

private fun buildTeams(results: List<GameResult>): List<BingoTeam> {
    val map = mutableMapOf<String, Pair<MutableMap<String, BingoPlayerStat>, MutableList<GameResult>>>()
    for (r in results) {
        val ids = r.playerIds.sorted()
        val key = if (ids.isEmpty()) r.winnerId else ids.joinToString("|")
        val entry = map.getOrPut(key) { Pair(mutableMapOf(), mutableListOf()) }
        entry.second.add(r)

        r.playerIds.forEachIndexed { i, uid ->
            val name = r.playerNames.getOrElse(i) { uid }
            val avatar = r.playerAvatars.getOrElse(i) { "" }
            val existing = entry.first[uid]
            val won = uid == r.winnerId
            entry.first[uid] = existing?.copy(wins = existing.wins + if (won) 1 else 0, played = existing.played + 1)
                ?: BingoPlayerStat(uid, name, avatar, if (won) 1 else 0, 1)
        }
        // Fallback if playerIds missing — use winnerId only
        if (r.playerIds.isEmpty() && r.winnerId.isNotEmpty()) {
            val existing = entry.first[r.winnerId]
            entry.first[r.winnerId] = existing?.copy(wins = existing.wins + 1, played = existing.played + 1)
                ?: BingoPlayerStat(r.winnerId, r.winnerName, r.winnerAvatar, 1, 1)
        }
    }
    return map.entries.map { (key, pair) ->
        val stats = pair.first.values.sortedByDescending { it.wins }
        BingoTeam(
            key = key,
            name = teamName(key),
            playerStats = stats,
            results = pair.second.sortedByDescending { it.finishedAt },
        )
    }.sortedByDescending { it.results.size }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    val results by viewModel.userResults.collectAsStateWithLifecycle()
    val teams = remember(results) { buildTeams(results) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN) }
    val uid = Firebase.auth.currentUser?.uid ?: ""

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("BeachBingo Ergebnisse 🏆", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = OceanBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        if (teams.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🏖️", fontSize = DrawNumberPhone)
                Spacer(Modifier.height(16.dp))
                Text("Noch keine abgeschlossenen Spiele", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Text("Beende ein Spiel, um es hier zu sehen.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(teams, key = { it.key }) { team ->
                    BingoTeamCard(team = team, currentUid = uid, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun BingoTeamCard(team: BingoTeam, currentUid: String, dateFormat: SimpleDateFormat) {
    val total = team.playerStats.size
    val lastResult = team.results.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "🎱 ${team.name}",
                style = MaterialTheme.typography.titleSmall,
                color = OceanBlue,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${team.results.size} ${if (team.results.size == 1) "Spiel" else "Spiele"}" +
                    (lastResult?.let { " · Zuletzt: ${dateFormat.format(Date(it.finishedAt))}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Surface2Dark)
            Spacer(Modifier.height(10.dp))

            team.playerStats.forEachIndexed { rank, p ->
                val isLast = rank == total - 1
                val winPct = if (p.played > 0) (p.wins * 100 / p.played) else 0
                val isMe = p.userId == currentUid
                val isFirst = rank == 0

                if (rank > 0) Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = rankEmoji(rank, isLast, total),
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        modifier = Modifier.width(30.dp)
                    )
                    Text(text = p.avatarUrl.ifEmpty { "🏄" }, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = p.displayName + if (isMe) " 👤" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isMe) FontWeight.Bold else if (isFirst) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isFirst) OceanBlue else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${p.wins} Siege",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isFirst) OceanBlue else SandGold
                        )
                        Text(
                            text = "${p.played} Sp. · $winPct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (lastResult != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Surface2Dark)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lastResult.winnerAvatar.ifEmpty { "🏆" }, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Letztes Spiel: ${lastResult.winnerName} hat gewonnen · ${lastResult.drawnNumbersCount} Zahlen",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
