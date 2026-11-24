package cz.kotu.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.contacts.ContactsPlayerScreen
import cz.kotu.game.contacts.ContactsPlayerViewModel
import cz.kotu.gamearena.AiPlayerAdapter
import cz.kotu.gamearena.AppComponent
import cz.kotu.gamearena.create
import cz.kotu.gamearena.createAuthHttpClient

@Composable
fun MultiPlayerScreen(
    remoteGameId: String,
    debugHttpClientFactory: DebugHttpClientFactory = { createAuthHttpClient(onUnauthorized = {}) },
) {
    val appComponent = remember { AppComponent::class.create() }
    val viewModel: MultiPlayerViewModel = viewModel(
        initializer = {
            appComponent.multiPlayerViewModelFactory(remoteGameId, debugHttpClientFactory)
        },
    )

    SideBySideScreens(
        items = viewModel.players,
        itemKey = { it.username },
    ) { player ->
        val playerViewModel: ContactsPlayerViewModel = viewModel(
            key = player.username,
            initializer = {
                ContactsPlayerViewModel(
                    gameFacade = viewModel.gameFacadeForPlayer(player.username),
                    aiPlayerAdapter = remoteGameId.takeIf(String::isNotBlank)?.let { gameId ->
                        AiPlayerAdapter(
                            aiClient = appComponent.aiClient,
                            gameId = gameId,
                            aiPlayerId = AiPlayerAdapter.GEMINI_AI_PLAYER_USERNAME,
                        )
                    },
                )
            },
        )

        ContactsPlayerScreen(playerViewModel)
    }
}
