package cz.kotu.game.tricks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.tricks.model.TricksGameConfig
import cz.kotu.game.tricks.model.TricksGameFacadeImpl
import cz.kotu.game.tricks.model.TricksGamePhase
import cz.kotu.game.tricks.model.TricksPlayer
import cz.kotu.game.tricks.model.TricksPlayerGameAdapter
import cz.kotu.game.tricks.model.TricksPlayerId
import cz.kotu.tools.SideBySideScreens
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.arena_back_to_games
import gamearena.app.shared.generated.resources.arena_one_at_a_time
import gamearena.app.shared.generated.resources.arena_player_screen_layout
import gamearena.app.shared.generated.resources.arena_side_by_side
import gamearena.app.shared.generated.resources.tricks_local_title
import gamearena.app.shared.generated.resources.tricks_new_game
import gamearena.app.shared.generated.resources.tricks_player_count
import gamearena.app.shared.generated.resources.tricks_viewing_as
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private data class LocalTricksGame(
    val generation: Int,
    val players: List<TricksPlayer>,
    val facade: TricksGameFacadeImpl,
)

@Composable
fun TricksMultiPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    animationDurationScale: Float = 1f,
) {
    var game by remember { mutableStateOf(createLocalGame(generation = 0, playerCount = 4)) }
    var selectedPlayerId by remember { mutableStateOf(game.players.first().id) }
    var showSideBySide by remember { mutableStateOf(false) }

    fun startNewGame(playerCount: Int) {
        val nextGame = createLocalGame(game.generation + 1, playerCount)
        game = nextGame
        selectedPlayerId = nextGame.players.first().id
    }

    val selectedPlayer = game.players.first { it.id == selectedPlayerId }
    val playerViewModel: TricksPlayerViewModel = viewModel(
        key = "${game.generation}:${selectedPlayer.id.value}",
        initializer = {
            TricksPlayerViewModel(
                TricksPlayerGameAdapter(
                    player = selectedPlayer,
                    gameFacade = game.facade,
                ),
            )
        },
    )
    val playerViewState by playerViewModel.viewState.collectAsState()

    LaunchedEffect(game.generation, playerViewState?.phase) {
        val phase = playerViewState?.phase
        if (phase is TricksGamePhase.PlayerTurn && phase.activePlayer != selectedPlayerId) {
            val justCompletedTrick = playerViewState?.completedTricks?.isNotEmpty() == true &&
                    playerViewState?.currentTrick?.plays?.isEmpty() == true
            if (justCompletedTrick) {
                delay(animationDurationMillis(1300, animationDurationScale).milliseconds)
            } else {
                delay(animationDurationMillis(700, animationDurationScale).milliseconds)
            }
            selectedPlayerId = phase.activePlayer
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(Res.string.arena_back_to_games))
            }
            Text(
                text = stringResource(Res.string.tricks_local_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Button(onClick = { startNewGame(game.players.size) }) {
                Text(stringResource(Res.string.tricks_new_game))
            }
        }

        Text(stringResource(Res.string.tricks_player_count))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            (2..4).forEachIndexed { index, count ->
                SegmentedButton(
                    selected = count == game.players.size,
                    onClick = { startNewGame(count) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                ) {
                    Text(count.toString())
                }
            }
        }

        Text(stringResource(Res.string.arena_player_screen_layout))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(false, true).forEachIndexed { index, sideBySide ->
                SegmentedButton(
                    selected = sideBySide == showSideBySide,
                    onClick = { showSideBySide = sideBySide },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                ) {
                    Text(
                        stringResource(
                            if (sideBySide) Res.string.arena_side_by_side
                            else Res.string.arena_one_at_a_time,
                        ),
                    )
                }
            }
        }

        if (showSideBySide) {
            SideBySideScreens(
                items = game.players,
                itemKey = { "${game.generation}:${it.id.value}" },
                modifier = Modifier.weight(1f),
            ) { player ->
                TricksPlayerPanel(
                    game = game,
                    player = player,
                    animationDurationScale = animationDurationScale,
                )
            }
        } else {
            Text(stringResource(Res.string.tricks_viewing_as))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                game.players.forEachIndexed { index, player ->
                    SegmentedButton(
                        selected = player.id == selectedPlayer.id,
                        onClick = { selectedPlayerId = player.id },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = game.players.size),
                    ) {
                        Text(player.username)
                    }
                }
            }

            key(selectedPlayerId) {
                TricksPlayerPanel(
                    game = game,
                    player = selectedPlayer,
                    animationDurationScale = animationDurationScale,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TricksPlayerPanel(
    game: LocalTricksGame,
    player: TricksPlayer,
    modifier: Modifier = Modifier,
    animationDurationScale: Float,
) {
    val playerViewModel: TricksPlayerViewModel = viewModel(
        key = "${game.generation}:${player.id.value}",
        initializer = {
            TricksPlayerViewModel(
                TricksPlayerGameAdapter(
                    player = player,
                    gameFacade = game.facade,
                ),
            )
        },
    )
    TricksPlayerScreen(
        viewModel = playerViewModel,
        modifier = modifier,
        animationDurationScale = animationDurationScale,
    )
}

private fun createLocalGame(generation: Int, playerCount: Int): LocalTricksGame {
    val players = (1..playerCount).map { number ->
        TricksPlayer(
            id = TricksPlayerId("local-player-$number"),
            username = "Player $number",
        )
    }
    val config = if (playerCount == 3) {
        TricksGameConfig(includeTrumpSuit = false, trumpSuit = null)
    } else {
        TricksGameConfig()
    }

    return LocalTricksGame(
        generation = generation,
        players = players,
        facade = TricksGameFacadeImpl(players = players, config = config),
    )
}
