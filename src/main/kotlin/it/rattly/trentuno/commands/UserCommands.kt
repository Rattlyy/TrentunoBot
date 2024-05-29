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
    sub("top", description = "Show the top players of the current game") {
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
                            chunk.joinToString("\n") { (name, place) -> "- ${name.mention} • **$place** wins" }
                    }
                }
            }
        }
    }

    sub(
        name = "reset",
        description = "Reset an user's wins in this server",
        requiredPermissions = Permissions(Permission.Administrator)
    ) {
        execute(UserArg) {
            val wins = database.gameDBs.removeIf { (it.gameWinner eq args.first.id) and (it.serverId eq guild.id) }

            respond {
                description = "Deleted **$wins** wins from ${args.first.mention}!"
            }
        }
    }
}