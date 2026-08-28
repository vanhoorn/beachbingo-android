package com.bestfriends.beachbingo.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.bestfriends.beachbingo.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.bestfriends.beachbingo.core.model.ACTION_GAMES
import com.bestfriends.beachbingo.core.model.ALL_GAMES
import com.bestfriends.beachbingo.core.model.CARD_GAMES
import com.bestfriends.beachbingo.core.model.COUCH_GAMES
import com.bestfriends.beachbingo.core.model.GameGenre
import com.bestfriends.beachbingo.core.model.GameMetadata
import com.bestfriends.beachbingo.core.model.PlayerCount
import com.bestfriends.beachbingo.core.model.RIDDLE_GAMES
import com.bestfriends.beachbingo.feature.raetsel.GameSave
import com.bestfriends.beachbingo.feature.raetsel.PUZZLE_DIFFICULTY_LABELS
import com.bestfriends.beachbingo.feature.raetsel.PUZZLE_GAME_INFO
import com.bestfriends.beachbingo.feature.raetsel.PuzzleSave
import com.bestfriends.beachbingo.feature.raetsel.SoloGameSaveManager
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.OceanBlue
import com.bestfriends.beachbingo.ui.theme.Surface2Dark
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub
import com.bestfriends.beachbingo.ui.theme.BingoCallSize
import com.bestfriends.beachbingo.ui.theme.CellNumber
import com.bestfriends.beachbingo.ui.theme.ChipLabel
import com.bestfriends.beachbingo.ui.theme.ChipLabelTiny
import com.bestfriends.beachbingo.ui.theme.Coral
import com.bestfriends.beachbingo.ui.theme.Danger
import com.bestfriends.beachbingo.ui.theme.DrawNumberPhone
import com.bestfriends.beachbingo.ui.theme.PurpleDeep
import com.bestfriends.beachbingo.ui.theme.SandGold
import com.bestfriends.beachbingo.ui.theme.ScoreLarge
import com.bestfriends.beachbingo.ui.theme.SkyBlue
import com.bestfriends.beachbingo.ui.theme.TitleHero
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private data class PlayerCountEntry(
    val key: PlayerCount,
    val label: String,
    val emoji: String,
)

private data class TourSlide(val emoji: String, val title: String, val description: String)

private val TOUR_SLIDES = listOf(
    TourSlide("🏖️", "Willkommen bei BeachBande!", "Diese kurze Tour zeigt dir die wichtigsten Funktionen. Du kannst sie jederzeit über das ? oben links erneut aufrufen."),
    TourSlide("🎮", "Rubriken", "Alle Spiele sind nach Art sortiert: Rätsel, Karten, Action und Couch. So findest du schnell das passende Spiel für eure Runde."),
    TourSlide("👥", "Nach Spieleranzahl filtern", "Tippe auf eine Spieleranzahl, um alle passenden Spiele zu sehen – ideal, wenn du schon weißt, wie viele mitspielen werden."),
    TourSlide("★", "Favoriten", "Öffne ein Spiel und tippe auf das Herz-Symbol, um es als Favorit zu markieren. Favoriten erscheinen immer oben auf dem Startbildschirm."),
    TourSlide("🔗", "Spiel beitreten", "Über das Ketten-Symbol oben rechts kannst du einem laufenden Spiel beitreten – per 6-stelligem Code oder QR-Code-Scan."),
    TourSlide("📱", "Spieler per QR-Code einladen", "In jeder Spiellobby findest du einen QR-Code. Zeige ihn deinen Mitspielern – sie können sofort beitreten, ohne den Code abzutippen."),
    TourSlide("👤", "Profil & Einstellungen", "Über das Personen-Symbol oben rechts erreichst du dein Profil. Dort kannst du Name, Avatar und Einstellungen anpassen und speichern."),
)

private val PLAYER_COUNT_LIST = listOf(
    PlayerCountEntry(PlayerCount.SOLO,      "Solo",        "🧘"),
    PlayerCountEntry(PlayerCount.ONE_TWO,   "1-2 Spieler", "🤝"),
    PlayerCountEntry(PlayerCount.TWO_FOUR,  "2-4 Spieler", "👥"),
    PlayerCountEntry(PlayerCount.FOUR_PLUS, "4+ Spieler",  "🎉"),
)

