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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cz.kotu.gamearena.model.RunningGame
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.arena_add_player
import gamearena.app.shared.generated.resources.arena_contacts_config
import gamearena.app.shared.generated.resources.arena_contacts_config_placeholder
import gamearena.app.shared.generated.resources.arena_create_contacts_game
import gamearena.app.shared.generated.resources.arena_creating_game
import gamearena.app.shared.generated.resources.arena_no_players_selected
import gamearena.app.shared.generated.resources.arena_players
import gamearena.app.shared.generated.resources.arena_remove_player
import org.jetbrains.compose.resources.stringResource

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
            text = stringResource(Res.string.arena_players),
            style = MaterialTheme.typography.labelLarge,
        )

        if (selectedPlayers.isEmpty()) {
            Text(
                text = stringResource(Res.string.arena_no_players_selected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                contentDescription = stringResource(Res.string.arena_remove_player, player.username),
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
            Text(stringResource(Res.string.arena_add_player))
        }

        TextField(
            value = configText,
            onValueChange = viewModel::updateConfigText,
            label = { Text(stringResource(Res.string.arena_contacts_config)) },
            placeholder = { Text(stringResource(Res.string.arena_contacts_config_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 10,
        )

        Button(
            onClick = { viewModel.createGame(onSuccess = onGameCreated) },
            enabled = !creatingGame,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (creatingGame) Res.string.arena_creating_game
                    else Res.string.arena_create_contacts_game,
                ),
            )
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
