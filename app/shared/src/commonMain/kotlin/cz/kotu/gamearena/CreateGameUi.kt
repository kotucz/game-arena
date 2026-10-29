package cz.kotu.gamearena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                .widthIn(min = 320.dp, max = 460.dp)
                .wrapContentSize()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
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
    val selectedPlayers by viewModel.selectedPlayers.collectAsState()
    val configText by viewModel.configText.collectAsState()
    var playerPickerVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Players",
            style = MaterialTheme.typography.labelLarge,
        )

        if (selectedPlayers.isEmpty()) {
            Text(
                text = "No players selected. Click \"Add player\" below to add players.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedPlayers.forEach { player ->
                    InputChip(
                        selected = false,
                        onClick = { viewModel.removePlayer(player) },
                        label = { Text(player.username) },
                        leadingIcon = {
                            PlayerAvatar(
                                pictureUrl = player.pictureUrl,
                                username = player.username,
                                modifier = Modifier.size(InputChipDefaults.AvatarSize),
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove ${player.username}",
                                modifier = Modifier.size(InputChipDefaults.AvatarSize),
                            )
                        },
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                playerPickerVisible = true
                viewModel.playerPickerViewModel.prepareForOpen()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Add player")
        }

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

    if (playerPickerVisible) {
        PlayerPickerDialog(
            viewModel = viewModel.playerPickerViewModel,
            selectedPlayers = selectedPlayers,
            onPlayerSelected = { player ->
                viewModel.addPlayer(player)
                playerPickerVisible = false
            },
            onClose = { playerPickerVisible = false },
        )
    }
}
