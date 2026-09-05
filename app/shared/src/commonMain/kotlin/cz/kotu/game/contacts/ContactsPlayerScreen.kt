package cz.kotu.game.contacts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.LogToken
import cz.kotu.game.contacts.model.PlayerViewState

private const val phi = 1.618f

val hintBackgroundColor = Color(0xFFCCDDCC)

val selectedContactColor = Color(0xFF1976D2)
val highlightedContactColor = Color(0xFFFFB300)
val highlightedLogContactColor = Color(0xFFAA00FF)

@Composable
fun ContactsPlayerScreen(
    viewModel: ContactsPlayerViewModel,
) {
    val currentGameState by viewModel.gameState.collectAsState()

    if (currentGameState == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Connecting to game...")
        }
        return
    }

    val gameState = currentGameState!!
    val actionSelectionState = viewModel.actionSelectionState
    val player = gameState.you
    val logs: List<GameLogEntry> by viewModel.gameFacade.logs.collectAsState()
    val isLogsExpanded = viewModel.isLogsExpanded

    val logItemContent: @Composable (GameLogEntry) -> Unit =
        { RichGameLogItem(it, gameState, player, viewModel.hoveredLogs) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualPane = maxWidth >= 600.dp

        if (isLogsExpanded && !isDualPane) {
            GameLogsDialog(
                logs = logs,
                logItemContent = logItemContent,
                onClose = { viewModel.isLogsExpanded = false },
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(text = "Player: " + player.username)

                    SolvedContactsPool(gameState.pool)

                    Text(
                        text = "Faults: " + if (gameState.faults == 0) "0" else "X".repeat(gameState.faults),
                        color = Color(0xFFCC0000),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    GameLogsCollapsedView(
                        logs = logs,
                        logItemContent = logItemContent,
                        onExpand = { viewModel.isLogsExpanded = true },
                    )

                    gameState.racks.filter { it.owner != player }.forEach { rack ->
                        RackView(
                            gameState = gameState,
                            rack = rack,
                            isOwner = false,
                            lastActionResult = gameState.lastActionResult,
                            hoveredLogs = viewModel.hoveredLogs,
                            selectedContacts = actionSelectionState.otherContacts,
                            onContactClick = { contact ->
                                viewModel.onOtherContactClick(contact)
                            },
                        )
                    }

                    gameState.racks.filter { it.owner == player }.forEach { rack ->
                        RackView(
                            gameState = gameState,
                            rack = rack,
                            isOwner = true,
                            lastActionResult = gameState.lastActionResult,
                            hoveredLogs = viewModel.hoveredLogs,
                            selectedContacts = actionSelectionState.playerContacts,
                        ) { contact ->
                            viewModel.onPlayerContactClick(contact)
                        }
                    }
                }

                if (isLogsExpanded && isDualPane) {
                    GameLogsSidePane(
                        logs = logs,
                        logItemContent = logItemContent,
                        onClose = { viewModel.isLogsExpanded = false },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .border(1.dp, Color(0xFFDDDDDD)),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8E8E8))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val selectedActionType = viewModel.selectedActionType
                    gameState.actions.allowedActionTypes.forEach { actionType ->
                        val isSelected = actionType == selectedActionType
                        Button(
                            onClick = { viewModel.selectActionType(actionType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFFBDBDBD),
                                contentColor = if (isSelected) Color.White else Color.Black,
                            ),
                        ) {
                            Text(actionType.name)
                        }
                    }
                }

                gameState.actions.actionStatusText?.let { text ->
                    Text(
                        text = text,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val validationError = viewModel.actionError()

                validationError?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFCC0000),
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // TODO Text(text = "Player on turn: " + gameState.activePlayer)

                    Button(
                        enabled = viewModel.validAction(),
                        onClick = {
                            viewModel.confirmAction()
                        },
                    ) {
                        Text("Confirm selection")
                    }
                }
            }
        }
    }
}

