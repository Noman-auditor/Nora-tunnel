package com.norabuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.norabuilder.ui.navigation.NoraNavGraph
import com.norabuilder.ui.theme.NoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoraTheme {
                NoraNavGraph(rememberNavController())
            }
        }
    }
}
