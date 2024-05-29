package it.rattly.trentuno.db.tables

import dev.kord.common.entity.Snowflake
import it.rattly.trentuno.db.SnowflakeSqlType
import org.ktorm.entity.Entity
import org.ktorm.ksp.annotation.Column
import org.ktorm.ksp.annotation.PrimaryKey
import org.ktorm.ksp.annotation.Table

@Table("players")
interface PlayerDB : Entity<PlayerDB> {
    @PrimaryKey
    @Column(sqlType = SnowflakeSqlType::class)
    var id: Snowflake
}