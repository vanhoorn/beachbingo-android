package com.bestfriends.beachbingo.feature.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.feature.auth.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedGameMode by remember(currentUser) {
        mutableStateOf(currentUser?.preferredGameMode ?: GameMode.MANUAL_MARK)
    }
    var selectedDrawStyle by remember(currentUser) {
        mutableStateOf(currentUser?.preferredDrawStyle ?: DrawStyle.INSTANT)
    }
    var eliminationInterval by remember(currentUser) {
        mutableStateOf(currentUser?.bossLevelEliminationInterval ?: 5)
    }
    var newEmail by remember { mutableStateOf("") }
    var emailPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Gespeichert ✓")
            newEmail = ""
            emailPassword = ""
            viewModel.clearState()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateGamePreferences(selectedGameMode, selectedDrawStyle, eliminationInterval)
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Speichern")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                "Diese Einstellungen werden beim Erstellen eines neuen Spiels übernommen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))
            Text("Erfahrungslevel", style = MaterialTheme.typography.titleMedium)

            SettingsOption(
                selected = selectedGameMode == GameMode.AUTO_MARK,
                onClick = { selectedGameMode = GameMode.AUTO_MARK },
                title = "🌊 1. Rookie",
                description = "Gezogene Zahlen werden sofort markiert. Spieler drücken BINGO! wenn sie gewonnen haben."
            )
            SettingsOption(
                selected = selectedGameMode == GameMode.MANUAL_MARK,
                onClick = { selectedGameMode = GameMode.MANUAL_MARK },
                title = "🎯 2. Sniper",
                description = "Spieler tippen selbst auf ihre Zahlen. Klassisches Bingo-Feeling."
            )
            SettingsOption(
                selected = selectedGameMode == GameMode.MINI_BOSS_LEVEL,
                onClick = { selectedGameMode = GameMode.MINI_BOSS_LEVEL },
                title = "🔵 3. Mini Boss Level",
                description = "Wie Boss Level, aber gezogene Zahlen werden blau umrandet — du siehst sofort welche du noch antippen kannst."
            )
            SettingsOption(
                selected = selectedGameMode == GameMode.BOSS_LEVEL,
                onClick = { selectedGameMode = GameMode.BOSS_LEVEL },
                title = "💪 4. Boss Level",
                description = "Alle N Züge wirft ein Spieler eine gezogene Zahl zurück in die Lostrommel — kein Highlight, voller Überblick selbst gefragt."
            )

            if (selectedGameMode == GameMode.BOSS_LEVEL || selectedGameMode == GameMode.MINI_BOSS_LEVEL) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Züge bis Elimination",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { if (eliminationInterval > 1) eliminationInterval-- }
                            ) {
                                Text(
                                    "−",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Text(
                                text = eliminationInterval.toString(),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { if (eliminationInterval < 20) eliminationInterval++ }
                            ) {
                                Text(
                                    "+",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Ziehungs-Animation", style = MaterialTheme.typography.titleMedium)

            SettingsOption(
                selected = selectedDrawStyle == DrawStyle.INSTANT,
                onClick = { selectedDrawStyle = DrawStyle.INSTANT },
                title = "⚡ Sofort",
                description = "Die Zahl erscheint direkt. Schnelles Spieltempo."
            )
            SettingsOption(
                selected = selectedDrawStyle == DrawStyle.DRUM,
                onClick = { selectedDrawStyle = DrawStyle.DRUM },
                title = "🥁 Lostrommel",
                description = "3-Sekunden-Animation auf allen Geräten baut Spannung auf."
            )

            HorizontalDivider()

            Text("Konto", style = MaterialTheme.typography.titleMedium)

            Text(
                "Aktuelle E-Mail: ${currentUser?.email ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Neue E-Mail-Adresse") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = emailPassword,
                onValueChange = { emailPassword = it },
                label = { Text("Aktuelles Passwort bestätigen") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { viewModel.updateEmail(newEmail, emailPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isLoading && newEmail.isNotBlank() && emailPassword.isNotBlank()
            ) {
                Text("E-Mail ändern", style = MaterialTheme.typography.labelLarge)
            }

            Text(
                "Nach der Änderung erhältst du eine Bestätigungs-E-Mail.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsOption(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
