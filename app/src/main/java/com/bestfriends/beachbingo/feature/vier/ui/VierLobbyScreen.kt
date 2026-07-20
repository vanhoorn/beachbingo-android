package com.bestfriends.beachbingo.feature.vier.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.feature.bingo.ui.components.QrCodeImage
import com.bestfriends.beachbingo.feature.vier.viewmodel.VierLobbyViewModel
import androidx.compose.material.icons.outlined.HelpOutline
import com.bestfriends.beachbingo.core.model.ALL_GAME_RULES
import com.bestfriends.beachbingo.feature.home.ui.GameRulesBottomSheet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub

private val BeerOrange = Color(0xFFC2410C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VierLobbyScreen(
    initialJoinCode: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGame: (mode: String, gameId: String?, myDrinkId: String, aiDrinkId: String?, aiDifficulty: String) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: VierLobbyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(if (initialJoinCode != null) "lobby" else "mode") }
    var mode by remember { mutableStateOf("ai") }
    var myDrinkId by remember { mutableStateOf("lager") }
    var joinCode by remember { mutableStateOf(initialJoinCode ?: "") }
    var joining by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }

    val vierAuth = FirebaseAuth.getInstance()
    val vierFirestore = FirebaseFirestore.getInstance()
    val vierUid = vierAuth.currentUser?.uid

    LaunchedEffect(vierUid) {
        if (vierUid == null) return@LaunchedEffect
        val snap = try { vierFirestore.collection("users").document(vierUid).get().await() } catch (_: Exception) { return@LaunchedEffect }
        @Suppress("UNCHECKED_CAST")
        isFavorite = (snap.get("favoriteGames") as? List<String>)?.contains("vier") == true
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        val update = if (isFavorite) FieldValue.arrayUnion("vier") else FieldValue.arrayRemove("vier")
        if (vierUid != null) vierFirestore.collection("users").document(vierUid).update("favoriteGames", update)
    }

    // Sync preferred drink + difficulty from VM
    LaunchedEffect(uiState.preferredDrinkId) {
        myDrinkId = uiState.preferredDrinkId
    }

    // Handle navigation from VM
    LaunchedEffect(uiState.navigateToGame) {
        uiState.navigateToGame?.let { nav ->
            viewModel.clearNavigate()
            onNavigateToGame(nav.mode, nav.gameId, nav.myDrinkId, nav.aiDrinkId, nav.aiDifficulty)
        }
    }

    val context = LocalContext.current

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VIER4BIER", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("🍺 Lobby", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
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
                    IconButton(onClick = { showRules = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Spielanleitung", tint = TextSub)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Step: Mode ──
            if (step == "mode") {
                Text(
                    text = "Spielmodus wählen",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                ModeCard(
                    emoji = "🤖",
                    title = "Gegen KI",
                    description = "Spiel allein gegen den Computer",
                    color = BeerOrange,
                    onClick = { mode = "ai"; step = "drink" },
                )
                ModeCard(
                    emoji = "📱",
                    title = "Online – 2 Spieler",
                    description = "Spielt gemeinsam in Echtzeit",
                    color = Color(0xFF0EA5E9),
                    onClick = { mode = "online"; step = "drink" },
                )
            }

            // ── Step: Drink ──
            if (step == "drink") {
                OutlinedButton(
                    onClick = { step = "mode" },
                    modifier = Modifier.align(Alignment.Start),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                ) {
                    Text("‹ Zurück")
                }

                Text(
                    text = "Wähle dein Getränk",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DRINKS.chunked(3).forEach { rowDrinks ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowDrinks.forEach { drink ->
                                val selected = myDrinkId == drink.id
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = 2.dp,
                                            color = if (selected) drink.color else Color(0xFF1E3050),
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .clickable { myDrinkId = drink.id },
                                    color = if (selected) drink.color.copy(alpha = 0.2f) else SurfaceDark,
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        DrinkPiece(drinkId = drink.id, size = 44.dp)
                                        Text(
                                            text = drink.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSub,
                                        )
                                    }
                                }
                            }
                            // Leere Zellen auffüllen falls letzte Zeile < 3 Elemente
                            repeat(3 - rowDrinks.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (mode == "ai") {
                            val others = DRINKS.filter { it.id != myDrinkId }
                            val aiDrinkId = others.random().id
                            onNavigateToGame("ai", null, myDrinkId, aiDrinkId, uiState.preferredDifficulty)
                        } else {
                            step = "lobby"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BeerOrange),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (mode == "ai") "Spiel starten 🍺" else "Weiter →")
                }
            }

            // ── Step: Online Lobby ──
            if (step == "lobby") {
                OutlinedButton(
                    onClick = {
                        if (uiState.onlineStep == "waiting") {
                            viewModel.cancelWaiting()
                        } else {
                            step = "drink"
                        }
                    },
                    modifier = Modifier.align(Alignment.Start),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                ) {
                    Text("‹ Zurück")
                }

                if (uiState.onlineStep == "choose") {
                    Text(
                        text = "Online-Spiel",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )

                    // Create card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Spiel erstellen", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "Erstelle ein Spiel und teile den Code mit deinem Gegner.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            Button(
                                onClick = { viewModel.createOnlineGame(myDrinkId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BeerOrange),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Neues Spiel erstellen")
                            }
                        }
                    }

                    // Join card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Spiel beitreten", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase().take(6) },
                                placeholder = { Text("6-stelliger Code", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 4.sp,
                                    color = TextPrimary,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BeerOrange,
                                    unfocusedBorderColor = Color(0xFF1E3050),
                                    cursorColor = BeerOrange,
                                ),
                            )
                            Button(
                                onClick = {
                                    joining = true
                                    viewModel.joinOnlineGame(joinCode, myDrinkId)
                                },
                                enabled = !joining && joinCode.length == 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (joining) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                else Text("Beitreten")
                            }
                        }
                    }

                    uiState.error?.let { err ->
                        Text(
                            text = err,
                            color = Danger,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (uiState.onlineStep == "waiting") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = "⏳ Warte auf Gegner…",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                        )

                        // Game code card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Surface2Dark,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "SPIELCODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    letterSpacing = 1.5.sp,
                                )
                                Text(
                                    text = uiState.gameCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SandGold,
                                    letterSpacing = 6.sp,
                                )
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Spielcode", uiState.gameCode))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSub),
                                ) {
                                    Text("📋 Kopieren")
                                }
                            }
                        }

                        // QR code
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.padding(4.dp),
                        ) {
                            QrCodeImage(
                                content = "https://thebeachbingo.netlify.app/vier/lobby?join=${uiState.gameCode}",
                                size = 160.dp,
                            )
                        }

                        Text(
                            text = "Gegner scannt den QR-Code oder gibt den Code ein",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DrinkPiece(drinkId = myDrinkId, size = 40.dp)
                            Text(
                                text = "${uiState.waitingPlayerCount} / 2 Spieler",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showRules) {
        ALL_GAME_RULES["vier"]?.let { GameRulesBottomSheet(rule = it, onDismiss = { showRules = false }) }
    }
}


@Composable
private fun ModeCard(
    emoji: String,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(emoji, fontSize = 28.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            Text("›", fontSize = 20.sp, color = TextMuted)
        }
    }
}
