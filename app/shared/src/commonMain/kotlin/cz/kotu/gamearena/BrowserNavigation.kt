package cz.kotu.gamearena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController

@Composable
internal fun BrowserNavigationEffect(navController: NavController) {
    LaunchedEffect(navController) {
        bindBrowserNavigation(navController)
    }
}

internal expect suspend fun bindBrowserNavigation(navController: NavController)
