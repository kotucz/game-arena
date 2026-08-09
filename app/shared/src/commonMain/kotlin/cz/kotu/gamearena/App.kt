package cz.kotu.gamearena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.game.gotfive.GameViewModelFactory
import cz.kotu.game.gotfive.Table

@Composable
@Preview
fun App() {
    MaterialTheme {
        val authenticated = remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (authenticated.value) {
                val gameViewModel: GameViewModel = viewModel(factory = GameViewModelFactory)
                Table(gameViewModel, Modifier.width(960.dp).align(Alignment.CenterHorizontally))
            } else {
                AuthScreen { authenticated.value = true }
            }
        }
    }
}
