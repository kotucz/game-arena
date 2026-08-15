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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import cz.kotu.game.gotfive.GameViewModelFactory
import cz.kotu.game.gotfive.Table
import cz.kotu.game.contacts.ContactsPlayerScreen
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.NetworkContactsGameFacade

internal const val AUTH_ROUTE = "auth"
internal const val GAMES_ROUTE = "games"
internal const val GOT_FIVE_ROUTE = "got-five"
internal const val CONTACTS_GAME_ROUTE = "game/{gameId}"
internal const val CONTACTS_GAME_ID_ARGUMENT = "gameId"

private data class ContactsGameNavigationState(
    val username: String,
    val players: List<String>,
)

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        val networkScope = rememberCoroutineScope()
        val contactsGames = remember { mutableMapOf<String, ContactsGameNavigationState>() }
        var username by remember { mutableStateOf<String?>(null) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NavHost(navController, startDestination = AUTH_ROUTE) {
                composable(AUTH_ROUTE) {
                    AuthScreen { authenticatedUsername ->
                        username = authenticatedUsername
                        navController.navigate(GAMES_ROUTE) {
                            popUpTo(AUTH_ROUTE) { inclusive = true }
                        }
                    }
                }

                composable(GAMES_ROUTE) {
                    val authenticatedUsername = username
                    if (authenticatedUsername == null) {
                        Text("Authentication required")
                    } else {
                    GamesScreen(
                        username = authenticatedUsername,
                        onStartGotFive = {
                            navController.navigate(GOT_FIVE_ROUTE)
                        },
                        onGameClick = { game ->
                            contactsGames[game.id] = ContactsGameNavigationState(
                                username = authenticatedUsername,
                                players = game.players,
                            )
                            navController.navigate("game/${game.id}")
                        },
                    )
                    }
                }

                composable(GOT_FIVE_ROUTE) {
                    GotFiveScreen(onBack = { navController.popBackStack() })
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
                    val game = gameId?.let(contactsGames::get)
                    if (gameId == null || game == null) {
                        Text("Game is not available")
                    } else {
                        ContactsGameScreen(
                            gameId = gameId,
                            username = game.username,
                            players = game.players,
                            networkScope = networkScope,
                            onBack = { navController.popBackStack() },
                        )
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
    players: List<String>,
    networkScope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
) {
    val player = ContactsBoardState.Player(username)
    val boardPlayers = players.map(ContactsBoardState::Player)
    val gameFacade = remember(gameId) {
        NetworkContactsGameFacade(
            httpClient = createAuthHttpClient(),
            endpoint = authBaseUrl().trimEnd('/') + "/api",
            gameId = gameId,
            initialState = ContactsBoardState.create(boardPlayers),
            scope = networkScope,
        )
    }
    TextButton(onClick = onBack) { Text("Back to games") }
    ContactsPlayerScreen(gameFacade = gameFacade, player = player)
}

@Composable
private fun GotFiveScreen(onBack: () -> Unit) {
    val gameViewModel: GameViewModel = viewModel(factory = GameViewModelFactory)
    TextButton(onClick = onBack) { Text("Back to games") }
    Table(gameViewModel, Modifier.width(960.dp))
}
