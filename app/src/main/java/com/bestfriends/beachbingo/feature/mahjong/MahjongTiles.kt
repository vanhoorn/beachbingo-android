package com.bestfriends.beachbingo.feature.mahjong

enum class TileGroup {
    MUSCHELN, WELLEN, FISCHE,
    WINDE, DRACHEN,
    JAHRESZEITEN, BLUMEN
}

data class TileType(
    val id: String,
    val group: TileGroup,
    val rank: Int,
    val label: String,
    val svgIcon: String,
    val color: Long,
    val copies: Int,
)

private const val C_MUSCHELN   = 0xFF0EA5E9L
private const val C_WELLEN     = 0xFF06B6D4L
private const val C_FISCHE     = 0xFF22C55EL
private const val C_WINDE      = 0xFFF59E0BL
private const val C_JAHRESZEIT = 0xFFA855F7L
private const val C_BLUMEN     = 0xFFF97316L

private fun suit(group: TileGroup, color: Long): List<TileType> =
    (1..9).map { i ->
        TileType(
            id = "${group.name.lowercase()}_$i",
            group = group,
            rank = i,
            label = "${group.name.lowercase().replaceFirstChar { it.uppercase() }} $i",
            svgIcon = "${group.name.lowercase()}_$i",
            color = color,
            copies = 4,
        )
    }

val ALL_TILE_TYPES: List<TileType> = buildList {
    addAll(suit(TileGroup.MUSCHELN, C_MUSCHELN))
    addAll(suit(TileGroup.WELLEN,   C_WELLEN))
    addAll(suit(TileGroup.FISCHE,   C_FISCHE))

    add(TileType("wind_ost",  TileGroup.WINDE, 1, "Sonnenaufgang",    "wind_ost",  C_WINDE, 4))
    add(TileType("wind_sued", TileGroup.WINDE, 2, "Palme",            "wind_sued", C_WINDE, 4))
    add(TileType("wind_west", TileGroup.WINDE, 3, "Sonnenuntergang",  "wind_west", C_WINDE, 4))
    add(TileType("wind_nord", TileGroup.WINDE, 4, "Leuchtturm",       "wind_nord", C_WINDE, 4))

    add(TileType("drache_rot",   TileGroup.DRACHEN, 1, "Roter Hai",     "drache_rot",   0xFFEF4444L, 4))
    add(TileType("drache_gruen", TileGroup.DRACHEN, 2, "Blauer Delfin", "drache_gruen", 0xFF22C55EL, 4))
    add(TileType("drache_weiss", TileGroup.DRACHEN, 3, "Oktopus",       "drache_weiss", 0xFFA855F7L, 4))

    add(TileType("jahreszeit_fruehling", TileGroup.JAHRESZEITEN, 1, "Fruehling", "jahreszeit_fruehling", C_JAHRESZEIT, 1))
    add(TileType("jahreszeit_sommer",    TileGroup.JAHRESZEITEN, 2, "Sommer",    "jahreszeit_sommer",    C_JAHRESZEIT, 1))
    add(TileType("jahreszeit_herbst",    TileGroup.JAHRESZEITEN, 3, "Herbst",    "jahreszeit_herbst",    C_JAHRESZEIT, 1))
    add(TileType("jahreszeit_winter",    TileGroup.JAHRESZEITEN, 4, "Winter",    "jahreszeit_winter",    C_JAHRESZEIT, 1))

    add(TileType("blume_hibiskus",     TileGroup.BLUMEN, 1, "Hibiskus",     "blume_hibiskus",     C_BLUMEN, 1))
    add(TileType("blume_anemone",      TileGroup.BLUMEN, 2, "Seeanemone",   "blume_anemone",      C_BLUMEN, 1))
    add(TileType("blume_seerose",      TileGroup.BLUMEN, 3, "Seerose",      "blume_seerose",      C_BLUMEN, 1))
    add(TileType("blume_stranddistel", TileGroup.BLUMEN, 4, "Stranddistel", "blume_stranddistel", C_BLUMEN, 1))
}

private val typeIndex: Map<String, TileType> = ALL_TILE_TYPES.associateBy { it.id }

fun getTileType(id: String): TileType = typeIndex[id] ?: error("Unknown tile type: $id")

fun buildDeck(): List<String> = ALL_TILE_TYPES.flatMap { t -> List(t.copies) { t.id } }

fun tilesMatch(a: String, b: String): Boolean {
    if (a == b) return true
    val ta = getTileType(a)
    val tb = getTileType(b)
    if (ta.group == TileGroup.JAHRESZEITEN && tb.group == TileGroup.JAHRESZEITEN) return true
    if (ta.group == TileGroup.BLUMEN       && tb.group == TileGroup.BLUMEN)       return true
    return false
}
