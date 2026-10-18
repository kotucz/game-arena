package cz.kotu.game.contacts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AiPanelViewModel(
    private val scope: CoroutineScope,
    private val commandAiPlayer: suspend (String) -> Result<Unit>,
) {
    var prompt by mutableStateOf("")
        private set

    var isSending by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun updatePrompt(value: String) {
        prompt = value
        error = null
    }

    fun send() {
        val command = prompt.trim()
        if (command.isEmpty() || isSending) return

        isSending = true
        error = null
        scope.launch {
            commandAiPlayer(command)
                .onSuccess { prompt = "" }
                .onFailure { failure -> error = failure.message ?: "AI command failed" }
            isSending = false
        }
    }
}
