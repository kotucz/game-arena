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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cz.kotu.game.contacts.formatLocalUi
import cz.kotu.gamearena.model.RunningGame

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    onStartGotFive: () -> Unit,
    onGameClick: (RunningGame) -> Unit,
) {
    val games by viewModel.games.collectAsState()
    val error by viewModel.error.collectAsState()
    val username by viewModel.username.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.observeLobby()
    }

    if (viewModel.createGameDialogVisible.value) {
        CreateGameDialog(
            viewModel = viewModel,
            onGameCreated = onGameClick,
            onClose = { viewModel.createGameDialogVisible.value = false },
        )
    }

    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            verticalAlignment = CenterVertically,
        ) {
            Text(username ?: "Not logged in")
            Button(onClick = viewModel::logout) {
                Text("Logout")
            }
        }

        Button(onClick = onStartGotFive, modifier = Modifier.fillMaxWidth()) {
            Text("Start Got Five")
        }

        Button(
            onClick = { viewModel.createGameDialogVisible.value = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create Contacts game")
        }

        HorizontalDivider()

        Text("Running games")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            verticalAlignment = CenterVertically,
        ) {

            GameFilterSegmentedButton(
                selectedTab = viewModel.gameFilterMyAll.value,
                onTabSelected = { viewModel.gameFilterMyAll.value = it },
            )
        }

        when {
            games == null && error == null -> CircularProgressIndicator()
            error != null -> {
                Text(error!!)
                Button(onClick = viewModel::loadGames) { Text("Try again") }
            }

            games!!.isEmpty() -> Text("There are no running multiplayer games.")
            else -> games!!
                .filter { viewModel.gameFilterMyAll.value == GameFilterTab.All || it.players.contains(username) }
                .sortedByDescending { it.createdAt }
                .forEach { game -> RunningGameCard(game, onClick = { onGameClick(game) }) }
        }
    }
}

enum class GameFilterTab(
    val label: String,
    val icon: ImageVector
) {
    All("All Games", Icons.Outlined.Public),
    My("My Games", Icons.Outlined.Person)
}

@Composable
fun GameFilterSegmentedButton(
    selectedTab: GameFilterTab,
    onTabSelected: (GameFilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = GameFilterTab.entries

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selectedTab == tab) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null
                        )
                    }
                }
            ) {
                Text(text = tab.label)
            }
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
            Text("Created: ${game.createdAt.formatLocalUi()}")
        }
    }
}
