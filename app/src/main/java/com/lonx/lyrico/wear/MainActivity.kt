package com.lonx.lyrico.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lonx.lyrico.wear.ui.LyricoWearApp as LyricoWearAppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LyricoWearAppScreen()
        }
    }
}
