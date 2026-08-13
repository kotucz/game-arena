package cz.kotu.game.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade

@Composable
fun ContactsPlayerScreen(
    gameFacade: ContactsGameFacade,
    player: ContactsBoardState.Player,
) {
    var playerContact by remember { mutableStateOf<ContactsBoardState.Contact?>(null) }
    var otherContact by remember { mutableStateOf<ContactsBoardState.Contact?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        val gameState: ContactsBoardState = gameFacade.gameState.value

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(text = "Player: " + player.username)

            gameState.racks.filter { it.owner != player }.forEach { rack ->
                RackView(
                    rack = rack,
                    isOwner = false,
                    selectedContact = otherContact,
                    onContactClick = { otherContact = if (otherContact == it) null else it },
                )
            }

            gameState.racks.filter { it.owner == player }.forEach { rack ->
                RackView(
                    rack = rack,
                    isOwner = true,
                    selectedContact = playerContact,
                    onContactClick = { playerContact = if (playerContact == it) null else it },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8E8E8))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                enabled = playerContact != null && otherContact != null,
                onClick = {
                    val selectedPlayerContact = playerContact ?: return@Button
                    val selectedOtherContact = otherContact ?: return@Button
                    gameFacade.action(
                        player,
                        ContactsGameFacade.Action.Connect(
                            playerContact = selectedPlayerContact,
                            otherContact = selectedOtherContact,
                        ),
                    )
                    playerContact = null
                    otherContact = null
                },
            ) {
                Text("Confirm selection")
            }
        }
    }
}

@Composable
private fun RackView(
    rack: ContactsBoardState.Rack,
    isOwner: Boolean,
    selectedContact: ContactsBoardState.Contact?,
    onContactClick: (ContactsBoardState.Contact) -> Unit,
) {
    Column {
        Text(text = "Owner: " + rack.owner.username)

        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rack.contacts.forEach { contact ->
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .aspectRatio(1f / 1.618f)
                        .background(
                            if (contact == selectedContact) Color(0xFF1976D2) else Color(0xFF4A4A4A),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(8.dp)
                        .clickable { onContactClick(contact) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contact.number.toString(),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
