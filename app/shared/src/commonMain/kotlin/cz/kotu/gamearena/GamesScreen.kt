package cz.kotu.gamearena

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cz.kotu.game.contacts.formatLocalUi
import cz.kotu.gamearena.model.RunningGame
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.arena_create_contacts_game
import gamearena.app.shared.generated.resources.arena_created
import gamearena.app.shared.generated.resources.arena_debug
import gamearena.app.shared.generated.resources.arena_filter_all_games
import gamearena.app.shared.generated.resources.arena_filter_my_games
import gamearena.app.shared.generated.resources.arena_no_running_games
import gamearena.app.shared.generated.resources.arena_not_logged_in
import gamearena.app.shared.generated.resources.arena_logout
import gamearena.app.shared.generated.resources.arena_running_games
import gamearena.app.shared.generated.resources.arena_start_got_five
import gamearena.app.shared.generated.resources.arena_status
import gamearena.app.shared.generated.resources.arena_theme
import gamearena.app.shared.generated.resources.arena_theme_dark
import gamearena.app.shared.generated.resources.arena_theme_light
import gamearena.app.shared.generated.resources.arena_theme_system
import gamearena.app.shared.generated.resources.arena_try_again
import gamearena.app.shared.generated.resources.contacts_game_name
import org.jetbrains.compose.resources.stringResource

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onStartGotFive: () -> Unit,
    onDebugClick: () -> Unit,
    onGameClick: (RunningGame) -> Unit,
) {
    val games by viewModel.games.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val username = currentUser?.username

    LaunchedEffect(Unit) {
        viewModel.observeLobby()
    }

    if (viewModel.createGameDialogVisible.value) {
        CreateGameDialog(
            viewModel = viewModel,
            onGameCreated = onGameClick,
            onClose = { viewModel.createGameDialogVisible.value = false },
        )
    }

    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            ThemeModeSelector(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            verticalAlignment = CenterVertically,
        ) {
            currentUser?.let { user ->
                PlayerAvatar(
                    pictureUrl = user.pictureUrl,
                    username = user.username,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(username ?: stringResource(Res.string.arena_not_logged_in))
            Button(onClick = viewModel::logout) {
                Text(stringResource(Res.string.arena_logout))
            }
        }

        Button(onClick = onStartGotFive, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.arena_start_got_five))
        }

        Button(
            onClick = { viewModel.createGameDialogVisible.value = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.arena_create_contacts_game))
        }

        Button(onClick = onDebugClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.arena_debug))
        }

        HorizontalDivider()

        Text(stringResource(Res.string.arena_running_games))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            verticalAlignment = CenterVertically,
        ) {

            GameFilterSegmentedButton(
                selectedTab = viewModel.gameFilterMyAll.value,
                onTabSelected = { viewModel.gameFilterMyAll.value = it },
            )
        }

        when {
            games == null && error == null -> CircularProgressIndicator()
            error != null -> {
                Text(error!!)
                Button(onClick = viewModel::loadGames) { Text(stringResource(Res.string.arena_try_again)) }
            }

            games!!.isEmpty() -> Text(stringResource(Res.string.arena_no_running_games))
            else -> games!!
                .filter {
                    viewModel.gameFilterMyAll.value == GameFilterTab.All ||
                        currentUser?.uid?.let { uid -> it.players.any { player -> player.uid == uid } } == true
                }
                .sortedByDescending { it.createdAt }
                .forEach { game ->
                    RunningGameCard(
                        game = game,
                        onClick = { onGameClick(game) },
                    )
                }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(Res.string.arena_theme, stringResource(themeMode.stringResource)))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.stringResource)) },
                    onClick = {
                        expanded = false
                        onThemeModeChange(mode)
                    },
                )
            }
        }
    }
}

enum class GameFilterTab(
    val icon: ImageVector,
) {
    All(Icons.Outlined.Public),
    My(Icons.Outlined.Person)
}

@Composable
fun GameFilterSegmentedButton(
    selectedTab: GameFilterTab,
    onTabSelected: (GameFilterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = GameFilterTab.entries

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selectedTab == tab) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    }
                },
            ) {
                Text(
                    text = stringResource(
                        when (tab) {
                            GameFilterTab.All -> Res.string.arena_filter_all_games
                            GameFilterTab.My -> Res.string.arena_filter_my_games
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun RunningGameCard(
    game: RunningGame,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    when (game.type) {
                        "contacts" -> stringResource(Res.string.contacts_game_name)
                        else -> game.type.replaceFirstChar { it.uppercase() }
                    },
                )
                Text("  ${game.id}")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                game.players.forEach { player ->
                    Row(verticalAlignment = CenterVertically) {
                        PlayerAvatar(
                            pictureUrl = player.pictureUrl,
                            username = player.username,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = player.username,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            Text(stringResource(Res.string.arena_created, game.createdAt.formatLocalUi()))
            Text(stringResource(Res.string.arena_status, game.status))
        }
    }
}

private val AppThemeMode.stringResource
    get() = when (this) {
        AppThemeMode.SYSTEM -> Res.string.arena_theme_system
        AppThemeMode.LIGHT -> Res.string.arena_theme_light
        AppThemeMode.DARK -> Res.string.arena_theme_dark
    }
