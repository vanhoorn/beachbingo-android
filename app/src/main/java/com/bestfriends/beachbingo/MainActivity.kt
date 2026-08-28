package com.bestfriends.beachbingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bestfriends.beachbingo.core.audio.AudioRegistry
import com.bestfriends.beachbingo.navigation.AppNavigation
import com.bestfriends.beachbingo.ui.theme.BeachbingoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onPause() {
        super.onPause()
        AudioRegistry.current?.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        AudioRegistry.current?.resumeMusic()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeachbingoTheme {
                AppNavigation()
            }
        }
    }
}
