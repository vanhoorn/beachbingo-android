package com.bestfriends.beachbingo.feature.raetsel

import org.json.JSONArray
import org.json.JSONObject

val HASHI_GRID_SIZES = mapOf("leicht" to 7, "mittel" to 9, "schwer" to 11, "experte" to 13)
val HASHI_ISLAND_COUNTS = mapOf("leicht" to 8, "mittel" to 14, "schwer" to 20, "experte" to 28)

data class HashiIsland(val id: Int, val row: Int, val col: Int, val value: Int)
data class HashiBridge(val from: Int, val to: Int, val count: Int)

data class HashiPuzzle(
    val gridSize: Int,
    val islands: List<HashiIsland>,
)

data class HashiState(
    val puzzle: HashiPuzzle,
    val bridges: List<HashiBridge>,
    val solved: Boolean = false,
)

// ── Generation ────────────────────────────────────────────────────────────────

private fun crossesExisting(
    r1: Int, c1: Int, r2: Int, c2: Int,
    islands: List<HashiIsland>, existing: List<HashiBridge>,
): Boolean {
    val horiz = r1 == r2
    // Check if any island lies on the bridge path (not the endpoints)
    for (isl in islands) {
        if (horiz) {
            if (isl.row == r1 && isl.col in (minOf(c1,c2)+1 until maxOf(c1,c2)) && !(isl.row==r1 && (isl.col==c1||isl.col==c2))) return true
        } else {
            if (isl.col == c1 && isl.row in (minOf(r1,r2)+1 until maxOf(r1,r2)) && !(isl.col==c1 && (isl.row==r1||isl.row==r2))) return true
        }
    }
    // Check crossing bridges
    for (b in existing) {
        val a = islands.find { it.id == b.from }!!
        val bb = islands.find { it.id == b.to }!!
        val bHoriz = a.row == bb.row
        if (horiz == bHoriz) continue // parallel can't cross
        // One horizontal, one vertical – check if they cross
        val hr: Int; val hc1: Int; val hc2: Int
        val vc: Int; val vr1: Int; val vr2: Int
        if (horiz) {
            hr = r1; hc1 = minOf(c1,c2); hc2 = maxOf(c1,c2)
            vc = a.col; vr1 = minOf(a.row,bb.row); vr2 = maxOf(a.row,bb.row)
        } else {
            hr = a.row; hc1 = minOf(a.col,bb.col); hc2 = maxOf(a.col,bb.col)
            vc = c1; vr1 = minOf(r1,r2); vr2 = maxOf(r1,r2)
        }
        if (vc in (hc1+1 until hc2) && hr in (vr1+1 until vr2)) return true
    }
    return false
}

private fun isFullyConnected(islands: List<HashiIsland>, bridges: List<HashiBridge>): Boolean {
    if (islands.isEmpty()) return true
    val adj = mutableMapOf<Int,MutableSet<Int>>()
    for (isl in islands) adj[isl.id] = mutableSetOf()
    for (b in bridges) { adj[b.from]!!.add(b.to); adj[b.to]!!.add(b.from) }
    val visited = mutableSetOf(islands[0].id)
    val queue = ArrayDeque<Int>()
    queue.add(islands[0].id)
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        for (nb in adj[cur] ?: emptySet()) {
            if (visited.add(nb)) queue.add(nb)
        }
    }
    return visited.size == islands.size
}

private fun tryGenerateHashi(size: Int, targetIslands: Int, rng: Mulberry32): HashiPuzzle? {
    val islands = mutableListOf<HashiIsland>()
    val solution = mutableListOf<HashiBridge>()
    var nextId = 0

    // Place first island
    val startR = rng.nextInt(size); val startC = rng.nextInt(size)
    islands.add(HashiIsland(nextId++, startR, startC, 0))

    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    repeat(targetIslands - 1) {
        val from = islands.random(rng)
        val (dr, dc) = dirs[rng.nextInt(4)]
        val dist = rng.nextInt(size / 2 - 1) + 2
        val nr = from.row + dr * dist; val nc = from.col + dc * dist
        if (nr !in 0 until size || nc !in 0 until size) return@repeat
        if (islands.any { it.row == nr && it.col == nc }) return@repeat
        val bridgeCount = if (rng.next() < 0.4) 2 else 1
        if (crossesExisting(from.row, from.col, nr, nc, islands, solution)) return@repeat
        val newIsland = HashiIsland(nextId++, nr, nc, 0)
        islands.add(newIsland)
        solution.add(HashiBridge(from.id, newIsland.id, bridgeCount))
    }

    if (islands.size < 3) return null
    if (!isFullyConnected(islands, solution)) return null

    // Compute island values from solution
    val valueMap = mutableMapOf<Int,Int>()
    for (b in solution) {
        valueMap[b.from] = (valueMap[b.from] ?: 0) + b.count
        valueMap[b.to] = (valueMap[b.to] ?: 0) + b.count
    }

    val finalIslands = islands.map { it.copy(value = valueMap[it.id] ?: 0) }
    return HashiPuzzle(size, finalIslands)
}

