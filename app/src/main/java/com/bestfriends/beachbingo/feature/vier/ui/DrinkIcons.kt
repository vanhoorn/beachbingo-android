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
import com.bestfriends.beachbingo.ui.theme.*

data class DrinkInfo(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color,
)

val DRINKS = listOf(
    DrinkInfo("lager",     "Lager",    "🍺", SandGoldDark),
    DrinkInfo("weizen",    "Weizen",   "🍺", AmberBrown),
    DrinkInfo("dunkles",   "Dunkles",  "🍻", DrinkDunkles),
    DrinkInfo("prosecco",  "Prosecco", "🥂", DrinkProsecco),
    DrinkInfo("rotwein",   "Rotwein",  "🍷", DrinkRotwein),
    DrinkInfo("weisswein", "Weißwein", "🍸", DrinkWeisswein),
    DrinkInfo("rose",      "Rosé",     "🍹", DrinkRose),
    DrinkInfo("whisky",    "Whisky",   "🥃", BurntAmber),
    DrinkInfo("gin",       "Gin",      "🍸", DrinkGin),
    DrinkInfo("rum",       "Rum",      "🍹", DrinkRum),
    DrinkInfo("tequila",   "Tequila",  "🥃", DrinkTequila),
    DrinkInfo("aperol",    "Aperol",   "🍊", BeerOrange),
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
