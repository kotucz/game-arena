package cz.kotu.game.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(modifier = Modifier.wrapContentWidth().fillMaxHeight().verticalScroll(rememberScrollState())) {
        val gameState: ContactsBoardState = gameFacade.gameState.value

        Text(text = "Player: " + player.username)

        gameState.racks.filter { it.owner != player }.forEach { rack ->
            RackView(rack, isOwner = false)
        }

        gameState.racks.filter { it.owner == player }.forEach { rack ->
            RackView(rack, isOwner = true)
        }
    }
}

@Composable
private fun RackView(
    rack: ContactsBoardState.Rack,
    isOwner: Boolean,
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
                        .background(Color(0xFF4A4A4A), RoundedCornerShape(8.dp))
                        .padding(8.dp),
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
