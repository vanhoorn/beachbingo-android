package com.bestfriends.beachbingo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.BgDark
import com.bestfriends.beachbingo.ui.theme.BorderColor
import com.bestfriends.beachbingo.ui.theme.ChipLabel
import com.bestfriends.beachbingo.ui.theme.SurfaceDark
import com.bestfriends.beachbingo.ui.theme.TextMuted
import com.bestfriends.beachbingo.ui.theme.TextPrimary
import com.bestfriends.beachbingo.ui.theme.TextSub

/**
 * Standard-Chrome für alle Spiel-Einstellungsscreens.
 *
 * Enthält: TopAppBar (zurück + Speichern-Button), scrollbaren Column-Body,
 * Audio-Hinweis (wenn onNavigateToProfile != null), Gespeichert-Banner und Bottom-Spacer.
 * Der content-Slot liefert die spielspezifischen Einstellungswidgets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScaffold(
    gameLabel: String,
    accentColor: Color,
    saving: Boolean,
    saved: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(gameLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            "⚙️ Einstellungen",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !saving) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = accentColor,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Speichern", color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
            if (onNavigateToProfile != null) {
                SettingsAudioHintRow(accentColor = accentColor, onNavigateToProfile = onNavigateToProfile)
            }
            SettingsSavedBanner(visible = saved, accentColor = accentColor)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Einheitliche Abschnitts-Ueberschrift (z.B. "Schwierigkeit", "Steuerung"). */
@Composable
fun SettingsGroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextSub,
        modifier = modifier.padding(start = 4.dp),
    )
}

/** Auswahl-Kachel mit farbigem Rahmen – Content-Slot liefert den Row-Inhalt. */
@Composable
fun SettingsOptionCard(
    selected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else SurfaceDark,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accentColor else BorderColor,
                shape = RoundedCornerShape(10.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Radio-Zeile: Kreis-Dot + Titel + Beschreibung. Einheitlich in allen Einstellungsscreens. */
@Composable
fun SettingsRadioRow(
    title: String,
    desc: String,
    selected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accentColor.copy(alpha = 0.12f) else SurfaceDark)
            .border(1.5.dp, if (selected) accentColor else BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) accentColor else Color.Transparent)
                .border(2.dp, if (selected) accentColor else TextMuted, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(desc, fontSize = ChipLabel, color = TextMuted)
        }
    }
}

/** Toggle-Zeile: Titel + Beschreibung + Switch. Einheitlich in Brandung und MeerMau. */
@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** Hinweis auf Musik/Sound-Einstellungen im Profilscreen. */
@Composable
fun SettingsAudioHintRow(
    accentColor: Color,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "💡 Musik & Soundeffekte findest du in Profil & Abmelden.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Öffnen →",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onNavigateToProfile),
            )
        }
    }
}

/** Gruenes Feedback-Banner nach erfolgreichem Speichern. Nur sichtbar wenn visible = true. */
@Composable
fun SettingsSavedBanner(visible: Boolean, accentColor: Color, modifier: Modifier = Modifier) {
    if (!visible) return
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            "✓ Einstellungen gespeichert",
            color = accentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}
