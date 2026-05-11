package com.immedio.toevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.ui.navigation.AppNavHost
import com.immedio.toevent.ui.theme.ToEventTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToEventTheme {
                AppNavHost(preferencesRepository)
            }
        }
    }
}
