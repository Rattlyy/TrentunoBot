package it.rattly.trentuno.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.rest.builder.message.actionRow
import it.rattly.trentuno.addImage
import it.rattly.trentuno.button
import it.rattly.trentuno.services.GameService
import it.rattly.trentuno.services.GameType
import kotlinx.coroutines.delay
import me.jakejmattson.discordkt.arguments.Error
import me.jakejmattson.discordkt.arguments.StringArgument
import me.jakejmattson.discordkt.arguments.Success
import me.jakejmattson.discordkt.commands.DiscordContext
import me.jakejmattson.discordkt.commands.subcommand

// Channel - List of Players
val playerQueueMap = mutableMapOf<Snowflake, MutableList<Snowflake>>()

fun game() = subcommand("game") {
    sub("start", description = "Start a new game in the current channel") {
        execute(GameArg.instance) {
            val gameType = args.first
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

            val game = GameService.addGame(channel.id, playerQueueMap[channel.id]!!, gameType)
            playerQueueMap[channel.id]!!.clear()
            playerQueueMap.remove(channel.id)

            GameService.startGame(discord.kord, game)
        }
    }

    sub("deck", "Show your deck of cards in the current game playing in the channel") {
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

open class GameArg : StringArgument<GameType> {
    companion object {
        private val suggestions = GameType.entries.map { it.name }
        val instance = GameArg() //.autocomplete { suggestions }
    }

    override val description = "Game to join"
    override val name = "game"

    override fun isOptional() = false
    override suspend fun generateExamples(context: DiscordContext) = suggestions
    override suspend fun transform(input: String, context: DiscordContext) =
        GameType.entries.find { it.name.equals(input, true) }
            ?.let { Success(it) } ?: Error("Game not found")
}