/* Sorted contacts pool with solved tiles highlighted */
@Composable
private fun SolvedContactsPool(pool: List<PlayerViewState.PoolContact>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "Contacts Pool")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val groups = pool.groupBy { it.value }.entries.sortedBy { it.value.first().value }

            val spacing = 4.dp
            val poolTileWidth = (((maxWidth - spacing * groups.size) / groups.size)).coerceAtMost(40.dp)
            val poolTileHeight = poolTileWidth * phi

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                groups.forEach { (_, contacts) ->
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        contacts.forEach { contact ->
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(),
                            ) {

                                FlippableContactTile(
                                    contact = contact.value,
                                    size = DpSize(poolTileWidth, poolTileHeight),
                                    isSolved = contact.solved,
                                    isOwned = true,
                                    solvedBackgroundColor = Color(0xFF4CAF50),
                                    unsolvedBackgroundColor = Color(0xFFBDBDBD),
                                    solvedTextColor = Color.White,
                                    unsolvedTextColor = Color.Black,
                                )

                                contact.help?.let { hint ->
                                    Text(
                                        text = hint,
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        color = Color.DarkGray,
                                        fontSize = (poolTileHeight / 5.dp).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RackView(
    gameState: PlayerViewState,
    rack: PlayerViewState.Rack,
    isOwner: Boolean,
    lastActionResult: ContactsBoardState.ActionResult,
    hoveredLogs: MutableState<List<LogToken>>,
    selectedContacts: Set<ContactsBoardState.ContactId>,
    onContactClick: (ContactsBoardState.ContactId) -> Unit,
) {
    Column {
        Text(text = "Owner: " + rack.owner.username)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxContacts = gameState.racks.maxOf { it.contacts.size }

            val spacing = 8.dp
            val tileWidth = ((maxWidth - spacing * maxContacts) / maxContacts).coerceAtMost(48.dp)
            val tileHeight = tileWidth * phi
            val cornerRadius = tileWidth / 8
            val cornerShape = RoundedCornerShape(cornerRadius)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing, alignment = Alignment.CenterHorizontally),
            ) {
                rack.contacts.forEach { contact ->
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val resolveMultiConnectContacts = gameState.actions.resolveMultiConnectContacts
                        val isResolutionContact = resolveMultiConnectContacts?.contains(contact.id) ?: false

                        val shadowColor = when {
                            hoveredLogs.value.any { it is LogToken.Contact && it.contactId == contact.id } -> highlightedLogContactColor
                            isResolutionContact -> highlightedContactColor
                            lastActionResult.errorContacts.contains(contact.id) -> Color.Red
                            else -> null
                        }

                        val shakeOffset = remember { Animatable(0f) }

                        val isError = lastActionResult.errorContacts.contains(contact.id)

                        LaunchedEffect(isError) {
                            if (isError) {
                                shakeOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = keyframes {
                                        durationMillis = 400
                                        0f at 0
                                        -12f at 50
                                        12f at 100
                                        -9f at 150
                                        9f at 200
                                        -5f at 250
                                        5f at 300
                                        0f at 400
                                    },
                                )
                            }
                        }

                        FlippableContactTile(
                            contact = contact.value,
                            size = DpSize(tileWidth, tileHeight),
                            isSolved = contact.solved,
                            isOwned = isOwner,
                            solvedBackgroundColor = Color(0xFF808080),
                            unsolvedBackgroundColor = when {
                                contact.id in selectedContacts -> selectedContactColor
                                isResolutionContact -> highlightedContactColor
                                else -> Color(0xFF4A4A4A)
                            },
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = shakeOffset.value
                                }
                                .border(1.dp, color = shadowColor ?: Color.Transparent, cornerShape)
                                .shadow(
                                    elevation = if (shadowColor != null) 8.dp else 0.dp,
                                    shape = cornerShape,
                                    ambientColor = shadowColor ?: DefaultShadowColor,
                                    spotColor = shadowColor ?: DefaultShadowColor,
                                )
                                .clickable(
                                    enabled = !contact.solved &&
                                            (resolveMultiConnectContacts == null || isResolutionContact),
                                    onClick = { onContactClick(contact.id) },
                                ),
                        )

                        Box(
                            modifier = Modifier
                                .size(width = tileWidth, height = tileWidth / phi),
                            contentAlignment = Alignment.Center,
                        ) {
                            contact.hint?.let { hint ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(hintBackgroundColor, cornerShape)
                                        .border(1.dp, color = Color.Black, cornerShape),
                                )
                                Text(
                                    text = hint,
                                    color = Color.DarkGray,
                                    fontSize = (tileHeight / 4.dp).sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlippableContactTile(
    contact: ContactsBoardState.ContactValue?,
    size: DpSize,
    isSolved: Boolean,
    isOwned: Boolean,
    solvedBackgroundColor: Color,
    unsolvedBackgroundColor: Color,
    modifier: Modifier = Modifier,
    solvedTextColor: Color = Color.White,
    unsolvedTextColor: Color = Color.White,
) {

    Flippable(
        isFlipped = !isSolved,
        modifier = modifier.size(size),
        durationMillis = 1000,
        cameraDistanceDp = 12.dp,
        front = {
            // solved
            ContactTileView(
                contact = contact,
                size = size,
                backgroundColor = solvedBackgroundColor,
                modifier = modifier,
                textColor = solvedTextColor,
            )
        },
        back = {
            // unsolved
            ContactTileView(
                contact = if (isOwned) contact else null, // keep hidden for animation
                size = size,
                backgroundColor = unsolvedBackgroundColor,
                modifier = modifier,
                textColor = unsolvedTextColor,
            )
        },
    )
}

@Composable
private fun ContactTileView(
    contact: ContactsBoardState.ContactValue?,
    size: DpSize,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val cornerRadius = size.width / 8
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier.size(size).background(backgroundColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (contact != null) {
            Box(
                modifier = Modifier
                    .size(size.width, size.height / 5)
                    .align(Alignment.TopCenter)
                    .background(
                        color = when (contact.type) {
                            ContactsBoardState.ContactType.Blue -> Color.Blue
                            ContactsBoardState.ContactType.Yellow -> Color.Yellow
                            ContactsBoardState.ContactType.Red -> Color.Red
                        },
                        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
                    ),
            )
        }
        Text(
            text = contact?.number?.toString() ?: "?",
            color = textColor,
            fontSize = (size.height / 3.dp).sp,
            fontWeight = if (contact == null) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
