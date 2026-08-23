package com.bestfriends.beachbingo.feature.klontausch.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.components.GameSettingsScaffold
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary

private val KlontauschAccent = Color(0xFF8B5CF6)

@Composable
fun KlontauschSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGallery: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
) {
    GameSettingsScaffold(
        gameLabel = "KLONTAUSCH",
        accentColor = KlontauschAccent,
        saving = false,
        saved = false,
        onBack = onNavigateBack,
        onSave = onNavigateBack,
        onNavigateToProfile = onNavigateToProfile,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Spielfiguren", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Alle 38 Figuren im Ueberblick - schaut euch an, wer mitspielen kann.",
                    color = TextMuted,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                )
                OutlinedButton(
                    onClick = onNavigateToGallery,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KlontauschAccent),
                    border = BorderStroke(1.dp, KlontauschAccent.copy(0.5f)),
                ) {
                    Text("Figurengalerie anzeigen")
                }
            }
        }
    }
}
