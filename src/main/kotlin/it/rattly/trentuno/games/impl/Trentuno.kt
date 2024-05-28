package it.rattly.trentuno.games.impl

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
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

    override suspend fun startGameLoop(kord: Kord): Snowflake {
        val channel = kord.getChannelOf<TopGuildMessageChannel>(channelId)!!

        // Game loop runs until someone knocks or the deck is empty
        while (deck.isNotEmpty()) {
            rounds += 1
            turn += 1 // Started the turn, take new player or fall back to first player
            val player = players.getOrElse(turn) {
                turn = 0
                players[turn]
            }

            // Start turn with current card on the board & display the current player
            channel.createMessage {
                content = "${player.id.mention} is playing round $rounds! Current card: "

                swapRows(player, this)
                addImage(RenderService.renderSingleCard(centerCard))
                actionRow {
                    // Takes new card from the deck
                    button("New card", style = ButtonStyle.Primary, expiration = 60_000) {
                        if (!turnCheck(player)) return@button

                        waitingAction = WaitingStatus.NEW_CARD
                        // Ask the player to choose a move with the new card
                        interaction.respondEphemeral {
                            content = "You chose to take a new card!"
                            centerCard = deck.removeFirst()

                            addImage(RenderService.renderSingleCard(centerCard))
                            swapRows(player, this)
                            actionRow {
                                button("Drop new card", style = ButtonStyle.Primary, expiration = 60_000) btn2@{
                                    if (!turnCheck(player)) return@btn2

                                    waitingAction = WaitingStatus.CONTINUE
                                    interaction.respondPublic {
                                        content = "${player.id.mention} dropped the new card!"
                                    }
                                }
                            }
                        }

                        channel.createMessage("${player.id.mention} took a new card!")
                    }

                    button("Deck", style = ButtonStyle.Primary, expiration = 60_000) {
                        if (!turnCheck(player)) return@button

                        interaction.respondEphemeral {
                            content = "Current points: ${pointsMap[player.id]?.human()}"

                            addImage(player.renderDeck())
                        }
                    }

                    if (rounds > 2) {
                        button("Knock", style = ButtonStyle.Danger, expiration = 60_000) {
                            if (!turnCheck(player)) return@button
                            waitingAction = WaitingStatus.BREAK
                            channel.createMessage("${player.id.mention} knocked! Show your cards!")
                        }
                    } else {
                        interactionButton(ButtonStyle.Danger, uuid()) {
                            label = "Knock"
                            disabled = true
                        }
                    }
                }
            }

            waitingAction = WaitingStatus.WAITING
            var secondsPassed = 0
            // Waiting action: while the player is choosing the action in the background we wait for 60s or skip the turn
            while ((waitingAction == WaitingStatus.WAITING || waitingAction == WaitingStatus.NEW_CARD) && (secondsPassed < 30)) {
                delay(1000)
                secondsPassed += 1

                // Every 10 sec send remaining time alert
                if (secondsPassed % 10 == 0) {
                    channel.createMessage("${player.id.mention} you have ${60 - secondsPassed} seconds left to make a move!")
                }
            }

            // if time is up either pick a card or/and skip the turn
            if (waitingAction == WaitingStatus.NEW_CARD || waitingAction == WaitingStatus.WAITING) {
                if (waitingAction == WaitingStatus.WAITING) centerCard = deck.removeFirst()
                channel.createMessage("Time's up ${player.id.mention}! Skipping to the next player!")
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
                content = "${player.id.mention}'s cards (${pointsMap[player.id]?.human()})"
                addImage(player.renderDeck())
            }
        }

        channel.createMessage {
            content =
                "**${winner.key.mention} has won the game with ${winner.value.human()} points!**\n" +
                        pointsMap.map { " - ${it.key.mention} has ${it.value.human()} points." }.joinToString("\n")
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
            button("Swap with card 1", style = ButtonStyle.Primary, expiration = 60_000) {
                waitingAction = WaitingStatus.CONTINUE
                val card = player.cards[0]
                player.cards[0] = centerCard
                centerCard = card

                interaction.respondPublic { content = "${player.id.mention} is swapping with card 1!" }
            }

            button("Swap with card 2", style = ButtonStyle.Primary, expiration = 60_000) {
                waitingAction = WaitingStatus.CONTINUE
                val card = player.cards[1]
                player.cards[1] = centerCard
                centerCard = card

                interaction.respondPublic { content = "${player.id.mention} is swapping with card 2!" }
            }

            button("Swap with card 3", style = ButtonStyle.Primary, expiration = 60_000) {
                waitingAction = WaitingStatus.CONTINUE
                val card = player.cards[2]
                player.cards[2] = centerCard
                centerCard = card

                interaction.respondPublic { content = "${player.id.mention} is swapping with card 3!" }
            }
        }
    }

    // Checks if the clicker is the player who is currently playing the turn
    private suspend fun ButtonInteractionCreateEvent.turnCheck(player: Player): Boolean {
        if (player.id != interaction.user.id) {
            interaction.respondEphemeral { content = "It's not your turn!" }
            return false
        } else return true
    }
}