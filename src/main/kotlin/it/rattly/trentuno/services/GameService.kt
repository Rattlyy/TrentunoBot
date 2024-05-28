package it.rattly.trentuno.services

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import it.rattly.trentuno.db.tables.GameDB
import it.rattly.trentuno.db.tables.GamePlayer
import it.rattly.trentuno.db.tables.PlayerDB
import it.rattly.trentuno.games.Card
import it.rattly.trentuno.games.CardType
import it.rattly.trentuno.games.Game
import it.rattly.trentuno.games.Player
import it.rattly.trentuno.games.impl.Trentuno
import it.rattly.trentuno.removeRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KClass

val gameScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

object GameService {
    private val games = hashSetOf<Game>()

    // Get a random deck of cards then build the game by giving each player a random card
    fun addGame(channelId: Snowflake, players: List<Snowflake>, gameType: GameType) = makeDeck().let { deck ->
        gameType.klass.constructors.first().call( // do not alter order of parameters
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

    fun startGame(kord: Kord, game: Game) {
        transaction {
            for (player in game.players) {
                PlayerDB.new(player.id.value) {}
            }
        }
        gameScope.launch {
            val winner = game.startGameLoop(kord)

            transaction {
                val gameDb = GameDB.new {
                    channelId = game.channelId.value
                    gameType = game.type
                    gameWinner = PlayerDB[winner.value]
                }

                for (player in game.players) {
                    GamePlayer.new {
                        this.game = gameDb
                        this.player = PlayerDB[player.id.value]
                    }
                }
            }
        }
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

enum class GameType(val klass: KClass<out Game>) {
    TRENTUNO(Trentuno::class)
}