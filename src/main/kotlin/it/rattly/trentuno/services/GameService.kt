package it.rattly.trentuno.services

import dev.kord.common.entity.Snowflake
import it.rattly.trentuno.removeRandom
import it.rattly.trentuno.games.Card
import it.rattly.trentuno.games.CardType
import it.rattly.trentuno.games.Game
import it.rattly.trentuno.games.Player
import it.rattly.trentuno.games.impl.Trentuno
import kotlin.reflect.KClass

val gameTypes: List<KClass<out Game>> = listOf(Trentuno::class)

object GameService {
    private val games = hashSetOf<Game>()

    // Get a random deck of cards then build the game by giving each player a random card
    fun addGame(channelId: Snowflake, players: List<Snowflake>, gameClazz: KClass<out Game>) = makeDeck().let { deck ->
        gameClazz.constructors.first().call( // do not alter order of parameters
            channelId,
            players.map { playerId ->
                Player(
                    playerId,
                    mutableListOf(deck.removeRandom(), deck.removeRandom(), deck.removeRandom())
                )
            },
            deck
        ).also { games.add(it) }
    }

    fun hasGame(channelId: Snowflake): Boolean {
        return games.any { it.channelId == channelId }
    }

    private fun makeDeck() = mutableListOf<Card>().apply {
        CardType.entries.forEach { type ->
            repeat(10) {
                add(Card(type, it + 1))
            }
        }

        shuffle()
        shuffle()
    }

    operator fun get(channelId: Snowflake): Game? {
        return games.find { it.channelId == channelId }
    }
}