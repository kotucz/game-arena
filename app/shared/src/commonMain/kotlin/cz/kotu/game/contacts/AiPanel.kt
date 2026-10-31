package cz.kotu.game.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.contacts_ai_prompt
import gamearena.app.shared.generated.resources.contacts_send
import gamearena.app.shared.generated.resources.contacts_sending
import org.jetbrains.compose.resources.stringResource

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
                label = { Text(stringResource(Res.string.contacts_ai_prompt)) },
                enabled = !viewModel.isSending,
                maxLines = 3,
            )
            Button(
                onClick = viewModel::send,
                enabled = viewModel.prompt.isNotBlank() && !viewModel.isSending,
            ) {
                Text(
                    stringResource(
                        if (viewModel.isSending) Res.string.contacts_sending else Res.string.contacts_send,
                    ),
                )
            }
        }
        viewModel.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}
