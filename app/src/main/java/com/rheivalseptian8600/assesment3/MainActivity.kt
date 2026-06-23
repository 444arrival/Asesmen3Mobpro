package com.rheivalseptian8600.assesment3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rheivalseptian8600.assesment3.ui.theme.Assesment3Theme
import com.rheivalseptian8600.assesment3.ui.theme.screen.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Assesment3Theme {
                MainScreen()
            }
        }
    }
}