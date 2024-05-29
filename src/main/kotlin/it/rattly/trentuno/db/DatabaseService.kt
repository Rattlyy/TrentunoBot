package it.rattly.trentuno.db

import ch.qos.logback.classic.Level
import com.impossibl.postgres.jdbc.PGDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.entity.Snowflake
import it.rattly.trentuno.dotenv
import me.jakejmattson.discordkt.annotations.Service
import org.flywaydb.core.Flyway
import org.ktorm.database.Database
import org.ktorm.logging.Slf4jLoggerAdapter
import org.ktorm.schema.SqlType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

val database get() = backingDatabase
private lateinit var backingDatabase: Database

@Service
class DatabaseService {
    private val logger: Logger = LoggerFactory.getLogger(DatabaseService::class.java)

    init {
        val dev = dotenv["DEV"].toBoolean()
        val config = HikariConfig().apply {
            maximumPoolSize = 10
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            dataSource = PGDataSource().apply {
                if (dev) {
                    logger.warn("Starting testcontainer...")

                    val db = PostgreSQLContainer("postgres:15-alpine")
                        .withReuse(true).also { it.start() }

                    databaseUrl = db.jdbcUrl.replace("postgresql", "pgsql")
                    user = db.username
                    password = db.password

                    logger.warn("Testcontainer started!")
                } else {
                    databaseUrl = dotenv["JDBC_URL"] ?: throw Exception("Missing database url")
                    user = dotenv["DB_USER"] ?: throw Exception("Missing database username")
                    password = dotenv["DB_PASS"] ?: throw Exception("Missing database password")
                }
            }
        }

        logger.warn("Migrating database...")

        val dataSource = HikariDataSource(config)
        val result = Flyway
            .configure()
            .loggers("slf4j")
            .locations("classpath:flyway/")
            .dataSource(dataSource)
            .load().migrate()

        logger.warn("Database migrated!")

        if (!result.success)
            throw IllegalStateException(
                "Database migration failed: ${result.migrations.joinToString { it.description }}",
                result.exceptionObject
            )

        backingDatabase = Database.connect(dataSource, logger = Slf4jLoggerAdapter("Ktorm"))
        if (dev) (LoggerFactory.getLogger("Ktorm") as ch.qos.logback.classic.Logger).level = Level.TRACE
    }
}

object SnowflakeSqlType : SqlType<Snowflake>(Types.BIGINT, typeName = "bigint") {
    override fun doGetResult(rs: ResultSet, index: Int) = Snowflake(rs.getLong(index))

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Snowflake) {
        ps.setLong(index, parameter.value.toLong())
    }
}