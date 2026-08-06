package cz.kotu.game.gotfive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cz.kotu.game.gotfive.model.GameState
import cz.kotu.game.gotfive.model.Tile
import cz.kotu.game.gotfive.model.Tiles

private val showSecretTileValues = false

@Composable
fun Table(gameViewModel: GameViewModel, modifier: Modifier = Modifier) {
    val game = gameViewModel.gameState
    // Notes change on every drag event and must not restart the tile transition.
    val tileState = game.copy(notes = emptySet())

    SharedTransitionLayout {
        val sharedScope = this

        Column(modifier = modifier.fillMaxWidth()) {
            AnimatedContent(
                targetState = tileState,
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                label = "tile sections state",
            ) { state ->
                TileSections(
                    gameViewModel = gameViewModel,
                    game = state,
                    sharedScope = sharedScope,
                    visibilityScope = this,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                Button(onClick = gameViewModel::checkSolution) {
                    Text("Check solution")
                }

                Text(text = gameViewModel.result, modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp))

            }
            Text(text = "Notes:")
            PlayerTileNotes(
                modifier = Modifier.fillMaxWidth(),
                notes = game.notes,
                gameViewModel = gameViewModel,
            )
        }
    }
}

@Composable
private fun TileSections(
    gameViewModel: GameViewModel,
    game: GameState,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
) {
    @Composable
    fun sharedTileModifier(tile: Tile): Modifier = with(sharedScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(tile),
            animatedVisibilityScope = visibilityScope,
        )
    }

    Column {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")

        val animatedColor by infiniteTransition.animateColor(
            initialValue = Color.LightGray,
            targetValue = Color.White,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "colorPulse"
        )

        Text(text = "Pool:")
        HiddenPool(
            game = game,
            modifier = if (game.phase is GameState.Phase.PickFromPool) Modifier.background(animatedColor) else Modifier,
            sharedModifier = ::sharedTileModifier,
        ) { tile -> gameViewModel.pickOfferTile(tile) }

        Text(text = "Offer:")
        TileOffer(
            game = game,
            modifier = if (game.phase is GameState.Phase.PickFromOffer) Modifier.background(animatedColor) else Modifier,
            sharedModifier = ::sharedTileModifier,
        ) { tile -> gameViewModel.pickSortHint(tile) }

        Text("Phase: ")
        Row {
            val phase = game.phase
            when (phase) {
                is GameState.Phase.PickFromPool -> {
                    Text("Pick new tile color from the pool")
                }

                is GameState.Phase.PickFromOffer -> {
                    Text("Pick new tile from offer for number sort or dots compare")
                }
            }
        }

        Text(text = "Secret:")
        PlayerSecretFive(
            game = game,
            secretVisible = gameViewModel.secretVisible,
            sharedModifier = ::sharedTileModifier,
        )

    }
}

@Composable
fun HiddenPool(
    game: GameState,
    modifier: Modifier = Modifier,
    sharedModifier: @Composable (Tile) -> Modifier = { Modifier },
    onTileClick: (Tile) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 4.dp
        val tileSize = (maxWidth - spacing * 11) / 12

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            repeat(5) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    repeat(12) { column ->
                        val tile = game.shuffledPool.get(column * 5 + row)
                        if (tile in game.tilePool) {
                            TileView(
                                tile = tile,
                                showValues = showSecretTileValues,
                                onClick = { onTileClick(tile) },
                                modifier = Modifier
                                    .size(tileSize)
                                    .then(sharedModifier(tile)),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(tileSize))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TileOffer(
    game: GameState,
    modifier: Modifier = Modifier,
    sharedModifier: @Composable (Tile) -> Modifier = { Modifier },
    onTileClick: (Tile) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            game.tileOffer.forEach { tile ->
                TileView(
                    tile = tile,
                    onClick = { onTileClick(tile) },
                    modifier = Modifier
                        .size(64.dp)
                        .then(sharedModifier(tile)),
                )
            }
        }
    }
}

@Composable
fun PlayerSecretFive(
    game: GameState,
    secretVisible: Boolean,
    sharedModifier: @Composable (Tile) -> Modifier = { Modifier },
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val tiles = (game.secretTiles + game.sortTileHints).sorted()
            tiles.forEach { tile ->
                TileView(
                    tile = tile,
                    showValues = (tile !in game.secretTiles) || showSecretTileValues || secretVisible,
                    modifier = Modifier
                        .size(64.dp)
                        .then(sharedModifier(tile)),
                )
            }
        }
    }
}

@Composable
private fun PlayerTileNotes(
    modifier: Modifier,
    notes: Set<Tile>,
    gameViewModel: GameViewModel,
) {
    val currentNotes by rememberUpdatedState(notes)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = 4.dp
        val tileSize = (maxWidth - spacing * 11) / 12
        val density = LocalDensity.current
        val tileSizePx = with(density) { tileSize.toPx() }
        val spacingPx = with(density) { spacing.toPx() }

        fun tileAt(x: Float, y: Float): Tile? {
            val column = (x / (tileSizePx + spacingPx)).toInt()
            val row = (y / (tileSizePx + spacingPx)).toInt()
            val localX = x % (tileSizePx + spacingPx)
            val localY = y % (tileSizePx + spacingPx)

            return if (
                column in 0..11 && row in 0..4 &&
                localX <= tileSizePx && localY <= tileSizePx
            ) {
                Tiles.all[column * 5 + row]
            } else {
                null
            }
        }

        Column(
            modifier = Modifier.pointerInput(tileSize) {
                var targetState: Boolean? = null

                detectDragGestures(
                    onDragStart = { offset ->
                        tileAt(offset.x, offset.y)?.let { tile ->
                            targetState = tile !in currentNotes
                            gameViewModel.setNote(tile, targetState == true)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        tileAt(change.position.x, change.position.y)?.let { tile ->
                            targetState?.let { gameViewModel.setNote(tile, it) }
                        }
                    },
                    onDragEnd = { targetState = null },
                    onDragCancel = { targetState = null },
                )
            },
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            repeat(5) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    repeat(12) { column ->
                        val tile = Tiles.all[column * 5 + row]
                        val selected = tile in notes
                        // Recreate the tile's draw modifiers when selection changes.
                        // This avoids stale background rendering during rapid drag updates.
                        key(tile.number, selected) {
                            TileView(
                                tile = tile,
                                selected = selected,
                                onClick = { gameViewModel.toggleNote(tile) },
                                modifier = Modifier.size(tileSize),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    showValues: Boolean = true,
    onClick: () -> Unit = {},
) {
    val baseColor = Color(
        red = tile.color.red / 255f,
        green = tile.color.green / 255f,
        blue = tile.color.blue / 255f,
    )
    val tileColor = if (selected) {
        baseColor
    } else {
        val luminance =
            baseColor.red * 0.299f +
                    baseColor.green * 0.587f +
                    baseColor.blue * 0.114f
        Color(luminance, luminance, luminance)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tileColor)
            .clickable(onClick = onClick)
            .then(if (selected) Modifier else Modifier.alpha(0.35f))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showValues) {
            Text(
                text = tile.number.toString(),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(tile.dots) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
        }
    }
}
