package com.bestfriends.beachbingo.feature.raetsel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bestfriends.beachbingo.feature.raetsel.*
import com.bestfriends.beachbingo.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

private val KkOnlinePlacementAccent = Color(0xFFFB7185)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KuestenkriegOnlinePlacementScreen(
    gameCode: String,
    onNavigateBack: () -> Unit,
    onNavigateToBattle: (code: String) -> Unit,
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    var fleet by remember { mutableStateOf(listOf<PlacedShip>()) }
    var horiz by remember { mutableStateOf(true) }
    var fleetReady by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var navigated by remember { mutableStateOf(false) }

    val activeIdx = fleet.size
    val allPlaced = fleet.size == FLEET_DEFS.size

    fun buildOccupied(): Array<BooleanArray> {
        val g = Array(BATTLE_GRID) { BooleanArray(BATTLE_GRID) }
        fleet.forEach { placeOnGrid(g, it) }
        return g
    }

    fun handleCellTap(r: Int, c: Int) {
        if (allPlaced || fleetReady) return
        val def = FLEET_DEFS[activeIdx]
        if (canPlaceShip(buildOccupied(), r, c, def.size, horiz)) {
            fleet = fleet + PlacedShip(activeIdx, def.size, r, c, horiz)
        }
    }

    fun removeLastShip() {
        if (fleet.isEmpty() || fleetReady) return
        fleet = fleet.dropLast(1)
    }

    fun randomizeAll() {
        if (fleetReady) return
        val g = Array(BATTLE_GRID) { BooleanArray(BATTLE_GRID) }
        val newFleet = mutableListOf<PlacedShip>()
        FLEET_DEFS.forEachIndexed { id, def ->
            repeat(300) attempt@{
                val h = Random.nextBoolean()
                val r = Random.nextInt(BATTLE_GRID)
                val c = Random.nextInt(BATTLE_GRID)
                if (canPlaceShip(g, r, c, def.size, h)) {
                    val ship = PlacedShip(id, def.size, r, c, h)
                    placeOnGrid(g, ship)
                    newFleet.add(ship)
                    return@attempt
                }
            }
        }
        fleet = newFleet
    }

    fun submitFleet() {
        if (!allPlaced || submitting) return
        submitting = true
        scope.launch {
            try {
                val fleetData = fleet.map { ship ->
                    mapOf("id" to ship.id, "size" to ship.size, "row" to ship.row,
                        "col" to ship.col, "horiz" to ship.horiz, "sunk" to false)
                }
                db.collection("kuestenkriegGames").document(gameCode).update(
                    mapOf("players.$uid.fleet" to fleetData, "players.$uid.fleetReady" to true)
                ).await()
                fleetReady = true
            } catch (_: Exception) {
                submitting = false
            }
        }
    }

    // Firestore listener: detect when both players are ready or game starts
    LaunchedEffect(gameCode) {
        if (gameCode.isBlank()) return@LaunchedEffect
        db.collection("kuestenkriegGames").document(gameCode)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val status = snap.getString("status") ?: "PLACEMENT"

                if (status == "RUNNING" && !navigated) {
                    navigated = true
                    onNavigateToBattle(gameCode)
                    return@addSnapshotListener
                }

                // Idempotent PLACEMENT→RUNNING transition when both fleets are ready
                if (status == "PLACEMENT") {
                    @Suppress("UNCHECKED_CAST")
                    val pIds = (snap.get("playerIds") as? List<*>)?.filterIsInstance<String>() ?: return@addSnapshotListener
                    if (pIds.size < 2) return@addSnapshotListener
                    @Suppress("UNCHECKED_CAST")
                    val playersMap = snap.get("players") as? Map<*, *> ?: return@addSnapshotListener
                    val allReady = pIds.all { id ->
                        (playersMap[id] as? Map<*, *>)?.get("fleetReady") as? Boolean == true
                    }
                    if (allReady) {
                        val admin = snap.getString("adminId") ?: pIds[0]
                        val emptyShots = List(BATTLE_GRID * BATTLE_GRID) { "unknown" }
                        scope.launch {
                            try {
                                val updates = mutableMapOf<String, Any>("status" to "RUNNING", "turn" to admin)
                                pIds.forEach { id -> updates["shots.$id"] = emptyShots }
                                db.collection("kuestenkriegGames").document(gameCode).update(updates).await()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
    }

    val occupied = buildOccupied()
    val currentDef = if (!allPlaced) FLEET_DEFS[activeIdx] else null
    val cellDp = 28.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = Surface2Dark,
                        modifier = Modifier.size(40.dp).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).clickable { onNavigateBack() },
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = TextSub, modifier = Modifier.size(20.dp)) } }
                    Spacer(Modifier.width(14.dp))
                    Text("⚓", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("KÜSTENKRIEG · ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.5.sp)
                        Text("Schiffe setzen", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    }
                    Text("${fleet.size}/${FLEET_DEFS.size}", fontSize = 13.sp, color = TextMuted)
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Active ship hint
                if (currentDef != null && !fleetReady) {
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = SurfaceDark,
                        modifier = Modifier.fillMaxWidth().border(1.dp, KkOnlinePlacementAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(currentDef.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentDef.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Länge ${currentDef.size}", fontSize = 12.sp, color = TextMuted)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp), color = Surface2Dark,
                                modifier = Modifier.border(1.dp, BorderColor, RoundedCornerShape(8.dp)).clickable { horiz = !horiz },
                            ) {
                                Text(
                                    if (horiz) "↔ Horizontal" else "↕ Vertikal",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }

                // Grid
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(start = 22.dp)) {
                        repeat(BATTLE_GRID) { c ->
                            Box(modifier = Modifier.size(cellDp), contentAlignment = Alignment.Center) {
                                Text(('A' + c).toString(), fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    repeat(BATTLE_GRID) { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.CenterEnd) {
                                Text("${r + 1}", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 2.dp))
                            }
                            repeat(BATTLE_GRID) { c ->
                                Box(
                                    modifier = Modifier
                                        .size(cellDp)
                                        .background(if (occupied[r][c]) KkOnlinePlacementAccent.copy(alpha = 0.55f) else SurfaceDark)
                                        .border(0.5.dp, BorderColor)
                                        .let { if (!fleetReady) it.clickable { handleCellTap(r, c) } else it },
                                )
                            }
                        }
                    }
                }

                // Controls (hidden once ready)
                if (!fleetReady) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = ::removeLastShip, enabled = fleet.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        ) { Text("⌫ Rückgängig", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = ::randomizeAll,
                            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        ) { Text("🎲 Zufall", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                    Button(
                        onClick = ::submitFleet,
                        enabled = allPlaced && !submitting,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KkOnlinePlacementAccent,
                            disabledContainerColor = KkOnlinePlacementAccent.copy(alpha = 0.3f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BgDark, strokeWidth = 2.dp)
                        else Text("Bereit! ✓", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BgDark)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // Waiting overlay after submitting
        if (fleetReady) {
            Box(
                modifier = Modifier.fillMaxSize().background(BgDark.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = KkOnlinePlacementAccent, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                    Text("Warte auf Gegner…", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Beide müssen ihre Schiffe setzen", fontSize = 13.sp, color = TextMuted)
                }
            }
        }
    }
}
