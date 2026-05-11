package com.immedio.toevent.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.EventList,
    ) {
        composable<NavRoute.Onboarding> {
            PlaceholderScreen("Onboarding")
        }
        composable<NavRoute.EventList> {
            PlaceholderScreen("Event List")
        }
        composable<NavRoute.Settings> {
            PlaceholderScreen("Settings")
        }
        composable<NavRoute.EventDetail> {
            PlaceholderScreen("Event Detail")
        }
        composable<NavRoute.AddAccount> {
            PlaceholderScreen("Add Account")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = name)
    }
}
