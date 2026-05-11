package com.immedio.toevent.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.ui.events.EventDetailScreen
import com.immedio.toevent.ui.events.EventListScreen
import com.immedio.toevent.ui.events.MainViewModel
import com.immedio.toevent.ui.onboarding.OnboardingScreen
import com.immedio.toevent.ui.settings.AddAccountSheet
import com.immedio.toevent.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(preferencesRepository: UserPreferencesRepository) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Collect async instead of runBlocking
    val hasCompletedIntro by preferencesRepository.hasCompletedIntro.collectAsState(initial = null)

    // Show loading while we wait for DataStore
    if (hasCompletedIntro == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination: NavRoute = if (hasCompletedIntro == true) NavRoute.EventList else NavRoute.Onboarding

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<NavRoute.Onboarding> {
            OnboardingScreen(
                onComplete = { selectedSurface ->
                    scope.launch {
                        preferencesRepository.setHasCompletedIntro(true)
                        preferencesRepository.setActiveSurface(selectedSurface)
                    }
                    navController.navigate(NavRoute.EventList) {
                        popUpTo(NavRoute.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable<NavRoute.EventList> {
            val viewModel: MainViewModel = hiltViewModel()
            EventListScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(NavRoute.Settings)
                },
                onNavigateToDetail = { eventId ->
                    navController.navigate(NavRoute.EventDetail(eventId))
                },
            )
        }
        composable<NavRoute.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAddAccount = { navController.navigate(NavRoute.AddAccount) },
            )
        }
        composable<NavRoute.EventDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<NavRoute.EventDetail>()
            // Fix C5: use own hiltViewModel instead of backstack lookup that can crash
            val viewModel: MainViewModel = hiltViewModel()
            EventDetailScreen(
                eventId = route.eventId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable<NavRoute.AddAccount> {
            AddAccountSheet(
                onDismiss = { navController.popBackStack() },
            )
        }
    }
}
