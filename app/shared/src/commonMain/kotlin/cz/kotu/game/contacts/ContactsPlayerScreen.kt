package cz.kotu.game.contacts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameDetails
import cz.kotu.game.contacts.model.ContactsGameOverReason
import cz.kotu.game.contacts.model.ContactsGameState
import cz.kotu.game.contacts.model.ContactsPlayer
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.LogToken
import cz.kotu.game.contacts.model.PlayerViewState
import cz.kotu.game.contacts.model.findContact
import cz.kotu.gamearena.PlayerAvatar
import gamearena.app.shared.generated.resources.Res
import gamearena.app.shared.generated.resources.contacts_action_add_hint
import gamearena.app.shared.generated.resources.contacts_action_double_connect
import gamearena.app.shared.generated.resources.contacts_action_finish_reds
import gamearena.app.shared.generated.resources.contacts_action_my_double_connect
import gamearena.app.shared.generated.resources.contacts_action_resolve_multi_connect
import gamearena.app.shared.generated.resources.contacts_action_solo_connect_rest
import gamearena.app.shared.generated.resources.contacts_action_standard_connect
import gamearena.app.shared.generated.resources.contacts_action_triple_connect
import gamearena.app.shared.generated.resources.contacts_bonus_hint
import gamearena.app.shared.generated.resources.contacts_bonus_my_double
import gamearena.app.shared.generated.resources.contacts_bonus_other_double
import gamearena.app.shared.generated.resources.contacts_bonus_other_triple
import gamearena.app.shared.generated.resources.contacts_bonus_state_locked
import gamearena.app.shared.generated.resources.contacts_bonus_state_ready
import gamearena.app.shared.generated.resources.contacts_bonus_state_used
import gamearena.app.shared.generated.resources.contacts_common_bonuses
import gamearena.app.shared.generated.resources.contacts_confirm_selection
import gamearena.app.shared.generated.resources.contacts_connecting
import gamearena.app.shared.generated.resources.contacts_count
import gamearena.app.shared.generated.resources.contacts_faults
import gamearena.app.shared.generated.resources.contacts_owner
import gamearena.app.shared.generated.resources.contacts_player
import gamearena.app.shared.generated.resources.contacts_pool
import gamearena.app.shared.generated.resources.contacts_retry
import gamearena.app.shared.generated.resources.contacts_solo_action
import gamearena.app.shared.generated.resources.contacts_solo_category_number
import gamearena.app.shared.generated.resources.contacts_status_empty_game
import gamearena.app.shared.generated.resources.contacts_status_game_over_all_contacts_solved
import gamearena.app.shared.generated.resources.contacts_status_game_over_red_connected
import gamearena.app.shared.generated.resources.contacts_status_original_contact
import gamearena.app.shared.generated.resources.contacts_status_player_turn
import gamearena.app.shared.generated.resources.contacts_status_waiting_for_multi_connect
import gamearena.app.shared.generated.resources.contacts_yellow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val phi = 1.618f

val selectedContactColor = Color(0xFF1976D2)
val highlightedContactColor = Color(0xFFFFB300)
val highlightedLogContactColor = Color(0xFFAA00FF)

