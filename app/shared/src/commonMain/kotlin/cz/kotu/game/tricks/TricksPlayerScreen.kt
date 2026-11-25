package cz.kotu.game.tricks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionDefaults
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.kotu.game.tricks.model.Card
import cz.kotu.game.tricks.model.PlayedCard
import cz.kotu.game.tricks.model.Suit
import cz.kotu.game.tricks.model.Trick
import cz.kotu.game.tricks.model.TricksGamePhase
import cz.kotu.game.tricks.model.TricksPlayerId
import cz.kotu.game.tricks.model.TricksPlayerViewState
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.tricks_current_trick
import gamearena.app.shared.generated.resources.tricks_empty_trick
import gamearena.app.shared.generated.resources.tricks_game_over
import gamearena.app.shared.generated.resources.tricks_lead_suit
import gamearena.app.shared.generated.resources.tricks_no_trump
import gamearena.app.shared.generated.resources.tricks_trick_won_by
import gamearena.app.shared.generated.resources.tricks_tricks_won
import gamearena.app.shared.generated.resources.tricks_trump
import gamearena.app.shared.generated.resources.tricks_waiting_for_turn
import gamearena.app.shared.generated.resources.tricks_your_turn
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private class TricksCardTracker {
    private val opponentHandTokens = mutableMapOf<TricksPlayerId, MutableList<String>>()
    private val playedCardTokens = mutableMapOf<Card, String>()
    private val nextTokenIndex = mutableMapOf<TricksPlayerId, Int>()

    fun sync(completedTricksCount: Int, activePlaysCount: Int) {
        if (completedTricksCount == 0 && activePlaysCount == 0 && playedCardTokens.isNotEmpty()) {
            opponentHandTokens.clear()
            playedCardTokens.clear()
            nextTokenIndex.clear()
        }
    }

    fun tokenForHandCard(player: TricksPlayerId, slotIndex: Int): String {
        val list = opponentHandTokens.getOrPut(player) { mutableListOf() }
        while (list.size <= slotIndex) {
            val idx = nextTokenIndex.getOrPut(player) { 0 }
            nextTokenIndex[player] = idx + 1
            list.add("opp_${player.value}_c$idx")
        }
        return list[slotIndex]
    }

    fun tokenForPlayedCard(player: TricksPlayerId, card: Card, isMyPlayer: Boolean): String {
        if (isMyPlayer) return card.toString()
        return playedCardTokens.getOrPut(card) {
            val list = opponentHandTokens[player]
            if (!list.isNullOrEmpty()) {
                list.removeLast()
            } else {
                val idx = nextTokenIndex.getOrPut(player) { 0 }
                nextTokenIndex[player] = idx + 1
                "opp_${player.value}_c$idx"
            }
        }
    }
}

/**
 * @param animationDurationScale multiplier for animation durations; values above 1 slow them down for debugging.
 */
