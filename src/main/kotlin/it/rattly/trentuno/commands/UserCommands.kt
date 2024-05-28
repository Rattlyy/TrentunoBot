package it.rattly.trentuno.commands

import it.rattly.trentuno.db.tables.GameDB
import it.rattly.trentuno.db.tables.GameTable
import it.rattly.trentuno.mention
import it.rattly.trentuno.services.GameType
import me.jakejmattson.discordkt.commands.subcommand
import org.jetbrains.exposed.sql.transactions.transaction

fun userCommands() = subcommand("user") {
    sub("top") {
        execute {
            val topPlayers = transaction {
                GameDB.find { GameTable.gameType eq GameType.TRENTUNO }
                    .groupBy { it.gameWinner }
                    .map { it.key to it.value.count() }
                    .sortedByDescending { it.second }
                    .take(10).toMap()
            }

            respondPublic {
                title = "TOP PLAYERS"
                description = topPlayers.map { "- **${it.value}** • ${it.key.id.value.mention}" }.joinToString("\n")
            }
        }
    }
}