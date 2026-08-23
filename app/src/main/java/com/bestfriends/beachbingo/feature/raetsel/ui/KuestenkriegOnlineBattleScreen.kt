package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bestfriends.beachbingo.feature.raetsel.BATTLE_GRID
import com.bestfriends.beachbingo.feature.raetsel.PlacedShip
import com.bestfriends.beachbingo.feature.raetsel.shipCells
import com.bestfriends.beachbingo.ui.components.GameHudBar
import com.bestfriends.beachbingo.ui.components.QuitConfirmDialog
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


private enum class OnlineCellView { UNKNOWN, MISS, HIT, SUNK, MYSHIP }

private fun parseOnlineShip(map: Map<*, *>): PlacedShip? {
    val id   = (map["id"]   as? Long)?.toInt() ?: return null
    val size = (map["size"] as? Long)?.toInt() ?: return null
    val row  = (map["row"]  as? Long)?.toInt() ?: return null
    val col  = (map["col"]  as? Long)?.toInt() ?: return null
    val horiz = map["horiz"] as? Boolean ?: true
    return PlacedShip(id, size, row, col, horiz)
}

private val KK_CONSTELLATION_NAMES = listOf(
    "Korallenflotte|Sandburgbataillon", "SprottenGirls|DorschBabys",
    "PalmenBoys|SchlauchbootMatrosen", "Wattjäger|Muschelsammler",
    "Möwenpiraten|Krakenflüsterer", "Brandungsreiter|Sandkastenkapitäne",
    "Tintenfischbande|Strandwächter", "Nordseeadler|Wattwurmbrigade",
    "Barrakuda-Crew|Seepferdchen-Staffel", "Wellenreiter|Sanddünenkommando",
    "Heringsjäger|Austernretter", "Salzwasserwölfe|Bademeister-Union",
    "Kormorantruppe|Strandkorbverteidiger", "Anker-Asse|Flaggen-Flatterer",
    "Neptunsgarde|Strandräuber-Koalition", "Krabbenklau-Clan|Muschelpiraten",
    "Tiefseebande|Flachlandmatrosen", "Sturmflut-Staffel|Sandburg-Söldner",
    "Möwenkönige|Plastikenten-Piraten", "Blauwal-Brigade|Minigolf-Miliz",
    "Sardellen-Syndrom|Lachs-Legion", "Schaumkronen-Crew|Treibholz-Truppe",
    "Quallen-Quartier|Sonnencrème-Söldner", "Brandungs-Barbaren|Wellenbrecher",
    "Ebbe-Allianz|Flut-Front",
)

private fun constellationTitle(uid1: String, uid2: String): String {
    val sorted = listOf(uid1, uid2).sorted()
    val key = sorted.joinToString("|")
    var hash = 0L
    for (c in key) hash = ((hash * 31L) + c.code.toLong()) and Long.MAX_VALUE
    val idx = (hash % 25L).toInt()
    val pair = KK_CONSTELLATION_NAMES[idx].split("|")
    return if ((hash / 25L) % 2L == 0L) "${pair[0]} vs. ${pair[1]}" else "${pair[1]} vs. ${pair[0]}"
}

private fun isOnlineShipSunk(ship: PlacedShip, shots: List<String>): Boolean =
    shipCells(ship).all { (r, c) ->
        val v = shots.getOrNull(r * BATTLE_GRID + c) ?: "unknown"
        v == "hit" || v == "sunk"
    }

private fun computeOnlineShot(
    flatShots: List<String>,
    r: Int, c: Int,
    opponentFleet: List<PlacedShip>,
): Pair<List<String>, Boolean> {
    val newShots = flatShots.toMutableList()
    val hitShip = opponentFleet.find { ship -> shipCells(ship).any { (sr, sc) -> sr == r && sc == c } }
    if (hitShip != null) {
        newShots[r * BATTLE_GRID + c] = "hit"
        val sunkNow = shipCells(hitShip).all { (sr, sc) ->
            val i = sr * BATTLE_GRID + sc
            newShots[i] == "hit" || newShots[i] == "sunk"
        }
        if (sunkNow) {
            shipCells(hitShip).forEach { (sr, sc) -> newShots[sr * BATTLE_GRID + sc] = "sunk" }
        }
    } else {
        newShots[r * BATTLE_GRID + c] = "miss"
    }
    val won = opponentFleet.all { ship -> shipCells(ship).all { (sr, sc) -> newShots[sr * BATTLE_GRID + sc] == "sunk" } }
    return newShots to won
}

