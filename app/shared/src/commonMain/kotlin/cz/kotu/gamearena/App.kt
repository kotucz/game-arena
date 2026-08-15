package cz.kotu.gamearena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.savedstate.read
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.game.gotfive.Table
import cz.kotu.game.contacts.ContactsPlayerScreen
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import io.ktor.client.HttpClient

internal const val AUTH_ROUTE = "auth"
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
        var username by remember { mutableStateOf<String?>(null) }
        var authenticationChecked by remember { mutableStateOf(false) }

        // Authentication is app state, not auth-screen state. A deep link can
        // open the game route without ever composing AuthScreen.
        LaunchedEffect(Unit) {
            appComponent.authClient.currentUser()
                .onSuccess {
                    it.trim().takeIf(String::isNotEmpty)?.let { restoredUsername ->
                        username = restoredUsername
                    }
                }
            authenticationChecked = true
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NavHost(navController, startDestination = AUTH_ROUTE) {
                composable(AUTH_ROUTE) {
                    if (username == null) {
                        AuthScreen(appComponent.authClient) { authenticatedUsername ->
                            username = authenticatedUsername
                            navController.navigate(GAMES_ROUTE) {
                                popUpTo(AUTH_ROUTE) { inclusive = true }
                            }
                        }
                    } else {
                        // Session restoration may complete while this is still
                        // the current destination. Deep-link navigation can
                        // replace it independently.
                        LaunchedEffect(username) {
                            navController.navigate(GAMES_ROUTE) {
                                popUpTo(AUTH_ROUTE) { inclusive = true }
                            }
                        }
                    }
                }

                composable(GAMES_ROUTE) {
                    if (!authenticationChecked) {
                        Text("Restoring session…")
                    } else {
                        val authenticatedUsername = username
                        if (authenticatedUsername == null) {
                            Text("Authentication required")
                        } else {
                            GamesScreen(
                                gamesClient = appComponent.gamesClient,
                                username = authenticatedUsername,
                                onStartGotFive = {
                                    navController.navigate(GOT_FIVE_ROUTE)
                                },
                                onGameClick = { game ->
                                    navController.navigate("game/${game.id}")
                                },
                            )
                        }
                    }
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
                    if (!authenticationChecked) {
                        Text("Restoring session…")
                    } else {
                        val authenticatedUsername = username
                        if (gameId == null || authenticatedUsername == null) {
                            Text("Authentication required")
                        } else {
                            ContactsGameScreen(
                                gameId = gameId,
                                username = authenticatedUsername,
                                httpClient = appComponent.httpClient,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
            BrowserNavigationEffect(navController)
        }
    }
}

@Composable
private fun ContactsGameScreen(
    gameId: String,
    username: String,
    httpClient: HttpClient,
    onBack: () -> Unit,
) {
    val networkScope = rememberCoroutineScope()
    val player = ContactsBoardState.Player(username)
    val gameFacade = remember(gameId) {
        NetworkContactsGameFacade(
            httpClient = httpClient,
            endpoint = authBaseUrl().trimEnd('/') + "/api",
            gameId = gameId,
            initialState = ContactsBoardState.empty(),
            scope = networkScope,
        )
    }
    TextButton(onClick = onBack) { Text("Back to games") }
    ContactsPlayerScreen(gameFacade = gameFacade, player = player)
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
