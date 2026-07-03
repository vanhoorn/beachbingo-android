package com.bestfriends.beachbingo.core.tablet

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SecondaryFirebase {
    private const val APP_NAME = "secondary"

    @Volatile private var auth: FirebaseAuth? = null
    @Volatile private var firestore: FirebaseFirestore? = null

    fun initialize(context: Context) {
        if (auth != null) return
        synchronized(this) {
            if (auth != null) return
            val app = runCatching { FirebaseApp.getInstance(APP_NAME) }
                .getOrElse {
                    FirebaseApp.initializeApp(
                        context,
                        FirebaseApp.getInstance().options,
                        APP_NAME
                    )!!
                }
            auth = FirebaseAuth.getInstance(app)
            firestore = FirebaseFirestore.getInstance(app)
        }
    }

    fun getAuth(): FirebaseAuth = auth ?: error("SecondaryFirebase not initialized")
    fun getFirestore(): FirebaseFirestore = firestore ?: error("SecondaryFirebase not initialized")
}
