package com.bestfriends.beachbingo.feature.pong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.core.model.PongGame
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.feature.pong.viewmodel.PongLobbyViewModel
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Success
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub

private fun sideColor(side: String): Color = when (side) {
    "left" -> OceanBlue
    "right" -> Coral
    "top" -> SandGold
    "bottom" -> Success
    else -> TextMuted
}

private fun sideLabel(side: String): String = when (side) {
    "left" -> "◀ Links"
    "right" -> "▶ Rechts"
    "top" -> "▲ Oben"
    "bottom" -> "▼ Unten"
    else -> side
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongLobbyScreen(
    initialJoinCode: String? = null,
    onNavigateToHome: () -> Unit,
    onNavigateToGame: (gameId: String?, totalPaddles: Int, humanCount: Int, difficulty: String, scoreLimit: Int, isHost: Boolean, mySide: String) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PongLobbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val currentUid by viewModel.currentUid.collectAsStateWithLifecycle()

    var totalPaddles by rememberSaveable { mutableIntStateOf(2) }
    var humanCount by rememberSaveable { mutableIntStateOf(1) }
    var difficulty by rememberSaveable { mutableStateOf(PongDifficulty.ROOKIE) }
    var scoreLimit by rememberSaveable { mutableIntStateOf(7) }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var deleteGameId by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    val pongAuth = FirebaseAuth.getInstance()
    val pongFirestore = FirebaseFirestore.getInstance()
    val pongUid = pongAuth.currentUser?.uid

    LaunchedEffect(pongUid) {
        if (pongUid == null) return@LaunchedEffect
        val snap = try { pongFirestore.collection("users").document(pongUid).get().await() } catch (_: Exception) { return@LaunchedEffect }
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("pong") == true
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("pong") else FieldValue.arrayRemove("pong")
        if (pongUid != null) pongFirestore.collection("users").document(pongUid).update("favoriteGames", update)
    }

    // QR scanner
    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        // Extract game ID from URL (?join=XXXXXX) or use raw value directly
        val code = Uri.parse(raw).getQueryParameter("join")?.uppercase()
            ?: raw.trim().uppercase().takeLast(6)
        joinCode = code
        viewModel.clearError()
    }

    // Apply preferences from ViewModel once loaded
    LaunchedEffect(uiState.totalPaddles, uiState.difficulty, uiState.scoreLimit) {
        totalPaddles = uiState.totalPaddles
        difficulty = uiState.difficulty
        scoreLimit = uiState.scoreLimit
    }

    // Handle deep link join code
    LaunchedEffect(initialJoinCode) {
        if (!initialJoinCode.isNullOrBlank()) {
            totalPaddles = 2
            humanCount = 2
            joinCode = initialJoinCode
        }
    }

    // Clamp humanCount when totalPaddles changes
    LaunchedEffect(totalPaddles) {
        if (humanCount > totalPaddles) humanCount = totalPaddles
    }

    // Observe lobby games for multiplayer
    LaunchedEffect(currentUid, humanCount) {
        val uid = currentUid ?: return@LaunchedEffect
        viewModel.observeLobbyGames(uid, humanCount)
    }

    // Delete confirmation dialog
    if (deleteGameId != null) {
        AlertDialog(
            onDismissRequest = { deleteGameId = null },
            title = { Text("Spiel löschen?") },
            text = { Text("Das Spiel wird für alle Spieler gelöscht.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteGame(deleteGameId!!); deleteGameId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deleteGameId = null }) { Text("Abbrechen") }
            }
        )
    }

    val hasAI = humanCount < totalPaddles
    val needsLobby = humanCount > 1
    val canStart = activeGame != null && (activeGame!!.players.size >= activeGame!!.humanCount)
    val joinUrl = activeGame?.let { "https://thebeachbingo.netlify.app/pong/lobby?join=${it.gameId}" } ?: ""

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BEACHVOLLEY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🏓 Lobby", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = OceanBlue)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToResults) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Ergebnisse", tint = SandGold)
                    }
                    IconButton(onClick = { toggleFavorite() }) {
                        Text(
                            if (isFavorite) "★" else "☆",
                            fontSize = 22.sp,
                            color = if (isFavorite) SandGold else TextMuted,
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Total paddles ──
            item {
                SectionHeader("Gesamtanzahl Paddles")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(2, 3, 4).forEach { n ->
                        PillButton(
                            label = "$n Paddles",
                            active = totalPaddles == n,
                            color = Coral,
                            modifier = Modifier.weight(1f)
                        ) { totalPaddles = n }
                    }
                }
                if (totalPaddles == 3) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚡ Eine Seite wird zufällig zur Wand", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                if (totalPaddles == 4) {
                    Spacer(Modifier.height(8.dp))
                    Text("🏟️ Alle vier Seiten — mit Eck-Deflektoren", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }

            // ── Human count ──
            item {
                SectionHeader("Davon menschliche Spieler")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (1..totalPaddles).forEach { n ->
                        PillButton(
                            label = if (n == 1) "$n\nMensch" else "$n\nMenschen",
                            active = humanCount == n,
                            color = OceanBlue,
                            modifier = Modifier.weight(1f)
                        ) { humanCount = n }
                    }
                }
                if (hasAI) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OceanBlue.copy(alpha = 0.15f))
                            .padding(10.dp, 8.dp)
                    ) {
                        Text(
                            "🤖 ${totalPaddles - humanCount} KI-${if (totalPaddles - humanCount == 1) "Gegner" else "Gegner"}",
                            color = OceanBlue,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── Difficulty ──
            if (hasAI) {
                item {
                    SectionHeader("KI-Schwierigkeit")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(PongDifficulty.ROOKIE, "Rookie", "Langsam, macht Fehler"),
                            Triple(PongDifficulty.SNIPER, "Sniper", "Schnell, trifft meistens"),
                            Triple(PongDifficulty.BOSS_LEVEL, "Boss Level", "Unerbittlich — viel Spaß 😈"),
                        ).forEach { (opt, label, desc) ->
                            val active = difficulty == opt
                            val interactionSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) OceanBlue.copy(alpha = 0.12f) else SurfaceDark)
                                    .border(
                                        width = if (active) 2.dp else 1.5.dp,
                                        color = if (active) OceanBlue else BorderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(interactionSource = interactionSource, indication = null) { difficulty = opt }
                                    .padding(13.dp, 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(50))
                                            .border(
                                                width = if (active) 5.dp else 2.dp,
                                                color = if (active) OceanBlue else BorderColor,
                                                shape = RoundedCornerShape(50)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Score limit ──
            item {
                SectionHeader("Punkte zum Sieg / Limit")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(12.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepButton("-") { scoreLimit = (scoreLimit - 1).coerceAtLeast(1) }
                    Text(
                        "$scoreLimit Punkte",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    StepButton("+") { scoreLimit = (scoreLimit + 1).coerceAtMost(21) }
                }
            }

            // ── Solo start ──
            if (!needsLobby) {
                item {
                    Button(
                        onClick = {
                            onNavigateToGame(null, totalPaddles, humanCount, difficulty.name, scoreLimit, true, "left")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Coral)
                    ) {
                        Text("🏓 Spiel starten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }

            // ── Multi-player lobby ──
            if (needsLobby) {
                item {
                    if (activeGame != null) {
                        LobbyCard(
                            game = activeGame!!,
                            joinUrl = joinUrl,
                            canStart = canStart,
                            hasAI = hasAI,
                            totalPaddles = totalPaddles,
                            difficulty = difficulty,
                            currentUid = currentUid ?: "",
                            onStart = {
                                val me = activeGame!!.players.find { it.userId == currentUid }
                                onNavigateToGame(
                                    activeGame!!.gameId,
                                    activeGame!!.totalPaddles,
                                    activeGame!!.humanCount,
                                    activeGame!!.difficulty.name,
                                    activeGame!!.scoreLimit,
                                    true,
                                    me?.side ?: "left"
                                )
                            },
                            onDelete = { deleteGameId = activeGame!!.gameId }
                        )
                    } else {
                        Button(
                            onClick = { viewModel.createGame(totalPaddles, humanCount, difficulty, scoreLimit) },
                            enabled = !uiState.isCreating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Coral)
                        ) {
                            if (uiState.isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("🎮 Neues Spiel erstellen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }

                // ── Join divider ──
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                        Text("  oder beitreten  ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase(); viewModel.clearError() },
                            placeholder = { Text("Spiel-Code", color = TextMuted, fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                letterSpacing = 2.sp,
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OceanBlue,
                                unfocusedBorderColor = BorderColor,
                                cursorColor = OceanBlue,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val opts = ScanOptions().apply {
                                        setPrompt("QR-Code scannen")
                                        setBeepEnabled(false)
                                        setOrientationLocked(false)
                                    }
                                    qrScanLauncher.launch(opts)
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "QR scannen", tint = OceanBlue)
                                }
                            },
                        )
                        Button(
                            onClick = {
                                viewModel.joinGame(joinCode) { gid, tp, hc, diff, sl, host, side ->
                                    onNavigateToGame(gid, tp, hc, diff.name, sl, host, side)
                                }
                            },
                            enabled = !uiState.isJoining && joinCode.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Surface2Dark),
                            modifier = Modifier.height(56.dp)
                        ) {
                            if (uiState.isJoining) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Beitreten", color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (uiState.joinError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(uiState.joinError!!, color = Danger, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun LobbyCard(
    game: PongGame,
    joinUrl: String,
    canStart: Boolean,
    hasAI: Boolean,
    totalPaddles: Int,
    difficulty: PongDifficulty,
    currentUid: String,
    onStart: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.5.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "DEIN OFFENES SPIEL",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.2.sp
        )

        // Game code
        Column {
            Text("Spiel-Code", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface2Dark)
                    .padding(10.dp, 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    game.gameId,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = SandGold
                )
            }
        }

        // QR code
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                QrCodeImage(content = joinUrl, size = 150.dp)
            }
        }

        // Players
        Column {
            Text(
                "Spieler (${game.players.size}/${game.humanCount})",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            game.players.forEach { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(player.avatarUrl.ifEmpty { "🏄" }, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        player.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        sideLabel(player.side),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = sideColor(player.side)
                    )
                }
                HorizontalDivider(color = BorderColor)
            }
            if (hasAI) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤖", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${totalPaddles - game.humanCount}× KI (${
                            when (difficulty) {
                                PongDifficulty.ROOKIE -> "Rookie"
                                PongDifficulty.SNIPER -> "Sniper"
                                PongDifficulty.BOSS_LEVEL -> "Boss Level"
                            }
                        })",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Coral,
                    disabledContainerColor = Coral.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    if (canStart) "🏓 Spiel starten" else "⏳ Warte… (${game.players.size}/${game.humanCount})",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface2Dark)
                    .border(1.5.dp, BorderColor, RoundedCornerShape(10.dp))
                    .size(50.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Danger)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
}

@Composable
private fun PillButton(
    label: String,
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) color.copy(alpha = 0.15f) else SurfaceDark)
            .border(
                width = if (active) 2.dp else 1.5.dp,
                color = if (active) color else BorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp, 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2Dark)
            .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
