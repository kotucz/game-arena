package cz.kotu.gamearena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import cz.kotu.game.contacts.ContactsGameViewModel
import cz.kotu.game.contacts.ContactsPlayerScreen
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.game.gotfive.Table

internal const val GAMES_ROUTE = "games"
internal const val GOT_FIVE_ROUTE = "got-five"
internal const val CONTACTS_GAME_ROUTE = "game/{gameId}"
internal const val CONTACTS_GAME_ID_ARGUMENT = "gameId"


@Composable
@Preview
fun App() {
    MaterialTheme {
        val appComponent = remember { AppComponent::class.create() }
        val navController = rememberNavController()
        val authManager = appComponent.authManager
        var showAuthModal by remember { mutableStateOf(false) }

        LaunchedEffect(authManager) {
            authManager.unauthorizedEvent.collect {
                showAuthModal = true
            }
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NavHost(navController, startDestination = GAMES_ROUTE) {
                composable(GAMES_ROUTE) {
                    val gamesViewModel: GamesViewModel = viewModel { appComponent.gamesViewModelFactory() }
                    GamesScreen(
                        viewModel = gamesViewModel,
                        onStartGotFive = { navController.navigate(GOT_FIVE_ROUTE) },
                        onGameClick = { game -> navController.navigate("game/${game.id}") },
                    )
                }

                composable(GOT_FIVE_ROUTE) {
                    GotFiveScreen(
                        appComponent = appComponent,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = CONTACTS_GAME_ROUTE,
                    arguments = listOf(navArgument(CONTACTS_GAME_ID_ARGUMENT) {
                        type = NavType.StringType
                    }),
                ) { entry ->
                    val gameId = entry.arguments?.read {
                        getString(CONTACTS_GAME_ID_ARGUMENT)
                    }
                    if (gameId != null) {
                        ContactsGameScreen(
                            appComponent = appComponent,
                            gameId = gameId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
            BrowserNavigationEffect(navController)
        }

        if (showAuthModal) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    AuthScreen(
                        authManager = authManager,
                        onAuthenticated = { showAuthModal = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsGameScreen(
    appComponent: AppComponent,
    gameId: String,
    onBack: () -> Unit,
) {
    val gameViewModel: ContactsGameViewModel = viewModel {
        appComponent.contactsGameViewModelFactory(gameId)
    }

    Column {
        TextButton(onClick = onBack) { Text("Back to games") }

        val username = gameViewModel.username.collectAsState().value
        if (username == null) {
            Text("Not logged in")
        } else {
            ContactsPlayerScreen(gameFacade = gameViewModel.gameFacade, username = username)
        }
    }
}

@Composable
private fun GotFiveScreen(
    appComponent: AppComponent,
    onBack: () -> Unit,
) {
    val gameViewModel: GameViewModel = viewModel { appComponent.gameViewModelFactory() }
    TextButton(onClick = onBack) { Text("Back to games") }
    Table(gameViewModel, Modifier.width(960.dp))
}
