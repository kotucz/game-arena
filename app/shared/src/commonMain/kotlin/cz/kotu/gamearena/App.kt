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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.game.gotfive.GameViewModelFactory
import cz.kotu.game.gotfive.Table
import cz.kotu.game.contacts.ContactsPlayerScreen
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.model.RunningGame

@Composable
@Preview
fun App() {
    MaterialTheme {
        var authenticated by remember { mutableStateOf(false) }
        var showGotFive by remember { mutableStateOf(false) }
        var username by remember { mutableStateOf<String?>(null) }
        var selectedGame by remember { mutableStateOf<RunningGame?>(null) }
        val networkScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (authenticated) {
                if (showGotFive) {
                    GotFiveScreen(onBack = { showGotFive = false })
                } else if (selectedGame != null) {
                    val game = selectedGame!!
                    val player = ContactsBoardState.Player(username!!)
                    val players = game.players.map(ContactsBoardState::Player)
                    val gameFacade = remember(game.id) {
                        NetworkContactsGameFacade(
                            httpClient = createAuthHttpClient(),
                            endpoint = authBaseUrl().trimEnd('/') + "/api",
                            gameId = game.id,
                            initialState = ContactsBoardState.create(players),
                            scope = networkScope,
                        )
                    }
                    TextButton(onClick = { selectedGame = null }) { Text("Back to games") }
                    ContactsPlayerScreen(
                        gameFacade = gameFacade,
                        player = player,
                    )
                } else {
                    GamesScreen(
                        onStartGotFive = { showGotFive = true },
                        onGameClick = { selectedGame = it },
                    )
                }
            } else {
                AuthScreen {
                    username = it
                    authenticated = true
                }
            }
        }
    }
}

@Composable
private fun GotFiveScreen(onBack: () -> Unit) {
    val gameViewModel: GameViewModel = viewModel(factory = GameViewModelFactory)
    TextButton(onClick = onBack) { Text("Back to games") }
    Table(gameViewModel, Modifier.width(960.dp))
}
