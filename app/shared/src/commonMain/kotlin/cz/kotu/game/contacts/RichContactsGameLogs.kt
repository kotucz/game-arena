package cz.kotu.game.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.LogToken
import cz.kotu.game.contacts.model.PlayerViewState
import cz.kotu.game.contacts.model.findContact
import kotlinx.serialization.json.Json

@Composable
fun RichGameLogItem(log: GameLogEntry, gameState: PlayerViewState, player: ContactsBoardState.Player) {
    Row {
        // TODO move parsing from UI
        val logTokens = remember(log.text) {
            runCatching {
                Json.decodeFromString<List<LogToken>>(log.text)
            }.getOrNull()
        }
        if (logTokens != null) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                logTokens.forEach { token ->
                    when (token) {
                        is LogToken.Contact -> {
                            // logs may be loaded but game state not
                            val contact = gameState.findContact(token.contactId)
                            val value = contact?.value
                            Text(
                                text = value?.number?.toString() ?: "?",
                                modifier = Modifier.background(
                                    when (value?.type) {
                                        ContactsBoardState.ContactType.Yellow -> Color.Yellow
                                        ContactsBoardState.ContactType.Red -> Color.Red
                                        else -> Color.LightGray
                                    },
                                ).padding(horizontal = 4.dp),
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            contact?.hint?.let { hint ->
                                Text(
                                    text = hint,
                                    modifier = Modifier.background(hintBackgroundColor)
                                        .padding(horizontal = 4.dp),
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        is LogToken.Player -> Text(
                            text = token.username,
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        is LogToken.Text -> Text(
                            text = token.text,
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                        )
                    }
                }

                Text(
                    text = log.timestamp.formatLocalUi(),
                    modifier = Modifier.weight(1f),
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.End,
                )
            }
        } else {
            Text(
                text = log.text,
                modifier = Modifier.weight(1f),
                color = Color.DarkGray,
                fontSize = 13.sp,
            )
        }

    }
}