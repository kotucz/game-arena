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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.ic_check
import gamearena.app.shared.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import cz.kotu.game.gotfive.model.GameState
import cz.kotu.game.gotfive.model.Tile
import cz.kotu.game.gotfive.model.Tiles

private val showSecretTileValues = false

@Composable
fun Table(gameViewModel: GameViewModel, modifier: Modifier = Modifier) {
    val game = gameViewModel.gameState
    val resultIsWin = gameViewModel.result.startsWith("Correct")
    val resultContainer = when {
        !gameViewModel.secretVisible -> MaterialTheme.colorScheme.surfaceVariant
        resultIsWin -> Color(0xFFDFF3E3)
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val resultContent = when {
        !gameViewModel.secretVisible -> MaterialTheme.colorScheme.onSurfaceVariant
        resultIsWin -> Color(0xFF14532D)
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    // Notes change on every drag event and must not restart the tile transition.
    val tileState = game.copy(notes = emptySet())

    SharedTransitionLayout {
        val sharedScope = this

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = gameViewModel::checkSolution,
                ) {
                    Text("Check solution")
                }

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(resultContainer)
                        .border(1.dp, resultContent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (gameViewModel.secretVisible) {
                            Icon(
                                painter = painterResource(
                                    if (resultIsWin) Res.drawable.ic_check else Res.drawable.ic_close
                                ),
                                contentDescription = null,
                                tint = resultContent,
                            )
                        }
                        Text(
                            text = gameViewModel.result,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (gameViewModel.secretVisible) FontWeight.Bold else FontWeight.Normal,
                            color = resultContent,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Notes",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Tap or drag across tiles to mark your solution.",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

    BoxWithConstraints {
        val sharedTileSize = ((maxWidth - 16.dp) / 9).coerceIn(24.dp, 64.dp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")

            val animatedColor by infiniteTransition.animateColor(
                initialValue = Color.LightGray, targetValue = Color.White, animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse,
                ), label = "colorPulse"
            )

            Text(
                text = "Pool",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            HiddenPool(
                game = game,
                modifier = if (game.phase is GameState.Phase.PickFromPool) Modifier.background(animatedColor) else Modifier,
                sharedModifier = ::sharedTileModifier,
            ) { tile -> gameViewModel.pickOfferTile(tile) }

            Text(
                text = "Offer",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            TileOffer(
                game = game,
                tileSize = sharedTileSize,
                modifier = if (game.phase is GameState.Phase.PickFromOffer) Modifier.background(animatedColor) else Modifier,
                sharedModifier = ::sharedTileModifier,
            ) { tile -> gameViewModel.pickHintTile(tile) }

            Text(
                "Next move",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).then(
                        if (game.phase is GameState.Phase.ChooseHint) {
                            Modifier.background(animatedColor)
                        } else {
                            Modifier
                        }
                    ).padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val phase = game.phase
                when (phase) {
                    is GameState.Phase.PickFromPool -> {
                        Text(
                            "Pick new tile color from the pool",
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }

                    is GameState.Phase.PickFromOffer -> {
                        Text(
                            "Pick new tile from offer for number sort or dots compare",
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }

                    is GameState.Phase.ChooseHint -> {
                        Button(onClick = { gameViewModel.pickSortHint(phase.tile) }) {
                            Text(text = "Use ${phase.tile.number} for sorting")
                        }
                        Text(
                            text = "or",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Pick a secret tile to compare dots: ${
                                (1..phase.tile.dots).joinToString(separator = "") { "•" }
                            }",
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Secret tiles",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Find the values in ascending order from left to right.",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlayerSecretFive(
                game = game,
                tileSize = sharedTileSize,
                modifier = if (game.phase is GameState.Phase.ChooseHint) Modifier.background(animatedColor) else Modifier,
                gameViewModel = gameViewModel,
                secretVisible = gameViewModel.secretVisible,
                sharedModifier = ::sharedTileModifier,
            )

        }
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
        val tileSize = ((maxWidth - spacing * 11) / 12).coerceAtLeast(16.dp)

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
                                clickable = game.phase is GameState.Phase.PickFromPool,
                                onClick = { onTileClick(tile) },
                                modifier = Modifier.size(tileSize).then(sharedModifier(tile)),
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
    tileSize: Dp,
    modifier: Modifier = Modifier,
    sharedModifier: @Composable (Tile) -> Modifier = { Modifier },
    onTileClick: (Tile) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val spacing = 4.dp
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            game.tileOffer.forEach { tile ->
                val gamePhase = game.phase
                TileView(
                    tile = tile,
                    outline = if (gamePhase is GameState.Phase.ChooseHint && gamePhase.tile == tile) Color.Black else Color.Transparent,
                    clickable = gamePhase is GameState.Phase.PickFromOffer || gamePhase is GameState.Phase.ChooseHint,
                    onClick = { onTileClick(tile) },
                    modifier = Modifier.size(tileSize).then(sharedModifier(tile)),
                )
            }
        }
    }
}

@Composable
fun PlayerSecretFive(
    game: GameState,
    tileSize: Dp,
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel,
    secretVisible: Boolean,
    sharedModifier: @Composable (Tile) -> Modifier = { Modifier },
) {
    Box(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        val hintTileSize = (tileSize * 0.66f).coerceIn(16.dp, 42.dp)
        Row(
            modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val tiles = (game.secretTiles + game.sortTileHints).sorted()
            tiles.forEach { tile ->
                if (tile in game.secretTiles) {
                    // secret tile
                    Column {
                        TileView(
                            tile = tile,
                            showValues = showSecretTileValues || secretVisible,
                            hiddenText = if (showSecretTileValues || secretVisible) null else "?",
                            modifier = Modifier.size(tileSize).then(sharedModifier(tile)),
                            outline = Color.Black,
                            clickable = game.phase is GameState.Phase.ChooseHint,
                            onClick = { gameViewModel.pickDotsHintSecretTile(tile) },
                        )
                        game.dotHints[tile]?.forEach { dotHintTile ->
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val match = dotHintTile.dots == tile.dots
                                TileView(
                                    tile = dotHintTile,
                                    modifier = Modifier.size(hintTileSize).then(sharedModifier(dotHintTile)),
                                    outline = if (match) Color.Green else Color.Red,
                                )
                                Icon(
                                    painter = painterResource(
                                        if (match) Res.drawable.ic_check else Res.drawable.ic_close
                                    ),
                                    contentDescription = null,
                                    tint = if (match) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                } else {
                    // sorted hint tile
                    TileView(
                        tile = tile,
                        modifier = Modifier.size(hintTileSize).then(sharedModifier(tile)),
                    )
                }
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
        val tileSize = ((maxWidth - spacing * 11) / 12).coerceAtLeast(16.dp)
        val density = LocalDensity.current
        val tileSizePx = with(density) { tileSize.toPx() }
        val spacingPx = with(density) { spacing.toPx() }

        fun tileAt(x: Float, y: Float): Tile? {
            val column = (x / (tileSizePx + spacingPx)).toInt()
            val row = (y / (tileSizePx + spacingPx)).toInt()
            val localX = x % (tileSizePx + spacingPx)
            val localY = y % (tileSizePx + spacingPx)

            return if (column in 0..11 && row in 0..4 && localX <= tileSizePx && localY <= tileSizePx) {
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
                                clickable = true,
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
    hiddenText: String? = null,
    outline: Color? = null,
    clickable: Boolean = false,
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
        val luminance = baseColor.red * 0.299f + baseColor.green * 0.587f + baseColor.blue * 0.114f
        Color(luminance, luminance, luminance)
    }

    BoxWithConstraints(
        modifier = modifier.then(if (selected) Modifier else Modifier.alpha(0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        // TileView is used at several sizes (the pool, notes, secret tiles, and hints).
        // Scale all content from the available width so small tiles remain readable.
        val contentScale = (maxWidth / 64.dp).coerceIn(0.45f, 1f)
        val dotSize = 8.dp * contentScale
        val dotSpacing = 4.dp * contentScale
        val tileShape = RoundedCornerShape(16.dp * contentScale)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(tileShape)
                .background(tileColor)
                .border(2.dp, outline ?: Color.Transparent, tileShape)
                .clickable(enabled = clickable, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (showValues) {
                    Text(
                        text = tile.number.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize * contentScale,
                            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight * contentScale,
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(tile.dots) {
                            Box(
                                modifier = Modifier.size(dotSize).clip(CircleShape).background(Color.White),
                            )
                        }
                    }
                } else if (hiddenText != null) {
                    Text(
                        text = hiddenText,
                        color = Color.Black,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize * contentScale,
                            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight * contentScale,
                        ),
                    )
                }
            }
        }
    }
}
