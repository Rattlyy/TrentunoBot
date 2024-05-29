package it.rattly.trentuno.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.entity.Snowflake
import it.rattly.trentuno.dotenv
import me.jakejmattson.discordkt.annotations.Service
import org.flywaydb.core.Flyway
import org.ktorm.database.Database
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.SqlType
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

val database get() = backingDatabase
private lateinit var backingDatabase: Database

@Service
class DatabaseService {
    init {
        val dev = dotenv["DEV"].toBoolean()
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            if (dev) {
                println("Starting testcontainer...")
                val db = PostgreSQLContainer("postgres:15-alpine")
                    .withReuse(true).also { it.start() }

                jdbcUrl = db.jdbcUrl
                username = db.username
                password = db.password

                println("Testcontainer started!")
            } else {
                jdbcUrl = dotenv["JDBC_URL"] ?: throw Exception("Missing database url")
                username = dotenv["DB_USER"] ?: throw Exception("Missing database username")
                password = dotenv["DB_PASS"] ?: throw Exception("Missing database password")
            }

            maximumPoolSize = 10
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        println("Migrating database...")

        val dataSource = HikariDataSource(config)
        val result = Flyway
            .configure()
            .locations("classpath:flyway/")
            .dataSource(dataSource)
            .load().migrate()

        println("Database migrated!")

        if (!result.success)
            throw IllegalStateException(
                "Database migration failed: ${result.migrations.joinToString { it.description }}",
                result.exceptionObject
            )

        backingDatabase =
            Database.connect(dataSource, logger = ConsoleLogger(if (dev) LogLevel.TRACE else LogLevel.INFO))
    }
}

object SnowflakeSqlType : SqlType<Snowflake>(Types.BIGINT, typeName = "bigint") {
    override fun doGetResult(rs: ResultSet, index: Int) = Snowflake(rs.getLong(index))

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Snowflake) {
        ps.setLong(index, parameter.value.toLong())
    }
}