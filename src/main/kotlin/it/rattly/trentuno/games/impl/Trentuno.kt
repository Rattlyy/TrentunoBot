package it.rattly.trentuno.games.impl

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.create.AbstractMessageCreateBuilder
import it.rattly.trentuno.addImage
import it.rattly.trentuno.button
import it.rattly.trentuno.games.*
import it.rattly.trentuno.mention
import it.rattly.trentuno.services.GameType
import it.rattly.trentuno.services.RenderService
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.util.uuid

class Trentuno(channelId: Snowflake, players: List<Player>, deck: MutableList<Card>, type: GameType) :
    Game(type, channelId, players, deck) {
    private var centerCard: Card = deck.removeFirst()
    private var waitingAction: WaitingStatus = WaitingStatus.WAITING
    private var pointsMap = pointsMap()
    private var rounds = 0
    private var turn = 0
    private lateinit var player: Player
    private var bussaPlayer: Player? = null

    override suspend fun startGameLoop(kord: Kord): Snowflake {
        val channel = kord.getChannelOf<TopGuildMessageChannel>(channelId)!!

        // Game loop runs until someone knocks or the deck is empty
        while (deck.isNotEmpty()) {
            rounds += 1
            turn += 1 // Started the turn, take new player or fall back to first player
            player = players.getOrElse(turn) {
                turn = 0
                players[turn]
            }

            // If bussato exit out the game loop
            if (bussaPlayer == player) { break }

            // Start turn with current card on the board & display the current player
            channel.createMessage {
                content = "Turno di ${player.id.mention} ($rounds)! Carta a terra: "

                swapRows(player, this)
                addImage(RenderService.renderSingleCard(centerCard))
                actionRow {
                    // Takes new card from the deck
                    button("Pesca", style = ButtonStyle.Primary, expiration = 60_000) {
                        if (!turnCheck()) return@button

                        waitingAction = WaitingStatus.NEW_CARD
                        // Ask the player to choose a move with the new card
                        interaction.respondEphemeral {
                            content = "Hai scelto di pescare!"
                            centerCard = deck.removeFirst()

                            addImage(RenderService.renderSingleCard(centerCard))
                            swapRows(player, this)
                            actionRow {
                                button("Butta", style = ButtonStyle.Primary, expiration = 60_000) btn2@{
                                    if (!turnCheck()) return@btn2

                                    waitingAction = WaitingStatus.CONTINUE
                                    interaction.respondPublic {
                                        content = "${player.id.mention} ha buttato la carta appena pescata!"
                                    }
                                }
                            }
                        }

                        channel.createMessage("${player.id.mention} ha pescato una nuova carta!")
                    }

                    button("Deck", style = ButtonStyle.Primary, expiration = 60_000) {
                        if (!turnCheck()) return@button

                        interaction.deferEphemeralResponse().respond {
                            content = "Punti: ${pointsMap[player.id]?.human()}"

                            addImage(player.renderDeck())
                        }
                    }

                    if (rounds > 2) {
                        button("Bussa", style = ButtonStyle.Danger, expiration = 60_000) {
                            if (!turnCheck()) return@button
                            channel.createMessage("Toc toc... ${player.id.mention} ha bussato... Ultimo turno!")
                            bussaPlayer = player
                            waitingAction = WaitingStatus.CONTINUE
                        }
                    } else {
                        interactionButton(ButtonStyle.Danger, uuid()) {
                            label = "Bussa"
                            disabled = true
                        }
                    }
                }
            }

            waitingAction = WaitingStatus.WAITING
            var secondsPassed = 0
            // Waiting action: while the player is choosing the action in the background we wait for 60s or skip the turn
            while ((waitingAction == WaitingStatus.WAITING || waitingAction == WaitingStatus.NEW_CARD) && (secondsPassed < 60)) {
                delay(1000)
                secondsPassed += 1

                // Every 10 sec send remaining time alert
                if (secondsPassed % 10 == 0) {
                    channel.createMessage("${player.id.mention} hai ${60 - secondsPassed} secondi per fare la tua mossa!")
                }
            }

            // if time is up either pick a card or/and skip the turn
            if (waitingAction == WaitingStatus.NEW_CARD || waitingAction == WaitingStatus.WAITING) {
                if (waitingAction == WaitingStatus.WAITING) centerCard = deck.removeFirst()
                channel.createMessage("Il tempo è finito ${player.id.mention}! Passo al prossimo giocatore!")
            }

            // Refresh the points map
            pointsMap = pointsMap()
            // If someone has 31 points, or break is pressed, break the game loop
            if (pointsMap.any { it.value.value == 31 } || waitingAction == WaitingStatus.BREAK) break
        }

        // Game loop ended so deck is empty, somebody knocked or someone has 31 points
        val winner = pointsMap.maxByOrNull { it.value.value }!! // Get the player with the most points
        for (player in players) {
            channel.createMessage {
                content = "Carte di ${player.id.mention} (${pointsMap[player.id]?.human()})"
                addImage(player.renderDeck())
            }
        }

        channel.createMessage {
            content =
                "**${winner.key.mention} ha vinto la partita con ${winner.value.human()} punti!**\n" +
                        pointsMap.map { " - ${it.key.mention} ha ${it.value.human()} punti." }.joinToString("\n")
        }

        return winner.key
    }


    private fun pointsMap() =
        // For each player, calculate the points
        players.map { player ->
            // Transform player map into a map of player -> pair of card type with the most points
            player.id to ((mutableMapOf<CardType, Int>().apply {
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
    private fun swapRows(player: Player, builder: AbstractMessageCreateBuilder) {
        builder.actionRow {
            repeat(3) {
                button("Scambia con la ${it + 1} carta", style = ButtonStyle.Primary, expiration = 60_000) {
                    waitingAction = WaitingStatus.CONTINUE
                    val card = player.cards[it]
                    player.cards[it] = centerCard
                    centerCard = card

                    interaction.respondPublic { content = "${player.id.mention} sta scambiando la carta dal mazzo con la carta ${it + 1}!" }
                }
            }
        }
    }

    // Checks if the clicker is the player who is currently playing the turn
    private suspend fun ButtonInteractionCreateEvent.turnCheck(): Boolean {
        if (player.id != this.interaction.user.id) {
            interaction.respondEphemeral { content = "Non è il tuo turno!" }
            return false
        } else return true
    }
}