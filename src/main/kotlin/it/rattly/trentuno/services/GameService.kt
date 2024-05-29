package it.rattly.trentuno.services

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.GuildMessageChannel
import it.rattly.trentuno.db.database
import it.rattly.trentuno.db.tables.*
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
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.first
import org.ktorm.support.postgresql.bulkInsertOrUpdate
import java.util.concurrent.CancellationException
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
            deck,
            gameType
        ).also { games.add(it) }
    }

    fun startGame(kord: Kord, game: Game) {
        database.bulkInsertOrUpdate(PlayerDBs) {
            onConflict { doNothing() }

            for (player in game.players) {
                item { set(it.id, player.id) }
            }
        }

        game.job = gameScope.launch {
            val server = kord.getChannelOf<GuildMessageChannel>(game.channelId)!!.guildId
            val winner = game.startGameLoop(kord)
            val gameDb = GameDB(
                serverId = server,
                channelId = game.channelId,
                gameType = game.type,
                gameWinner = winner
            ).also { database.gameDBs.add(it) }

            for (player in game.players) {
                database.gamePlayerses.add(
                    GamePlayers(
                        game = gameDb,
                        player = database.playerDBs.first { it.id eq winner },
                    )
                )
            }
        }
    }

    fun endGame(channelId: Snowflake) {
        val game = games.find { it.channelId == channelId } ?: return
        game.job.cancel(CancellationException("Game was terminated"))
        games.remove(game)
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

    fun hasGame(channelId: Snowflake) = games.any { it.channelId == channelId }
    operator fun get(channelId: Snowflake): Game? {
        return games.find { it.channelId == channelId }
    }
}

enum class GameType(val klass: KClass<out Game>) {
    TRENTUNO(Trentuno::class)
}