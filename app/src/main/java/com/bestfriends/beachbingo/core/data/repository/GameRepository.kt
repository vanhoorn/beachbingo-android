package com.bestfriends.beachbingo.core.data.repository

import com.bestfriends.beachbingo.core.model.BingoGame
import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.core.model.GameResult
import com.bestfriends.beachbingo.core.model.User
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGame(gameId: String): Flow<BingoGame>
    fun observeUserGames(userId: String): Flow<List<BingoGame>>
    fun observeUserResults(userId: String): Flow<List<GameResult>>
    suspend fun createGame(user: User, gameMode: GameMode, drawStyle: DrawStyle): Result<String>
    suspend fun joinGame(gameId: String, user: User): Result<Unit>
    suspend fun startGame(gameId: String): Result<Unit>
    suspend fun startDrawAnimation(gameId: String): Result<Unit>
    suspend fun drawNumber(gameId: String): Result<Int>
    suspend fun markNumber(gameId: String, userId: String, number: Int): Result<Boolean>
    suspend fun claimBingo(gameId: String, userId: String): Result<Boolean>
    suspend fun leaveGame(gameId: String, userId: String): Result<Unit>
    suspend fun deleteGame(gameId: String): Result<Unit>
    suspend fun eliminateNumber(gameId: String, number: Int): Result<Unit>
    suspend fun clearEliminationAnimation(gameId: String): Result<Unit>
}
