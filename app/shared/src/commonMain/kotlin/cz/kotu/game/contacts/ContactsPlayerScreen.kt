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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.GameLogEntry

private const val phi = 1.618f

@Composable
fun ContactsPlayerScreen(
    viewModel: ContactsPlayerViewModel,
) {
    val actionSelectionState = viewModel.actionSelectionState
    val gameState: ContactsBoardState by viewModel.gameFacade.gameState.collectAsState()
    val player = viewModel.player
    val logs: List<GameLogEntry> by viewModel.gameFacade.logs.collectAsState()
    val isLogsExpanded = viewModel.isLogsExpanded
    val resolution = gameState.resolveMultiConnect
    val availableActionTypes = viewModel.availableActionTypes()
    val selectedActionType = viewModel.selectedActionType
    val resolutionTargetContacts = viewModel.resolutionTargetContacts()
    val resolutionClickableContacts = viewModel.resolutionClickableContacts()

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
                onClose = { viewModel.isLogsExpanded = false },
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            LaunchedEffect(gameState.solved) {
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

                    SolvedContactsPool(gameState)

                    Text(
                        text = "Faults: " + if (gameState.faults == 0) "0" else "X".repeat(gameState.faults),
                        color = Color(0xFFCC0000),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    GameLogsCollapsedView(
                        logs = logs,
                        onExpand = { viewModel.isLogsExpanded = true },
                    )

                    gameState.racks.filter { it.owner != player }.forEach { rack ->
                        RackView(
                            gameState = gameState,
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

                    gameState.racks.filter { it.owner == player }.forEach { rack ->
                        RackView(
                            gameState = gameState,
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
                            Text("Original contact: ${gameState.contact(resolution.originalContact)?.number ?: "?"}")
                        } else {
                            Text("Waiting for ${resolution.targetPlayer.username} to resolve the multi-connect")
                        }
                    }

                    val validationError = viewModel.validationError()
                    val validAction = viewModel.validAction()

                    validationError?.let { error ->
                        Text(
                            text = error,
                            color = Color(0xFFCC0000),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Button(
                        enabled = validAction,
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
private fun SolvedContactsPool(gameState: ContactsBoardState) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = "Contacts Pool")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val groups = gameState.pool.groupBy { it.number to it.type }.entries.sortedBy { it.value.first() }

            val spacing = 4.dp
            val poolTileWidth = (((maxWidth - spacing * groups.size) / groups.size)).coerceAtMost(40.dp)
            val poolTileHeight = poolTileWidth * phi
            val poolFontSize = (18f * (poolTileWidth / 30.dp).coerceIn(0.55f, 1f)).sp

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
                            ContactTileView(
                                contact = contact,
                                tileWidth = poolTileWidth,
                                tileHeight = poolTileHeight,
                                backgroundColor = if (gameState.isSolved(contact)) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                                isSecret = false,
                                fontSize = poolFontSize,
                                fontWeight = FontWeight.Bold,
                                textColor = if (gameState.isSolved(contact)) Color.White else Color.Black,
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
    gameState: ContactsBoardState,
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
            val maxContacts = gameState.racks.maxOf { it.contactIds.size }

            val spacing = 8.dp
            val tileWidth = ((maxWidth - spacing * maxContacts) / maxContacts).coerceAtMost(64.dp)
            val tileHeight = tileWidth * phi
            val numberFontSize = (24f * (tileWidth / 50.dp).coerceIn(0.55f, 1f)).sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing, alignment = Alignment.CenterHorizontally),
            ) {
                gameState.rackContacts(rack).forEach { contact ->
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ContactTileView(
                            contact = contact,
                            tileWidth = tileWidth,
                            tileHeight = tileHeight,
                            backgroundColor = when {
                                gameState.isSolved(contact) -> Color(0xFF808080)
                                contact in selectedContacts -> Color(0xFF1976D2)
                                contact in highlightedContacts -> Color(0xFFFFB300)
                                else -> Color(0xFF4A4A4A)
                            },
                            isSecret = !isOwner && !gameState.isSolved(contact),
                            fontSize = numberFontSize,
                            fontWeight = if (isOwner || gameState.isSolved(contact)) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .shadow(
                                    elevation = if (contact in highlightedContacts) 8.dp else 0.dp,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable(
                                    enabled = !gameState.isSolved(contact) &&
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
                                        .background(Color(0xFFCCDDCC), RoundedCornerShape(8.dp))
                                        .border(1.dp, color = Color.Black, RoundedCornerShape(8.dp)),
                                )
                                Text(
                                    text = hint,
                                    color = Color.DarkGray,
                                    fontSize = numberFontSize,
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
private fun ContactTileView(
    contact: ContactsBoardState.Contact,
    tileWidth: androidx.compose.ui.unit.Dp,
    tileHeight: androidx.compose.ui.unit.Dp,
    backgroundColor: Color,
    isSecret: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
) {
    val cornerRadius = tileWidth / 8
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier.size(tileWidth, tileHeight).background(backgroundColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (!isSecret) {
            Box(
                modifier = Modifier
                    .size(tileWidth, tileHeight / 5)
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
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
        )
    }
}
