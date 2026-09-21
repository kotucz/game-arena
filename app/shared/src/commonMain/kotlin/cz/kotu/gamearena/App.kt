package cz.kotu.gamearena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.firebase.firebase
import com.mmk.kmpauth.google.google
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
import cz.kotu.game.contacts.ContactsPlayerViewModel
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.game.gotfive.Table

internal const val GAMES_ROUTE = "games"
internal const val GOT_FIVE_ROUTE = "got-five"
internal const val DEBUG_ROUTE = "debug"
internal const val CONTACTS_GAME_ROUTE = "game/{gameId}"
internal const val CONTACTS_GAME_ID_ARGUMENT = "gameId"

@Composable
@Preview
fun App(
    appComponent: AppComponent = remember { AppComponent::class.create() },
) {
    LaunchedEffect(Unit) {
        KMPAuth.initialize {
            google(serverId = ClientAuthConfig.GOOGLE_WEB_CLIENT_ID)
            firebase(
                apiKey = ClientAuthConfig.FIREBASE_API_KEY,
                projectId = ClientAuthConfig.FIREBASE_PROJECT_ID,
                applicationId = ClientAuthConfig.FIREBASE_APPLICATION_ID,
            )
        }
    }

    MaterialTheme {
        val navController = rememberNavController()
        val authManager = appComponent.authManager
        val notifications = appComponent.notifications
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
                        onDebugClick = { navController.navigate(DEBUG_ROUTE) },
                        onGameClick = { game -> navController.navigate("game/${game.id}") },
                    )
                }

                composable(GOT_FIVE_ROUTE) {
                    GotFiveScreen(
                        appComponent = appComponent,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(DEBUG_ROUTE) {
                    DebugScreen(
                        notifications = notifications,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(
                    route = CONTACTS_GAME_ROUTE,
                    arguments = listOf(
                        navArgument(CONTACTS_GAME_ID_ARGUMENT) {
                            type = NavType.StringType
                        },
                    ),
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
                        onGoogleSignIn = {
                            // TODO: integrate KMPAuth Google flow and then submit Firebase UID to the server.
                        },
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

        val gameNotFound = gameViewModel.gameNotFound.collectAsState().value
        val username = gameViewModel.username.collectAsState().value

        if (gameNotFound) {
            Text(
                text = "Game '$gameId' was not found or has ended.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error,
            )
        } else if (username == null) {
            Text("Not logged in")
        } else {
            val playerViewModel: ContactsPlayerViewModel = viewModel {
                ContactsPlayerViewModel(gameViewModel.gameFacade)
            }
            ContactsPlayerScreen(viewModel = playerViewModel)
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
