package com.bestfriends.beachbingo.feature.perlentaucher.ui

import androidx.compose.runtime.Composable
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.theme.OceanBlue

@Composable
fun PerlentaucherSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    GameSettingsScaffold(
        gameLabel = "PERLENTAUCHER",
        accentColor = OceanBlue,
        saving = false,
        saved = false,
        onBack = onNavigateBack,
        onSave = onNavigateBack,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        // Keine spielspezifischen Einstellungen — Audio-Hint-Row wird via onNavigateToProfile automatisch angezeigt
    }
}
