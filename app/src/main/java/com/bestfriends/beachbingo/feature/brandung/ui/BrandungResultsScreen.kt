package com.bestfriends.beachbingo.feature.brandung.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandungTeal = Color(0xFF0D9488)

private data class BrandungResultEntry(
    val id: String,
    val winnerId: String,
    val winnerName: String,
    val winnerAvatar: String,
    val players: List<Triple<String, String, String>>, // userId, displayName, avatarUrl
    val rounds: Int,
    val mode: String,
    val difficulty: String,
    val createdAt: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandungResultsScreen(onNavigateBack: () -> Unit) {
    val uid = Firebase.auth.currentUser?.uid ?: ""
    val db = Firebase.firestore
    var results by remember { mutableStateOf<List<BrandungResultEntry>>(emptyList()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN) }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        db.collection("brandungResults")
            .whereArrayContains("playerIds", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                results = snap.documents.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val rawPlayers = doc.get("players") as? List<Map<String, Any>> ?: emptyList()
                    val players = rawPlayers.map { p ->
                        Triple(
                            p["userId"] as? String ?: "",
                            p["displayName"] as? String ?: "",
                            p["avatarUrl"] as? String ?: "🏄",
                        )
                    }
                    val winnerId = doc.getString("winnerId") ?: return@mapNotNull null
                    val winner = players.find { it.first == winnerId }
                    BrandungResultEntry(
                        id = doc.id,
                        winnerId = winnerId,
                        winnerName = winner?.second ?: "Unbekannt",
                        winnerAvatar = winner?.third ?: "🌊",
                        players = players,
                        rounds = (doc.getLong("rounds") ?: 1L).toInt(),
                        mode = doc.getString("mode") ?: "ai",
                        difficulty = doc.getString("difficulty") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                    )
                }.sortedByDescending { it.createdAt }
            }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Brandung Ergebnisse 🌊", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück", tint = BrandungTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        if (results.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🌊", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("Noch keine Ergebnisse", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                Text("Beende ein Spiel, um es hier zu sehen.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { result ->
                    BrandungResultCard(result = result, currentUid = uid, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun BrandungResultCard(
    result: BrandungResultEntry,
    currentUid: String,
    dateFormat: SimpleDateFormat,
) {
    val isMyWin = result.winnerId == currentUid
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (result.mode == "ai") "🤖 Gegen KI" else "📱 Online",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandungTeal,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = dateFormat.format(Date(result.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${result.rounds} Runde${if (result.rounds == 1) "" else "n"}" +
                    if (result.difficulty.isNotBlank() && result.mode == "ai") " · ${result.difficulty}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Surface2Dark)
            Spacer(Modifier.height(10.dp))

            // Winner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🏆", fontSize = 20.sp)
                Text(
                    text = "${result.winnerAvatar} ${result.winnerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandGold,
                    fontWeight = FontWeight.Bold,
                )
                if (isMyWin) {
                    Text(
                        "Du!",
                        style = MaterialTheme.typography.labelSmall,
                        color = SandGold,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Other players
            val others = result.players.filter { it.first != result.winnerId }
            if (others.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                others.forEach { (playerId, name, avatar) ->
                    val isMe = playerId == currentUid
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Text("$avatar $name" + if (isMe) " 👤" else "", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
    }
}
