package com.bestfriends.beachbingo.feature.raetsel

import kotlin.random.Random

// ── Constants ─────────────────────────────────────────────────────────────────

const val BATTLE_GRID = 10

data class ShipDef(val size: Int, val name: String, val emoji: String)

val FLEET_DEFS = listOf(
    ShipDef(5, "Schlachtschiff", "⛴"),
    ShipDef(4, "Kreuzer",        "🚢"),
    ShipDef(3, "Zerstörer",      "🛥"),
    ShipDef(3, "Zerstörer",      "🛥"),
    ShipDef(2, "U-Boot",         "🤿"),
    ShipDef(2, "U-Boot",         "🤿"),
    ShipDef(2, "U-Boot",         "🤿"),
)

// ── Types ──────────────────────────────────────────────────────────────────────

enum class ShotResult { UNKNOWN, MISS, HIT, SUNK }
enum class AiMode { MATROSE, KAPITAEN, ADMIRAL }
enum class BattleTurn { PLAYER, AI }

data class PlacedShip(
    val id: Int,
    val size: Int,
    val row: Int,
    val col: Int,
    val horiz: Boolean,
    val sunk: Boolean = false,
)

data class BattleState(
    val playerFleet: List<PlacedShip>,
    val aiFleet: List<PlacedShip>,
    val playerGrid: Array<Array<ShotResult>>,
    val aiGrid: Array<Array<ShotResult>>,
    val turn: BattleTurn,
    val gameOver: Boolean,
    val winner: BattleTurn? = null,
    val aiHits: List<Pair<Int, Int>> = emptyList(),
    val aiTargets: List<Pair<Int, Int>> = emptyList(),
)

// ── Ship helpers ───────────────────────────────────────────────────────────────

fun shipCells(ship: PlacedShip): List<Pair<Int, Int>> =
    (0 until ship.size).map { i ->
        (ship.row + if (ship.horiz) 0 else i) to (ship.col + if (ship.horiz) i else 0)
    }

fun canPlaceShip(grid: Array<BooleanArray>, r: Int, c: Int, size: Int, horiz: Boolean): Boolean {
    for (i in 0 until size) {
        val nr = r + if (horiz) 0 else i
        val nc = c + if (horiz) i else 0
        if (nr < 0 || nr >= BATTLE_GRID || nc < 0 || nc >= BATTLE_GRID) return false
        for (dr in -1..1) for (dc in -1..1) {
            val tr = nr + dr; val tc = nc + dc
            if (tr in 0 until BATTLE_GRID && tc in 0 until BATTLE_GRID && grid[tr][tc]) return false
        }
    }
    return true
}

fun placeOnGrid(grid: Array<BooleanArray>, ship: PlacedShip) {
    shipCells(ship).forEach { (r, c) -> grid[r][c] = true }
}

fun fleetToGrid(fleet: List<PlacedShip>): Array<BooleanArray> {
    val g = Array(BATTLE_GRID) { BooleanArray(BATTLE_GRID) }
    fleet.forEach { placeOnGrid(g, it) }
    return g
}

// ── AI fleet placement ─────────────────────────────────────────────────────────

fun placeFleetAi(): List<PlacedShip> {
    val grid = Array(BATTLE_GRID) { BooleanArray(BATTLE_GRID) }
    val fleet = mutableListOf<PlacedShip>()
    FLEET_DEFS.forEachIndexed { id, def ->
        repeat(300) {
            val horiz = Random.nextBoolean()
            val r = Random.nextInt(BATTLE_GRID)
            val c = Random.nextInt(BATTLE_GRID)
            if (canPlaceShip(grid, r, c, def.size, horiz)) {
                val ship = PlacedShip(id, def.size, r, c, horiz)
                placeOnGrid(grid, ship)
                fleet.add(ship)
                return@forEachIndexed
            }
        }
    }
    return fleet
}

// ── Create initial state ───────────────────────────────────────────────────────

fun createBattleState(playerFleet: List<PlacedShip>): BattleState {
    val emptyGrid = { Array(BATTLE_GRID) { Array(BATTLE_GRID) { ShotResult.UNKNOWN } } }
    return BattleState(
        playerFleet = playerFleet,
        aiFleet = placeFleetAi(),
        playerGrid = emptyGrid(),
        aiGrid = emptyGrid(),
        turn = BattleTurn.PLAYER,
        gameOver = false,
    )
}

// ── Player shoots ─────────────────────────────────────────────────────────────

