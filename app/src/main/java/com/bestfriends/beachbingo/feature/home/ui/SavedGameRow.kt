package com.bestfriends.beachbingo.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.ui.theme.*

@Composable
fun SavedGameRow(
    title: String,
    subtitle: String,
    color: Color,
    onResume: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = CellNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = ChipLabel, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
            }
            OutlinedButton(
                onClick = onResume,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = color.copy(alpha = 0.15f),
                    contentColor = color,
                ),
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Text("Fortsetzen", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Danger.copy(alpha = 0.15f),
                    contentColor = Danger,
                ),
                border = BorderStroke(1.dp, Danger.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Text("✕", fontSize = MaterialTheme.typography.labelMedium.fontSize)
            }
        }
    }
}
