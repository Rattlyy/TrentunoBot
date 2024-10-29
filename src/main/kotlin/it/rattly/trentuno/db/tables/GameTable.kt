package it.rattly.trentuno.db.tables

import dev.kord.common.entity.Snowflake
import it.rattly.trentuno.db.SnowflakeSqlType
import it.rattly.trentuno.db.database
import it.rattly.trentuno.services.GameType
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.find
import org.ktorm.ksp.annotation.Column
import org.ktorm.ksp.annotation.PrimaryKey
import org.ktorm.ksp.annotation.References
import org.ktorm.ksp.annotation.Table

@Table("games")
interface GameDB : Entity<GameDB> {
    @PrimaryKey
    var id: Int
    @Column(sqlType = SnowflakeSqlType::class)
    var serverId: Snowflake
    @Column(sqlType = SnowflakeSqlType::class)
    var channelId: Snowflake
    var gameType: GameType
    @Column(sqlType = SnowflakeSqlType::class)
    var gameWinner: Snowflake?

    val players get() = database.gamePlayerses.find { it.gameId eq this.id }
}

@Table("game_players")
interface GamePlayers : Entity<GamePlayers> {
    @References @PrimaryKey
    var game: GameDB
    @References @PrimaryKey
    var player: PlayerDB
}