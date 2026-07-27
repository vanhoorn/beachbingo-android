package com.bestfriends.beachbingo.core.model

enum class PlayerCount(val label: String) {
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
    CARD("Kartenspiel"),
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
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.LOGICAL),
    ),
    GameMetadata(
        id = "pirates",
        emoji = "🐙",
        title = "BeachPirates",
        description = "Verteidige den Strand! Besiege Quallen, Muscheln und Fische.",
        color = 0xFFA855F7,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.ACTION),
    ),
    GameMetadata(
        id = "worm",
        emoji = "🪱",
        title = "Wattwurm",
        description = "Frisst Krabben, Muscheln und Fische. Werde nie die Grenzen!",
        color = 0xFF22C55E,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.ACTION),
    ),
    GameMetadata(
        id = "strandturm",
        emoji = "🗼",
        title = "Strandturm",
        description = "Klettere den Pier hoch, weiche Kokosnüssen aus — bis zum Gipfel!",
        color = 0xFFDC2626,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.ACTION),
    ),
    GameMetadata(
        id = "brandung",
        emoji = "🌊",
        title = "Brandung",
        description = "Schwimm nicht unter! Sammle 31 Punkte mit gleicher Farbe.",
        color = 0xFF0D9488,
        playerCounts = listOf(PlayerCount.ONE_TWO, PlayerCount.TWO_FOUR, PlayerCount.FOUR_PLUS),
        genres = listOf(GameGenre.PARTY, GameGenre.LOGICAL, GameGenre.CARD),
    ),
    GameMetadata(
        id = "meermau",
        emoji = "🂠",
        title = "MeerMau",
        description = "Werde als Erster alle Karten los! Mau-Mau mit Strand-Feeling.",
        color = 0xFF7C3AED,
        playerCounts = listOf(PlayerCount.ONE_TWO, PlayerCount.TWO_FOUR),
        genres = listOf(GameGenre.PARTY, GameGenre.LOGICAL, GameGenre.CARD),
    ),
    GameMetadata(
        id = "strandraeuber",
        emoji = "🦹",
        title = "Strandräuber",
        description = "Karten ziehen & Paare ablegen. Wer hält am Ende den Strandräuber?",
        color = 0xFFE11D48,
        playerCounts = listOf(PlayerCount.ONE_TWO, PlayerCount.TWO_FOUR),
        genres = listOf(GameGenre.PARTY, GameGenre.CARD),
    ),
    // ── Rätsel-Spiele ──────────────────────────────────────────────────────────
    GameMetadata(
        id = "strandoku",
        emoji = "🔢",
        title = "Strandoku",
        description = "Das meistgespielte Logikrätsel der Welt — 6 Varianten von Classic bis Samurai.",
        color = 0xFF38BDF8,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.RIDDLE, GameGenre.LOGICAL),
    ),
    GameMetadata(
        id = "wellensumme",
        emoji = "➕",
        title = "WellenSumme",
        description = "Kreuzworträtsel mit Zahlen — Blöcke addieren sich zur angegebenen Summe.",
        color = 0xFFC084FC,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.RIDDLE, GameGenre.LOGICAL),
    ),
    GameMetadata(
        id = "kuestenkrieg",
        emoji = "⚓",
        title = "Küstenkrieg",
        description = "Solo-Logik-Rätsel oder klassisches 2-Spieler-Duell — Flotten versenken!",
        color = 0xFFFB7185,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.RIDDLE, GameGenre.LOGICAL),
    ),
    GameMetadata(
        id = "duenenschatten",
        emoji = "◼",
        title = "DünenSchatten",
        description = "Schwärze Felder ein — das japanische Zahlen-Ausschluss-Rätsel.",
        color = 0xFFFBBF24,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.RIDDLE, GameGenre.LOGICAL),
    ),
    GameMetadata(
        id = "inselbruecke",
        emoji = "🌉",
        title = "Inselbrücke",
        description = "Verbinde alle Inseln mit Brücken — das japanische Hashi-Rätsel.",
        color = 0xFF4ADE80,
        playerCounts = listOf(PlayerCount.ONE_TWO),
        genres = listOf(GameGenre.RIDDLE, GameGenre.LOGICAL),
    ),
)

val CARD_GAMES: List<GameMetadata> = ALL_GAMES.filter { GameGenre.CARD in it.genres }
val RIDDLE_GAMES: List<GameMetadata> = ALL_GAMES.filter { GameGenre.RIDDLE in it.genres }
