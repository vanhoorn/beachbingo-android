package com.bestfriends.beachbingo.feature.pong.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.core.model.PongGame
import com.bestfriends.beachbingo.core.model.PongStatus
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
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

private data class PlayerStat(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val wins: Int,
    val played: Int,
)

private data class PongTeam(
    val key: String,
    val name: String,
    val isSolo: Boolean,
    val playerStats: List<PlayerStat>,
    val games: List<PongGame>,
)

private fun buildTeams(games: List<PongGame>): List<PongTeam> {
    val map = mutableMapOf<String, Pair<MutableMap<String, PlayerStat>, MutableList<PongGame>>>()
    for (g in games) {
        val key = g.playerIds.sorted().joinToString("|")
        val entry = map.getOrPut(key) { Pair(mutableMapOf(), mutableListOf()) }
        entry.second.add(g)
        for (p in g.players) {
            val existing = entry.first[p.userId]
            val wins = if (p.userId == g.winnerId) 1 else 0
            entry.first[p.userId] = existing?.copy(wins = existing.wins + wins, played = existing.played + 1)
                ?: PlayerStat(p.userId, p.displayName, p.avatarUrl, wins, 1)
        }
    }
    return map.entries.map { (key, pair) ->
        val stats = pair.first.values.sortedByDescending { it.wins }
        PongTeam(
            key = key,
            name = teamName(key),
            isSolo = pair.first.size == 1,
            playerStats = stats,
            games = pair.second.sortedByDescending { it.createdAt },
        )
    }.sortedByDescending { it.games.size }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongResultsScreen(onNavigateBack: () -> Unit) {
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var teams by remember { mutableStateOf<List<PongTeam>>(emptyList()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        Firebase.firestore.collection("pongGames")
            .whereArrayContains("playerIds", uid)
            .whereEqualTo("status", "FINISHED")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val games = snap.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    PongGame(
                        gameId = doc.id,
                        adminId = data["adminId"] as? String ?: "",
                        status = PongStatus.FINISHED,
                        totalPaddles = (data["totalPaddles"] as? Long)?.toInt() ?: 2,
                        humanCount = (data["humanCount"] as? Long)?.toInt() ?: 1,
                        players = (data["players"] as? List<*>)?.mapNotNull { p ->
                            (p as? Map<*, *>)?.let { m ->
                                com.bestfriends.beachbingo.core.model.PongPlayer(
                                    userId = m["userId"] as? String ?: "",
                                    displayName = m["displayName"] as? String ?: "",
                                    avatarUrl = m["avatarUrl"] as? String ?: "",
                                    side = m["side"] as? String ?: "left",
                                )
                            }
                        } ?: emptyList(),
                        playerIds = (data["playerIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        scoreLeft = (data["scoreLeft"] as? Long)?.toInt() ?: 0,
                        scoreRight = (data["scoreRight"] as? Long)?.toInt() ?: 0,
                        scoreTop = (data["scoreTop"] as? Long)?.toInt() ?: 0,
                        scoreBottom = (data["scoreBottom"] as? Long)?.toInt() ?: 0,
                        scoreLimit = (data["scoreLimit"] as? Long)?.toInt() ?: 7,
                        winnerId = data["winnerId"] as? String,
                        createdAt = (data["createdAt"] as? Long) ?: 0L,
                    )
                }.sortedByDescending { it.createdAt }
                teams = buildTeams(games)
            }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("BeachVolley Ergebnisse 🏆", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                Text("🏓", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("Noch keine Ergebnisse", style = MaterialTheme.typography.titleMedium, color = TextMuted)
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
                    PongTeamCard(team = team, currentUid = uid, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun PongTeamCard(team: PongTeam, currentUid: String, dateFormat: SimpleDateFormat) {
    val total = team.playerStats.size
    val lastGame = team.games.firstOrNull()
    val lastWinner = lastGame?.players?.find { it.userId == lastGame.winnerId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Team header
            Text(
                text = if (team.isSolo) "🎮 Solo vs KI" else "🏄 ${team.name}",
                style = MaterialTheme.typography.titleSmall,
                color = OceanBlue,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${team.games.size} ${if (team.games.size == 1) "Spiel" else "Spiele"}" +
                    (lastGame?.let { " · Zuletzt: ${dateFormat.format(Date(it.createdAt))}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Surface2Dark)
            Spacer(Modifier.height(10.dp))

            // Leaderboard
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
                        fontSize = 18.sp,
                        modifier = Modifier.width(30.dp)
                    )
                    Text(
                        text = p.avatarUrl.ifEmpty { "🏄" },
                        fontSize = 24.sp
                    )
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

            // Last game info
            if (lastGame != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Surface2Dark)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lastWinner?.avatarUrl?.ifEmpty { "🏓" } ?: "🏓", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lastWinner != null) "Letztes Spiel: ${lastWinner.displayName} hat gewonnen"
                               else "Letztes Spiel beendet",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
