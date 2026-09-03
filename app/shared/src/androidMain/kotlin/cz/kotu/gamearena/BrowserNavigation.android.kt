package cz.kotu.gamearena


import androidx.navigation.NavController

internal actual suspend fun bindBrowserNavigation(navController: NavController) {
    // no-op on android
}