fun playerShoot(state: BattleState, r: Int, c: Int): BattleState {
    if (state.turn != BattleTurn.PLAYER || state.gameOver) return state
    if (state.aiGrid[r][c] != ShotResult.UNKNOWN) return state

    val aiGrid = state.aiGrid.map { it.clone() }.toTypedArray()
    var aiFleet = state.aiFleet.toMutableList()

    val hitShip = aiFleet.firstOrNull { ship -> shipCells(ship).any { (sr, sc) -> sr == r && sc == c } }
    if (hitShip != null) {
        aiGrid[r][c] = ShotResult.HIT
        val sunk = shipCells(hitShip).all { (sr, sc) -> aiGrid[sr][sc] != ShotResult.UNKNOWN }
        if (sunk) {
            aiFleet = aiFleet.map { s ->
                if (s === hitShip) s.copy(sunk = true) else s
            }.toMutableList()
            shipCells(hitShip).forEach { (sr, sc) -> aiGrid[sr][sc] = ShotResult.SUNK }
        }
    } else {
        aiGrid[r][c] = ShotResult.MISS
    }

    val allSunk = aiFleet.all { it.sunk }
    return state.copy(
        aiGrid = aiGrid, aiFleet = aiFleet,
        turn = if (allSunk) BattleTurn.PLAYER else if (hitShip != null) BattleTurn.PLAYER else BattleTurn.AI,
        gameOver = allSunk, winner = if (allSunk) BattleTurn.PLAYER else null,
    )
}

// ── AI shoots ────────────────────────────────────────────────────────────────

fun aiShoot(state: BattleState, aiMode: AiMode): BattleState {
    if (state.turn != BattleTurn.AI || state.gameOver) return state

    val cell = pickAiCell(state, aiMode) ?: return state.copy(turn = BattleTurn.PLAYER)
    val (r, c) = cell

    val playerGrid = state.playerGrid.map { it.clone() }.toTypedArray()
    var playerFleet = state.playerFleet.toMutableList()
    var aiHits = state.aiHits.toMutableList()
    var aiTargets = state.aiTargets.toMutableList()

    val hitShip = playerFleet.firstOrNull { ship -> shipCells(ship).any { (sr, sc) -> sr == r && sc == c } }
    if (hitShip != null) {
        playerGrid[r][c] = ShotResult.HIT
        aiHits.add(r to c)
        val sunk = shipCells(hitShip).all { (sr, sc) -> playerGrid[sr][sc] != ShotResult.UNKNOWN }
        if (sunk) {
            playerFleet = playerFleet.map { s -> if (s === hitShip) s.copy(sunk = true) else s }.toMutableList()
            shipCells(hitShip).forEach { (sr, sc) -> playerGrid[sr][sc] = ShotResult.SUNK }
            val sunkSet = shipCells(hitShip).toSet()
            aiHits.removeAll { it in sunkSet }
            aiTargets.clear()
        } else if (aiMode == AiMode.ADMIRAL) {
            aiTargets = buildTargets(aiHits, playerGrid).toMutableList()
        }
    } else {
        playerGrid[r][c] = ShotResult.MISS
        aiTargets.removeAll { it == (r to c) }
    }

    val allSunk = playerFleet.all { it.sunk }
    return state.copy(
        playerGrid = playerGrid, playerFleet = playerFleet,
        turn = if (allSunk) BattleTurn.AI else if (hitShip != null) BattleTurn.AI else BattleTurn.PLAYER,
        gameOver = allSunk, winner = if (allSunk) BattleTurn.AI else null,
        aiHits = aiHits, aiTargets = aiTargets,
    )
}

private fun buildTargets(hits: List<Pair<Int, Int>>, grid: Array<Array<ShotResult>>): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    val seen = mutableSetOf<Pair<Int, Int>>()
    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    for ((hr, hc) in hits) {
        for ((dr, dc) in dirs) {
            val nr = hr + dr; val nc = hc + dc
            val p = nr to nc
            if (nr in 0 until BATTLE_GRID && nc in 0 until BATTLE_GRID
                && grid[nr][nc] == ShotResult.UNKNOWN && p !in seen) {
                result.add(p); seen.add(p)
            }
        }
    }
    return result
}

private fun pickAiCell(state: BattleState, aiMode: AiMode): Pair<Int, Int>? {
    val unknown = (0 until BATTLE_GRID).flatMap { r ->
        (0 until BATTLE_GRID).filter { c -> state.playerGrid[r][c] == ShotResult.UNKNOWN }.map { c -> r to c }
    }
    if (unknown.isEmpty()) return null

    return when (aiMode) {
        AiMode.MATROSE -> unknown.random()

        AiMode.ADMIRAL -> {
            val targets = state.aiTargets
            if (targets.isNotEmpty()) targets.random()
            else {
                val checker = unknown.filter { (r, c) -> (r + c) % 2 == 0 }
                (if (checker.isNotEmpty()) checker else unknown).random()
            }
        }

        AiMode.KAPITAEN -> {
            // Probability density
            val density = Array(BATTLE_GRID) { IntArray(BATTLE_GRID) }
            state.playerFleet.filter { !it.sunk }.forEach { ship ->
                for (r in 0 until BATTLE_GRID) {
                    for (c in 0 until BATTLE_GRID) {
                        listOf(true, false).forEach { horiz ->
                            val cells = (0 until ship.size).map { i ->
                                (r + if (horiz) 0 else i) to (c + if (horiz) i else 0)
                            }
                            if (cells.all { (cr, cc) ->
                                    cr in 0 until BATTLE_GRID && cc in 0 until BATTLE_GRID &&
                                    state.playerGrid[cr][cc] != ShotResult.MISS &&
                                    state.playerGrid[cr][cc] != ShotResult.SUNK
                                }) {
                                val bonus = if (cells.any { (cr, cc) -> state.playerGrid[cr][cc] == ShotResult.HIT }) 5 else 1
                                cells.forEach { (cr, cc) -> density[cr][cc] += bonus }
                            }
                        }
                    }
                }
            }
            unknown.filter { (r, c) -> density[r][c] == unknown.maxOfOrNull { (er, ec) -> density[er][ec] } ?: 0 }.random()
        }
    }
}

