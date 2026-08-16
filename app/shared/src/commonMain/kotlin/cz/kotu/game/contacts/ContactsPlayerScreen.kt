package cz.kotu.game.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.game.contacts.model.ActionSelectionState
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade

private const val phi = 1.618f

@Composable
fun ContactsPlayerScreen(
    gameFacade: ContactsGameFacade,
    player: ContactsBoardState.Player,
) {
    var actionSelectionState by remember { mutableStateOf<ActionSelectionState>(ActionSelectionState.None) }
    val gameState: ContactsBoardState by gameFacade.gameState.collectAsState()
    var selectedActionType by remember(gameState.allowedActionTypes) { 
        mutableStateOf(gameState.allowedActionTypes.firstOrNull()) 
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LaunchedEffect(gameState.solved) {
            val state = actionSelectionState
            val newPlayerContacts = state.playerContacts.filter { !gameState.isSolved(it) }.toSet()
            val newOtherContacts = state.otherContacts.filter { !gameState.isSolved(it) }.toSet()
            if (newPlayerContacts != state.playerContacts || newOtherContacts != state.otherContacts) {
                actionSelectionState = if (newPlayerContacts.isEmpty() && newOtherContacts.isEmpty()) {
                    ActionSelectionState.None
                } else {
                    ActionSelectionState.MultiConnect(playerContacts = newPlayerContacts, otherContacts = newOtherContacts)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(text = "Player: " + player.username)

            SolvedContactsPool(gameState)

            Text(
                text = "Faults: " + if (gameState.faults == 0) "0" else "X".repeat(gameState.faults),
                color = Color(0xFFCC0000),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            gameState.racks.filter { it.owner != player }.forEach { rack ->
                RackView(
                    gameState = gameState,
                    rack = rack,
                    isOwner = false,
                    selectedContacts = actionSelectionState.otherContacts,
                    onContactClick = { contact ->
                        val state = actionSelectionState
                        val newOtherContacts = if (contact in state.otherContacts) state.otherContacts - contact else state.otherContacts + contact
                        actionSelectionState = ActionSelectionState.MultiConnect(playerContacts = state.playerContacts, otherContacts = newOtherContacts)
                    },
                )
            }

            gameState.racks.filter { it.owner == player }.forEach { rack ->
                RackView(
                    gameState = gameState,
                    rack = rack,
                    isOwner = true,
                    selectedContacts = actionSelectionState.playerContacts,
                    onContactClick = { contact ->
                        val state = actionSelectionState
                        val newPlayerContacts = if (contact in state.playerContacts) state.playerContacts - contact else state.playerContacts + contact
                        actionSelectionState = ActionSelectionState.MultiConnect(playerContacts = newPlayerContacts, otherContacts = state.otherContacts)
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8E8E8))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val playerContacts = actionSelectionState.playerContacts
            val otherContacts = actionSelectionState.otherContacts

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gameState.allowedActionTypes.forEach { actionType ->
                    val isSelected = actionType == selectedActionType
                    Button(
                        onClick = { selectedActionType = actionType },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF1976D2) else Color(0xFFBDBDBD),
                            contentColor = if (isSelected) Color.White else Color.Black
                        )
                    ) {
                        Text(actionType.name)
                    }
                }
            }

            val validAction = selectedActionType?.matches(playerContacts.size, otherContacts.size) == true
            
            Button(
                enabled = validAction,
                onClick = {
                    gameFacade.action(
                        player,
                        selectedActionType!!,
                        playerContacts,
                        otherContacts,
                    )
                    actionSelectionState = ActionSelectionState.None
                },
            ) {
                Text("Confirm selection")
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
            val spacing = 4.dp
            val poolTileWidth = (((maxWidth - spacing * 11) / 12) * 0.75f).coerceAtMost(40.dp)
            val poolFontSize = (18f * (poolTileWidth / 30.dp).coerceIn(0.55f, 1f)).sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                // Group contacts by number and sort by number
                gameState.pool.groupBy { it.number }.entries.sortedBy { it.key }.forEach { (_, contacts) ->
                    Column(
                        modifier = Modifier.width(poolTileWidth),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        contacts.forEach { contact ->
                            Box(
                                modifier = Modifier
                                    .width(poolTileWidth)
                                    .aspectRatio(1f / phi)
                                    .background(
                                        if (gameState.isSolved(contact)) Color(0xFF4CAF50) else Color(
                                            0xFFBDBDBD
                                        ),
                                        RoundedCornerShape(6.dp),
                                    )
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = contact.number.toString(),
                                    color = if (gameState.isSolved(contact)) Color.White else Color.Black,
                                    fontSize = poolFontSize,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
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
private fun RackView(
    gameState: ContactsBoardState,
    rack: ContactsBoardState.Rack,
    isOwner: Boolean,
    selectedContacts: Set<ContactsBoardState.Contact>,
    onContactClick: (ContactsBoardState.Contact) -> Unit,
) {
    Column {
        Text(text = "Owner: " + rack.owner.username)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 8.dp
            val tileWidth = ((maxWidth - spacing * 11) / 12).coerceAtMost(64.dp)
            val numberFontSize = (24f * (tileWidth / 50.dp).coerceIn(0.55f, 1f)).sp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                gameState.contacts(rack).forEach { contact ->
                    Column(
                        modifier = Modifier.width(tileWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(tileWidth)
                                .aspectRatio(1f / phi)
                                .background(
                                    when {
                                        gameState.isSolved(contact) -> Color(0xFF808080)
                                        contact in selectedContacts -> Color(0xFF1976D2)
                                        else -> Color(0xFF4A4A4A)
                                    },
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(8.dp)
                                .clickable(
                                    enabled = !gameState.isSolved(contact),
                                    onClick = { onContactClick(contact) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isOwner || gameState.isSolved(contact)) {
                                    contact.number.toString()
                                } else {
                                    "?"
                                },
                                color = Color.White,
                                fontSize = numberFontSize,
                                fontWeight = if (isOwner || gameState.isSolved(contact)) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                textAlign = TextAlign.Center,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tileWidth / phi),
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
//                                    fontSize = (12f * (tileWidth / 50.dp).coerceIn(0.55f, 1f)).sp,
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
