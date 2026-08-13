package cz.kotu.tools

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.contacts.ContactsPlayerScreen

@Composable
fun MultiPlayerScreen() {
    val viewModel: MultiPlayerViewModel = viewModel(factory = MultiPlayerViewModelFactory)

    Row {
        viewModel.players.forEach { player ->
            ContactsPlayerScreen(viewModel.gameFacade, player)
        }
    }
}