// ── Count remaining cells ─────────────────────────────────────────────────────

fun countRemainingCells(fleet: List<PlacedShip>): Int =
    fleet.filter { !it.sunk }.sumOf { it.size }

// ── Placement grid helper (raw params) ────────────────────────────────────────

fun markShipOnGrid(grid: Array<BooleanArray>, r: Int, c: Int, size: Int, horiz: Boolean) {
    for (i in 0 until size) {
        val nr = r + if (horiz) 0 else i
        val nc = c + if (horiz) i else 0
        if (nr in 0 until BATTLE_GRID && nc in 0 until BATTLE_GRID) grid[nr][nc] = true
    }
}

// ── Battle state serialization ────────────────────────────────────────────────

fun serializeBattleState(state: BattleState): String {
    val sb = StringBuilder()
    sb.append(if (state.turn == BattleTurn.PLAYER) "P" else "A")
    sb.append("|")
    for (r in 0 until BATTLE_GRID) for (c in 0 until BATTLE_GRID)
        sb.append(when (state.playerGrid[r][c]) { ShotResult.MISS -> 'M'; ShotResult.HIT -> 'H'; ShotResult.SUNK -> 'S'; else -> 'U' })
    sb.append("|")
    for (r in 0 until BATTLE_GRID) for (c in 0 until BATTLE_GRID)
        sb.append(when (state.aiGrid[r][c]) { ShotResult.MISS -> 'M'; ShotResult.HIT -> 'H'; ShotResult.SUNK -> 'S'; else -> 'U' })
    sb.append("|")
    sb.append(state.playerFleet.joinToString(";") { "${it.id},${it.size},${it.row},${it.col},${if (it.horiz) 1 else 0},${if (it.sunk) 1 else 0}" })
    sb.append("|")
    sb.append(state.aiFleet.joinToString(";") { "${it.id},${it.size},${it.row},${it.col},${if (it.horiz) 1 else 0},${if (it.sunk) 1 else 0}" })
    sb.append("|")
    sb.append(state.aiHits.joinToString(";") { "${it.first},${it.second}" })
    sb.append("|")
    sb.append(state.aiTargets.joinToString(";") { "${it.first},${it.second}" })
    return sb.toString()
}

fun deserializeBattleState(s: String): BattleState? = try {
    val p = s.split("|")
    val turn = if (p[0] == "P") BattleTurn.PLAYER else BattleTurn.AI
    fun parseGrid(str: String): Array<Array<ShotResult>> {
        val g = Array(BATTLE_GRID) { Array(BATTLE_GRID) { ShotResult.UNKNOWN } }
        str.forEachIndexed { i, ch ->
            g[i / BATTLE_GRID][i % BATTLE_GRID] = when (ch) { 'M' -> ShotResult.MISS; 'H' -> ShotResult.HIT; 'S' -> ShotResult.SUNK; else -> ShotResult.UNKNOWN }
        }
        return g
    }
    fun parseFleet(str: String) = if (str.isBlank()) emptyList() else str.split(";").map { e ->
        val v = e.split(","); PlacedShip(v[0].toInt(), v[1].toInt(), v[2].toInt(), v[3].toInt(), v[4] == "1", v[5] == "1")
    }
    fun parsePairs(str: String) = if (str.isBlank()) emptyList() else str.split(";").map { e ->
        val v = e.split(","); v[0].toInt() to v[1].toInt()
    }
    BattleState(
        turn = turn,
        playerGrid = parseGrid(p[1]),
        aiGrid = parseGrid(p[2]),
        playerFleet = parseFleet(p[3]),
        aiFleet = parseFleet(p[4]),
        aiHits = parsePairs(p[5]),
        aiTargets = parsePairs(p[6]),
        gameOver = false,
    )
} catch (_: Exception) { null }

// ── Session state (shared between Placement → Battle) ─────────────────────────

object KuestenkriegSession {
    var playerFleet: List<PlacedShip> = emptyList()
    var aiMode: AiMode = AiMode.KAPITAEN
    var resumedState: BattleState? = null
    var resumedSaveId: String? = null
}
