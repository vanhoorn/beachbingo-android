package com.bestfriends.beachbingo.feature.vier.ui

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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.bestfriends.beachbingo.core.model.VierGame
import com.bestfriends.beachbingo.core.model.VierPlayer
import com.bestfriends.beachbingo.core.model.VierStatus
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Coral
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

private data class VierPlayerStat(
    val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val drinkId: String,
    val wins: Int,
    val draws: Int,
    val played: Int,
)

private data class VierTeam(
    val key: String,
    val name: String,
    val isSolo: Boolean,
    val playerStats: List<VierPlayerStat>,
    val games: List<VierGame>,
)

private fun buildTeams(games: List<VierGame>): List<VierTeam> {
    val map = mutableMapOf<String, Pair<MutableMap<String, VierPlayerStat>, MutableList<VierGame>>>()
    for (g in games) {
        val key = g.playerIds.sorted().joinToString("|")
        val entry = map.getOrPut(key) { Pair(mutableMapOf(), mutableListOf()) }
        entry.second.add(g)
        for (p in g.players) {
            val existing = entry.first[p.userId]
            val isWin = p.userId == g.winnerId
            val isDraw = g.isDraw
            entry.first[p.userId] = existing?.copy(
                wins = existing.wins + if (isWin) 1 else 0,
                draws = existing.draws + if (isDraw) 1 else 0,
                played = existing.played + 1,
            ) ?: VierPlayerStat(
                userId = p.userId,
                displayName = p.displayName,
                avatarUrl = p.avatarUrl,
                drinkId = p.drinkId,
                wins = if (isWin) 1 else 0,
                draws = if (isDraw) 1 else 0,
                played = 1,
            )
        }
    }
    return map.entries.map { (key, pair) ->
        val stats = pair.first.values.sortedByDescending { it.wins * 2 + it.draws }
        VierTeam(
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
fun VierResultsScreen(onNavigateBack: () -> Unit) {
    val uid = Firebase.auth.currentUser?.uid ?: ""
    var teams by remember { mutableStateOf<List<VierTeam>>(emptyList()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        Firebase.firestore.collection("vierGames")
            .whereArrayContains("playerIds", uid)
            .whereEqualTo("status", VierStatus.FINISHED.name)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val games = snap.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val rawPlayers = doc.get("players") as? List<Map<String, Any>> ?: emptyList()
                    val players = rawPlayers.map { p ->
                        VierPlayer(
                            userId = p["userId"] as? String ?: "",
                            displayName = p["displayName"] as? String ?: "",
                            avatarUrl = p["avatarUrl"] as? String ?: "",
                            drinkId = p["drinkId"] as? String ?: "lager",
                        )
                    }
                    val playerIds = (doc.get("playerIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    VierGame(
                        gameId = doc.id,
                        adminId = doc.getString("adminId") ?: "",
                        humanCount = (doc.getLong("humanCount") ?: 2L).toInt(),
                        players = players,
                        playerIds = playerIds,
                        winnerId = doc.getString("winnerId"),
                        isDraw = doc.getBoolean("isDraw") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                    )
                }.sortedByDescending { it.createdAt }
                teams = buildTeams(games)
            }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Vier4Bier Ergebnisse 🏆", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = Coral)
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
                Text("🍺", fontSize = 64.sp)
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
                    VierTeamCard(team = team, currentUid = uid, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun VierTeamCard(team: VierTeam, currentUid: String, dateFormat: SimpleDateFormat) {
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
            Text(
                text = if (team.isSolo) "🤖 Solo vs KI" else "🍺 ${team.name}",
                style = MaterialTheme.typography.titleSmall,
                color = Coral,
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
                    DrinkPiece(drinkId = p.drinkId, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = p.displayName + if (isMe) " 👤" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isMe) FontWeight.Bold else if (isFirst) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isFirst) Coral else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${p.wins} Siege",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isFirst) Coral else SandGold
                        )
                        Text(
                            text = "${p.played} Sp. · $winPct%" + if (p.draws > 0) " · ${p.draws}U" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (lastGame != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Surface2Dark)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            lastGame.isDraw -> "🤝"
                            lastWinner != null -> ""
                            else -> "🍺"
                        },
                        fontSize = 18.sp
                    )
                    if (lastWinner != null) {
                        DrinkPiece(drinkId = lastWinner.drinkId, size = 20.dp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            lastGame.isDraw -> "Letztes Spiel: Unentschieden"
                            lastWinner != null -> "Letztes Spiel: ${lastWinner.displayName} hat gewonnen"
                            else -> "Letztes Spiel beendet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