@Composable
fun KuestenkriegOnlineBattleScreen(
    gameCode: String,
    onNavigateBack: () -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("RUNNING") }
    var turn by remember { mutableStateOf("") }
    var winnerUid by remember { mutableStateOf<String?>(null) }
    var playerIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var myFleet by remember { mutableStateOf<List<PlacedShip>>(emptyList()) }
    var oppFleet by remember { mutableStateOf<List<PlacedShip>>(emptyList()) }
    var myShots by remember { mutableStateOf(List(BATTLE_GRID * BATTLE_GRID) { "unknown" }) }
    var oppShots by remember { mutableStateOf(List(BATTLE_GRID * BATTLE_GRID) { "unknown" }) }
    var myName by remember { mutableStateOf("") }
    var myAvatar by remember { mutableStateOf("") }
    var oppName by remember { mutableStateOf("Gegner") }
    var oppAvatar by remember { mutableStateOf("") }
    var shooting by remember { mutableStateOf(false) }
    var lastMsg by remember { mutableStateOf<String?>(null) }
    var showQuit by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }

    val oppId = playerIds.find { it != uid } ?: ""

    LaunchedEffect(gameCode) {
        if (gameCode.isBlank()) return@LaunchedEffect
        db.collection("kuestenkriegGames").document(gameCode)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                status = snap.getString("status") ?: "RUNNING"
                turn = snap.getString("turn") ?: ""
                winnerUid = snap.getString("winner")
                @Suppress("UNCHECKED_CAST")
                val pIds = (snap.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                playerIds = pIds
                val oId = pIds.find { it != uid } ?: ""
                @Suppress("UNCHECKED_CAST")
                val playersMap = snap.get("players") as? Map<*, *> ?: emptyMap<Any, Any>()

                fun parseFleet(id: String): List<PlacedShip> {
                    val pData = playersMap[id] as? Map<*, *> ?: return emptyList()
                    return (pData["fleet"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let { m -> parseOnlineShip(m) } } ?: emptyList()
                }

                myFleet = parseFleet(uid)
                oppFleet = parseFleet(oId)
                oppName = ((playersMap[oId] as? Map<*, *>)?.get("displayName") as? String) ?: "Gegner"
                myName = ((playersMap[uid] as? Map<*, *>)?.get("displayName") as? String) ?: "Du"
                myAvatar = ((playersMap[uid] as? Map<*, *>)?.get("avatarUrl") as? String) ?: "👤"
                oppAvatar = ((playersMap[oId] as? Map<*, *>)?.get("avatarUrl") as? String) ?: "👤"
                @Suppress("UNCHECKED_CAST")
                val shotsMap = snap.get("shots") as? Map<*, *> ?: emptyMap<Any, Any>()
                myShots = (shotsMap[uid] as? List<*>)?.mapNotNull { it as? String } ?: List(BATTLE_GRID * BATTLE_GRID) { "unknown" }
                oppShots = (shotsMap[oId] as? List<*>)?.mapNotNull { it as? String } ?: List(BATTLE_GRID * BATTLE_GRID) { "unknown" }
            }
    }

    LaunchedEffect(lastMsg) {
        if (lastMsg != null) { delay(2500); lastMsg = null }
    }

    val isMyTurn = turn == uid && status == "RUNNING"
    val isOver = status == "FINISHED"
    val iWon = winnerUid == uid
    val myRemainingCells = myFleet.filter { !isOnlineShipSunk(it, oppShots) }.sumOf { it.size }
    val oppRemainingCells = oppFleet.filter { !isOnlineShipSunk(it, myShots) }.sumOf { it.size }

    fun handleShoot(r: Int, c: Int) {
        if (!isMyTurn || shooting || isOver) return
        if (myShots.getOrNull(r * BATTLE_GRID + c) != "unknown") return
        shooting = true
        val (newShots, isWinner) = computeOnlineShot(myShots, r, c, oppFleet)
        val cellVal = newShots[r * BATTLE_GRID + c]
        val colLabel = ('A' + c).toString()
        val hitShip = oppFleet.find { ship -> shipCells(ship).any { (sr, sc) -> sr == r && sc == c } }
        val sunk = cellVal == "sunk" || (cellVal == "hit" && hitShip != null && isOnlineShipSunk(hitShip, newShots))
        lastMsg = when { sunk -> "$colLabel${r + 1} — Versenkt! 💥"; cellVal == "hit" -> "$colLabel${r + 1} — Treffer! 🎯"; else -> "$colLabel${r + 1} — Wasser!" }
        scope.launch {
            try {
                val isHit = cellVal != "miss"
                val updates = mutableMapOf<String, Any>("shots.$uid" to newShots, "turn" to if (isWinner) uid else if (isHit) uid else oppId)
                if (isWinner) { updates["winner"] = uid; updates["status"] = "FINISHED" }
                db.collection("kuestenkriegGames").document(gameCode).update(updates).await()
                if (isWinner) {
                    db.collection("kuestenkriegResults").add(mapOf(
                        "gameCode" to gameCode,
                        "winnerId" to uid,
                        "loserId" to oppId,
                        "playerIds" to listOf(uid, oppId),
                        "playerNames" to listOf(myName, oppName),
                        "playerAvatars" to listOf(myAvatar, oppAvatar),
                        "shotsFiredByWinner" to newShots.count { it != "unknown" },
                        "shotsFiredByLoser" to oppShots.count { it != "unknown" },
                        "constellationTitle" to constellationTitle(uid, oppId),
                        "mode" to "online",
                        "createdAt" to System.currentTimeMillis(),
                    )).await()
                }
            } catch (_: Exception) {}
            shooting = false
        }
    }

    // Build enemy grid (my shots — don't show opponent's ships)
    val enemyGrid = Array(BATTLE_GRID) { r ->
        Array(BATTLE_GRID) { c ->
            when (myShots.getOrNull(r * BATTLE_GRID + c) ?: "unknown") {
                "hit" -> OnlineCellView.HIT; "sunk" -> OnlineCellView.SUNK; "miss" -> OnlineCellView.MISS
                else -> OnlineCellView.UNKNOWN
            }
        }
    }

    // Build my grid (opponent's shots on my fleet — show my ships)
    val myGrid = Array(BATTLE_GRID) { r ->
        Array(BATTLE_GRID) { c ->
            when (val v = oppShots.getOrNull(r * BATTLE_GRID + c) ?: "unknown") {
                "hit" -> OnlineCellView.HIT; "sunk" -> OnlineCellView.SUNK; "miss" -> OnlineCellView.MISS
                else -> {
                    val hasShip = myFleet.any { ship ->
                        shipCells(ship).any { (sr, sc) -> sr == r && sc == c } && !isOnlineShipSunk(ship, oppShots)
                    }
                    if (hasShip) OnlineCellView.MYSHIP else OnlineCellView.UNKNOWN
                }
            }
        }
    }

    val headerText = when {
        isOver -> if (iWon) "🏆 Sieg!" else "💀 Niederlage!"
        shooting -> "Schuss…"
        isMyTurn -> "Dein Schuss 🎯"
        else -> "$oppName schießt…"
    }

    BackHandler {
        if (paused) paused = false else showQuit = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(BgDark).navigationBarsPadding()) {
        val cellDp = ((maxWidth - 46.dp) / BATTLE_GRID).coerceIn(18.dp, 52.dp)
    Column(modifier = Modifier.fillMaxSize()) {
        GameHudBar(
            paused = paused,
            onPauseToggle = { paused = !paused },
            onQuit = { showQuit = true },
            onShowRules = { showRules = true },
        ) {
            Text("⚓", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("ONLINE", fontSize = StatusTiny, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                Text(headerText, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Message banner
            lastMsg?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp), color = RoseRed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().border(1.dp, RoseRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                ) { Text(msg, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = RoseRed, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp)) }
            }
            if (!isMyTurn && !isOver && lastMsg == null) {
                Surface(
                    shape = RoundedCornerShape(10.dp), color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                ) { Text("$oppName ist am Zug…", style = MaterialTheme.typography.labelMedium, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp)) }
            }

            // Enemy grid — player shoots here
            Text(
                text = "${oppName}s Gewässer${if (isMyTurn && !isOver) " ← Tippen!" else ""}",
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = if (isMyTurn && !isOver) RoseRed else TextMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), letterSpacing = 1.sp,
            )
            OnlineKriegGrid(grid = enemyGrid, onCell = ::handleShoot, clickable = isMyTurn && !isOver && !shooting, cellDp = cellDp)

            // My grid
            Text("Dein Gewässer", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), letterSpacing = 1.sp)
            OnlineKriegGrid(grid = myGrid, onCell = { _, _ -> }, clickable = false, cellDp = cellDp)

            // Game over card
            if (isOver) {
                Surface(
                    shape = RoundedCornerShape(16.dp), color = SurfaceDark,
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(if (iWon) "🏆" else "💀", fontSize = EmojiLarge)
                        Text(
                            if (iWon) "Du hast gewonnen!" else "$oppName hat gewonnen!",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                        )
                        Text(
                            if (iWon) "Alle feindlichen Schiffe versenkt!" else "Deine Flotte wurde vernichtet!",
                            fontSize = ChipLabel, color = TextMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Zurück zur Lobby", fontSize = CellNumber, fontWeight = FontWeight.ExtraBold, color = BgDark) }
                    }
                }
            }
        }

        if (paused && !isOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Pause, null, tint = TextPrimary, modifier = Modifier.size(48.dp))
                    Text(
                        "Spiel pausiert",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        "Das Online-Spiel läuft weiter.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                    OutlinedButton(onClick = { paused = false }) { Text("Weiterspielen", color = TextSub) }
                }
            }
        }
    }
    } // BoxWithConstraints

    if (showQuit) {
        QuitConfirmDialog(
            emoji = "⚓",
            message = "Das Online-Spiel wird abgebrochen.",
            onConfirm = onNavigateBack,
            onDismiss = { showQuit = false },
        )
    }

    if (showRules) {
        KuestenkriegRulesDialog(onDismiss = { showRules = false })
    }
}

