package it.rattly.trentuno.db.tables

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

class PlayerDB(id: EntityID<ULong>) : Entity<ULong>(id) {
    companion object : EntityClass<ULong, PlayerDB>(PlayerTable)
}

object PlayerTable : IdTable<ULong>() {
    override var id: Column<EntityID<ULong>> = ulong("discord_id").entityId()
    override val primaryKey = PrimaryKey(id)
}