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
import com.bestfriends.beachbingo.feature.shared.rankEmoji
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KlontauschResultsScreen(
    onNavigateBack: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    var results by remember { mutableStateOf<List<KlonResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    DisposableEffect(uid) {
        if (uid == null) { loading = false; return@DisposableEffect onDispose {} }
        val listener = db.collection("klontauschResults")
            .whereArrayContains("playerIds", uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    results = snap.documents.mapNotNull { doc ->
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
                        val sorted = players.sortedByDescending { it.first == winnerId }
                        KlonResult(team, winnerId, winnerName, winnerAvatar, sorted, mode, difficulty, createdAt)
                    }.sortedByDescending { it.createdAt }
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

        if (results.isEmpty()) {
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

            results.forEachIndexed { idx, result ->
                val isFirst = idx == 0
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFirst) KlontauschAccent.copy(0.08f) else SurfaceDark,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(
                        1.dp,
                        if (isFirst) KlontauschAccent.copy(0.5f) else BorderColor,
                        RoundedCornerShape(16.dp),
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Team name + mode
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                result.teamName,
                                color = if (isFirst) KlontauschAccent else TextSub,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                when (result.mode) {
                                    "AI" -> "🤖 KI"
                                    "ONLINE" -> "📱 Online"
                                    else -> result.mode
                                } + when (result.difficulty) {
                                    "ROOKIE" -> " · 🐣"
                                    "SNIPER" -> " · 🎯"
                                    "BOSS_LEVEL" -> " · 💀"
                                    else -> ""
                                },
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        // Winner
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🏆", style = MaterialTheme.typography.titleLarge)
                            Text(result.winnerAvatar, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${result.winnerName} hat gewonnen!",
                                color = if (isFirst) KlontauschAccent else SandGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        HorizontalDivider(color = BorderColor)

                        // All players
                        result.players.forEachIndexed { rank, (pId, name, avatar) ->
                            val isWinner = pId == result.winnerId
                            val total = result.players.size
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(rankEmoji(rank, rank == total - 1, total),
                                    color = if (isWinner) SandGold else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text(avatar, style = MaterialTheme.typography.titleSmall)
                                Text(name + if (pId == uid) " (Du)" else "",
                                    color = if (isWinner) TextPrimary else TextSub,
                                    fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
