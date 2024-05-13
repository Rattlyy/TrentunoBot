package it.rattly.services

import dev.kord.common.entity.Snowflake
import it.rattly.objects.Game
import it.rattly.objects.Player
import it.rattly.objects.possibleCards

object GameService {
    private val games = hashSetOf<Game>()

    fun addGame(channelId: Snowflake, players: List<Snowflake>): Game {
        val shuffledDeck = possibleCards.shuffled().shuffled().toMutableList()
        val deckPlayers = players.map { playerId ->
            Player(
                playerId,
                mutableListOf(shuffledDeck.removeFirst(), shuffledDeck.removeFirst(), shuffledDeck.removeFirst())
            )
        }

        val game = Game(channelId, deckPlayers, shuffledDeck)
        games.add(game)
        return game
    }

    fun hasGame(channelId: Snowflake): Boolean {
        return games.any { it.channelId == channelId }
    }

    operator fun get(channelId: Snowflake): Game? {
        return games.find { it.channelId == channelId }
    }
}