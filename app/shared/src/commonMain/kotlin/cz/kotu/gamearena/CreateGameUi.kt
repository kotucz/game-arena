package cz.kotu.gamearena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cz.kotu.gamearena.model.RunningGame

@Composable
fun CreateGameDialog(
    viewModel: GamesViewModel,
    onGameCreated: (RunningGame) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .navigationBarsPadding(),
            color = Color.White,
        ) {
            CreateContactsGameForm(
                viewModel = viewModel,
                modifier = Modifier.padding(16.dp),
                onGameCreated = onGameCreated,
            )
        }
    }
}

@Composable
private fun CreateContactsGameForm(
    viewModel: GamesViewModel,
    modifier: Modifier = Modifier,
    onGameCreated: (RunningGame) -> Unit,
) {
    val creatingGame by viewModel.creatingGame.collectAsState()
    val playersText by viewModel.playersText.collectAsState()
    val configText by viewModel.configText.collectAsState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

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
            onClick = { viewModel.createGame(onSuccess = onGameCreated) },
            enabled = !creatingGame,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (creatingGame) "Creating game…" else "Create Contacts game")
        }
    }
}
