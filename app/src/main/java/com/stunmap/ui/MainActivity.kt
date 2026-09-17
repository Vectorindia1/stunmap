package com.stunmap.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.stunmap.ui.navigation.AppNavGraph
import com.stunmap.ui.theme.BackgroundDark
import com.stunmap.ui.theme.StunMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StunMapTheme {
                val navController = rememberNavController()
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                        .safeDrawingPadding()
                ) {
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
