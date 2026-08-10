package com.bestfriends.beachbingo.feature.bingo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bestfriends.beachbingo.core.model.BingoCard
import com.bestfriends.beachbingo.ui.theme.BingoCallSize
import com.bestfriends.beachbingo.ui.theme.CellNumber
import com.bestfriends.beachbingo.ui.theme.FreeCellBrown
import com.bestfriends.beachbingo.ui.theme.FreeCellYellow

private val COLUMN_LABELS = listOf("B", "I", "N", "G", "O")

@Composable
fun BingoCardView(
    card: BingoCard,
    drawnNumbers: List<Int>,
    onNumberClick: (Int) -> Unit,
    interactive: Boolean = true,
    autoMarkWithDrawn: Boolean = false,
    highlightDrawn: Boolean = true,
    modifier: Modifier = Modifier
) {
    val effectiveMarked = if (autoMarkWithDrawn) {
        card.markedNumbers + drawnNumbers.toSet()
    } else {
        card.markedNumbers
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Column header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            COLUMN_LABELS.forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Grid rows
        card.grid.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEachIndexed { colIndex, number ->
                    val isFree = number == 0
                    val isMarked = number in effectiveMarked
                    val isDrawn = number in drawnNumbers || isFree

                    BingoCell(
                        number = number,
                        isFree = isFree,
                        isMarked = isMarked,
                        isDrawn = isDrawn,
                        showDrawnHighlight = highlightDrawn,
                        onClick = { if (interactive && isDrawn && !isMarked) onNumberClick(number) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BingoCell(
    number: Int,
    isFree: Boolean,
    isMarked: Boolean,
    isDrawn: Boolean,
    showDrawnHighlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isFree -> FreeCellYellow
        isMarked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isFree -> FreeCellBrown
        isMarked -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        isDrawn && showDrawnHighlight && !isMarked && !isFree ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val borderWidth = if (isDrawn && showDrawnHighlight && !isMarked && !isFree) 2.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .clickable(enabled = !isMarked && isDrawn && !isFree, onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isFree) "★" else number.toString(),
            color = textColor,
            fontSize = if (isFree) BingoCallSize else CellNumber,
            fontWeight = if (isMarked) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}
