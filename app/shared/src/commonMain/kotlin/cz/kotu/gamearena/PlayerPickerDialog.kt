package cz.kotu.gamearena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cz.kotu.gamearena.model.AvailablePlayer

@Composable
fun PlayerPickerDialog(
    viewModel: PlayerPickerViewModel,
    selectedPlayers: List<AvailablePlayer>,
    onPlayerSelected: (AvailablePlayer) -> Unit,
    onClose: () -> Unit,
) {
    val availablePlayers by viewModel.availablePlayers.collectAsState()
    val filteredPlayers by viewModel.filteredPlayers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Add player")
                OutlinedTextField(
                    value = filter,
                    onValueChange = viewModel::updateFilter,
                    label = { Text("Filter by username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                when {
                    loading -> CircularProgressIndicator()
                    error != null -> {
                        Text(error!!)
                        TextButton(onClick = viewModel::loadPlayers) { Text("Try again") }
                    }

                    availablePlayers.isNullOrEmpty() -> Text("No users available.")
                    filteredPlayers.isEmpty() -> Text("No users match this filter.")
                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(filteredPlayers, key = AvailablePlayer::username) { player ->
                            val alreadySelected = player in selectedPlayers
                            TextButton(
                                onClick = { onPlayerSelected(player) },
                                enabled = !alreadySelected,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PlayerAvatar(
                                        pictureUrl = player.pictureUrl,
                                        username = player.username,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Spacer(Modifier.size(12.dp))
                                    Text(player.username)
                                    if (alreadySelected) Text("  (added)")
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}
