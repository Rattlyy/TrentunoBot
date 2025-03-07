package it.rattly.trentuno.services

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TopGuildMessageChannel
import it.rattly.trentuno.db.database
import it.rattly.trentuno.db.tables.*
import it.rattly.trentuno.games.Card
import it.rattly.trentuno.games.CardType
import it.rattly.trentuno.games.Game
import it.rattly.trentuno.games.Player
import it.rattly.trentuno.games.impl.SetteEMezzo
import it.rattly.trentuno.games.impl.Trentuno
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
val basicDeck = mutableListOf<Card>().also { list ->
    CardType.entries.forEach() { type ->
        repeat(10) {
            list.add(Card(type, it + 1))
        }
    }
}

object GameService {
    private val games = hashSetOf<Game>()

    // Get a random deck of cards then build the game by giving each player a random card
    fun addGame(channel: TopGuildMessageChannel, players: List<Snowflake>, gameType: GameType) =
        basicDeck.shuffled().toMutableList().let { deck ->
            gameType.klass.constructors.first().call( // do not alter order of parameters
                channel,
                players.map { playerId ->
                    Player(
                        playerId,
                        mutableListOf()
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
            val winner = game.startGameLoop(kord)
            val gameDb = GameDB(
                serverId = game.channel.id,
                channelId = game.channel.id,
                gameType = game.type,
                gameWinner = winner?.id
            ).also { database.gameDBs.add(it) }

            for (player in game.players) {
                database.gamePlayerses.add(
                    GamePlayers(
                        game = gameDb,
                        player = database.playerDBs.first { it.id eq player.id },
                    )
                )
            }
        }.also {
            it.invokeOnCompletion { games.remove(game) }
        }
    }

    fun endGame(channelId: Snowflake) {
        val game = games.find { it.channel.id == channelId } ?: return
        game.job.cancel(CancellationException("Game was terminated"))
        games.remove(game)
    }

    fun hasGame(channelId: Snowflake) = games.any { it.channel.id == channelId }
    operator fun get(channelId: Snowflake): Game? {
        return games.find { it.channel.id == channelId }
    }
}

enum class GameType(val klass: KClass<out Game>) {
    TRENTUNO(Trentuno::class),
    SETTE_E_MEZZO(SetteEMezzo::class)
}