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

@Composable
fun GameLogsCollapsedView(
    logs: List<GameLogEntry>,
    logItemContent: LogItemContent = ::DefaultItemRenderer,
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

        GameLogsList(logs, logItemContent)
    }
}

@Composable
fun GameLogsDialog(
    logs: List<GameLogEntry>,
    logItemContent: LogItemContent = ::DefaultItemRenderer,
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
                logItemContent = logItemContent,
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
    logItemContent: LogItemContent = ::DefaultItemRenderer,
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFAFAFA),
    ) {
        GameLogsFullView(
            logs = logs,
            logItemContent = logItemContent,
            onClose = onClose,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun GameLogsFullView(
    logs: List<GameLogEntry>,
    modifier: Modifier = Modifier,
    logItemContent: LogItemContent = ::DefaultItemRenderer,
    onClose: () -> Unit,
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

        GameLogsList(logs, logItemContent)
    }
}

@Composable
private fun GameLogsList(logs: List<GameLogEntry>, logItemContent: LogItemContent = ::DefaultItemRenderer) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items = logs.asReversed()) { log ->
            logItemContent(log)
        }
    }
}

typealias LogItemContent = @Composable (log: GameLogEntry) -> Unit
@Composable
private fun DefaultItemRenderer(log: GameLogEntry) {
    Row {
        Text(
            text = log.text,
            modifier = Modifier.weight(1f),
            color = Color.DarkGray,
            fontSize = 13.sp,
        )
        Text(
            text = log.timestamp.formatLocalUi(),
            color = Color.DarkGray,
            fontSize = 13.sp,
        )
    }
}

