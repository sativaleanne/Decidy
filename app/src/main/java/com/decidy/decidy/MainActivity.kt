package com.decidy.decidy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.decidy.decidy.ui.MainView
import com.decidy.decidy.ui.theme.DecidyTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DecidyTheme {
                // A surface container using the 'background' color from the theme
                MainView()
            }
        }
    }
}
