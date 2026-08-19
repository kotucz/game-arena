package cz.kotu.game.contacts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.kotu.game.contacts.model.GameLogEntry
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun GameLogsCollapsedView(
    logs: List<GameLogEntry>,
    onExpand: () -> Unit,
) {
    if (logs.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onExpand)
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
            .padding(8.dp)
            .sizeIn(minHeight = 56.dp)
            .height(96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Logs",
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tap to expand",
                color = Color.DarkGray,
                fontSize = 12.sp,
            )
        }

        GameLogsList(logs)
    }
}

@Composable
fun GameLogsDialog(
    logs: List<GameLogEntry>,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            color = Color.White,
        ) {
            GameLogsFullView(
                logs = logs,
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun GameLogsSidePane(
    logs: List<GameLogEntry>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFAFAFA),
    ) {
        GameLogsFullView(
            logs = logs,
            onClose = onClose,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun GameLogsFullView(
    logs: List<GameLogEntry>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Logs",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Button(onClick = onClose) {
                Text("Close")
            }
        }

        GameLogsList(logs)
    }
}

@Composable
private fun GameLogsList(logs: List<GameLogEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items = logs.asReversed()) { log ->
            Row {
                Text(
                    text = log.text,
                    modifier = Modifier.weight(1f),
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                )
                Text(
                    text = Instant.fromEpochMilliseconds(log.timestamp)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).time.format(LocalTime.Formats.ISO),
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