@Composable
fun ContactsPlayerScreen(
    viewModel: ContactsPlayerViewModel,
    gameDetailsState: ContactsGameDetailsUiState = ContactsGameDetailsUiState(isLoading = false),
    onRetryLoadGameDetails: () -> Unit = {},
) {
    val currentGameState by viewModel.gameState.collectAsState()

    if (currentGameState == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(Res.string.contacts_connecting))
        }
        return
    }

    val gameState = currentGameState!!
    val actionSelectionState = viewModel.actionSelectionState
    val player = gameState.you
    val logs: List<GameLogEntry> by viewModel.gameFacade.logs.collectAsState()
    val isLogsExpanded = viewModel.isLogsExpanded

    val logItemContent: @Composable (GameLogEntry) -> Unit =
        { RichGameLogItem(it, gameState, viewModel.hoveredLogs, gameDetailsState.details) }
    val aiPanel: (@Composable () -> Unit)? = if (viewModel.hasAiPlayer) {
        { AiPanel(viewModel.aiPanelViewModel) }
    } else {
        null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualPane = maxWidth >= 600.dp

        if (isLogsExpanded && !isDualPane) {
            GameLogsDialog(
                logs = logs,
                logItemContent = logItemContent,
                onClose = { viewModel.isLogsExpanded = false },
                aiPanel = aiPanel,
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ContactsPlayerIdentity(
                        label = stringResource(
                            Res.string.contacts_player,
                            gameDetailsState.details?.player(player.uid)?.username ?: player.username,
                        ),
                        username = gameDetailsState.details?.player(player.uid)?.username ?: player.username,
                        pictureUrl = gameDetailsState.details?.player(player.uid)?.pictureUrl,
                    )

                    gameDetailsState.error?.let { error ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = error, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetryLoadGameDetails) {
                                Text(stringResource(Res.string.contacts_retry))
                            }
                        }
                    }

                    SolvedContactsPool(gameState.pool)

                    val commonBonuses = gameState.bonuses.filter { it.player == null }
                    if (commonBonuses.isNotEmpty()) {
                        CommonBonusesSection(commonBonuses)
                    }

                    Text(
                        text = stringResource(
                            Res.string.contacts_faults,
                            if (gameState.faults == 0) "0" else "X".repeat(gameState.faults),
                        ),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    GameLogsCollapsedView(
                        logs = logs,
                        logItemContent = logItemContent,
                        onExpand = { viewModel.isLogsExpanded = true },
                    )

                    gameState.racks.filter { it.owner != player }
                        .groupBy { it.owner }
                        .forEach { (owner, racks) ->
                            PlayerRacksGroup(
                                player = owner,
                                gameDetails = gameDetailsState.details,
                                racks = racks,
                                bonuses = gameState.bonuses,
                                isOwner = false,
                                gameState = gameState,
                                lastActionResult = gameState.lastActionResult,
                                hoveredLogs = viewModel.hoveredLogs,
                                selectedContacts = actionSelectionState.otherContacts,
                                aiEnabled = viewModel.hasAiPlayer,
                                onContactClick = { contact ->
                                    viewModel.onOtherContactClick(contact)
                                },
                            )
                        }

                    val yourRacks = gameState.racks.filter { it.owner == player }
                    if (yourRacks.isNotEmpty()) {
                        PlayerRacksGroup(
                            player = player,
                            gameDetails = gameDetailsState.details,
                            racks = yourRacks,
                            bonuses = gameState.bonuses,
                            isOwner = true,
                            gameState = gameState,
                            lastActionResult = gameState.lastActionResult,
                            hoveredLogs = viewModel.hoveredLogs,
                            selectedContacts = actionSelectionState.playerContacts,
                            aiEnabled = viewModel.hasAiPlayer,
                            onContactClick = { contact ->
                                viewModel.onPlayerContactClick(contact)
                            },
                        )
                    }
                }

                if (isLogsExpanded && isDualPane) {
                    GameLogsSidePane(
                        logs = logs,
                        logItemContent = logItemContent,
                        onClose = { viewModel.isLogsExpanded = false },
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        aiPanel = aiPanel,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val selectedActionType = viewModel.selectedActionType
                    gameState.actions.allowedActionTypes.forEach { actionType ->
                        if (actionType == ContactsBoardState.ActionType.SoloConnectRest) {
                            gameState.actions.soloConnectRestGroups.forEach groupLoop@{ group ->
                                val value = group.firstOrNull()?.let { gameState.findContact(it)?.value }
                                    ?: return@groupLoop
                                val category = if (value.type == ContactsBoardState.ContactType.Yellow) {
                                    stringResource(Res.string.contacts_yellow)
                                } else {
                                    stringResource(Res.string.contacts_solo_category_number, value.number)
                                }
                                val count = pluralStringResource(
                                    Res.plurals.contacts_count,
                                    group.size,
                                    group.size,
                                )
                                ActionSelectionButton(
                                    text = stringResource(Res.string.contacts_solo_action, category, count),
                                    selected = selectedActionType == actionType &&
                                            actionSelectionState.playerContacts == group &&
                                            actionSelectionState.otherContacts.isEmpty(),
                                    enabled = !viewModel.actionInProgress,
                                    onClick = { viewModel.selectSoloConnectRestGroup(group) },
                                )
                            }
                        } else {
                            ActionSelectionButton(
                                text = stringResource(
                                    when (actionType) {
                                        ContactsBoardState.ActionType.AddHint -> Res.string.contacts_action_add_hint
                                        ContactsBoardState.ActionType.StandardConnect ->
                                            Res.string.contacts_action_standard_connect
                                        ContactsBoardState.ActionType.DoubleConnect ->
                                            Res.string.contacts_action_double_connect
                                        ContactsBoardState.ActionType.TripleConnect ->
                                            Res.string.contacts_action_triple_connect
                                        ContactsBoardState.ActionType.MyDoubleConnect ->
                                            Res.string.contacts_action_my_double_connect
                                        ContactsBoardState.ActionType.SoloConnectRest ->
                                            Res.string.contacts_action_solo_connect_rest
                                        ContactsBoardState.ActionType.FinishReds ->
                                            Res.string.contacts_action_finish_reds
                                        ContactsBoardState.ActionType.ResolveMultiConnect ->
                                            Res.string.contacts_action_resolve_multi_connect
                                    },
                                ),
                                selected = actionType == selectedActionType,
                                enabled = !viewModel.actionInProgress,
                                onClick = { viewModel.selectActionType(actionType) },
                            )
                        }
                    }
                }

                gameState.actions.status?.let { status ->
                    Text(
                        text = when (status) {
                            is ContactsGameState.PlayerActionStatus.PlayerTurn ->
                                stringResource(
                                    Res.string.contacts_status_player_turn,
                                    status.player.displayUsername(gameState.players, gameDetailsState.details),
                                )
                            is ContactsGameState.PlayerActionStatus.OriginalContact ->
                                stringResource(Res.string.contacts_status_original_contact, status.matchKey)
                            is ContactsGameState.PlayerActionStatus.WaitingForMultiConnectResolution ->
                                stringResource(
                                    Res.string.contacts_status_waiting_for_multi_connect,
                                    status.player.displayUsername(gameState.players, gameDetailsState.details),
                                )
                            is ContactsGameState.PlayerActionStatus.GameOver -> when (status.reason) {
                                ContactsGameOverReason.AllContactsSolved ->
                                    stringResource(Res.string.contacts_status_game_over_all_contacts_solved)
                                ContactsGameOverReason.RedConnected ->
                                    stringResource(Res.string.contacts_status_game_over_red_connected)
                                ContactsGameOverReason.EmptyGame ->
                                    stringResource(Res.string.contacts_status_empty_game)
                            }
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }

                val validationError = viewModel.actionError()

                validationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // TODO Text(text = "Player on turn: " + gameState.activePlayer)

                    Button(
                        enabled = viewModel.validAction(),
                        onClick = {
                            viewModel.confirmAction()
                        },
                    ) {
                        Text(stringResource(Res.string.contacts_confirm_selection))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSelectionButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedContactColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text)
    }
}

/* Sorted contacts pool with solved tiles highlighted */
@Composable
private fun SolvedContactsPool(pool: List<PlayerViewState.PoolContact>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = stringResource(Res.string.contacts_pool))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val groups = pool.groupBy { it.value }.entries.sortedBy { it.value.first().value }

            val spacing = 4.dp
            val poolTileWidth = (((maxWidth - spacing * groups.size) / groups.size)).coerceAtMost(40.dp)
            val poolTileHeight = poolTileWidth * phi

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                groups.forEach { (_, contacts) ->
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        contacts.forEach { contact ->
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(),
                            ) {

                                FlippableContactTile(
                                    contact = contact.value,
                                    size = DpSize(poolTileWidth, poolTileHeight),
                                    isSolved = contact.solved,
                                    isOwned = true,
                                    solvedBackgroundColor = Color(0xFF4CAF50),
                                    unsolvedBackgroundColor = Color(0xFFBDBDBD),
                                    solvedTextColor = Color.White,
                                    unsolvedTextColor = Color.Black,
                                )

                                contact.help?.let { hint ->
                                    Text(
                                        text = hint,
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        color = Color.Black,
                                        fontSize = (poolTileHeight / 5.dp).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonBonusesSection(commonBonuses: List<ContactsBoardState.Bonus>) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.contacts_common_bonuses),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            commonBonuses.forEach { bonus ->
                BonusChip(bonus)
            }
        }
    }
}

@Composable
private fun PlayerRacksGroup(
    player: ContactsPlayer,
    gameDetails: ContactsGameDetails?,
    racks: List<PlayerViewState.Rack>,
    bonuses: List<ContactsBoardState.Bonus>,
    isOwner: Boolean,
    gameState: PlayerViewState,
    lastActionResult: ContactsBoardState.ActionResult,
    hoveredLogs: MutableState<List<LogToken>>,
    selectedContacts: Set<ContactsBoardState.ContactId>,
    aiEnabled: Boolean,
    onContactClick: (ContactsBoardState.ContactId) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val profile = gameDetails?.player(player.uid)
        val profileUsername = profile?.username?.takeIf { it.isNotBlank() }
        val rawUsername = player.username
        val playerTitle = if (profileUsername != null && profileUsername != rawUsername) {
            "$profileUsername ($rawUsername)"
        } else {
            profileUsername ?: rawUsername
        }

        ContactsPlayerIdentity(
            label = stringResource(Res.string.contacts_owner, playerTitle),
            username = profileUsername ?: rawUsername,
            pictureUrl = profile?.pictureUrl,
        )

        val playerBonuses = bonuses.filter { it.player == player.playerId }
        if (playerBonuses.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                playerBonuses.forEach { bonus ->
                    BonusChip(bonus)
                }
            }
        }

        racks.forEach { rack ->
            RackView(
                gameState = gameState,
                rack = rack,
                isOwner = isOwner,
                lastActionResult = lastActionResult,
                hoveredLogs = hoveredLogs,
                selectedContacts = selectedContacts,
                aiEnabled = aiEnabled,
                onContactClick = onContactClick,
            )
        }
    }
}

