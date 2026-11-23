package cz.kotu.game.tricks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.kotu.game.tricks.model.Card
import cz.kotu.game.tricks.model.PlayedCard
import cz.kotu.game.tricks.model.TricksGamePhase
import cz.kotu.game.tricks.model.TricksPlayerViewState
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.tricks_cards_in_hand
import gamearena.app.shared.generated.resources.tricks_current_trick
import gamearena.app.shared.generated.resources.tricks_empty_trick
import gamearena.app.shared.generated.resources.tricks_game_over
import gamearena.app.shared.generated.resources.tricks_lead_suit
import gamearena.app.shared.generated.resources.tricks_no_trump
import gamearena.app.shared.generated.resources.tricks_tricks_won
import gamearena.app.shared.generated.resources.tricks_trump
import gamearena.app.shared.generated.resources.tricks_waiting_for_turn
import gamearena.app.shared.generated.resources.tricks_your_turn
import org.jetbrains.compose.resources.stringResource

@Composable
fun TricksPlayerScreen(
    viewModel: TricksPlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsState()
    val errorMessage = viewModel.errorMessage

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val state = viewState
        if (state == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading game state…", style = MaterialTheme.typography.bodyLarge)
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Player identity, turn status, trump indicator, tricks won
            HeaderSection(state = state)

            // Opponents summary
            if (state.opponents.isNotEmpty()) {
                OpponentsSection(state = state)
            }

            HorizontalDivider()

            // Active Trick Area
            CurrentTrickSection(state = state)

            // Error display
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Player's Hand
            PlayerHandSection(
                state = state,
                actionInProgress = viewModel.actionInProgress,
                onCardClick = viewModel::playCard,
            )
        }
    }
}

@Composable
private fun HeaderSection(
    state: TricksPlayerViewState,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isMyTurn) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.myPlayer.username.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = state.myPlayer.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Trump indicator
                val trumpSuit = state.trumpSuit
                val trumpText = if (trumpSuit != null) {
                    stringResource(Res.string.tricks_trump, trumpSuit.label)
                } else {
                    stringResource(Res.string.tricks_no_trump)
                }
                Badge(
                    containerColor = trumpSuit?.let { suitColor(it) } ?: MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = trumpText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Turn status
                val statusText = when (val phase = state.phase) {
                    is TricksGamePhase.PlayerTurn -> {
                        if (phase.activePlayer == state.myPlayer.id) {
                            stringResource(Res.string.tricks_your_turn)
                        } else {
                            val activeName = state.opponents.firstOrNull { it.player.id == phase.activePlayer }?.player?.username
                                ?: phase.activePlayer.value
                            stringResource(Res.string.tricks_waiting_for_turn, activeName)
                        }
                    }
                    is TricksGamePhase.GameOver -> stringResource(Res.string.tricks_game_over)
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (state.isMyTurn) FontWeight.Bold else FontWeight.Normal,
                    color = if (state.isMyTurn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val myTricksWon = state.completedTricks.count { it.winner == state.myPlayer.id }
                Text(
                    text = stringResource(Res.string.tricks_tricks_won, myTricksWon),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OpponentsSection(state: TricksPlayerViewState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.opponents.forEach { opponent ->
            val phase = state.phase
            val isOpponentTurn = phase is TricksGamePhase.PlayerTurn &&
                    phase.activePlayer == opponent.player.id

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOpponentTurn) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.border(
                    width = if (isOpponentTurn) 2.dp else 1.dp,
                    color = if (isOpponentTurn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = opponent.player.username,
                        fontWeight = if (isOpponentTurn) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = stringResource(Res.string.tricks_cards_in_hand, opponent.cardCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "🏆 ${opponent.tricksWon}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentTrickSection(state: TricksPlayerViewState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    Res.string.tricks_current_trick,
                    state.completedTricks.size + if (state.phase is TricksGamePhase.GameOver) 0 else 1,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            state.currentTrick.leadSuit?.let { lead ->
                Text(
                    text = stringResource(Res.string.tricks_lead_suit, lead.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = suitColor(lead),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (state.currentTrick.plays.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.tricks_empty_trick),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.currentTrick.plays.forEach { play ->
                    PlayedCardItem(play = play, state = state)
                }
            }
        }
    }
}

@Composable
private fun PlayedCardItem(
    play: PlayedCard,
    state: TricksPlayerViewState,
) {
    val playerName = if (play.player == state.myPlayer.id) {
        state.myPlayer.username
    } else {
        state.opponents.firstOrNull { it.player.id == play.player }?.player?.username
            ?: play.player.value
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = playerName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
        TricksCardView(
            card = play.card,
            isPlayable = false,
        )
    }
}

@Composable
private fun PlayerHandSection(
    state: TricksPlayerViewState,
    actionInProgress: Boolean,
    onCardClick: (Card) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Your Hand (${state.myHand.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.myHand.forEach { card ->
                val isPlayable = state.isMyTurn &&
                        !actionInProgress &&
                        state.legalPlays.contains(card)

                TricksCardView(
                    card = card,
                    isPlayable = isPlayable,
                    onClick = if (isPlayable) {
                        { onCardClick(card) }
                    } else null,
                )
            }
        }
    }
}
