package it.rattly.trentuno.games.impl

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.create.AbstractMessageCreateBuilder
import it.rattly.trentuno.addImage
import it.rattly.trentuno.button
import it.rattly.trentuno.games.*
import it.rattly.trentuno.mention
import it.rattly.trentuno.removeRandom
import it.rattly.trentuno.services.GameType
import it.rattly.trentuno.services.RenderService
import kotlinx.coroutines.CompletableDeferred
import me.jakejmattson.discordkt.util.uuid

class Trentuno(channel: TopGuildMessageChannel, players: List<Player>, deck: MutableList<Card>, type: GameType) :
    RoundGameLoop(channel, players, deck, type) {
    private var bussaPlayer: Player? = null
    private var centerCard: Card = deck.removeFirst()

    override suspend fun gameLoop(isOver: CompletableDeferred<Unit>) {
        // If bussato exit out the game loop
        if (bussaPlayer == currentPlayer) {
            throw GameOverException()
        }

        // Start turn with current card on the board & display the current player
        channel.createMessage {
            content = "Turno di ${currentPlayer.mention} ($roundsPassed)! Carta a terra: "

            swapRows(currentPlayer, this, WaitingStatus.WAITING, isOver)
            addImage(RenderService.renderSingleCard(centerCard))
            actionRow {
                // Takes new card from the deck
                button("Pesca", style = ButtonStyle.Primary, expiration = 60_000) {
                    if (!turnCheck()) return@button

                    waitingAction = TrentunoWaitingStatus.PESCATO
                    // Ask the player to choose a move with the new card
                    interaction.respondEphemeral {
                        content = "Hai scelto di pescare!"
                        centerCard = deck.removeFirst()

                        addImage(RenderService.renderSingleCard(centerCard))
                        swapRows(currentPlayer, this, TrentunoWaitingStatus.PESCATO, isOver)
                        actionRow {
                            button("Butta", style = ButtonStyle.Primary, expiration = 60_000) btn2@{
                                if (!turnCheck()) return@btn2
                                if (!statusCheck(TrentunoWaitingStatus.PESCATO)) return@btn2

                                isOver.complete(Unit)
                                interaction.respondPublic {
                                    content = "${currentPlayer.id.mention} ha buttato la carta appena pescata!"
                                }
                            }
                        }
                    }

                    channel.createMessage("${currentPlayer.mention} ha pescato una nuova carta!")
                }

                button("Deck", style = ButtonStyle.Primary, expiration = 60_000) {
                    if (!turnCheck()) return@button

                    interaction.deferEphemeralResponse().respond {
                        content = "Punti: ${playerPoints[currentPlayer]}"

                        addImage(currentPlayer.renderDeck())
                    }
                }

                if (roundsPassed > 2) {
                    button("Bussa", style = ButtonStyle.Danger, expiration = 60_000) {
                        if (!turnCheck()) return@button
                        channel.createMessage("Toc toc... ${currentPlayer.mention} ha bussato... Ultimo turno!")
                        bussaPlayer = currentPlayer
                        isOver.complete(Unit)
                    }
                } else {
                    interactionButton(ButtonStyle.Danger, uuid()) {
                        label = "Bussa"
                        disabled = true
                    }
                }
            }
        }
    }

    override fun recalculatePlayerPoints() = pointsMap().map { it.key to it.value.value }.toMap().toMutableMap()
    override fun winCheck() = playerPoints.any { it.value == 31 }
    override fun calculateWinner() = playerPoints.maxBy { it.value }.key
    override fun winMessage(winner: Player) =
        "**${winner.mention} ha vinto la partita con ${playerPoints[winner]!!} punti!**\n" +
                playerPoints.map { " - ${it.key.mention} ha ${it.value} punti." }.joinToString("\n")

    override suspend fun beforeGameStart() {
        for (player in players) {
            repeat(3) {
                player.cards.add(deck.removeRandom())
            }
        }
    }

    override suspend fun timeIsUpAction() {}

    private fun pointsMap() =
        // For each player, calculate the points
        players.map { player ->
            // Transform player map into a map of player -> pair of card type with the most points
            player to ((mutableMapOf<CardType, Int>().apply {
                player.cards.forEach { card ->
                    // Add points to the type
                    this[card.type] = this[card.type]?.plus(card.points()) ?: card.points()
                }
            }.maxByOrNull { it.value }?.toPair()?.toCard()) ?: Card(
                CardType.COPPE,
                0
            )) // Only take the type with the most points
        }.sortedByDescending { it.second.value }.toMap() // Sort by most points and convert to map

    // Adds the actionRow with the buttons for the swap card actions
    private fun swapRows(
        player: Player,
        builder: AbstractMessageCreateBuilder,
        statusCheck: WaitingStatus,
        isOver: CompletableDeferred<Unit>
    ) {
        builder.actionRow {
            repeat(3) {
                button("Scambia con la ${it + 1} carta", style = ButtonStyle.Primary, expiration = 60_000) {
                    if (!turnCheck()) return@button
                    if (!statusCheck(statusCheck)) return@button

                    isOver.complete(Unit)
                    val card = player.cards[it]
                    player.cards[it] = centerCard
                    centerCard = card

                    interaction.respondPublic {
                        content = "${player.id.mention} sta scambiando la carta dal mazzo con la carta ${it + 1}!"
                    }
                }
            }
        }
    }
}

private fun Card.points() = if (value > 7) 10 else if (value == 1) 11 else value