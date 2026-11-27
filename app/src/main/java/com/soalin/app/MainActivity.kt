package com.soalin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.soalin.app.ui.SoalinApp
import com.soalin.app.ui.SoalinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoalinTheme {
                SoalinApp()
            }
        }
    }
}
