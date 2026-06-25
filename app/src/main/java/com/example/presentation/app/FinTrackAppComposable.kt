package com.example.presentation.app

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.AppContainer
import com.example.presentation.ViewModelFactory
import com.example.presentation.settings.SettingsViewModel
import com.example.ui.theme.KdachTheme

/**
 * Top-level composable that wraps the entire app with theme, RTL direction,
 * and system bar configuration. Extracted from MainActivity.setContent{}.
 *
 * @param container The AppContainer for dependency injection
 * @param factory The ViewModelFactory for creating ViewModels
 * @param startDestination The initial navigation destination (Onboarding or Home)
 * @param isFirstLaunch Whether this is the first app launch (used to skip dark theme during onboarding)
 */
@Composable
fun FinTrackApp(
    container: AppContainer,
    factory: ViewModelFactory,
    startDestination: String,
    isFirstLaunch: Boolean = false
) {
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    // isDarkTheme does NOT depend on currentRoute — stable during navigation
    // Only check onboarding once; after that, use settings preference
    val isDarkTheme = if (isFirstLaunch) false else settingsUiState.isDarkTheme

    // Adjust status bar and navigation bar system icons based on light/dark mode
    val context = LocalContext.current
    LaunchedEffect(isDarkTheme) {
        val activity = context as? ComponentActivity
        activity?.enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
    }

    KdachTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            FinTrackAppShell(
                container = container,
                factory = factory,
                settingsViewModel = settingsViewModel,
                startDestination = startDestination,
                isFirstLaunch = isFirstLaunch
            )
        }
    }
}
