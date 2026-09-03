package cz.kotu.game.contacts

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.GameLogEntry

private const val phi = 1.618f

val hintBackgroundColor = Color(0xFFCCDDCC)

@Composable
fun ContactsPlayerScreen(
    viewModel: ContactsPlayerViewModel,
) {
    val actionSelectionState = viewModel.actionSelectionState
    val gameState: ContactsGameState by viewModel.gameFacade.gameState.collectAsState()
    val boardState = gameState.board
    val player = viewModel.player
    val logs: List<GameLogEntry> by viewModel.gameFacade.logs.collectAsState()
    val isLogsExpanded = viewModel.isLogsExpanded
    val resolution = boardState.resolveMultiConnect
    val availableActionTypes = viewModel.availableActionTypes()
    val selectedActionType = viewModel.selectedActionType
    val resolutionTargetContacts = viewModel.resolutionTargetContacts()
    val resolutionClickableContacts = viewModel.resolutionClickableContacts()

    val logItemContent: @Composable (GameLogEntry) -> Unit = { RichGameLogItem(it, boardState, player) }

    LaunchedEffect(resolution) {
        viewModel.resetActionSelection()
    }

    LaunchedEffect(availableActionTypes) {
        viewModel.updateSelectedActionTypeIfNeeded(availableActionTypes)
    }

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
            LaunchedEffect(boardState.solved) {
                viewModel.updateActionSelectionForSolved()
            }

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
                    Text(text = "Player: " + player?.username)

                    SolvedContactsPool(boardState)

                    Text(
                        text = "Faults: " + if (boardState.faults == 0) "0" else "X".repeat(boardState.faults),
                        color = Color(0xFFCC0000),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    GameLogsCollapsedView(
                        logs = logs,
                        logItemContent = logItemContent,
                        onExpand = { viewModel.isLogsExpanded = true },
                    )

                    boardState.racks.filter { it.owner != player }.forEach { rack ->
                        RackView(
                            board = boardState,
                            rack = rack,
                            isOwner = false,
                            selectedContacts = actionSelectionState.otherContacts,
                            clickableContacts = resolutionClickableContacts,
                            highlightedContacts = resolutionTargetContacts.orEmpty(),
                            onContactClick = { contact ->
                                viewModel.onOtherContactClick(contact)
                            },
                        )
                    }

                    boardState.racks.filter { it.owner == player }.forEach { rack ->
                        RackView(
                            board = boardState,
                            rack = rack,
                            isOwner = true,
                            selectedContacts = actionSelectionState.playerContacts,
                            clickableContacts = resolutionClickableContacts,
                            highlightedContacts = resolutionTargetContacts.orEmpty(),
                            onContactClick = { contact ->
                                viewModel.onPlayerContactClick(contact)
                            },
                        )
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

            if (player != null)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8E8E8))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableActionTypes.forEach { actionType ->
                            val isSelected = actionType == selectedActionType
                            Button(
                                onClick = { viewModel.selectActionType(actionType) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFFBDBDBD),
                                    contentColor = if (isSelected) Color.White else Color.Black
                                )
                            ) {
                                Text(actionType.name)
                            }
                        }
                    }

                    if (resolution != null) {
                        if (resolution.targetPlayer == player) {
                            Text("Original contact: ${boardState.contact(resolution.originalContact)?.number ?: "?"}")
                        } else {
                            Text("Waiting for ${resolution.targetPlayer.username} to resolve the multi-connect")
                        }
                    }

                    val validationError = viewModel.actionError()

                    validationError?.let { error ->
                        Text(
                            text = error,
                            color = Color(0xFFCC0000),
                            textAlign = TextAlign.Center,
                        )
                    }

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

/* Sorted contacts pool with solved tiles highlighted */
@Composable
private fun SolvedContactsPool(board: ContactsBoardState) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "Contacts Pool")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val groups = board.pool.groupBy { it.number to it.type }.entries.sortedBy { it.value.first() }

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
                            FlippableContactTile(
                                contact = contact,
                                size = DpSize(poolTileWidth, poolTileHeight),
                                isSolved = board.isSolved(contact),
                                isOwned = true,
                                solvedBackgroundColor = Color(0xFF4CAF50),
                                unsolvedBackgroundColor = Color(0xFFBDBDBD),
                                solvedTextColor = Color.White,
                                unsolvedTextColor = Color.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RackView(
    board: ContactsBoardState,
    rack: ContactsBoardState.Rack,
    isOwner: Boolean,
    selectedContacts: Set<ContactsBoardState.Contact>,
    clickableContacts: Set<ContactsBoardState.Contact>?,
    highlightedContacts: Set<ContactsBoardState.Contact>,
    onContactClick: (ContactsBoardState.Contact) -> Unit,
) {
    Column {
        Text(text = "Owner: " + rack.owner.username)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxContacts = board.racks.maxOf { it.contactIds.size }

            val spacing = 8.dp
            val tileWidth = ((maxWidth - spacing * maxContacts) / maxContacts).coerceAtMost(64.dp)
            val tileHeight = tileWidth * phi

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing, alignment = Alignment.CenterHorizontally),
            ) {
                board.rackContacts(rack).forEach { contact ->
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FlippableContactTile(
                            contact = contact,
                            size = DpSize(tileWidth, tileHeight),
                            isSolved = board.isSolved(contact),
                            isOwned = isOwner,
                            solvedBackgroundColor = Color(0xFF808080),
                            unsolvedBackgroundColor = when {
                                contact in selectedContacts -> Color(0xFF1976D2)
                                contact in highlightedContacts -> Color(0xFFFFB300)
                                else -> Color(0xFF4A4A4A)
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = if (contact in highlightedContacts) 8.dp else 0.dp,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable(
                                    enabled = !board.isSolved(contact) &&
                                            (clickableContacts == null || contact in clickableContacts),
                                    onClick = { onContactClick(contact) },
                                ),
                        )

                        Box(
                            modifier = Modifier
                                .size(width = tileWidth, height = tileWidth / phi),
                            contentAlignment = Alignment.Center,
                        ) {
                            rack.hint(contact)?.let { hint ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(hintBackgroundColor, RoundedCornerShape(8.dp))
                                        .border(1.dp, color = Color.Black, RoundedCornerShape(8.dp)),
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
    contact: ContactsBoardState.Contact,
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
                isSecret = false,
                modifier = modifier,
                textColor = solvedTextColor,
            )
        },
        back = {
            // unsolved
            ContactTileView(
                contact = contact,
                size = size,
                backgroundColor = unsolvedBackgroundColor,
                isSecret = !isOwned,
                modifier = modifier,
                textColor = unsolvedTextColor,
            )
        }
    )
}

@Composable
private fun ContactTileView(
    contact: ContactsBoardState.Contact,
    size: DpSize,
    isSecret: Boolean,
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
        if (!isSecret) {
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
            text = if (isSecret) "?" else contact.number.toString(),
            color = textColor,
            fontSize = (size.height / 3.dp).sp,
            fontWeight = if (isSecret) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
