package com.bestfriends.beachbingo.feature.shared

val TEAM_NAMES = listOf(
    "Die Strandflöhe", "Team Ebbe", "Die Wellenreiter", "Sandburg-Crew",
    "Die Seesterne", "Team Flut", "Die Möwen", "Strandbande",
    "Team Muschel", "Die Krabbe", "Team Riff", "Die Piraten",
    "Sandkorn-Gang", "Die Delfine", "Team Welle", "Brandungsgang",
)

fun teamName(key: String): String {
    var hash = 0
    for (c in key) hash = (hash * 31 + c.code) and 0x7fffffff
    return TEAM_NAMES[hash % TEAM_NAMES.size]
}

fun rankEmoji(rank: Int, isLast: Boolean, total: Int): String = when {
    rank == 0              -> "🥇"
    rank == 1 && total > 2 -> "🥈"
    rank == 2 && total > 3 -> "🥉"
    isLast && total > 2    -> "🦀"
    else                   -> "${rank + 1}."
}
