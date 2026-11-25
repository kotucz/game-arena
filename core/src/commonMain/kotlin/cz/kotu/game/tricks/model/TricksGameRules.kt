package cz.kotu.game.tricks.model

object TricksGameRules {

    fun generateDeck(config: TricksGameConfig): List<Card> {
        val colorSuits = Suit.entries.filter { !it.isTrump }
        val cards = mutableListOf<Card>()

        for (suit in colorSuits) {
            for (rankValue in 1..config.maxRank) {
                cards += Card(suit, Rank(rankValue))
            }
        }

        if (config.includeTrumpSuit) {
            for (rankValue in 1..config.trumpMaxRank) {
                cards += Card(Suit.Trump, Rank(rankValue))
            }
        }

        return cards
    }

    fun dealHands(
        players: List<TricksPlayer>,
        shuffledDeck: List<Card>,
    ): Map<TricksPlayerId, List<Card>> {
        require(players.isNotEmpty()) { "Players list cannot be empty" }
        val hands = players.associate { it.id to mutableListOf<Card>() }

        shuffledDeck.forEachIndexed { index, card ->
            val player = players[index % players.size]
            hands.getValue(player.id).add(card)
        }

        return hands.mapValues { (_, cardList) -> cardList.sorted() }
    }

    fun isPlayLegal(
        state: TricksGameState,
        playerId: TricksPlayerId,
        card: Card,
    ): String? {
        val activeTurn = (state.phase as? TricksGamePhase.PlayerTurn)?.activePlayer
            ?: return "Game is not in a player turn phase"

        if (activeTurn != playerId) {
            return "Not your turn"
        }

        val hand = state.hands[playerId] ?: return "Player not in game"
        if (!hand.contains(card)) {
            return "Card not in player hand: $card"
        }

        val leadSuit = state.currentTrick.leadSuit
        if (leadSuit != null && card.suit != leadSuit) {
            val hasLeadSuit = hand.any { it.suit == leadSuit }
            if (hasLeadSuit) {
                return "Must follow lead suit: ${leadSuit.label}"
            }
        }

        return null
    }

    fun legalPlays(state: TricksGameState, playerId: TricksPlayerId): Set<Card> {
        val activeTurn = (state.phase as? TricksGamePhase.PlayerTurn)?.activePlayer
        if (activeTurn != playerId) return emptySet()

        val hand = state.hands[playerId] ?: return emptySet()
        val leadSuit = state.currentTrick.leadSuit ?: return hand.toSet()

        val cardsMatchingLead = hand.filter { it.suit == leadSuit }
        return if (cardsMatchingLead.isNotEmpty()) {
            cardsMatchingLead.toSet()
        } else {
            hand.toSet()
        }
    }

    fun initialGameState(
        players: List<TricksPlayer>,
        config: TricksGameConfig = TricksGameConfig(),
        deckShuffler: (List<Card>) -> List<Card> = { it.shuffled() },
        startingPlayerId: TricksPlayerId? = null,
    ): TricksGameState {
        require(players.size >= 2) { "At least 2 players required" }
        val deck = generateDeck(config)
        val shuffled = deckShuffler(deck)
        val hands = dealHands(players, shuffled)

        // Starting player: highest trump holder, or provided starter, or first player
        val starter = startingPlayerId ?: players.first().id

        return TricksGameState(
            players = players,
            hands = hands,
            currentTrick = Trick(),
            completedTricks = emptyList(),
            trumpSuit = config.trumpSuit,
            phase = TricksGamePhase.PlayerTurn(starter),
        )
    }

    fun playCard(
        state: TricksGameState,
        playerId: TricksPlayerId,
        card: Card,
    ): Result<TricksGameState> {
        val error = isPlayLegal(state, playerId, card)
        if (error != null) {
            return Result.failure(IllegalArgumentException(error))
        }

        val currentHand = state.hands.getValue(playerId)
        val updatedHand = currentHand.filterIndexed { index, c ->
            index != currentHand.indexOf(card)
        }
        val updatedHands = state.hands + (playerId to updatedHand)

        val updatedTrick = Trick(state.currentTrick.plays + PlayedCard(playerId, card))

        if (updatedTrick.plays.size == state.players.size) {
            // Trick is complete
            val winnerPlay = updatedTrick.evaluateWinner(state.trumpSuit)
                ?: return Result.failure(IllegalStateException("Unable to determine trick winner"))
            val trickWinner = winnerPlay.player
            val newCompleted = state.completedTricks + CompletedTrick(updatedTrick, trickWinner)

            val allHandsEmpty = updatedHands.values.all { it.isEmpty() }
            val nextPhase = if (allHandsEmpty) {
                val tricksWon = state.players.associate { it.id to newCompleted.count { ct -> ct.winner == it.id } }
                TricksGamePhase.GameOver(tricksWon)
            } else {
                TricksGamePhase.PlayerTurn(trickWinner)
            }

            return Result.success(
                state.copy(
                    hands = updatedHands,
                    currentTrick = Trick(),
                    completedTricks = newCompleted,
                    phase = nextPhase,
                ),
            )
        } else {
            // Trick continues to next player
            val currentIndex = state.players.indexOfFirst { it.id == playerId }
            val nextPlayer = state.players[(currentIndex + 1) % state.players.size].id

            return Result.success(
                state.copy(
                    hands = updatedHands,
                    currentTrick = updatedTrick,
                    phase = TricksGamePhase.PlayerTurn(nextPlayer),
                ),
            )
        }
    }

    fun toPlayerViewState(state: TricksGameState, playerId: TricksPlayerId): TricksPlayerViewState {
        val me = state.players.firstOrNull { it.id == playerId }
            ?: throw IllegalArgumentException("Player $playerId is not part of this game")

        val myHand = state.hands[playerId] ?: emptyList()

        val opponents = state.players
            .filter { it.id != playerId }
            .map { opponent ->
                OpponentPlayerView(
                    player = opponent,
                    cardCount = state.hands[opponent.id]?.size ?: 0,
                    tricksWon = state.tricksWon(opponent.id),
                )
            }

        return TricksPlayerViewState(
            myPlayer = me,
            myHand = myHand,
            opponents = opponents,
            playerOrder = state.players.map { it.id },
            currentTrick = state.currentTrick,
            completedTricks = state.completedTricks,
            trumpSuit = state.trumpSuit,
            phase = state.phase,
            legalPlays = legalPlays(state, playerId),
        )
    }
}
