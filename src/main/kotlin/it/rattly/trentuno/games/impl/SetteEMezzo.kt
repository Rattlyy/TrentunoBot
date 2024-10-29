package it.rattly.trentuno.games.impl

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.builder.message.actionRow
import it.rattly.trentuno.addImage
import it.rattly.trentuno.button
import it.rattly.trentuno.games.*
import it.rattly.trentuno.services.GameType
import it.rattly.trentuno.services.RenderService
import kotlinx.coroutines.CompletableDeferred
import kotlin.properties.Delegates

class SetteEMezzo(channel: TopGuildMessageChannel, players: List<Player>, deck: MutableList<Card>, type: GameType) :
    RoundGameLoop(channel, players, deck, type) {

    override suspend fun gameLoop(isJoever: CompletableDeferred<Unit>) {
        val firstMessageAction = CompletableDeferred<Action>()
        var privateInteraction by Delegates.notNull<DeferredEphemeralMessageInteractionResponseBehavior>()

        suspend fun sendActionMessage(deferred: CompletableDeferred<Action>) {
            privateInteraction.respond {
                addImage(RenderService.renderCards(currentPlayer.cards))
                content = "Scegli la tua azione."

                actionRow {
                    button("STAI", style = ButtonStyle.Danger, expiration = 60_000) staiButton@{
                        if (!turnCheck()) return@staiButton
                        deferred.complete(Action.STAI)
                        interaction.respondEphemeral { content = "Fatto." }
                    }

                    button("CARTA", style = ButtonStyle.Danger, expiration = 60_000) cartaButton@{
                        if (!turnCheck()) return@cartaButton
                        secondsPassed = 0
                        deferred.complete(Action.CARTA)
                        privateInteraction = interaction.deferEphemeralResponse()
                    }
                }
            }
        }

        channel.createMessage {
            content = "E' il turno di ${currentPlayer.mention}! Clicca qui per vedere la tua carta."
            actionRow {
                button("Scopri", expiration = 60_000) {
                    if (!turnCheck()) return@button

                    privateInteraction = interaction.deferEphemeralResponse()
                    sendActionMessage(firstMessageAction)
                }
            }
        }

        when (firstMessageAction.await()) {
            Action.STAI -> {
                waitingAction = WaitingStatus.CONTINUE
                return
            }

            Action.CARTA -> {
                val pescate = mutableListOf<Card>()
                var isOver = false
                while (!isOver) {
                    val action = CompletableDeferred<Action>()

                    deck.removeFirst().also { c ->
                        pescate.add(c)
                        currentPlayer.cards.add(c)
                    }

                    val points = currentPlayer.cards.sumOf { it.points() }
                    if (points == 7.5) {
                        isOver = true
                        isJoever.complete(Unit)
                        continue
                    } else if (points > 7.5) {
                        throw GameOverException()
                    }

                    sendActionMessage(action)

                    channel.createMessage {
                        addImage(RenderService.renderCards(pescate))
                        content = "${currentPlayer.mention} ha preso!"
                    }

                    when (action.await()) {
                        Action.CARTA -> {} // reloop
                        Action.STAI -> {
                            isJoever.complete(Unit)
                            isOver = true
                            continue
                        }
                    }
                }
            }
        }

        isJoever.complete(Unit)
    }

    enum class Action {
        STAI,
        CARTA
    }

    override suspend fun beforeGameStart() {
        for (player in players) {
            player.cards.add(deck.removeFirst())
        }
    }

    override suspend fun timeIsUpAction() {
    }

    override fun calculateWinner() = players
        .map { it to it.cards.sumOf { c -> c.points() } }
        .filter { (_, points) -> points <= 7.5 }
        .maxByOrNull { (_, points) -> points }?.first

    override fun winMessage(winner: Player) = "gay ${winner.mention}"
    override fun recalculatePlayerPoints() = mutableMapOf<Player, Int>()
    override fun winCheck() = roundsPassed > 2
}

private fun Card.points(): Double = if (value <= 7) value.toDouble() else 0.5