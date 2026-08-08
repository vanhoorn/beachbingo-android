package com.bestfriends.beachbingo.feature.mahjong

enum class LayoutId(val label: String, val emoji: String) {
    SCHILDKROETE("Schildkroete", "🐢"),
    PYRAMIDE("Pyramide", "🔺"),
    KREUZ("Kreuz", "✚"),
    DRACHEN("Drachen", "🐉"),
    LEUCHTTURM("Leuchtturm", "🗼"),
}

data class LayoutDef(
    val id: LayoutId,
    val positions: List<Triple<Int, Int, Int>>, // col, row, layer
    val tileCount: Int,
)

private fun rect(cols: Int, rows: Int, startCol: Int, startRow: Int, layer: Int): List<Triple<Int, Int, Int>> =
    (0 until rows).flatMap { r -> (0 until cols).map { c -> Triple(startCol + c * 2, startRow + r * 2, layer) } }

// ── Schildkroete (144 tiles) ──────────────────────────────────────────────────
private fun turtle144(): List<Triple<Int, Int, Int>> = buildList {
    for (r in 0 until 4) for (c in 0 until 12) add(Triple(c * 2, r * 2, 0))
    add(Triple(-2, 2, 0)); add(Triple(24, 2, 0))
    add(Triple(10, 8, 0)); add(Triple(12, 8, 0))
    for (r in 0 until 3) for (c in 0 until 10) add(Triple(2 + c * 2, 1 + r * 2, 1))
    for (r in 0 until 2) for (c in 0 until 8)  add(Triple(2 + c * 2, 2 + r * 2, 2))
    for (c in 0 until 4) add(Triple(4 + c * 2, 3, 3))
    add(Triple(6, 4, 4)); add(Triple(8, 4, 4))
    add(Triple(7, 5, 5))
    for (c in 0 until 8) add(Triple(2 + c * 2, 9, 0))
    for (c in 0 until 6) add(Triple(4 + c * 2, 11, 0))
    for (c in 0 until 6) add(Triple(4 + c * 2, 13, 0))
    for (c in 1 until 10) add(Triple(c * 2, -2, 0))
    for (c in 1 until 11) add(Triple(c * 2, -4, 0))
}

// ── Pyramide (136 tiles) ──────────────────────────────────────────────────────
// 5-layer step pyramid: (w=12,h=5)+(10,4)+(8,2)+(6,2)+(4,2) = 60+40+16+12+8 = 136
private fun pyramide136(): List<Triple<Int, Int, Int>> = buildList {
    val configs = listOf(12 to 5, 10 to 4, 8 to 2, 6 to 2, 4 to 2)
    configs.forEachIndexed { layer, (w, h) ->
        val off = (12 - w)   // center each layer within maxWidth=12
        for (r in 0 until h) for (c in 0 until w) add(Triple((off + c) * 2, r * 2, layer))
    }
}

// ── Kreuz ─────────────────────────────────────────────────────────────────────
private fun kreuz140(): List<Triple<Int, Int, Int>> = buildList {
    for (r in 0 until 3) for (c in 0 until 14) add(Triple(c * 2, (3 + r) * 2, 0))
    for (r in 0 until 9) for (c in 0 until 4)
        if (r < 3 || r > 5) add(Triple((5 + c) * 2, r * 2, 0))
    for (r in 0 until 3) for (c in 0 until 4) add(Triple((5 + c) * 2, (3 + r) * 2, 1))
    add(Triple(12, 8, 2)); add(Triple(14, 8, 2))
}

// ── Drachen ───────────────────────────────────────────────────────────────────
private fun drachen144(): List<Triple<Int, Int, Int>> = buildList {
    for (i in 0 until 8) add(Triple(i * 2, i * 2, 0))
    for (i in 1 until 7) { add(Triple(i * 2, (i - 1) * 2, 0)); add(Triple(i * 2, (i + 1) * 2, 0)) }
    for (r in 0 until 3) for (c in 0 until 4) add(Triple(c * 2, r * 2, 0))
    for (i in 8 until 12) add(Triple(i * 2, i * 2, 0))
    for (i in 2 until 6) { add(Triple((i + 2) * 2, (i - 2) * 2, 0)); add(Triple((i - 2) * 2, (i + 2) * 2, 0)) }
    for (i in 2 until 6) add(Triple(i * 2, i * 2, 1))
    add(Triple(6, 6, 2)); add(Triple(8, 8, 2))
    for (c in 0 until 3) add(Triple(c * 2, 8, 0))
    for (c in 0 until 3) add(Triple(14 + c * 2, 2, 0))
    for (r in 0 until 5) add(Triple(20 + r * 2, r * 2, 0))
    for (r in 0 until 5) add(Triple(r * 2, 18 + r * 2, 0))
    for (r in 0 until 4) add(Triple(22 + r * 2, 2 + r * 2, 0))
}

// ── Leuchtturm ────────────────────────────────────────────────────────────────
private fun leuchtturm(): List<Triple<Int, Int, Int>> = buildList {
    for (r in 0 until 2) for (c in 0 until 6) add(Triple(c * 2, (8 + r) * 2, 0))
    val towerWidths = listOf(4, 4, 4, 3, 3, 2, 2, 1, 1)
    towerWidths.forEachIndexed { layer, w ->
        val off = (6 - w) / 2
        for (c in 0 until w) add(Triple((off + c) * 2, (7 - layer) * 2, layer))
    }
    add(Triple(4, 0, 8)); add(Triple(6, 0, 8))
    for (c in 0 until 8) add(Triple(c * 2 - 2, 20, 0))
    for (c in 0 until 6) add(Triple(c * 2, 22, 0))
    for (c in 0 until 4) { add(Triple(c * 2 - 2, 18, 0)); add(Triple(12 + c * 2, 18, 0)) }
}

val LAYOUT_DEFS: Map<LayoutId, LayoutDef> = mapOf(
    LayoutId.SCHILDKROETE to LayoutDef(LayoutId.SCHILDKROETE, turtle144(),   144),
    LayoutId.PYRAMIDE     to LayoutDef(LayoutId.PYRAMIDE,     pyramide136(), 136),
    LayoutId.KREUZ        to LayoutDef(LayoutId.KREUZ,        kreuz140(),    140),
    LayoutId.DRACHEN      to LayoutDef(LayoutId.DRACHEN,      drachen144(),  144),
    LayoutId.LEUCHTTURM   to LayoutDef(LayoutId.LEUCHTTURM,   leuchtturm(),  120),
)

val LAYOUT_ORDER = listOf(
    LayoutId.SCHILDKROETE, LayoutId.PYRAMIDE, LayoutId.KREUZ,
    LayoutId.DRACHEN, LayoutId.LEUCHTTURM,
)
