package com.bestfriends.beachbingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bestfriends.beachbingo.navigation.AppNavigation
import com.bestfriends.beachbingo.ui.theme.BeachbingoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
