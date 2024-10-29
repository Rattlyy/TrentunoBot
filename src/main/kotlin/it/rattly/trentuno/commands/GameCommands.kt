package it.rattly.trentuno.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
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
    sub("start", description = "Avvia una partita") {
        execute(GameArg.instance) {
            val (gameType) = args
            if (GameService.hasGame(channel.id) || playerQueueMap.containsKey(interaction!!.channel.id)) {
                interaction!!.respondEphemeral {
                    content = "C'è già una partita in questo canale!"
                }

                return@execute
            }

            playerQueueMap[channel.id] = mutableListOf(author.id)

            interaction!!.respondPublic {
                content = "Partita avviata! Avete **30 secondi** per entrare nel match!"

                actionRow {
                    button("Join", style = ButtonStyle.Success, expiration = 30_000) {
                        if (playerQueueMap[channel.id]?.contains(interaction.user.id) == true) {
                            interaction.respondEphemeral { content = "Sei già in partita!" }
                            return@button
                        }

                        if (playerQueueMap.size == 2 && gameType == GameType.SETTE_E_MEZZO) {
                            interaction.respondEphemeral { content = "Questa partita è piena!" }
                            return@button
                        }

                        playerQueueMap[channel.id]?.add(interaction.user.id)
                        interaction.respondPublic { content = "${interaction.user.mention} è entrato nella partita!" }
                    }

                    button("Leave", style = ButtonStyle.Danger, expiration = 30_000) {
                        if (playerQueueMap[channel.id]?.contains(interaction.user.id) == false) {
                            interaction.respondEphemeral { content = "Non sei nella partita!" }
                            return@button
                        }

                        playerQueueMap[channel.id]?.remove(interaction.user.id)
                        interaction.respondPublic { content = "${interaction.user.mention} è uscito dalla partita!" }
                    }
                }
            }

            delay(10_000)
            channel.createMessage("20s rimasti per entrare in partita!")
            delay(10_000)
            channel.createMessage("10s per entrare in partita!")
            delay(10_000)

            val game = GameService.addGame(channel.asChannelOf(), playerQueueMap[channel.id]!!, gameType)
            playerQueueMap[channel.id]!!.clear()
            playerQueueMap.remove(channel.id)

            GameService.startGame(discord.kord, game)
        }
    }

    sub("deck", "Mostra le carte che hai in mano (solo a te)") {
        execute {
            if (!GameService.hasGame(channel.id)) {
                interaction!!.respondEphemeral {
                    content = "Non c'è nessuna partita in corso nel canale in cui hai eseguito il comando!"
                }

                return@execute
            }

            interaction!!.respondEphemeral {
                GameService[channel.id]!!.players.find { it.id == interaction!!.user.id }?.let {
                    addImage(it.renderDeck())
                } ?: run { content = "Non sei in partita!" }
            }
        }
    }

    sub("end", "Termina la partita nel canale corrente", requiredPermissions = Permissions(Permission.Administrator)) {
        execute {
            if (!GameService.hasGame(channel.id)) {
                interaction!!.respondEphemeral {
                    content = "Non c'è nessuna partita in questo canale!"
                }
                return@execute
            }

            GameService.endGame(channel.id)
        }
    }
}

open class GameArg : StringArgument<GameType> {
    companion object {
        private val suggestions = GameType.entries.map { it.name }
        val instance = GameArg().autocomplete { suggestions.filter { it.contains(input, true) } }
    }

    override val description = "Game"
    override val name = "game"

    override suspend fun generateExamples(context: DiscordContext) = suggestions
    override suspend fun transform(input: String, context: DiscordContext) =
        GameType.entries.find { it.name.equals(input, true) }
            ?.let { Success(it) } ?: Error("Game non trovato")
}
