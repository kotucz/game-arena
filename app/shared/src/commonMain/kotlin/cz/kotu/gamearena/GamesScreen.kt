package cz.kotu.gamearena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.kotu.gamearena.model.RunningGame
import kotlinx.coroutines.launch

@Composable
fun GamesScreen(onStartGotFive: () -> Unit, onGameClick: (RunningGame) -> Unit) {
    val gamesClient = remember { GamesClient() }
    val scope = rememberCoroutineScope()
    var games by remember { mutableStateOf<List<RunningGame>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadGames() {
        scope.launch {
            error = null
            games = null
            gamesClient.runningGames().fold(
                onSuccess = { games = it },
                onFailure = { error = it.message ?: "Could not load running games" },
            )
        }
    }

    LaunchedEffect(Unit) {
        loadGames()
    }

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

        when {
            games == null && error == null -> CircularProgressIndicator()
            error != null -> {
                Text(error!!)
                Button(onClick = ::loadGames) { Text("Try again") }
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
