package it.rattly.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.rest.builder.message.actionRow
import it.rattly.addImage
import it.rattly.button
import it.rattly.services.GameService
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.commands.subcommand

// Channel - List of Players
val playerQueueMap = mutableMapOf<Snowflake, MutableList<Snowflake>>()

fun game() = subcommand("game") {
    sub("start") {
        execute {
            if (GameService.hasGame(channel.id) || playerQueueMap.containsKey(interaction!!.channel.id)) {
                interaction!!.respondEphemeral {
                    content = "There is already a game in progress!"
                }

                return@execute
            }

            playerQueueMap[channel.id] = mutableListOf(author.id)

            interaction!!.respondPublic {
                content = "Game started! You have 30 seconds to join the game!"

                actionRow {
                    button("Join", style = ButtonStyle.Success, expiration = 30_000) {
                        if (playerQueueMap[channel.id]?.contains(interaction.user.id) == true) {
                            interaction.respondEphemeral { content = "You are already in the game!" }
                            return@button
                        }

                        playerQueueMap[channel.id]?.add(interaction.user.id)
                        interaction.respondPublic { content = "${interaction.user.mention} has joined the game!" }
                    }

                    button("Leave", style = ButtonStyle.Danger, expiration = 30_000) {
                        if (playerQueueMap[channel.id]?.contains(interaction.user.id) == false) {
                            interaction.respondEphemeral { content = "You are not in the game!" }
                            return@button
                        }

                        playerQueueMap[channel.id]?.remove(interaction.user.id)
                        interaction.respondPublic { content = "${interaction.user.mention} has left the game!" }
                    }
                }
            }

            delay(10_000)
            channel.createMessage("20s left to join the game!")
            delay(10_000)
            channel.createMessage("10s left to join the game!")
            delay(10_000)

            val game = GameService.addGame(channel.id, playerQueueMap[channel.id]!!)
            playerQueueMap[channel.id]!!.clear()
            playerQueueMap.remove(channel.id)

            game.startGameLoop(this.discord.kord)
        }
    }

    sub("deck") {
        execute {
            if (!GameService.hasGame(channel.id)) {
                interaction!!.respondEphemeral {
                    content = "There is no game in progress in this channel!"
                }
                return@execute
            }

            interaction!!.respondEphemeral {
                GameService[channel.id]!!.players.find { it.id == interaction!!.user.id }?.let {
                    addImage(it.renderDeck())
                } ?: run { content = "You are not in the game!" }
            }
        }
    }
}