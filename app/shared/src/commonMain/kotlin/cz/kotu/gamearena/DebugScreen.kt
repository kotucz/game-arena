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

@Composable
fun DebugScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Debug Screen", modifier = Modifier.padding(top = 16.dp))
        Button(onClick = {
            Notifications().showNotification()
        }) { Text("Notify") }
    }
}
