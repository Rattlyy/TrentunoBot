package it.rattly.trentuno.db.tables

import it.rattly.trentuno.services.GameType
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class GameDB(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GameDB>(GameTable)

    var channelId by GameTable.channelId
    var gameType by GameTable.gameType
    var gameWinner by PlayerDB referencedOn GameTable.winner
    val players by GamePlayer referrersOn GamePlayerTable.gameId
}

object GameTable : IntIdTable() {
    val channelId = ulong("channel_id")
    val gameType = enumeration<GameType>("game_type")
    val winner = reference("winner", PlayerTable)
}

class GamePlayer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GamePlayer>(GamePlayerTable)

    var game by GameDB referencedOn GamePlayerTable.gameId
    var player by PlayerDB referencedOn GamePlayerTable.playerId
}

object GamePlayerTable : IntIdTable() {
    val gameId = reference("game_id", GameTable)
    val playerId = reference("player_id", PlayerTable)
}