// We store the solution separately via a companion approach — for Android simplicity,
// we embed the solution into the puzzle via a list of "solution bridges" approach
// stored in a wrapper.
data class HashiPuzzleWithSolution(val puzzle: HashiPuzzle, val solution: List<HashiBridge>)

fun generateHashiWithSolution(difficulty: String, seed: Int): HashiPuzzleWithSolution {
    val size = HASHI_GRID_SIZES[difficulty] ?: 9
    val targetIslands = HASHI_ISLAND_COUNTS[difficulty] ?: 14
    val rng = Mulberry32(seed)

    for (attempt in 0 until 30) {
        val result = tryGenerateHashiWithSolution(size, targetIslands, rng)
        if (result != null) return result
    }
    return generateHashiWithSolution(difficulty, seed + 99999)
}

private fun <T> List<T>.random(rng: Mulberry32): T = this[rng.nextInt(this.size)]

private fun tryGenerateHashiWithSolution(size: Int, targetIslands: Int, rng: Mulberry32): HashiPuzzleWithSolution? {
    val islands = mutableListOf<HashiIsland>()
    val solution = mutableListOf<HashiBridge>()
    var nextId = 0

    val startR = rng.nextInt(size); val startC = rng.nextInt(size)
    islands.add(HashiIsland(nextId++, startR, startC, 0))

    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    repeat(targetIslands * 3) {
        if (islands.size >= targetIslands) return@repeat
        val from = islands.random(rng)
        val (dr, dc) = dirs[rng.nextInt(4)]
        val dist = rng.nextInt(size / 2) + 2
        val nr = from.row + dr * dist; val nc = from.col + dc * dist
        if (nr !in 0 until size || nc !in 0 until size) return@repeat
        if (islands.any { it.row == nr && it.col == nc }) return@repeat
        // Reject if the new island's position lies on an existing solution bridge path,
        // which would block that bridge and make the puzzle unsolvable.
        val onExistingBridge = solution.any { b ->
            val a = islands.find { it.id == b.from }!!
            val bb = islands.find { it.id == b.to }!!
            val horiz = a.row == bb.row
            if (horiz) nr == a.row && nc in (minOf(a.col, bb.col) + 1 until maxOf(a.col, bb.col))
            else nc == a.col && nr in (minOf(a.row, bb.row) + 1 until maxOf(a.row, bb.row))
        }
        if (onExistingBridge) return@repeat
        val bridgeCount = if (rng.next() < 0.4) 2 else 1
        if (crossesExisting(from.row, from.col, nr, nc, islands, solution)) return@repeat
        val newIsland = HashiIsland(nextId++, nr, nc, 0)
        islands.add(newIsland)
        solution.add(HashiBridge(from.id, newIsland.id, bridgeCount))
    }

    if (islands.size < 3) return null
    if (!isFullyConnected(islands, solution)) return null

    val valueMap = mutableMapOf<Int,Int>()
    for (b in solution) {
        valueMap[b.from] = (valueMap[b.from] ?: 0) + b.count
        valueMap[b.to] = (valueMap[b.to] ?: 0) + b.count
    }
    val finalIslands = islands.map { it.copy(value = valueMap[it.id] ?: 0) }
    return HashiPuzzleWithSolution(HashiPuzzle(size, finalIslands), solution)
}

// ── State management ──────────────────────────────────────────────────────────

fun createHashiState(puzzle: HashiPuzzle): HashiState = HashiState(puzzle, emptyList())

