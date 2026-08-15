package cz.kotu.gamearena

import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation
import androidx.savedstate.read

@OptIn(ExperimentalBrowserHistoryApi::class)
internal actual suspend fun bindBrowserNavigation(navController: NavController) {
    navController.bindToBrowserNavigation(::browserRoute)
}

private fun browserRoute(entry: NavBackStackEntry): String {
    val route = entry.destination.route.orEmpty()
    return ROUTE_ARGUMENT_PATTERN.replace(route) { match ->
        entry.arguments?.read { getString(match.groupValues[1]) }.orEmpty()
    }
}

private val ROUTE_ARGUMENT_PATTERN = Regex("\\{([^}]+)}")
