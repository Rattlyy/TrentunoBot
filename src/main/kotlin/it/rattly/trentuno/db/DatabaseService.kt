package it.rattly.trentuno.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import it.rattly.trentuno.db.tables.GamePlayerTable
import it.rattly.trentuno.db.tables.GameTable
import it.rattly.trentuno.db.tables.PlayerTable
import it.rattly.trentuno.dotenv
import me.jakejmattson.discordkt.annotations.Service
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@Service
class DatabaseService {
    init {
        val config = HikariConfig().apply {

            if (dotenv["DEV"] == "true") {
                driverClassName = "org.h2.Driver"
                jdbcUrl = "jdbc:h2:mem:test"
                username = "sa"
                password = ""
            } else {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = dotenv["JDBC_URL"] ?: throw Exception("Missing database url")
                username = dotenv["DB_USER"] ?: throw Exception("Missing database username")
                password = dotenv["DB_PASS"] ?: throw Exception("Missing database password")
            }

            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        Database.connect(HikariDataSource(config))
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                GameTable,
                GamePlayerTable,
                PlayerTable
            )
        }

        println("Database connected!")
    }
}