@Composable
private fun OnlineKriegGrid(
    grid: Array<Array<OnlineCellView>>,
    onCell: (Int, Int) -> Unit,
    clickable: Boolean,
    cellDp: Dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 22.dp)) {
            repeat(BATTLE_GRID) { c ->
                Box(modifier = Modifier.size(cellDp), contentAlignment = Alignment.Center) {
                    Text(('A' + c).toString(), fontSize = StatusTiny, color = TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
        repeat(BATTLE_GRID) { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.CenterEnd) {
                    Text("${r + 1}", fontSize = StatusTiny, color = TextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 2.dp))
                }
                repeat(BATTLE_GRID) { c ->
                    val v = grid[r][c]
                    val bg = when (v) {
                        OnlineCellView.MISS   -> BorderColor
                        OnlineCellView.HIT    -> HitCell
                        OnlineCellView.SUNK   -> SunkCell
                        OnlineCellView.MYSHIP -> MyShipCell
                        OnlineCellView.UNKNOWN -> SurfaceDark
                    }
                    val label = when (v) { OnlineCellView.MISS -> "•"; OnlineCellView.HIT -> "●"; OnlineCellView.SUNK -> "✕"; else -> "" }
                    Box(
                        modifier = Modifier
                            .size(cellDp)
                            .background(bg)
                            .border(0.5.dp, BorderColor)
                            .let { if (clickable && v == OnlineCellView.UNKNOWN) it.clickable { onCell(r, c) } else it },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (label.isNotEmpty()) {
                            Text(label, fontSize = ChipLabelTiny, fontWeight = FontWeight.ExtraBold,
                                color = if (v == OnlineCellView.MISS) TextMuted else Color.White)
                        }
                    }
                }
            }
        }
    }
}