@Composable
fun TricksPlayerScreen(
    viewModel: TricksPlayerViewModel,
    modifier: Modifier = Modifier,
    animationDurationScale: Float = 1f,
) {
    require(animationDurationScale.isFinite() && animationDurationScale >= 1f) {
        "animationDurationScale must be finite and at least 1"
    }

    val viewState by viewModel.viewState.collectAsState()
    val errorMessage = viewModel.errorMessage
    var lastAvailableState by remember { mutableStateOf<TricksPlayerViewState?>(null) }
    LaunchedEffect(viewState) {
        if (viewState != null) lastAvailableState = viewState
    }
    val state = viewState ?: lastAvailableState

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading game state…", style = MaterialTheme.typography.bodyLarge)
            }
            return@Surface
        }

        SharedTransitionLayout {
            val sharedTransitionScope = this
            val cardTracker = remember(state.myPlayer.id) { TricksCardTracker() }
            cardTracker.sync(state.completedTricks.size, state.currentTrick.plays.size)

            var previousState by remember(state.myPlayer.id) { mutableStateOf(state) }
            var displayedState by remember(state.myPlayer.id) { mutableStateOf(state) }
            LaunchedEffect(state) {
                val oldState = previousState
                previousState = state

                val justCompletedTrick = state.completedTricks.lastOrNull()?.takeIf { completed ->
                    state.currentTrick.plays.isEmpty() &&
                            completed !in oldState.completedTricks
                }
                if (justCompletedTrick != null) {
                    displayedState = state.copy(
                        currentTrick = justCompletedTrick.trick,
                        completedTricks = oldState.completedTricks,
                    )
                    delay(animationDurationMillis(750, animationDurationScale).toLong())
                    displayedState = state
                } else {
                    displayedState = state
                }
            }
            val state = displayedState
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header: Player identity, turn status, trump indicator, tricks won
                HeaderSection(state = state)

                // Opponents summary
                if (state.opponents.isNotEmpty()) {
                    AnimatedContent(
                        targetState = state.opponents,
                        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                        label = "opponent cards",
                    ) { opponents ->
                        OpponentsSection(
                            state = state.copy(opponents = opponents),
                            cardTracker = cardTracker,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = this,
                            animationDurationScale = animationDurationScale,
                        )
                    }
                }

                HorizontalDivider()

                // Unified chat-like stream of tricks
                TricksFeedSection(
                    state = state,
                    cardTracker = cardTracker,
                    sharedTransitionScope = sharedTransitionScope,
                    animationDurationScale = animationDurationScale,
                    modifier = Modifier.weight(1f),
                )

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
                AnimatedContent(
                    targetState = state.myHand,
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    label = "player hand cards",
                ) { myHand ->
                    PlayerHandSection(
                        state = state.copy(myHand = myHand),
                        cardTracker = cardTracker,
                        actionInProgress = viewModel.actionInProgress,
                        onCardClick = viewModel::playCard,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this,
                        animationDurationScale = animationDurationScale,
                    )
                }
            }
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
                            val activeName =
                                state.opponents.firstOrNull { it.player.id == phase.activePlayer }?.player?.username
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
private fun OpponentsSection(
    state: TricksPlayerViewState,
    cardTracker: TricksCardTracker,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    animationDurationScale: Float,
) {
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
                    OpponentMiniHand(
                        playerId = opponent.player.id,
                        cardCount = opponent.cardCount,
                        cardTracker = cardTracker,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        animationDurationScale = animationDurationScale,
                    )
                    Text(
                        text = "(${opponent.cardCount})",
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
private fun OpponentMiniHand(
    playerId: TricksPlayerId,
    cardCount: Int,
    cardTracker: TricksCardTracker,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    animationDurationScale: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-11).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until cardCount) {
            val token = cardTracker.tokenForHandCard(playerId, i)
            key(token) {
                with(sharedTransitionScope) {
                    TricksCardView(
                        card = null,
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(token),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = scaledBoundsTransform(animationDurationScale),
                        ),
                        isFaceUp = false,
                        width = 18.dp,
                        height = 26.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TricksFeedSection(
    state: TricksPlayerViewState,
    cardTracker: TricksCardTracker,
    sharedTransitionScope: SharedTransitionScope,
    animationDurationScale: Float,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val isGameOver = state.phase is TricksGamePhase.GameOver
    val activeTrickNumber = state.completedTricks.size + 1

    LaunchedEffect(state.completedTricks.size, state.currentTrick.plays.size, isGameOver) {
        val totalCount = state.completedTricks.size + 1
        if (totalCount > 0) {
            withContext(
                object : MotionDurationScale {
                    override val scaleFactor = animationDurationScale
                },
            ) {
                listState.animateScrollToItem(totalCount - 1)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
    ) {
        itemsIndexed(
            items = state.completedTricks,
            key = { index, _ -> "trick_${index + 1}" },
        ) { index, completedTrick ->
            TrickItem(
                trickNumber = index + 1,
                trick = completedTrick.trick,
                winner = completedTrick.winner,
                isActive = false,
                state = state,
                cardTracker = cardTracker,
                sharedTransitionScope = sharedTransitionScope,
                animationDurationScale = animationDurationScale,
                modifier = Modifier.animateItem(),
            )
        }

        if (isGameOver) {
            item(key = "game_over_card") {
                GameOverSummaryCard(
                    phase = state.phase as TricksGamePhase.GameOver,
                    state = state,
                )
            }
        } else {
            item(key = "trick_$activeTrickNumber") {
                TrickItem(
                    trickNumber = activeTrickNumber,
                    trick = state.currentTrick,
                    winner = null,
                    isActive = true,
                    state = state,
                    cardTracker = cardTracker,
                    sharedTransitionScope = sharedTransitionScope,
                    animationDurationScale = animationDurationScale,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(
                            durationMillis = animationDurationMillis(350, animationDurationScale),
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun TrickItem(
    trickNumber: Int,
    trick: Trick,
    winner: TricksPlayerId?,
    isActive: Boolean,
    state: TricksPlayerViewState,
    cardTracker: TricksCardTracker,
    sharedTransitionScope: SharedTransitionScope,
    animationDurationScale: Float,
    modifier: Modifier = Modifier,
) {
    val allPlayersPlayed = trick.plays.size >= state.opponents.size + 1
    val winnerPlay = winner?.let { winnerId -> trick.plays.firstOrNull { it.player == winnerId } }
        ?: if (isActive && allPlayersPlayed) trick.evaluateWinner(state.trumpSuit) else null
    val winnerId = winner ?: winnerPlay?.player

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (winnerPlay != null) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (winnerPlay != null) 2.dp else 1.5.dp,
            color = if (winnerPlay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                alpha = 0.4f,
            ),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (isActive) {
                            stringResource(Res.string.tricks_current_trick, trickNumber)
                        } else {
                            stringResource(
                                Res.string.tricks_trick_won_by,
                                trickNumber,
                                winnerId?.let { playerName(it, state) }.orEmpty(),
                            )
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isActive && winnerPlay != null) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = "🏆 Won by ${playerName(winnerPlay.player, state)}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else if (isActive) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Text(
                                text = "Current",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                trick.leadSuit?.let { lead ->
                    Text(
                        text = stringResource(Res.string.tricks_lead_suit, lead.label),
                        style = MaterialTheme.typography.bodySmall,
                        color = suitColor(lead),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (trick.plays.isEmpty() && !isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
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
                AnimatedContent(
                    targetState = trick.plays,
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    label = "trick cards",
                ) { plays ->
                    val animatedVisibilityScope = this
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val firstPlayer = if (isActive) {
                            plays.firstOrNull()?.player
                                ?: (state.phase as? TricksGamePhase.PlayerTurn)?.activePlayer
                                ?: state.playerOrder.first()
                        } else {
                            plays.firstOrNull()?.player
                        }
                        val playerOrder = if (isActive) {
                            state.playerOrder.rotateFrom(firstPlayer ?: state.playerOrder.first())
                        } else {
                            plays.map { it.player }
                        }
                        val activePlayer = (state.phase as? TricksGamePhase.PlayerTurn)?.activePlayer

                        playerOrder.forEach { playerId ->
                            val play = plays.firstOrNull { it.player == playerId }
                            val cardModifier = play?.let {
                                val cardToken = cardTracker.tokenForPlayedCard(
                                    play.player,
                                    play.card,
                                    isMyPlayer = play.player == state.myPlayer.id,
                                )
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(cardToken),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = scaledBoundsTransform(animationDurationScale),
                                    )
                                }
                            }
                            TrickPlayerItem(
                                playerId = playerId,
                                play = play,
                                state = state,
                                modifier = cardModifier ?: Modifier,
                                isWinner = winnerId == playerId,
                                isActivePlayer = isActive && play == null && playerId == activePlayer,
                                leadSuit = trick.leadSuit,
                                animateFlip = isActive && play != null && play.player != state.myPlayer.id &&
                                        play == trick.plays.lastOrNull(),
                                animationDurationScale = animationDurationScale,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverSummaryCard(
    phase: TricksGamePhase.GameOver,
    state: TricksPlayerViewState,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🏁 ${stringResource(Res.string.tricks_game_over)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val winnerEntry = phase.tricksWon.maxByOrNull { it.value }
            if (winnerEntry != null) {
                val winnerName = playerName(winnerEntry.key, state)
                Text(
                    text = "Winner: $winnerName (${winnerEntry.value} tricks) 🏆",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun TrickPlayerItem(
    playerId: TricksPlayerId,
    play: PlayedCard?,
    state: TricksPlayerViewState,
    modifier: Modifier = Modifier,
    isWinner: Boolean,
    isActivePlayer: Boolean,
    leadSuit: Suit?,
    animateFlip: Boolean = false,
    animationDurationScale: Float,
    width: Dp = 64.dp,
    height: Dp = 90.dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = playerName(playerId, state),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isWinner || isActivePlayer) FontWeight.Bold else FontWeight.Normal,
                color = if (isWinner || isActivePlayer) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isWinner) {
                Text("👑", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (play != null) {
            TricksCardView(
                card = play.card,
                modifier = modifier,
                isFaceUp = true,
                animateFlip = animateFlip,
                isPlayable = false,
                isSelected = isWinner,
                width = width,
                height = height,
                flipDurationMillis = animationDurationMillis(350, animationDurationScale),
            )
        } else {
            val leadColor = leadSuit?.let(::suitColor)
            Box(
                modifier = Modifier
                    .size(width = width, height = height)
                    .background(
                        color = (leadColor ?: MaterialTheme.colorScheme.surface).copy(
                            alpha = if (leadColor == null) 0.25f else if (isActivePlayer) 0.2f else 0.1f,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = if (isActivePlayer) 2.dp else 1.dp,
                        color = if (isActivePlayer) MaterialTheme.colorScheme.primary
                        else leadColor?.copy(alpha = 0.7f)
                            ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
        }
    }
}

private fun <T> List<T>.rotateFrom(first: T): List<T> {
    val startIndex = indexOf(first).takeIf { it >= 0 } ?: return this
    return drop(startIndex) + take(startIndex)
}

private fun playerName(
    playerId: TricksPlayerId,
    state: TricksPlayerViewState,
): String = if (playerId == state.myPlayer.id) {
    state.myPlayer.username
} else {
    state.opponents.firstOrNull { it.player.id == playerId }?.player?.username
        ?: playerId.value
}

@Composable
private fun PlayerHandSection(
    state: TricksPlayerViewState,
    cardTracker: TricksCardTracker,
    actionInProgress: Boolean,
    onCardClick: (Card) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    animationDurationScale: Float,
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
                val cardToken = cardTracker.tokenForPlayedCard(state.myPlayer.id, card, isMyPlayer = true)

                key(cardToken) {
                    TricksCardView(
                        card = card,
                        modifier = with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(cardToken),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = scaledBoundsTransform(animationDurationScale),
                            )
                        },
                        isPlayable = isPlayable,
                        onClick = if (isPlayable) {
                            { onCardClick(card) }
                        } else null,
                    )
                }
            }
        }
    }
}

internal fun animationDurationMillis(durationMillis: Int, scale: Float): Int =
    (durationMillis * scale).roundToInt().coerceAtLeast(1)

private fun scaledBoundsTransform(scale: Float) =
    if (scale == 1f) {
        SharedTransitionDefaults.BoundsTransform
    } else {
        BoundsTransform { _, _ ->
            tween(durationMillis = animationDurationMillis(500, scale))
        }
    }