fun getNeighborIslands(puzzle: HashiPuzzle, island: HashiIsland, bridges: List<HashiBridge>): List<HashiIsland> {
    val result = mutableListOf<HashiIsland>()
    val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    for ((dr, dc) in dirs) {
        var r = island.row + dr; var c = island.col + dc
        while (r in 0 until puzzle.gridSize && c in 0 until puzzle.gridSize) {
            val found = puzzle.islands.find { it.row == r && it.col == c }
            if (found != null) { result.add(found); break }
            // Check if any existing bridge crosses this path
            val horiz = dr == 0
            var blocked = false
            for (b in bridges) {
                val a = puzzle.islands.find { it.id == b.from }!!
                val bb = puzzle.islands.find { it.id == b.to }!!
                val bHoriz = a.row == bb.row
                if (horiz == bHoriz) continue
                val hr: Int; val hc1: Int; val hc2: Int; val vc: Int; val vr1: Int; val vr2: Int
                if (horiz) {
                    hr = island.row; hc1 = minOf(island.col, c); hc2 = maxOf(island.col, c)
                    vc = a.col; vr1 = minOf(a.row,bb.row); vr2 = maxOf(a.row,bb.row)
                } else {
                    hr = a.row; hc1 = minOf(a.col,bb.col); hc2 = maxOf(a.col,bb.col)
                    vc = island.col; vr1 = minOf(island.row,r); vr2 = maxOf(island.row,r)
                }
                if (vc in (hc1+1 until hc2) && hr in (vr1+1 until vr2)) { blocked = true; break }
            }
            if (blocked) break
            r += dr; c += dc
        }
    }
    return result
}

fun getBridgeCount(bridges: List<HashiBridge>, fromId: Int, toId: Int): Int =
    bridges.find { (it.from == fromId && it.to == toId) || (it.from == toId && it.to == fromId) }?.count ?: 0

fun islandBridgeSum(island: HashiIsland, bridges: List<HashiBridge>): Int =
    bridges.filter { it.from == island.id || it.to == island.id }.sumOf { it.count }

fun toggleHashiBridge(state: HashiState, fromId: Int, toId: Int): HashiState {
    val current = getBridgeCount(state.bridges, fromId, toId)
    val newCount = (current + 1) % 3
    val bridges = state.bridges.filter { !((it.from == fromId && it.to == toId) || (it.from == toId && it.to == fromId)) }.toMutableList()
    if (newCount > 0) {
        // Validate no crossing
        val from = state.puzzle.islands.find { it.id == fromId }!!
        val to = state.puzzle.islands.find { it.id == toId }!!
        if (newCount > 0 && crossesExisting(from.row, from.col, to.row, to.col, state.puzzle.islands, bridges)) {
            return state // can't place
        }
        bridges.add(HashiBridge(fromId, toId, newCount))
    }
    val solved = checkHashiSolved(state.puzzle, bridges)
    return state.copy(bridges = bridges, solved = solved)
}

private fun checkHashiSolved(puzzle: HashiPuzzle, bridges: List<HashiBridge>): Boolean {
    for (isl in puzzle.islands) if (islandBridgeSum(isl, bridges) != isl.value) return false
    return isFullyConnected(puzzle.islands, bridges)
}

fun hashiAllSumsCorrect(puzzle: HashiPuzzle, bridges: List<HashiBridge>): Boolean =
    bridges.isNotEmpty() && puzzle.islands.all { islandBridgeSum(it, bridges) == it.value }

fun getHashiHint(state: HashiState, solution: List<HashiBridge>): Pair<Int,Int>? {
    for (sol in solution) {
        val cur = getBridgeCount(state.bridges, sol.from, sol.to)
        if (cur < sol.count) return sol.from to sol.to
    }
    return null
}

// ── Serialization ─────────────────────────────────────────────────────────────

fun serializeHashiState(state: HashiState): String {
    val arr = JSONArray()
    for (b in state.bridges) {
        arr.put(JSONObject().put("from", b.from).put("to", b.to).put("count", b.count))
    }
    return JSONObject().put("bridges", arr).toString()
}

fun deserializeHashiState(puzzle: HashiPuzzle, solution: List<HashiBridge>, raw: String): HashiState {
    return try {
        val obj = JSONObject(raw)
        val arr = obj.getJSONArray("bridges")
        val bridges = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HashiBridge(o.getInt("from"), o.getInt("to"), o.getInt("count"))
        }
        val solved = checkHashiSolved(puzzle, bridges)
        HashiState(puzzle, bridges, solved)
    } catch (_: Exception) {
        createHashiState(puzzle)
    }
}
