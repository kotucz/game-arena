package cz.kotu.tools

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.contacts.ContactsPlayerScreen
import cz.kotu.gamearena.AppComponent
import cz.kotu.gamearena.create
import cz.kotu.gamearena.createAuthHttpClient

@Composable
fun MultiPlayerScreen(
    debugHttpClientFactory: DebugHttpClientFactory = { createAuthHttpClient() },
) {
    val appComponent = remember { AppComponent::class.create() }
    val viewModel: MultiPlayerViewModel = viewModel(
        initializer = {
            appComponent.multiPlayerViewModelFactory("initial", debugHttpClientFactory)
        },
    )

    Row(modifier = Modifier.fillMaxSize()) {
        viewModel.players.forEach { player ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.Black),
            ) {
                ContactsPlayerScreen(remember{viewModel.gameFacadeForPlayer(player.username)}, player)
            }
        }
    }
}
