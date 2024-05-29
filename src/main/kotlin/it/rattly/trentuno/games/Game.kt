package it.rattly.trentuno.games

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import it.rattly.trentuno.services.GameType
import it.rattly.trentuno.services.RenderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO

// must implement this constructor or else reflection fails
abstract class Game(
    val type: GameType,
    val channelId: Snowflake,
    val players: List<Player>,
    internal val deck: MutableList<Card>,
) {
    lateinit var job: Job
    // main game loop function, coroutine that is supposed to hang until the game is over,
    // returns the winner of the game
    abstract suspend fun startGameLoop(kord: Kord): Snowflake

    override fun hashCode() = channelId.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Game) return false

        if (channelId != other.channelId) return false

        return true
    }
}

class Player(
    val id: Snowflake,
    val cards: MutableList<Card>,
) {
    suspend fun renderDeck() = RenderService.renderCards(cards)
}

class Card(val type: CardType, val value: Int) {
    fun points() = if (value > 7) 10 else if (value == 1) 11 else value
    fun human() = "${this.type} ${this.value}"
    suspend fun image() =
        withContext(Dispatchers.IO) {
            ImageIO.read(javaClass.getResourceAsStream("/carte/${value}-${type.name.lowercase()}.jpg"))
        } ?: error("Card not found in resources")
}

fun Pair<CardType, Int>.toCard() = Card(this.first, this.second)

enum class WaitingStatus {
    WAITING,
    NEW_CARD,
    CONTINUE,
    BREAK,
}

enum class CardType {
    COPPE, DENARI, SPADE, BASTONI
}