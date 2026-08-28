package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val KlontauschAccent = Color(0xFF8B5CF6)

private data class KlonResult(
    val teamName: String,
    val winnerId: String,
    val winnerName: String,
    val winnerAvatar: String,
    val players: List<Triple<String, String, String>>,  // userId, displayName, avatarUrl
    val mode: String,
    val difficulty: String,
    val createdAt: Long = 0L,
)

private data class KlonPlayerStat(val wins: Int, val played: Int)

private data class KlonTeam(
    val key: String,
    val teamName: String,
    val playerStats: Map<String, KlonPlayerStat>,
    val players: List<Triple<String, String, String>>,
    val games: List<KlonResult>,
)

private fun buildTeams(games: List<KlonResult>): List<KlonTeam> {
    val map = mutableMapOf<String, MutableList<KlonResult>>()
    for (g in games) {
        val key = g.players.map { it.first }.sorted().joinToString("|")
        map.getOrPut(key) { mutableListOf() }.add(g)
    }
    return map.entries.map { (key, gs) ->
        val wins   = mutableMapOf<String, Int>()
        val played = mutableMapOf<String, Int>()
        for (g in gs) {
            for ((pId, _, _) in g.players) {
                played[pId] = (played[pId] ?: 0) + 1
                if (pId == g.winnerId) wins[pId] = (wins[pId] ?: 0) + 1
            }
        }
        val stats = gs.first().players.associate { (pId, _, _) ->
            pId to KlonPlayerStat(wins[pId] ?: 0, played[pId] ?: 0)
        }
        KlonTeam(
            key = key,
            teamName = gs.first().teamName,
            playerStats = stats,
            players = gs.first().players,
            games = gs.sortedByDescending { it.createdAt },
        )
    }.sortedByDescending { it.games.first().createdAt }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlontauschResultsScreen(
    onNavigateBack: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var teams by remember { mutableStateOf<List<KlonTeam>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    DisposableEffect(uid) {
        if (uid == null) { loading = false; return@DisposableEffect onDispose {} }
        val listener = db.collection("klontauschResults")
            .whereArrayContains("playerIds", uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val results = snap.documents.mapNotNull { doc ->
                        val winnerId     = doc.getString("winnerId") ?: return@mapNotNull null
                        val winnerName   = doc.getString("winnerName") ?: ""
                        val winnerAvatar = doc.getString("winnerAvatar") ?: "🃏"
                        val team         = doc.getString("teamName") ?: ""
                        val mode         = doc.getString("mode") ?: ""
                        val difficulty   = doc.getString("difficulty") ?: ""
                        val createdAt    = doc.getLong("createdAt") ?: 0L
                        @Suppress("UNCHECKED_CAST")
                        val rawPlayers = (doc.get("players") as? List<*>) ?: emptyList<Any>()
                        val players = rawPlayers.mapNotNull { raw ->
                            @Suppress("UNCHECKED_CAST")
                            val pm = raw as? Map<String, Any> ?: return@mapNotNull null
                            Triple(
                                pm["userId"] as? String ?: "",
                                pm["displayName"] as? String ?: "",
                                pm["avatarUrl"] as? String ?: "🃏",
                            )
                        }
                        KlonResult(team, winnerId, winnerName, winnerAvatar, players, mode, difficulty, createdAt)
                    }.sortedByDescending { it.createdAt }
                    teams = buildTeams(results)
                }
                loading = false
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Klontausch – Ergebnisse", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KlontauschAccent)
            }
            return@Scaffold
        }

        if (teams.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🃏", fontSize = EmojiLarge)
                    Text("Noch keine Ergebnisse", color = TextSub,
                        style = MaterialTheme.typography.titleMedium)
                    Text("Spiele eine Runde, um dein erstes Ergebnis zu sehen.",
                        color = TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            teams.forEach { team ->
                TeamCard(team = team, uid = uid ?: "")
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun TeamCard(team: KlonTeam, uid: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    team.teamName,
                    color = KlontauschAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${team.games.size} Spiel${if (team.games.size != 1) "e" else ""}",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            HorizontalDivider(color = BorderColor)

            team.players.forEach { (pId, name, avatar) ->
                val stat = team.playerStats[pId] ?: KlonPlayerStat(0, 0)
                val isMe = pId == uid
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isMe) KlontauschAccent.copy(alpha = 0.08f) else Color.Transparent,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(avatar, style = MaterialTheme.typography.titleSmall)
                    Text(
                        name + if (isMe) " (Du)" else "",
                        color = if (isMe) TextPrimary else TextSub,
                        fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${stat.wins} / ${stat.played} Siege",
                        color = if (stat.wins > 0) SandGold else TextMuted,
                        fontWeight = if (stat.wins > 0) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (team.games.isNotEmpty()) {
                HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
                Text("Letzte Spiele", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                team.games.take(3).forEach { g ->
                    val dateStr = SimpleDateFormat("dd.MM.yy", Locale.GERMAN).format(Date(g.createdAt))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("🏆", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${g.winnerAvatar} ${g.winnerName}",
                                color = TextSub,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Text(dateStr, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
