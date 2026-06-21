package com.nickdegs.sosyalpanel

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nickdegs.sosyalpanel.data.LocaleHelper
import com.nickdegs.sosyalpanel.ui.RootScreen
import com.nickdegs.sosyalpanel.ui.theme.SosyalPanelTheme

class MainActivity : ComponentActivity() {

    // Seçilen dili tüm uygulamaya uygular (manuel dil değişimi).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SosyalPanelTheme {
                RootScreen()
            }
        }
    }
}
