package cz.kotu.gamearena

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.kotu.gamearena.model.RunningGame

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    onStartGotFive: () -> Unit,
    onGameClick: (RunningGame) -> Unit,
) {
    val games by viewModel.games.collectAsState()
    val error by viewModel.error.collectAsState()
    val creatingGame by viewModel.creatingGame.collectAsState()
    val playersText by viewModel.playersText.collectAsState()
    val configText by viewModel.configText.collectAsState()

    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Running games")
        Text("Join a game already in progress, or start a game just for yourself.")

        Button(onClick = onStartGotFive, modifier = Modifier.fillMaxWidth()) {
            Text("Start Got Five")
        }

        HorizontalDivider()

        TextField(
            value = playersText,
            onValueChange = viewModel::updatePlayersText,
            label = { Text("Players (one username per line)") },
            placeholder = { Text("Enter player usernames\none per line") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 10,
        )

        TextField(
            value = configText,
            onValueChange = viewModel::updateConfigText,
            label = { Text("Contacts config") },
            placeholder = { Text("ContactsGameConfig") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 10,
        )

        Button(
            onClick = { viewModel.createGame(onSuccess = onGameClick) },
            enabled = !creatingGame,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (creatingGame) "Creating game…" else "Create Contacts game")
        }

        HorizontalDivider()

        when {
            games == null && error == null -> CircularProgressIndicator()
            error != null -> {
                Text(error!!)
                Button(onClick = viewModel::loadGames) { Text("Try again") }
            }

            games!!.isEmpty() -> Text("There are no running multiplayer games.")
            else -> games!!.forEach { game -> RunningGameCard(game, onClick = { onGameClick(game) }) }
        }
    }
}

@Composable
private fun RunningGameCard(game: RunningGame, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(game.type.replaceFirstChar { it.uppercase() })
                Text("  ${game.id}")
            }
            Text("Players: ${game.players.joinToString()}")
            Text("Created: ${game.createdAt}")
        }
    }
}
