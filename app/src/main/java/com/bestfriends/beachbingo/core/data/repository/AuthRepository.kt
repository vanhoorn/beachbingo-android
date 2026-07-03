package com.bestfriends.beachbingo.core.data.repository

import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String, avatar: String): Result<User>
    suspend fun signOut()
    suspend fun updateProfile(displayName: String, avatarUrl: String = ""): Result<Unit>
    suspend fun updateGamePreferences(gameMode: GameMode, drawStyle: DrawStyle, eliminationInterval: Int): Result<Unit>
    suspend fun updateEmail(newEmail: String, currentPassword: String): Result<Unit>
}