@Composable
private fun BonusChip(bonus: ContactsBoardState.Bonus) {
    val typeName = stringResource(
        when (bonus.type) {
            ContactsBoardState.BonusType.Hint -> Res.string.contacts_bonus_hint
            ContactsBoardState.BonusType.OtherDouble -> Res.string.contacts_bonus_other_double
            ContactsBoardState.BonusType.OtherTriple -> Res.string.contacts_bonus_other_triple
            ContactsBoardState.BonusType.MyDouble -> Res.string.contacts_bonus_my_double
        },
    )

    val stateText = when (bonus.state) {
        ContactsBoardState.BonusState.Locked ->
            stringResource(
                Res.string.contacts_bonus_state_locked,
                bonus.unlockNumber?.toString() ?: "?",
            )

        ContactsBoardState.BonusState.Ready ->
            stringResource(Res.string.contacts_bonus_state_ready)

        ContactsBoardState.BonusState.Used ->
            stringResource(Res.string.contacts_bonus_state_used)
    }

    val (chipBackground, chipText) = when (bonus.state) {
        ContactsBoardState.BonusState.Ready ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer

        ContactsBoardState.BonusState.Locked ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant

        ContactsBoardState.BonusState.Used ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.5f,
            )
    }

    Box(
        modifier = Modifier
            .background(chipBackground, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$typeName: $stateText",
            color = chipText,
            fontSize = 12.sp,
            fontWeight = if (bonus.state == ContactsBoardState.BonusState.Ready) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RackView(
    gameState: PlayerViewState,
    rack: PlayerViewState.Rack,
    isOwner: Boolean,
    lastActionResult: ContactsBoardState.ActionResult,
    hoveredLogs: MutableState<List<LogToken>>,
    selectedContacts: Set<ContactsBoardState.ContactId>,
    aiEnabled: Boolean,
    onContactClick: (ContactsBoardState.ContactId) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxContacts = gameState.racks.maxOf { it.contacts.size }

        val spacing = 8.dp
        val tileWidth = ((maxWidth - spacing * maxContacts) / maxContacts).coerceAtMost(48.dp)
        val tileHeight = tileWidth * phi
        val cornerRadius = tileWidth / 8
        val cornerShape = RoundedCornerShape(cornerRadius)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing, alignment = Alignment.CenterHorizontally),
        ) {
            rack.contacts.forEach { contact ->
                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val resolveMultiConnectContacts = gameState.actions.resolveMultiConnectContacts
                    val isResolutionContact = resolveMultiConnectContacts?.contains(contact.id) ?: false

                    val shadowColor = when {
                        hoveredLogs.value.any { it is LogToken.Contact && it.contactId == contact.id } -> highlightedLogContactColor
                        isResolutionContact -> highlightedContactColor
                        lastActionResult.errorContacts.contains(contact.id) -> Color.Red
                        else -> null
                    }

                    val shakeOffset = remember { Animatable(0f) }

                    val isError = lastActionResult.errorContacts.contains(contact.id)

                    LaunchedEffect(isError) {
                        if (isError) {
                            shakeOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = keyframes {
                                    durationMillis = 400
                                    0f at 0
                                    -12f at 50
                                    12f at 100
                                    -9f at 150
                                    9f at 200
                                    -5f at 250
                                    5f at 300
                                    0f at 400
                                },
                            )
                        }
                    }

                    Box {
                        FlippableContactTile(
                            contact = contact.value,
                            size = DpSize(tileWidth, tileHeight),
                            isSolved = contact.solved,
                            isOwned = isOwner,
                            solvedBackgroundColor = Color(0xFF808080),
                            unsolvedBackgroundColor = when {
                                contact.id in selectedContacts -> selectedContactColor
                                isResolutionContact -> highlightedContactColor
                                else -> Color(0xFF4A4A4A)
                            },
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = shakeOffset.value
                                }
                                .border(1.dp, color = shadowColor ?: Color.Transparent, cornerShape)
                                .shadow(
                                    elevation = if (shadowColor != null) 8.dp else 0.dp,
                                    shape = cornerShape,
                                    ambientColor = shadowColor ?: DefaultShadowColor,
                                    spotColor = shadowColor ?: DefaultShadowColor,
                                )
                                .clickable(
                                    enabled = !contact.solved &&
                                            (resolveMultiConnectContacts == null || isResolutionContact),
                                    onClick = { onContactClick(contact.id) },
                                ),
                        )
                        if (aiEnabled) {
                            Text(
                                text = "#" + contact.id.value,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                color = Color.White,
                                fontSize = (tileHeight / 6.dp).sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(width = tileWidth, height = tileWidth / phi),
                        contentAlignment = Alignment.Center,
                    ) {
                        contact.hint?.let { hint ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondaryContainer, cornerShape)
                                    .border(1.dp, color = MaterialTheme.colorScheme.outline, cornerShape),
                            )
                            Text(
                                text = hint,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = (tileHeight / 4.dp).sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlippableContactTile(
    contact: ContactsBoardState.ContactValue?,
    size: DpSize,
    isSolved: Boolean,
    isOwned: Boolean,
    solvedBackgroundColor: Color,
    unsolvedBackgroundColor: Color,
    modifier: Modifier = Modifier,
    solvedTextColor: Color = Color.White,
    unsolvedTextColor: Color = Color.White,
) {

    Flippable(
        isFlipped = !isSolved,
        modifier = modifier.size(size),
        durationMillis = 1000,
        cameraDistanceDp = 12.dp,
        front = {
            // solved
            ContactTileView(
                contact = contact,
                size = size,
                backgroundColor = solvedBackgroundColor,
                modifier = modifier,
                textColor = solvedTextColor,
            )
        },
        back = {
            // unsolved
            ContactTileView(
                contact = if (isOwned) contact else null, // keep hidden for animation
                size = size,
                backgroundColor = unsolvedBackgroundColor,
                modifier = modifier,
                textColor = unsolvedTextColor,
            )
        },
    )
}

@Composable
private fun ContactsPlayerIdentity(
    label: String,
    username: String,
    pictureUrl: String?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlayerAvatar(
            pictureUrl = pictureUrl,
            username = username,
            modifier = Modifier.size(32.dp),
        )
        Text(text = label)
    }
}

@Composable
private fun ContactTileView(
    contact: ContactsBoardState.ContactValue?,
    size: DpSize,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val cornerRadius = size.width / 8
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier.size(size).background(backgroundColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (contact != null) {
            Box(
                modifier = Modifier
                    .size(size.width, size.height / 5)
                    .align(Alignment.TopCenter)
                    .background(
                        color = when (contact.type) {
                            ContactsBoardState.ContactType.Blue -> Color.Blue
                            ContactsBoardState.ContactType.Yellow -> Color.Yellow
                            ContactsBoardState.ContactType.Red -> Color.Red
                        },
                        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
                    ),
            )
        }
        Text(
            text = contact?.number?.toString() ?: "?",
            color = textColor,
            fontSize = (size.height / 3.dp).sp,
            fontWeight = if (contact == null) FontWeight.Normal else FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

private fun ContactsBoardState.Player.displayUsername(
    players: List<ContactsPlayer>,
    gameDetails: ContactsGameDetails?,
): String {
    val player = players.firstOrNull { it.playerId == this } ?: return username
    return gameDetails?.player(player.uid)?.username ?: player.username
}
