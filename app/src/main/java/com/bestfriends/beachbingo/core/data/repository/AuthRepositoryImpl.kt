package com.bestfriends.beachbingo.core.data.repository

import com.bestfriends.beachbingo.core.model.DrawStyle
import com.bestfriends.beachbingo.core.model.GameMode
import com.bestfriends.beachbingo.core.model.PiratesDifficulty
import com.bestfriends.beachbingo.core.model.PongDifficulty
import com.bestfriends.beachbingo.core.model.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val fbUser = firebaseAuth.currentUser
            firestoreListener?.remove()
            firestoreListener = null

            if (fbUser == null) {
                trySend(null)
            } else {
                firestoreListener = firestore.collection("users").document(fbUser.uid)
                    .addSnapshotListener { doc, _ ->
                        val data = doc?.data
                        val user = if (data != null) {
                            User(
                                uid = fbUser.uid,
                                email = fbUser.email ?: "",
                                displayName = data["displayName"] as? String ?: fbUser.displayName ?: "",
                                avatarUrl = data["avatarUrl"] as? String ?: "",
                                age = (data["age"] as? Long)?.toInt() ?: 0,
                                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                                preferredGameMode = (data["preferredGameMode"] as? String)?.let {
                                    runCatching { GameMode.valueOf(it) }.getOrDefault(GameMode.MANUAL_MARK)
                                } ?: GameMode.MANUAL_MARK,
                                preferredDrawStyle = (data["preferredDrawStyle"] as? String)?.let {
                                    runCatching { DrawStyle.valueOf(it) }.getOrDefault(DrawStyle.INSTANT)
                                } ?: DrawStyle.INSTANT,
                                bossLevelEliminationInterval = (data["bossLevelEliminationInterval"] as? Long)?.toInt() ?: 5,
                                preferredPongDifficulty = (data["preferredPongDifficulty"] as? String)?.let {
                                    runCatching { PongDifficulty.valueOf(it) }.getOrNull()
                                },
                                preferredPongScoreLimit = (data["preferredPongScoreLimit"] as? Long)?.toInt(),
                                preferredPongPaddles = (data["preferredPongPaddles"] as? Long)?.toInt(),
                                preferredPiratesDifficulty = (data["preferredPiratesDifficulty"] as? String)?.let {
                                    runCatching { PiratesDifficulty.valueOf(it) }.getOrNull()
                                },
                                preferredPiratesFireRate = (data["preferredPiratesFireRate"] as? Long)?.toInt(),
                                preferredPiratesControlMode = data["preferredPiratesControlMode"] as? String,
                                piratesHighScores = (data["piratesHighScores"] as? Map<*, *>)
                                    ?.mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? Long)?.let { score -> key to score } } }
                                    ?.toMap() ?: emptyMap(),
                                preferredStrandturmControlMode = data["preferredStrandturmControlMode"] as? String,
                                strandturmHighScore = (data["strandturmHighScore"] as? Long)?.toInt() ?: 0,
                                strandturmBestLevel = (data["strandturmBestLevel"] as? Long)?.toInt() ?: 0,
                                soundEnabled = data["soundEnabled"] as? Boolean ?: true,
                                musicEnabled = data["musicEnabled"] as? Boolean ?: true,
                            )
                        } else {
                            User(uid = fbUser.uid, email = fbUser.email ?: "", displayName = fbUser.displayName ?: "")
                        }
                        trySend(user)
                    }
            }
        }

        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListener?.remove()
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val resolvedEmail = if (email.contains("@")) {
            email
        } else {
            // Try the public-readable displaynames lookup first (direct document read).
            // Requires Firestore rule: match /usernames/{n} { allow read: if true; }
            val nameDoc = try {
                firestore.collection("usernames")
                    .document(email.trim().lowercase())
                    .get().await()
            } catch (_: Exception) { null }

            if (nameDoc != null && nameDoc.exists()) {
                nameDoc.getString("email")
                    ?: error("Kein Benutzer mit diesem Anzeigenamen gefunden")
            } else {
                // Fallback: direct collection query (works only if Firestore rules allow it)
                val query = try {
                    firestore.collection("users")
                        .whereEqualTo("displayName", email.trim())
                        .get().await()
                } catch (_: Exception) { null }
                query?.documents?.firstOrNull()?.getString("email")
                    ?: error("Anzeigename nicht gefunden. Bitte E-Mail-Adresse verwenden.")
            }
        }
        val result = auth.signInWithEmailAndPassword(resolvedEmail, password).await()
        val uid = result.user!!.uid
        val doc = firestore.collection("users").document(uid).get().await()
        val user = doc.toObject(User::class.java) ?: User(uid = uid, email = resolvedEmail)

        // Bootstrap the displaynames lookup so future logins by display name work.
        if (user.displayName.isNotBlank()) {
            try {
                firestore.collection("usernames")
                    .document(user.displayName.trim().lowercase())
                    .set(mapOf("email" to resolvedEmail), SetOptions.merge())
                    .await()
            } catch (_: Exception) {}
        }
        user
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        avatar: String
    ): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val fbUser = result.user!!

        fbUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        ).await()

        val user = User(uid = fbUser.uid, email = email, displayName = displayName, avatarUrl = avatar)
        firestore.collection("users").document(fbUser.uid).set(user).await()

        // Write the displaynames lookup so the user can log in with display name later.
        try {
            firestore.collection("usernames")
                .document(displayName.trim().lowercase())
                .set(mapOf("email" to email))
                .await()
        } catch (_: Exception) {}
        user
    }

    override suspend fun signOut() = auth.signOut()

    override suspend fun updateProfile(displayName: String, avatarUrl: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Nicht eingeloggt")
        firestore.collection("users").document(uid)
            .set(mapOf("displayName" to displayName, "avatarUrl" to avatarUrl), SetOptions.merge())
            .await()
        auth.currentUser?.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        )?.await()
    }

    override suspend fun updateEmail(newEmail: String, currentPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Nicht eingeloggt")
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential).await()
        user.verifyBeforeUpdateEmail(newEmail).await()
        firestore.collection("users").document(user.uid)
            .set(mapOf("email" to newEmail), SetOptions.merge()).await()
    }

    override suspend fun updateGamePreferences(gameMode: GameMode, drawStyle: DrawStyle, eliminationInterval: Int): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Nicht eingeloggt")
        firestore.collection("users").document(uid)
            .set(
                mapOf(
                    "preferredGameMode" to gameMode.name,
                    "preferredDrawStyle" to drawStyle.name,
                    "bossLevelEliminationInterval" to eliminationInterval
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun updateAudioPreferences(soundEnabled: Boolean, musicEnabled: Boolean): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Nicht eingeloggt")
        firestore.collection("users").document(uid)
            .set(mapOf("soundEnabled" to soundEnabled, "musicEnabled" to musicEnabled), SetOptions.merge())
            .await()
    }
}
