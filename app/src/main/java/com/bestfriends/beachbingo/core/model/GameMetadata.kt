package com.bestfriends.beachbingo.core.model

enum class PlayerCount(val label: String) {
    ONE("1 Spieler"),
    ONE_TWO("1-2 Spieler"),
    TWO_FOUR("2-4 Spieler"),
    FOUR_PLUS("4+ Spieler"),
}

enum class GameGenre(val label: String) {
    ACTION("Action"),
    PARTY("Party"),
    LOGICAL("Logical"),
    COUCH("Couch"),
    RIDDLE("Riddle"),
}

data class GameMetadata(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val color: Long,
    val playerCounts: List<PlayerCount>,
    val genres: List<GameGenre>,
)

val ALL_GAMES: List<GameMetadata> = listOf(
    GameMetadata(
        id = "bingo",
        emoji = "🎱",
        title = "BeachBingo",
        description = "Ziehe Zahlen, markiere deine Karte – BINGO!",
        color = 0xFF0EA5E9,
        playerCounts = listOf(PlayerCount.TWO_FOUR, PlayerCount.FOUR_PLUS),
        genres = listOf(GameGenre.PARTY),
    ),
    GameMetadata(
        id = "pong",
        emoji = "🏓",
        title = "BeachVolley",
        description = "Klassisches Volleyball am Strand – wer gewinnt die Runde?",
        color = 0xFFF97316,
        playerCounts = listOf(PlayerCount.ONE_TWO, PlayerCount.TWO_FOUR),
        genres = listOf(GameGenre.ACTION, GameGenre.PARTY),
    ),
    GameMetadata(
        id = "vier",
        emoji = "🍺",
        title = "Vier4Bier",
        description = "Vier in einer Reihe mit Beach-Twist.",
        color = 0xFFF59E0B,
        playerCounts = listOf(PlayerCount.ONE, PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.LOGICAL),
    ),
    GameMetadata(
        id = "pirates",
        emoji = "🐙",
        title = "BeachPirates",
        description = "Verteidige den Strand! Besiege Quallen, Muscheln und Fische.",
        color = 0xFFA855F7,
        playerCounts = listOf(PlayerCount.ONE),
        genres = listOf(GameGenre.ACTION),
    ),
    GameMetadata(
        id = "worm",
        emoji = "🪱",
        title = "Wattwurm",
        description = "Frisst Krabben, Muscheln und Fische. Werde nie die Grenzen!",
        color = 0xFF22C55E,
        playerCounts = listOf(PlayerCount.ONE),
        genres = listOf(GameGenre.ACTION),
    ),
    GameMetadata(
        id = "strandturm",
        emoji = "🗼",
        title = "Strandturm",
        description = "Klettere den Pier hoch, weiche Kokosnüssen aus — bis zum Gipfel!",
        color = 0xFFDC2626,
        playerCounts = listOf(PlayerCount.ONE),
        genres = listOf(GameGenre.ACTION),
    ),
    GameMetadata(
        id = "brandung",
        emoji = "🌊",
        title = "Brandung",
        description = "Schwimm nicht unter! Sammle 31 Punkte mit gleicher Farbe.",
        color = 0xFF0D9488,
        playerCounts = listOf(PlayerCount.ONE_TWO, PlayerCount.TWO_FOUR, PlayerCount.FOUR_PLUS),
        genres = listOf(GameGenre.PARTY, GameGenre.LOGICAL),
    ),
)
