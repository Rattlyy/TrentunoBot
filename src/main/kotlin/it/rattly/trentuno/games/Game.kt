package it.rattly.trentuno.games

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TopGuildMessageChannel
import it.rattly.trentuno.services.GameType
import it.rattly.trentuno.services.RenderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

// must implement this constructor or else reflection fails
abstract class Game(
    val type: GameType,
    val channel: TopGuildMessageChannel,
    val players: List<Player>,
    internal val deck: MutableList<Card>,
) {
    lateinit var job: Job

    // main game loop function, coroutine that is supposed to hang until the game is over,
    // returns the winner of the game
    abstract suspend fun startGameLoop(kord: Kord): Player?

    override fun hashCode() = channel.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Game) return false

        if (channel != other.channel) return false

        return true
    }
}

class Player(
    val id: Snowflake,
    val cards: MutableList<Card>,
) {
    val mention get() = "<@$id>"
    suspend fun renderDeck() = RenderService.renderCards(cards)
}

class Card(val type: CardType, val value: Int) {
    fun human() = "${this.type} ${this.value}"
    suspend fun image() =
        getImage("/carte/${value}-${type.name.lowercase()}.jpg")
}

val imageCache = mutableMapOf<String, BufferedImage>()
private suspend fun getImage(str: String) = imageCache.getOrPut(str) {
    withContext(Dispatchers.IO) {
        ImageIO.read(Card::class.java.getResourceAsStream(str))
    } ?: error("Card not found in resources")
}

fun Pair<CardType, Int>.toCard() = Card(this.first, this.second)

enum class CardType {
    COPPE, DENARI, SPADE, BASTONI
}