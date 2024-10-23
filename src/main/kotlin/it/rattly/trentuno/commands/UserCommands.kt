package it.rattly.trentuno.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import it.rattly.trentuno.db.database
import it.rattly.trentuno.db.tables.gameDBs
import it.rattly.trentuno.mention
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.subcommand
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.groupBy
import org.ktorm.entity.removeIf

fun userCommands() = subcommand("user") {
    sub("top", description = "Mostra la classifica di questo server") {
        execute(GameArg.instance) {
            val topPlayers = database.gameDBs
                .filter { (it.gameType eq args.first) and (it.serverId eq guild.id) }
                .groupBy { it.gameWinner }
                .map { it.key to it.value.count() }
                .sortedByDescending { it.second }

            respondMenu {
                for (chunk in topPlayers.chunked(10)) {
                    page {
                        title = "TOP PLAYERS"
                        description =
                            chunk.joinToString("\n") { (name, place) -> "- ${name.mention} • **$place** vittorie" }
                    }
                }
            }
        }
    }

    sub(
        name = "reset",
        description = "Resetta le vittorie di un player",
        requiredPermissions = Permissions(Permission.Administrator)
    ) {
        execute(UserArg) {
            val wins = database.gameDBs.removeIf { (it.gameWinner eq args.first.id) and (it.serverId eq guild.id) }

            respond {
                description = "Rimosse **$wins** vittorie da ${args.first.mention}!"
            }
        }
    }
}