package cz.kotu.gamearena

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.kotu.common.Notifications
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.arena_back
import gamearena.app.shared.generated.resources.arena_debug_screen
import gamearena.app.shared.generated.resources.arena_notify
import org.jetbrains.compose.resources.stringResource

@Composable
fun DebugScreen(
    notifications: Notifications,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(Res.string.arena_back)) }
        Text(stringResource(Res.string.arena_debug_screen), modifier = Modifier.padding(top = 16.dp))
        Button(
            onClick = {
                notifications.showNotification()
            },
        ) { Text(stringResource(Res.string.arena_notify)) }
    }
}
