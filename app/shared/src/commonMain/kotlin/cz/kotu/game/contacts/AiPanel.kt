package cz.kotu.game.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AiPanel(viewModel: AiPanelViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = viewModel.prompt,
                onValueChange = viewModel::updatePrompt,
                modifier = Modifier.weight(1f),
                label = { Text("AI prompt") },
                enabled = !viewModel.isSending,
                maxLines = 3,
            )
            Button(
                onClick = viewModel::send,
                enabled = viewModel.prompt.isNotBlank() && !viewModel.isSending,
            ) {
                Text(if (viewModel.isSending) "Sending…" else "Send")
            }
        }
        viewModel.error?.let { error ->
            Text(error, color = Color(0xFFCC0000))
        }
    }
}
