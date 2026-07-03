package com.bestfriends.beachbingo

import android.app.Application
import com.bestfriends.beachbingo.core.tablet.SecondaryFirebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BeachBingoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SecondaryFirebase.initialize(this)
    }
}
