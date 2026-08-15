package cz.kotu.gamearena

import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavController
import androidx.navigation.bindToBrowserNavigation

@OptIn(ExperimentalBrowserHistoryApi::class)
internal actual suspend fun bindBrowserNavigation(navController: NavController) {
    navController.bindToBrowserNavigation()
}
