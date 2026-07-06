package com.bestfriends.beachbingo.feature.vier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

data class DrinkInfo(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color,
)

val DRINKS = listOf(
    DrinkInfo("lager",     "Lager",    "🍺", Color(0xFFD97706)),
    DrinkInfo("weizen",    "Weizen",   "🍺", Color(0xFFB45309)),
    DrinkInfo("dunkles",   "Dunkles",  "🍻", Color(0xFF6B2600)),
    DrinkInfo("prosecco",  "Prosecco", "🥂", Color(0xFFA37C00)),
    DrinkInfo("rotwein",   "Rotwein",  "🍷", Color(0xFFB91C1C)),
    DrinkInfo("weisswein", "Weißwein", "🍸", Color(0xFFA16207)),
    DrinkInfo("rose",      "Rosé",     "🍹", Color(0xFFBE185D)),
    DrinkInfo("whisky",    "Whisky",   "🥃", Color(0xFF92400E)),
    DrinkInfo("gin",       "Gin",      "🍸", Color(0xFF0369A1)),
    DrinkInfo("rum",       "Rum",      "🍹", Color(0xFF7C2D12)),
    DrinkInfo("tequila",   "Tequila",  "🥃", Color(0xFF3F6212)),
    DrinkInfo("aperol",    "Aperol",   "🍊", Color(0xFFC2410C)),
)

fun getDrink(id: String): DrinkInfo = DRINKS.find { it.id == id } ?: DRINKS[0]

@Composable
fun DrinkPiece(drinkId: String, size: Dp, modifier: Modifier = Modifier) {
    val drink = getDrink(drinkId)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(drink.color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = drink.emoji,
            fontSize = (size.value * 0.5f).sp,
        )
    }
}