private data class ActiveGameInfo(
    val type: String,
    val gameId: String,
    val gameName: String,
    val emoji: String,
)

@Composable
fun HomeScreen(
    onNavigateToBingoLobby: () -> Unit,
    onNavigateToPongLobby: () -> Unit,
    onNavigateToVierLobby: () -> Unit,
    onNavigateToPiratesLobby: () -> Unit,
    onNavigateToWormLobby: () -> Unit,
    onNavigateToStrandturmLobby: () -> Unit,
    onNavigateToBrandungLobby: () -> Unit,
    onNavigateToMeermauLobby: () -> Unit,
    onNavigateToStrandraeuberLobby: () -> Unit,
    onNavigateToKlontauschLobby: () -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToCardGames: () -> Unit,
    onNavigateToActionGames: () -> Unit,
    onNavigateToCouchGames: () -> Unit,
    onNavigateToAllGames: () -> Unit,
    onNavigateToRaetsel: () -> Unit,
    onNavigateToDuenenschattenLobby: () -> Unit = {},
    onNavigateToInselbrueckeLobby: () -> Unit = {},
    onNavigateToStrandokuLobby: () -> Unit = {},
    onNavigateToWellensummeLobby: () -> Unit = {},
    onNavigateToKuestenkriegLobby: () -> Unit = {},
    onNavigateToWortWelleLobby: () -> Unit = {},
    onNavigateToPerlentaucherLobby: () -> Unit = {},
    onNavigateToRaetselGame: (save: PuzzleSave) -> Unit = {},
    onRejoinGame: (type: String, gameId: String) -> Unit = { _, _ -> },
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var favoriteIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeGame by remember { mutableStateOf<ActiveGameInfo?>(null) }
    var savedPuzzles by remember { mutableStateOf<List<PuzzleSave>>(emptyList()) }
    var savedGames by remember { mutableStateOf<List<GameSave>>(emptyList()) }
    var showTour by remember { mutableStateOf(false) }
    var tourSlide by remember { mutableStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    LaunchedEffect(Unit) {
        savedPuzzles = SoloGameSaveManager.getSaves(context)
        savedGames = SoloGameSaveManager.getGameSaves(context)
        val tourSeen = context.getSharedPreferences("beachbande_prefs", 0).getBoolean("tour_seen", false)
        if (!tourSeen) showTour = true
    }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = firestore.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            favoriteIds = (snap.get("favoriteGames") as? List<String>) ?: emptyList()
        } catch (_: Exception) {}
    }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val dismissedIds = context.getSharedPreferences("dismissed_games", 0)
                .getStringSet("dismissed_game_ids", emptySet()) ?: emptySet()
            val collections = listOf(
                Triple("strandraeuber", "strandraeuberGames", "🦹 Strandräuber"),
                Triple("meermau",       "meermauGames",       "🃏 MeerMau"),
                Triple("brandung",      "brandungGames",      "🌊 Brandung"),
                Triple("bingo",         "games",              "🎱 Bingo"),
            )
            for ((type, collection, displayName) in collections) {
                val snap = firestore.collection(collection)
                    .whereEqualTo("status", "RUNNING")
                    .whereArrayContains("playerIds", uid)
                    .get().await()
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    if (doc.id in dismissedIds) continue
                    val parts = displayName.split(" ", limit = 2)
                    activeGame = ActiveGameInfo(type, doc.id, parts.getOrElse(1) { displayName }, parts[0])
                    return@LaunchedEffect
                }
            }
        } catch (_: Exception) {}
    }

    val favoriteGames = ALL_GAMES
        .filter { it.id in favoriteIds }
        .sortedBy { it.title }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero ─────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Surface2Dark,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = currentUser?.avatarUrl ?: "🏖️", fontSize = ScoreLarge)
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WILLKOMMEN ZURÜCK",
                        fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold,
                        color = TextMuted, letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = currentUser?.displayName ?: "…",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp), color = Surface2Dark,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { showTour = true; tourSlide = 0 }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextMuted)
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp), color = Surface2Dark,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToJoin() }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("🔗", style = MaterialTheme.typography.titleLarge) }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp), color = Surface2Dark,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToProfile() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = TextSub, modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ── Aktives Spiel ─────────────────────────────────────────────────────────
        if (activeGame != null) {
            ActiveGameBanner(
                game = activeGame!!,
                onResume = { onRejoinGame(activeGame!!.type, activeGame!!.gameId) },
                onDismiss = {
                    activeGame?.gameId?.let { id ->
                        val prefs = context.getSharedPreferences("dismissed_games", 0)
                        val current = prefs.getStringSet("dismissed_game_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                        current.add(id)
                        prefs.edit().putStringSet("dismissed_game_ids", current).apply()
                    }
                    activeGame = null
                },
                onDelete = {
                    activeGame?.let { game ->
                        val collectionByType = mapOf(
                            "strandraeuber" to "strandraeuberGames",
                            "meermau"       to "meermauGames",
                            "brandung"      to "brandungGames",
                            "bingo"         to "games",
                        )
                        collectionByType[game.type]?.let { col ->
                            firestore.collection(col).document(game.gameId).delete()
                        }
                    }
                    activeGame = null
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }

        // ── Favoriten ─────────────────────────────────────────────────────────────
        if (favoriteGames.isNotEmpty()) {
            SectionHeader(
                title = "FAVORITEN", emoji = "★",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                favoriteGames.forEach { game ->
                    MiniGameCard(game = game, onClick = {
                        when (game.id) {
                            "bingo"          -> onNavigateToBingoLobby()
                            "pong"           -> onNavigateToPongLobby()
                            "vier"           -> onNavigateToVierLobby()
                            "pirates"        -> onNavigateToPiratesLobby()
                            "worm"           -> onNavigateToWormLobby()
                            "strandturm"     -> onNavigateToStrandturmLobby()
                            "brandung"       -> onNavigateToBrandungLobby()
                            "meermau"        -> onNavigateToMeermauLobby()
                            "strandraeuber"  -> onNavigateToStrandraeuberLobby()
                            "klontausch"     -> onNavigateToKlontauschLobby()
                            "duenenschatten" -> onNavigateToDuenenschattenLobby()
                            "inselbruecke"   -> onNavigateToInselbrueckeLobby()
                            "strandoku"      -> onNavigateToStrandokuLobby()
                            "wellensumme"    -> onNavigateToWellensummeLobby()
                            "kuestenkrieg"   -> onNavigateToKuestenkriegLobby()
                            "wortwelle"      -> onNavigateToWortWelleLobby()
                        }
                    })
                }
            }
        }

        // ── Rätsel ────────────────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .border(1.5.dp, SkyBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onNavigateToRaetsel() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "🧩", style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rätsel",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary,
                    )
                    Text(
                        text = "${RIDDLE_GAMES.size} Rätsel · Strandoku, WellenSumme & mehr",
                        fontSize = ChipLabel, color = TextMuted,
                    )
                }
                Text(text = "›", style = MaterialTheme.typography.titleLarge, color = SkyBlue)
            }
        }

        // ── Karten ────────────────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .border(1.5.dp, PurpleDeep.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onNavigateToCardGames() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "🃏", style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Karten",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary,
                    )
                    Text(
                        text = "${CARD_GAMES.size} Spiele · MeerMau, Brandung & mehr",
                        fontSize = ChipLabel, color = TextMuted,
                    )
                }
                Text(text = "›", style = MaterialTheme.typography.titleLarge, color = PurpleDeep)
            }
        }

        // ── Action ────────────────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .border(1.5.dp, Coral.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onNavigateToActionGames() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "⚡", style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Action",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary,
                    )
                    Text(
                        text = "${ACTION_GAMES.size} Spiele · BeachPirates, BeachVolley & mehr",
                        fontSize = ChipLabel, color = TextMuted,
                    )
                }
                Text(text = "›", style = MaterialTheme.typography.titleLarge, color = Coral)
            }
        }

        // ── Couch ─────────────────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .border(1.5.dp, SandGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onNavigateToCouchGames() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "🛋️", style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Couch",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary,
                    )
                    Text(
                        text = "${COUCH_GAMES.size} Spiele · BeachBingo, Vier4Bier & mehr",
                        fontSize = ChipLabel, color = TextMuted,
                    )
                }
                Text(text = "›", style = MaterialTheme.typography.titleLarge, color = SandGold)
            }
        }

        // ── Alle Spiele ───────────────────────────────────────────────────────────
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .border(1.5.dp, OceanBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onNavigateToAllGames() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "🎮", style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alle Spiele",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary,
                    )
                    Text(
                        text = "${ALL_GAMES.size} Spiele · alphabetisch sortiert",
                        fontSize = ChipLabel, color = TextMuted,
                    )
                }
                Text(text = "›", style = MaterialTheme.typography.titleLarge, color = OceanBlue)
            }
        }

        // ── Spieleranzahl ─────────────────────────────────────────────────────────
        SectionHeader(
            title = "SPIELERANZAHL", emoji = "👥",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PLAYER_COUNT_LIST.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { entry ->
                        val gameCount = ALL_GAMES.count { entry.key in it.playerCounts }
                        CategoryTile(
                            entry = entry,
                            gameCount = gameCount,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToCategory(entry.key.name) }
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // ── Gespeicherte Spiele ───────────────────────────────────────────────────
        if (savedPuzzles.isNotEmpty() || savedGames.isNotEmpty()) {
            SectionHeader(
                title = "GESPEICHERTE SPIELE", emoji = "💾",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                savedGames.forEach { save ->
                    val navigateTo: () -> Unit = when (save.gameType) {
                        "pirates"       -> onNavigateToPiratesLobby
                        "worm"          -> onNavigateToWormLobby
                        "strandturm"    -> onNavigateToStrandturmLobby
                        "vier"          -> onNavigateToVierLobby
                        "brandung"      -> onNavigateToBrandungLobby
                        "meermau"       -> onNavigateToMeermauLobby
                        "strandraeuber" -> onNavigateToStrandraeuberLobby
                        "klontausch"    -> onNavigateToKlontauschLobby
                        else -> ({})
                    }
                    SavedGameHomeCard(
                        save = save,
                        onClick = navigateTo,
                        onDelete = {
                            SoloGameSaveManager.deleteGameSave(context, save.gameType)
                            savedGames = savedGames.filter { it.gameType != save.gameType }
                        },
                    )
                }
                savedPuzzles.forEach { save ->
                    val info = PUZZLE_GAME_INFO[save.gameType]
                    if (info != null) {
                        SavedPuzzleCard(
                            save = save, info = info,
                            onClick = { onNavigateToRaetselGame(save) },
                            onDelete = {
                                SoloGameSaveManager.deleteSave(context, save.id)
                                savedPuzzles = savedPuzzles.filter { it.id != save.id }
                            },
                        )
                    }
                }
            }
        }

        // ── Tour ─────────────────────────────────────────────────────────────────
        if (showTour) {
            HelpTourDialog(
                slide = tourSlide,
                onNext = { if (tourSlide < TOUR_SLIDES.size - 1) tourSlide++ },
                onBack = { if (tourSlide > 0) tourSlide-- },
                onClose = {
                    context.getSharedPreferences("beachbande_prefs", 0)
                        .edit().putBoolean("tour_seen", true).apply()
                    showTour = false
                    tourSlide = 0
                },
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String, emoji: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleSmall)
        Text(
            text = title,
            fontSize = ChipLabel, fontWeight = FontWeight.Bold,
            color = TextMuted, letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun MiniGameCard(game: GameMetadata, onClick: () -> Unit) {
    val accentColor = Color(game.color)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        modifier = Modifier
            .width(90.dp)
            .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (game.id == "meermau") {
                Image(
                    painter = painterResource(R.drawable.ic_meermau_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(text = game.emoji, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = game.title,
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                color = TextPrimary, lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun ActiveGameBanner(
    game: ActiveGameInfo,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Spiel löschen?") },
            text = { Text("\"${game.gameName}\" wird für alle Spieler beendet.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Löschen", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Surface2Dark,
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, OceanBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(game.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AKTIVES SPIEL",
                        fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold,
                        color = OceanBlue, letterSpacing = 1.sp,
                    )
                    Text(
                        game.gameName,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary,
                    )
                    Text(
                        "Code: ${game.gameId}",
                        fontSize = ChipLabel, color = TextMuted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(40.dp),
                ) { Text("Ignorieren", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                    border = BorderStroke(1.dp, Danger),
                ) { Text("🗑 Löschen", color = Danger, style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Weiterspielen →", color = Color.White, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun CategoryTile(
    entry: PlayerCountEntry,
    gameCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        modifier = modifier
            .border(1.5.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = entry.emoji, fontSize = TitleHero)
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.label,
                fontSize = ChipLabel, fontWeight = FontWeight.Bold,
                color = TextPrimary, lineHeight = 15.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "$gameCount ${if (gameCount == 1) "Spiel" else "Spiele"}",
                fontSize = ChipLabelTiny, color = TextMuted,
            )
        }
    }
}

@Composable
private fun SavedPuzzleCard(
    save: PuzzleSave,
    info: Triple<String, String, Long>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val accentColor = Color(info.third)
    val diffLabel = PUZZLE_DIFFICULTY_LABELS[save.difficulty] ?: save.difficulty
    val elapsed = SoloGameSaveManager.formatElapsed(save.elapsedSeconds)

    Box(modifier = Modifier.width(140.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onClick() }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = info.second, fontSize = TitleHero)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = info.first,
                    fontSize = ChipLabel, fontWeight = FontWeight.Bold,
                    color = TextPrimary, lineHeight = 15.sp,
                    modifier = Modifier.padding(end = 22.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$diffLabel · ${save.variant}",
                    fontSize = ChipLabelTiny, fontWeight = FontWeight.Bold, color = accentColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⏱ $elapsed",
                    fontSize = ChipLabelTiny, color = TextMuted,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Danger.copy(alpha = 0.15f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", fontSize = ChipLabelTiny, color = Danger)
        }
    }
}

@Composable
private fun SavedGameHomeCard(
    save: GameSave,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val gameInfo = ALL_GAMES.find { it.id == save.gameType } ?: return
    val accentColor = Color(gameInfo.color)

    Box(modifier = Modifier.width(140.dp)) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable { onClick() }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (gameInfo.id == "meermau") {
                    Image(
                        painter = painterResource(R.drawable.ic_meermau_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    Text(text = gameInfo.emoji, fontSize = TitleHero)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = gameInfo.title,
                    fontSize = ChipLabel, fontWeight = FontWeight.Bold,
                    color = TextPrimary, lineHeight = 15.sp,
                    modifier = Modifier.padding(end = 22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = save.displayLabel,
                    fontSize = ChipLabelTiny, color = TextMuted,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Danger.copy(alpha = 0.15f))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", fontSize = ChipLabelTiny, color = Danger)
        }
    }
}

@Composable
private fun HelpTourDialog(
    slide: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(20.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.6f))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val current = TOUR_SLIDES[slide]

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(current.emoji, fontSize = DrawNumberPhone, lineHeight = 72.sp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            current.title,
                            fontSize = BingoCallSize, fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary, textAlign = TextAlign.Center, lineHeight = 26.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            current.description,
                            fontSize = CellNumber, color = TextMuted,
                            textAlign = TextAlign.Center, lineHeight = 22.sp,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TOUR_SLIDES.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(if (i == slide) 20.dp else 7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (i == slide) OceanBlue else Color.White.copy(alpha = 0.25f))
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (slide > 0) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(50.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("← Zurück", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Button(
                        onClick = if (slide == TOUR_SLIDES.size - 1) onClose else onNext,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            if (slide == TOUR_SLIDES.size - 1) "Los geht's! 🏖️" else "Weiter →",
                            color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
