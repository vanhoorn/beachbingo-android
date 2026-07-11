package com.bestfriends.beachbingo.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel

private val HOTPROMS_NAMES = mapOf(
    "🦁👑" to "Beyoncé",        "🐍👑" to "Taylor Swift",   "💜🎤" to "Prince",
    "🤠🎸" to "Elvis",          "🎸🔥" to "Freddie Mercury", "🤡🃏" to "Joker / Joaquin",
    "💣🎤" to "Eminem",         "🌹💃" to "Jennifer Lopez",  "🦆🎬" to "Tarantino",
    "💃🕺" to "ABBA",           "🎀🔮" to "Madonna",         "🌹🎸" to "The Smiths",
    "🎭✨" to "Lady Gaga",       "⚡🌟" to "David Bowie",    "☂️💄" to "Rihanna",
)
private val COCKTAIL_NAMES = mapOf(
    "🍸" to "Martini",   "🥂" to "Champagner", "🍾" to "Flasche",    "🥃" to "Whisky",
    "🍷" to "Rotwein",   "🧋" to "Bubble Tea", "🍺" to "Bier",        "🍻" to "Prost!",
    "🫗" to "Eingießen", "🧃" to "Saft",        "🍵" to "Matcha",      "🥤" to "Smoothie",
    "🍋" to "Limoncello","🫧" to "Sprudel",     "🍑" to "Bellini",
)

private fun avatarName(av: String) = HOTPROMS_NAMES[av] ?: COCKTAIL_NAMES[av] ?: ""

/** Returns first emoji from a (possibly two-emoji) avatar string. */
private fun String.firstEmoji(): String {
    if (isEmpty()) return this
    return if (length >= 2 && this[0].isHighSurrogate()) substring(0, 2) else substring(0, 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember(currentUser) {
        mutableStateOf(currentUser?.displayName ?: "")
    }
    var selectedAvatar by remember(currentUser) {
        mutableStateOf(currentUser?.avatarUrl?.ifEmpty { BEACH_AVATARS.first() } ?: BEACH_AVATARS.first())
    }
    var soundEnabled by remember(currentUser) { mutableStateOf(currentUser?.soundEnabled ?: true) }
    var musicEnabled by remember(currentUser) { mutableStateOf(currentUser?.musicEnabled ?: true) }
    var activeTab by remember(selectedAvatar) {
        val idx = AVATAR_CATEGORIES.indexOfFirst { selectedAvatar in it.avatars }.takeIf { it >= 0 } ?: 0
        mutableIntStateOf(idx)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearState()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Profil gespeichert ✓")
            viewModel.clearState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.signOut(); onSignOut() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Abmelden") }
                    TextButton(
                        onClick = {
                            viewModel.updateProfile(displayName, selectedAvatar)
                            viewModel.updateAudioPreferences(soundEnabled, musicEnabled)
                        },
                        enabled = !uiState.isLoading && displayName.isNotBlank(),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Speichern")
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Avatar-Anzeige
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Text(
                    text = selectedAvatar,
                    fontSize = if (selectedAvatar.length > 2) 40.sp else 56.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (selectedAvatar.length > 2) 18.dp else 10.dp)
                )
            }

            // Prominame-Badge (nur bei HotProms / Cocktails)
            val prominame = avatarName(selectedAvatar)
            if (prominame.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "⭐ $prominame",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Avatar ändern", style = MaterialTheme.typography.titleMedium)
                TabbedAvatarPicker(
                    selected = selectedAvatar,
                    activeTab = activeTab,
                    onTabChange = { activeTab = it },
                    onSelect = { selectedAvatar = it }
                )
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Anzeigename") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider()

            Text("🔊 Audio (alle Spiele)", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("🎵 Hintergrundmusik", style = MaterialTheme.typography.bodyMedium)
                    Text("Für alle Spiele", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = musicEnabled, onCheckedChange = { musicEnabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("🔔 Soundeffekte", style = MaterialTheme.typography.bodyMedium)
                    Text("Für alle Spiele", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun TabbedAvatarPicker(
    selected: String,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 0.dp,
        ) {
            AVATAR_CATEGORIES.forEachIndexed { index, cat ->
                Tab(
                    selected = activeTab == index,
                    onClick = { onTabChange(index) },
                    text = { Text("${cat.emoji} ${cat.label}", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AvatarPicker(
            avatars = AVATAR_CATEGORIES[activeTab].avatars,
            selected = selected,
            onSelect = onSelect,
        )
    }
}
